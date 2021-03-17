/********************************************************************************/
/*                                                                              */
/*              SepalValueFixer.java                                            */
/*                                                                              */
/*      Change values based on user differences                                 */
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootRepairFinderDefault;
import edu.brown.cs.rose.root.RootControl.AssertionData;

public class SepalValueFixer extends RootRepairFinderDefault
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

public SepalValueFixer()
{ }


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override protected double getFinderPriority()
{
   return 0.5;
}



@Override public void process()
{
   RootProblem rp = getProblem();
   String nval = null;
   String oval = null;
   String var = rp.getProblemDetail();
   
   if (rp.getProblemType() == RoseProblemType.VARIABLE) {
      nval = rp.getTargetValue();
      oval = rp.getOriginalValue();
    }
   else if (rp.getProblemType() == RoseProblemType.ASSERTION) {
      AssertionData ad = getProcessor().getController().getAssertionData(rp);
      ASTNode an = ad.getExpression();
      if (an instanceof Name) {
         var = an.toString();
         nval = ad.getTargetValue();
         oval = ad.getOriginalValue();
       }
    }
   if (nval == null) return;
   
   Assignment assign = null;
   Statement stmt = (Statement) getResolvedStatementForLocation(null);
   if (stmt instanceof ExpressionStatement) {
      ExpressionStatement estmt = (ExpressionStatement) stmt;
      if (estmt.getExpression() instanceof Assignment) {
         Assignment asgn = (Assignment) estmt.getExpression();
         if (asgn.getLeftHandSide() instanceof SimpleName) {
            SimpleName sn = (SimpleName) asgn.getLeftHandSide();
            if (sn.getIdentifier().equals(var)) {
               assign = asgn;
             }
          }
       }
    }
   if (assign == null) return;
   
   List<ValueFix> rslt = computeDifference(stmt.getAST(),oval,nval);
   if (rslt == null || rslt.isEmpty()) return;
   
   for (ValueFix vf : rslt) {
      vf.addValueRepair(stmt,assign);
    }
  
}



/********************************************************************************/
/*                                                                              */
/*      Find difference between values if possible                              */
/*                                                                              */
/********************************************************************************/

private List<ValueFix> computeDifference(AST ast,String oval,String nval)
{
   List<ValueFix> rslt = new ArrayList<>();
   
   int idx = oval.indexOf(" ");
   if (idx < 0) return rslt;
   String typ = oval.substring(0,idx);
   oval = oval.substring(idx+1);
   if (typ.equals("java.lang.String")) {
      computeStringDifference(ast,rslt,oval,nval);
    }
   else if (typ.equals("int") || typ.equals("long") || typ.equals("short") ||
         typ.equals("byte")) {
      computeNumericDifferece(ast,rslt,typ,oval,nval);
    }
   else if (typ.equals("float") || typ.equals("double")) {
      computeFloatDifferece(ast,rslt,typ,oval,nval);
    }
   
   return rslt;
}



private void computeStringDifference(AST ast,List<ValueFix> rslt,String s1,String s2)
{
   if (s1.equals(s2)) return;
   else if (s1.equals(s2.toLowerCase())) {
      rslt.add(new ValueFixMethod("toLowerCase"));
      // make lower case
    }
   else if (s1.equals(s2.toUpperCase())) {
      rslt.add(new ValueFixMethod("toUpperCase"));
    }
   else if (s2.startsWith(s1)) {
      String delta = s2.substring(s1.length());
      StringLiteral slit = ast.newStringLiteral();
      slit.setLiteralValue(delta);
      rslt.add(new ValueFixLeftOp(InfixExpression.Operator.PLUS,slit));
    }
   else if (s1.startsWith(s2)) {
      NumberLiteral l1 = ast.newNumberLiteral("0");
      NumberLiteral l2 = ast.newNumberLiteral(Integer.toString(s2.length()));
      rslt.add(new ValueFixMethod("substring",l1,l2));
      // might want to use length of s1 here
    }
   return;
}



private void computeNumericDifferece(AST ast,List<ValueFix> rslt,String typ,String s1,String s2)
{
   long l1,l2;
   try {
      l1 = Long.parseLong(s1);
      l2 = Long.parseLong(s2);
    }
   catch (NumberFormatException e) {
      return;
    }
   
   if (l1 == l2) return;
   if (Math.abs(l1-l2) <= 2) {
      NumberLiteral delta = ast.newNumberLiteral(Long.toString(Math.abs(l1-l2)));
      if (l1 > l2) {
         rslt.add(new ValueFixOp(InfixExpression.Operator.MINUS,delta));
       }
      else {
         rslt.add(new ValueFixOp(InfixExpression.Operator.PLUS,delta));
       }
    }
   return;
}



