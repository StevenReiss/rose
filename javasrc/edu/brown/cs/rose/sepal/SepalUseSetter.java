/********************************************************************************/
/*                                                                              */
/*              SepalUseSetter.java                                             */
/*                                                                              */
/*      Try using non-trivial setter if available                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.rose.sepal;

import java.util.List;


import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

public class SepalUseSetter extends RootRepairFinderDefault
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalUseSetter()
{ }



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public double getFinderPriority()
{
   return 0.5;
}




@SuppressWarnings("unchecked")
@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   if (!(stmt instanceof ExpressionStatement)) return;
   ExpressionStatement exprstmt = (ExpressionStatement) stmt;
   Expression expr = exprstmt.getExpression();
   if (!(expr instanceof Assignment)) return;
   Assignment asgn = (Assignment) expr;
   if (asgn.getOperator() != Assignment.Operator.ASSIGN) return;
   
   ASTNode mthd = null;
   for (mthd = stmt; mthd != null; mthd = mthd.getParent()) {
      if (mthd instanceof MethodDeclaration) break;
    }
   if (mthd == null) return;
   MethodDeclaration md = (MethodDeclaration) mthd;
   if (!md.isConstructor()) return;
   
   String nm = null;
   Expression lhs = asgn.getLeftHandSide();
   if (lhs instanceof FieldAccess) {
      FieldAccess fldacc = (FieldAccess) lhs;
      if (!(fldacc.getExpression() instanceof ThisExpression)) return;
      nm = fldacc.getName().getIdentifier();
    }
   else if (lhs instanceof SimpleName) {
      SimpleName sn = (SimpleName) lhs;
      JcompSymbol ref = JcompAst.getReference(sn);
      if (ref.isFieldSymbol()) nm = sn.getIdentifier();
    }
   if (nm == null) return;
   
   JcompSymbol csym = JcompAst.getDefinition(mthd);
   JcompType ctyp = csym.getClassType();
   List<JcompSymbol> mthds = ctyp.getDefinedMethods(JcompAst.getTyper(stmt));
   for (JcompSymbol m : mthds) {
      if (m.getName().equalsIgnoreCase("set"+nm)) {
         if (isGoodSetter(m,nm)) {
            AST ast = stmt.getAST();
            MethodInvocation mi = ast.newMethodInvocation();
            SimpleName mnm = JcompAst.getSimpleName(ast,m.getName());
            mi.setName(mnm);
            Expression arg = (Expression) ASTNode.copySubtree(ast,asgn.getRightHandSide());
            mi.arguments().add(arg);
            ASTRewrite rw = ASTRewrite.create(ast);
            rw.replace(expr,mi,null);
            addRepair(rw,"Use " + m.getName() + " rather than assignment",null,0.5);
          }
       }
    }
}



private boolean isGoodSetter(JcompSymbol m,String nm)
{
   if (m.isStatic()) return false;
   JcompType mtyp = m.getType();
   if (mtyp == null) return false;
   if (mtyp.getComponents().size() != 1) return false;
   
   ASTNode n = m.getDefinitionNode();
   if (n == null) return false;
   if (!(n instanceof MethodDeclaration)) return false;
   MethodDeclaration md = (MethodDeclaration) n;
   Block blk = md.getBody();
   if (blk == null) return false;
   if (blk.statements().size() > 2) {
      SetterChecker sc = new SetterChecker(nm);
      blk.accept(sc);
      if (!sc.isSetter()) return false;
    }
   
   return true;
}



private static class SetterChecker extends ASTVisitor {

   private String field_name;
   private boolean found_set;
   
   SetterChecker(String fld) {
      field_name = fld;
      found_set = false;
    }
   
   boolean isSetter()                   { return found_set; }
   
   @Override public void endVisit(Assignment n) {
      if (n.getOperator() != Assignment.Operator.ASSIGN) return;
      Expression lhs = n.getLeftHandSide();
      if (lhs instanceof FieldAccess) {
         FieldAccess fldacc = (FieldAccess) lhs;
         if (!(fldacc.getExpression() instanceof ThisExpression)) return;
         if (fldacc.getName().getIdentifier().equals(field_name)) found_set = true;
       }
      else if (lhs instanceof SimpleName) {
         SimpleName sn = (SimpleName) lhs;
         if (sn.getIdentifier().equals(field_name)) found_set = true;
       }
    }
   
}       // end of inner class SetterChecker


}       // end of class SepalUseSetter




/* end of SepalUseSetter.java */

