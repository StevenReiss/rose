/********************************************************************************/
/*                                                                              */
/*              SepalClassNullCondition.java                                    */
/*                                                                              */
/*      Add conditionals to skip access to null values                          */
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

public class SepalClassNullCondition extends RootRepairFinderDefault
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

public SepalClassNullCondition()
{ }



/********************************************************************************/
/*                                                                              */
/*      Basic processing methods                                                */
/*                                                                              */
/********************************************************************************/

@Override protected double getFinderPriority()
{
   return 0.5;
}



@Override public void process()
{
   RootProblem rp = getProblem();
   if (rp.getProblemType() != RoseProblemType.EXCEPTION ||
         !rp.getProblemDetail().equals("java.lang.NullPointerException")) {
      if (rp.getProblemType() != RoseProblemType.LOCATION)
         return;
    }
   // might also want to handle location errors where there is an exception node
   
   Statement stmt = (Statement) getResolvedStatementForLocation(null);
   RootLocation ploc = rp.getBugLocation();
   ASTNode bstmt = getResolvedStatementForLocation(ploc);
   if (stmt != bstmt) {
      // if the erroreous value is a variable computed here, then can add check afterwards
      // If this is a call passing in the erroreous value, skip the call
      return;
    }   
   
   ASTNode n = getProcessor().getController().getExceptionNode(rp);
   if (n == null) return;
   Expression base = (Expression) n;
   ASTNode blk = bstmt.getParent();
   CondScan cond = new CondScan(stmt,base);
   blk.accept(cond);
   Statement endstmt = cond.getEndStatement();
   
   ASTRewrite rw1 = skipStatements(base,stmt,endstmt);
   if (rw1 != null) addRepair(rw1,"Avoid using null value",0.75);
   ASTRewrite rw2 = loopContine(base,stmt);
   if (rw2 != null) addRepair(rw2,"Continue on null value",0.5);
   ASTRewrite rw3 = condReturn(base,stmt);
   if (rw3 != null) addRepair(rw3,"Return on null value",0.5);
   ASTRewrite rw4 = ifcondRepair(base,stmt);
   if (rw4 != null) addRepair(rw4,"Add null check in condition",0.7);
}



/********************************************************************************/
/*                                                                              */
/*      Generate a rewrite for start->end                                       */
/*                                                                              */
/********************************************************************************/

private ASTRewrite skipStatements(Expression base,Statement start,Statement end)
{
   ASTNode par = start.getParent();
   if (par instanceof Block) {
      return skipBlock(base,start,end);
    }
   else if (par instanceof IfStatement) {
      IfStatement s = (IfStatement) par;
      if (start.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) return null;
      return addToConditional(base,s.getExpression(),null);
    }
   else if (par instanceof WhileStatement) {
      WhileStatement s = (WhileStatement) par;
      return addToConditional(base,s.getExpression(),null);
    }
   else if (par instanceof ForStatement) {
      ForStatement f = (ForStatement) par;
      if (start.getLocationInParent() == ForStatement.BODY_PROPERTY) {
         if (f.getExpression() != null) {
            return addToConditional(base,f.getExpression(),null);
          }
         else {
            return createForCondition(base,f);
          }
       }
    }
   
   return null;
}



@SuppressWarnings("unchecked")
private ASTRewrite skipBlock(Expression base,Statement start,Statement end)
{
   AST ast = start.getAST();
   
   IfStatement ifs = ast.newIfStatement();
   ifs.setExpression(getCheckExpr(ast,base,true));
   Block nblk = ast.newBlock();
   ifs.setThenStatement(nblk);
   
   Block oblk = (Block) start.getParent();
   ASTRewrite rw = ASTRewrite.create(ast);
   ListRewrite lrw = rw.getListRewrite(oblk,Block.STATEMENTS_PROPERTY);
   
   boolean inblk = false;
   for (Object o1 : oblk.statements()) {
      Statement s1 = (Statement) o1;
      if (s1 == start) {
         inblk = true;
         lrw.insertBefore(ifs,s1,null);
       }
      if (inblk) {
         lrw.remove(s1,null);
         nblk.statements().add(ASTNode.copySubtree(ast,s1));
       }
      if (s1 == end) inblk = false;
    }
   
   return rw;
}