private void computeFloatDifferece(AST ast,List<ValueFix> rslt,String typ,String s1,String s2)
{
   double l1,l2;
   try {
      l1 = Double.parseDouble(s1);
      l2 = Double.parseDouble(s2);
    }
   catch (NumberFormatException e) {
      return;
    }
   
   if (l1 == l2) return;
   if (l1 == Math.round(l1) && l2 == Math.round(l2) && Math.abs(l1-l2) <= 2) {
      NumberLiteral delta = ast.newNumberLiteral(Double.toString(Math.abs(l1-l2)));
      if (l1 > l2) {
         rslt.add(new ValueFixOp(InfixExpression.Operator.MINUS,delta));
       }
      else {
         rslt.add(new ValueFixOp(InfixExpression.Operator.PLUS,delta));
       }
    }
   else {
      if (l2 == Math.round(l1)) {
         rslt.add(new ValueFixStaticMethod("Math.round"));
       }
      if (l2 == Math.floor(l1)) {
         rslt.add(new ValueFixStaticMethod("Math.floor"));
       }
      else if (l2 == Math.ceil(l1)) {
         rslt.add(new ValueFixStaticMethod("Math.ceil"));
       }
    }
   return;
}



/********************************************************************************/
/*                                                                              */
/*      Representation of a potential fix                                       */
/*                                                                              */
/********************************************************************************/

private abstract class ValueFix {

   ValueFix() { }
   
   abstract void addValueRepair(Statement stmt,Assignment asgn);
   
   @SuppressWarnings("unchecked")
   protected Assignment modifyAssign(Assignment asgn,InfixExpression.Operator op,Expression val,boolean left) {
      if (asgn == null) return null;
      if (asgn.getOperator() != Assignment.Operator.ASSIGN) {
         if (left) return null;
         if (asgn.getOperator() == Assignment.Operator.PLUS_ASSIGN &&
               op == InfixExpression.Operator.PLUS) ;
         else if (asgn.getOperator() == Assignment.Operator.MINUS_ASSIGN &&
               op == InfixExpression.Operator.MINUS) {
            op = InfixExpression.Operator.PLUS;
          }
         else return null;
       }
      
      AST ast = val.getAST();
      asgn = (Assignment) ASTNode.copySubtree(ast,asgn);
      if (!left && asgn.getRightHandSide() instanceof InfixExpression) {
         InfixExpression inf = (InfixExpression) asgn.getRightHandSide();
         if (inf.getOperator() == op) {
            inf.extendedOperands().add(val);
            return asgn;
          }
       }
      InfixExpression ex = ast.newInfixExpression();
      ex.setLeftOperand(asgn.getRightHandSide()); 
      ex.setRightOperand(val);
      ex.setOperator(op);
      asgn.setRightHandSide(ex);
      
      return asgn;
    }
   
   protected Assignment modifyAssign(Assignment asgn,MethodInvocation mthd) {
      if (asgn == null) return null;
      if (asgn.getOperator() != Assignment.Operator.ASSIGN) return null;
      
      AST ast = mthd.getAST();
      asgn = (Assignment) ASTNode.copySubtree(ast,asgn);
      asgn.setRightHandSide(mthd);
      
      return asgn;
    }
   
   
   protected void addToBlock(Statement stmt,Assignment asgn,InfixExpression.Operator op,Expression val,boolean left) {
      AST ast = val.getAST();
      Expression lhs = (Expression) ASTNode.copySubtree(ast,asgn.getLeftHandSide());
      Expression lhs1 = (Expression) ASTNode.copySubtree(ast,asgn.getLeftHandSide());
      Assignment asg2 = ast.newAssignment();
      asg2.setLeftHandSide(lhs);
      asg2.setOperator(Assignment.Operator.ASSIGN);
      InfixExpression rhs = ast.newInfixExpression();
      Expression rhsv = rhs;
      rhs.setOperator(op);
      if (left) {
         rhs.setLeftOperand(val);
         rhs.setRightOperand(lhs1);
       }
      else if (op == InfixExpression.Operator.PLUS) {
         asg2.setOperator(Assignment.Operator.PLUS_ASSIGN);
         rhsv = val;
       }
      else if (op == InfixExpression.Operator.MINUS) {
         asg2.setOperator(Assignment.Operator.MINUS_ASSIGN);
         rhsv = val;
       }
      else {
         rhs.setLeftOperand(lhs1);
         rhs.setRightOperand(val);
       }
      asg2.setRightHandSide(rhsv);
      addToBlock(stmt,asg2);
    }
   
   protected void addToBlock(Statement stmt,Expression expr) {
      ExpressionStatement es = expr.getAST().newExpressionStatement(expr);
      addToBlock(stmt,es);
    }
   
