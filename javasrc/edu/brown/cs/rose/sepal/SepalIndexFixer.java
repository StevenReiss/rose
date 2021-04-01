/********************************************************************************/
/*                                                                              */
/*              SepalIndexFixer.java                                            */
/*                                                                              */
/*      Try different index expressoins on for with >1 increments               */
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

import edu.brown.cs.rose.root.RootRepairFinderDefault;

import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.rose.bract.BractAstPattern;
import edu.brown.cs.rose.bract.BractConstants;

public class SepalIndexFixer extends RootRepairFinderDefault implements BractConstants, SepalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final BractAstPattern    for_pattern;
private static final BractAstPattern    index_pattern;

static {
   for_pattern = BractAstPattern.statement(
         "for (int Vi = Ey; Ex; Vi += Id) Sbody;",  
         "for (int Vi = Ey; Ex; Vi -= Id) Sbody;"); 
         
   index_pattern = BractAstPattern.expression(
         "Ea[Vi]", "Ea[Vi+Id1]", "Ea[Vi-Id1]" );
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalIndexFixer()                        { }



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   switch (stmt.getNodeType()) {
      case ASTNode.EXPRESSION_STATEMENT :
      case ASTNode.VARIABLE_DECLARATION_STATEMENT :
         break;
      default :
         return;
    }
   
   Map<ASTNode,PatternMap> rslt = index_pattern.matchAll(stmt,null);
   if (rslt == null || rslt.size() == 0) return;
   
   boolean done = false;
   for (ASTNode n = stmt.getParent(); n != null; n =  n.getParent()) {
      switch (n.getNodeType()) {
         case ASTNode.FOR_STATEMENT :
            checkForStatement((ForStatement) n,rslt);
            break;
         case ASTNode.METHOD_DECLARATION :
         case ASTNode.ANONYMOUS_CLASS_DECLARATION :
         case ASTNode.CLASS_INSTANCE_CREATION :
            done = true;
            break;
       }
      if (done) break;
    }
}



@Override public double getFinderPriority()
{
   return 0.5;
}



/********************************************************************************/
/*                                                                              */
/*      Check if indices are relevant to for statement                          */
/*                                                                              */
/********************************************************************************/

private void checkForStatement(ForStatement fs,Map<ASTNode,PatternMap> rslt)
{
   for (Map.Entry<ASTNode,PatternMap> ent : rslt.entrySet()) {
      ASTNode idxnode = ent.getKey();
      AST ast = idxnode.getAST();
      if (idxnode.getNodeType() != ASTNode.ARRAY_ACCESS) continue;
      ArrayAccess accnode = (ArrayAccess) idxnode;
      PatternMap pmap = new PatternMap(ent.getValue());
      if (for_pattern.match(fs,pmap)) {
         Object o2 = pmap.get("d");             // for increment
         if (o2 == null) continue;
         int incval = ((Integer) o2);
         if (Math.abs(incval) < 2) continue;
         ASTRewrite rw = ASTRewrite.create(ast);
         InfixExpression exp = ast.newInfixExpression();
         exp.setOperator(InfixExpression.Operator.PLUS);
         exp.setLeftOperand((Expression) ASTNode.copySubtree(ast,accnode.getIndex()));
         exp.setRightOperand(ast.newNumberLiteral("1"));
         rw.replace(accnode.getIndex(),exp,null);
         addRepair(rw,"Replace index " + accnode.getIndex() + " with " + accnode.getIndex() + "+1",0.4);
         rw = ASTRewrite.create(ast);
         exp = ast.newInfixExpression();
         exp.setOperator(InfixExpression.Operator.MINUS);
         exp.setLeftOperand((Expression) ASTNode.copySubtree(ast,accnode.getIndex()));
         exp.setRightOperand(ast.newNumberLiteral("1"));
         rw.replace(accnode.getIndex(),exp,null);
         addRepair(rw,"Replace index " + accnode.getIndex() + " with " + accnode.getIndex() + "-1",0.4);
       }
    }
}




}       // end of class SepalIndexFixer




/* end of SepalIndexFixer.java */