private ASTRewrite addToConditional(Expression base,Expression cond,Expression before)
{
   AST ast = cond.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   Expression nbase = getCheckExpr(ast,base,true);
   
   if (cond instanceof InfixExpression) {
      InfixExpression inf = (InfixExpression) cond;
      if (inf.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
         ListRewrite lrw = rw.getListRewrite(cond,InfixExpression.EXTENDED_OPERANDS_PROPERTY);
         if (before == null) lrw.insertLast(nbase,null);
         else lrw.insertBefore(nbase,before,null);
         return rw;
       }
    }
   
   InfixExpression inf = ast.newInfixExpression();
   inf.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
   inf.setLeftOperand(nbase);
   inf.setRightOperand((Expression) ASTNode.copySubtree(ast,cond));
   
   rw.replace(cond,inf,null);
   
   return rw;
}



private ASTRewrite ifcondRepair(Expression base,Statement s)
{
   if (s.getNodeType() != ASTNode.IF_STATEMENT) return null;
   
   ASTNode cond = null;
   ASTNode elt = null;
   for (ASTNode n = base; n != null; n = n.getParent()) {
      if (n == s) break;
      elt = cond;
      cond = n;
    }
   if (cond.getLocationInParent() != IfStatement.EXPRESSION_PROPERTY) return null;
   
   return addToConditional(base,(Expression) cond,(Expression) elt);
}



private ASTRewrite createForCondition(Expression base,Statement f)
{
   AST ast = f.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   Expression nbase = getCheckExpr(ast,base,true);
   
   rw.set(f,ForStatement.EXPRESSION_PROPERTY,nbase,null);
   
   return rw;
}




private Expression getCheckExpr(AST ast,Expression base,boolean neq)
{
   Expression e1 = (Expression) ASTNode.copySubtree(ast,base);
   
   InfixExpression inf = ast.newInfixExpression();
   inf.setLeftOperand(e1);
   inf.setRightOperand(ast.newNullLiteral());
   if (neq) inf.setOperator(InfixExpression.Operator.NOT_EQUALS);
   else inf.setOperator(InfixExpression.Operator.EQUALS);
   
   return inf;
   
}


/********************************************************************************/
/*                                                                              */
/*      Generate a continue for null value in a loop                            */
/*                                                                              */
/********************************************************************************/

private ASTRewrite loopContine(Expression base,Statement start)
{
   Boolean inloop = null;
   Block blk = null;
   ASTNode prev = start;
   for (ASTNode p = start.getParent(); p != null; p = p.getParent()) {
      switch (p.getNodeType()) {
         case ASTNode.METHOD_DECLARATION :
         case ASTNode.FIELD_DECLARATION :
         case ASTNode.INITIALIZER :
         case ASTNode.TYPE_DECLARATION :
            inloop = false;
            break;
         case ASTNode.FOR_STATEMENT :
         case ASTNode.WHILE_STATEMENT :
         case ASTNode.DO_STATEMENT :
         case ASTNode.ENHANCED_FOR_STATEMENT :
             inloop = true;
             break;
         case ASTNode.BLOCK :
            if (blk == null) blk = (Block) p;
            break;
       }
      if (inloop != null) break;
      if (blk == null) prev = p;
    }
   if (inloop == null || inloop == Boolean.FALSE || blk == null) return null;
   
   AST ast = start.getAST();
   ASTRewrite rw = ASTRewrite.create(ast);
   IfStatement s = ast.newIfStatement();
   s.setExpression(getCheckExpr(ast,base,false));
   s.setThenStatement(ast.newContinueStatement());
   ListRewrite lrw = rw.getListRewrite(blk,Block.STATEMENTS_PROPERTY);
   lrw.insertBefore(s,prev,null);
   
   return rw;
}



/********************************************************************************/
/*                                                                              */
/*      Generate a return for null value                                        */
/*                                                                              */
/********************************************************************************/