   @SuppressWarnings("unchecked")
   protected void addToBlock(Statement stmt,Statement newstmt) {
      AST ast = newstmt.getAST();
      ASTNode par = stmt.getParent();
      ASTRewrite rw = ASTRewrite.create(ast);
      if (par instanceof Block) {
         Block blk = (Block) par;
         ListRewrite lrw = rw.getListRewrite(blk,Block.STATEMENTS_PROPERTY);
         lrw.insertAfter(newstmt,stmt,null);
       }
      else {
         Block blk = ast.newBlock();
         ASTNode n1 = ASTNode.copySubtree(ast,stmt);
         blk.statements().add(n1);
         blk.statements().add(newstmt);
         rw.replace(stmt,blk,null);
       }
      if (rw != null) {
         String desc = "Add " + newstmt + " to change computed value";
         addRepair(rw,desc,0.5);
       }
    }
   
}       // end of inner class ValueFix



private class ValueFixOp extends ValueFix {
   
   private InfixExpression.Operator use_operator;
   private Expression rhs_value;
   
   ValueFixOp(InfixExpression.Operator op,Expression rhs) {
      use_operator = op;
      rhs_value = rhs;
    }
  
   void addValueRepair(Statement stmt,Assignment asgn) {
      AST ast = rhs_value.getAST();
      Assignment asg1 = modifyAssign(asgn,use_operator,rhs_value,false);
      if (asg1 != null) {
         ASTRewrite rw = ASTRewrite.create(ast);
         rw.replace(asgn,asg1,null);
         String desc = "Use " + asg1 + " instead of " + asgn;
         addRepair(rw,desc,0.5);
       }
      else {
         addToBlock(stmt,asgn,use_operator,rhs_value,false);
       }
    }
   
}       // end of inner class ValueFixOp



private class ValueFixLeftOp extends ValueFix {

   private InfixExpression.Operator use_operator;
   private Expression rhs_value;
   
   ValueFixLeftOp(InfixExpression.Operator op,Expression rhs) {
      use_operator = op;
      rhs_value = rhs;
    }
   
   void addValueRepair(Statement stmt,Assignment asgn) {
      Assignment asg1 = modifyAssign(asgn,use_operator,rhs_value,true);
      if (asg1 != null) {
         ASTRewrite rw = ASTRewrite.create(rhs_value.getAST());
         rw.replace(asgn,asg1,null);
         String desc = "Use " + asg1 + " instead of " + asgn;
         addRepair(rw,desc,0.5);
       }
      else {
         addToBlock(stmt,asgn,use_operator,rhs_value,true);
       }
    }

}       // end of inner class ValueFixLeftOp



private class ValueFixMethod extends ValueFix {
   
   private String method_name;
   private Expression [] method_args;
   
   ValueFixMethod(String nm,Expression ... args) {
      method_name = nm;
      method_args = args;
    }
   
   @SuppressWarnings("unchecked")
   void addValueRepair(Statement stmt,Assignment asgn) {
      AST ast = stmt.getAST();
      MethodInvocation mi = ast.newMethodInvocation();
      mi.setName(JcompAst.getSimpleName(ast,method_name));
      Expression ex = (Expression) ASTNode.copySubtree(ast,asgn.getRightHandSide());
      mi.setExpression(ex);
      for (Expression e1 : method_args) {
         mi.arguments().add(e1);
       }
      Assignment asg1 = modifyAssign(asgn,mi);
      if (asg1 != null) {
         ASTRewrite rw = ASTRewrite.create(ast);
         rw.replace(asgn,asg1,null);
         String desc = "Use " + asg1 + " instead of " + asgn;
         addRepair(rw,desc,0.5);
       }
      else {
         ex = (Expression) ASTNode.copySubtree(ast,asgn.getLeftHandSide());
         mi.setExpression(ex);
         addToBlock(stmt,mi);
       }
    }
   
}       // end of inner class ValueFixMethod



private class ValueFixStaticMethod extends ValueFix {

   private String method_name;
   
   ValueFixStaticMethod(String nm) {
      method_name = nm;
    }
   
   @SuppressWarnings("unchecked")
   void addValueRepair(Statement stmt,Assignment asgn) {
      AST ast = stmt.getAST();
      MethodInvocation mi = ast.newMethodInvocation();
      int idx = method_name.lastIndexOf(".");
      String mid = method_name.substring(idx+1);
      String mpfx = method_name.substring(0,idx);
      mi.setExpression(JcompAst.getQualifiedName(ast,mpfx));
      mi.setName(JcompAst.getSimpleName(ast,mid));
      Expression ex = (Expression) ASTNode.copySubtree(ast,asgn.getRightHandSide());
      mi.arguments().add(ex);
      Assignment asg1 = modifyAssign(asgn,mi);
      if (asg1 != null) {
         ASTRewrite rw = ASTRewrite.create(ast);
         rw.replace(asgn,asg1,null);
         String desc = "Use " + asg1 + " instead of " + asgn;
         addRepair(rw,desc,0.5);
       }
      else {
         ex = (Expression) ASTNode.copySubtree(ast,asgn.getLeftHandSide());
         mi.arguments().set(0,ex);
         addToBlock(stmt,mi);
       } 
      
    }

}       // end of inner class ValueFixStaticMethod



}       // end of class SepalValueFixer




/* end of SepalValueFixer.java */