private ASTRewrite condReturn(Expression base,Statement start)
{
   JcompSymbol mthd = null;
   Block blk = null;
   ASTNode prev = start;
   for (ASTNode p = start.getParent(); p != null; p = p.getParent()) {
      boolean done = false;
      switch (p.getNodeType()) {
         case ASTNode.METHOD_DECLARATION :
            mthd = JcompAst.getDefinition(p);
            done = true;
            break;
         case ASTNode.FIELD_DECLARATION :
         case ASTNode.INITIALIZER :
         case ASTNode.TYPE_DECLARATION :
            done = true;
            break;
         case ASTNode.BLOCK :
            if (blk == null) blk = (Block) p;
            break;
       }
      if (done) break;
      if (blk == null) prev = p;
    }
   if  (mthd == null || blk == null) return null;
   
   AST ast = start.getAST();
   
   JcompType typ = mthd.getType();
   Expression rslt = null;
   if (typ.isBooleanType()) rslt = ast.newBooleanLiteral(false);
   else if (typ.isNumericType()) rslt = ast.newNumberLiteral("0");
   else if (typ.isVoidType()) rslt = null;
   else rslt = ast.newNullLiteral();
   ReturnStatement ret = ast.newReturnStatement();
   if (rslt != null) ret.setExpression(rslt);
   
   ASTRewrite rw = ASTRewrite.create(ast);
   IfStatement s = ast.newIfStatement();
   s.setExpression(getCheckExpr(ast,base,false));
   s.setThenStatement(ret);
   ListRewrite lrw = rw.getListRewrite(blk,Block.STATEMENTS_PROPERTY);
   lrw.insertBefore(s,prev,null);
   
   return rw;
}


/********************************************************************************/
/*                                                                              */
/*      Scan to determine extent of conditional                                 */
/*                                                                              */
/********************************************************************************/

private class CondScan extends ASTVisitor {
   
   private Set<JcompSymbol> defined_symbols;
   private String null_expr;
   private Statement end_statement;
   private int nest_level;
   private Statement start_statement;
   private Set<JcompSymbol> add_symbols;
   private boolean is_required;
   
   CondScan(Statement start,Expression exp) {
      defined_symbols = new HashSet<>();
      add_symbols = new HashSet<>();
      if (start instanceof VariableDeclarationStatement) {
         VariableDeclarationStatement vds = (VariableDeclarationStatement) start;
         for (Object o1 : vds.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
            JcompSymbol js = JcompAst.getDefinition(vdf);
            defined_symbols.add(js);
          }
       }
      null_expr = exp.toString();
      nest_level = 0;
      start_statement = start;
      end_statement = null;
      is_required = false;
    }
   
   Statement getEndStatement()                  { return end_statement; }
   
   @Override public void preVisit(ASTNode n) {
      if (n instanceof Block) {
         ++nest_level;
       }
      if (n == start_statement) {
         end_statement = (Statement) n;
       }
    }
   
   @Override public void postVisit(ASTNode n) {
      if (n instanceof Block) {
         --nest_level;
       }
      else if (n instanceof Statement && nest_level == 0 && end_statement != null) {
         if (is_required) {
            end_statement = (Statement) n;
            defined_symbols.addAll(add_symbols);
            add_symbols.clear();
            is_required = false;
          }
       }
      else if (n instanceof Expression && end_statement != null) {
         JcompSymbol js = JcompAst.getReference(n);
         if (js != null && defined_symbols.contains(js)) is_required = true;
         checkExpression((Expression) n);
       }
    }
   
   @Override public boolean visit(Block b) {
      ++nest_level;
      return true;
    }
   
   @Override public void endVisit(Block b) {
      --nest_level;
    }
   
   @Override public boolean visit(VariableDeclarationFragment vdf) {
      if (end_statement != null && nest_level == 0) {
         JcompSymbol js = JcompAst.getDefinition(vdf);
         add_symbols.add(js);
       }
      return true;
    }
   
   private void checkExpression(Expression n) {
      String etxt = n.toString();
      if (etxt.equals(null_expr)) {
         StructuralPropertyDescriptor spd = n.getLocationInParent();
         if (spd == FieldAccess.EXPRESSION_PROPERTY ||
               spd == QualifiedName.QUALIFIER_PROPERTY ||
               spd == MethodInvocation.EXPRESSION_PROPERTY ||
               spd == ArrayAccess.ARRAY_PROPERTY ||
               spd == ClassInstanceCreation.EXPRESSION_PROPERTY)
            is_required = true;
       }
    }
   
}       // end of inner class CondScan



}       // end of class SepalClassNullCondition




/* end of SepalClassNullCondition.java */

