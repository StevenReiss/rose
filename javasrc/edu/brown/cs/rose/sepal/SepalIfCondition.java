/********************************************************************************/
/*                                                                              */
/*              SepalIfCondition.java                                           */
/*                                                                              */
/*      description of class                                                    */
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
import edu.brown.cs.rose.root.RoseLog;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.rose.bract.BractConstants;

public class SepalIfCondition extends RootRepairFinderDefault implements BractConstants
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

public SepalIfCondition() 
{ }



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void process()
{
   ASTNode stmt = getResolvedStatementForLocation(null);
   if (stmt == null) return;
   
   Expression cond = null;
   switch (stmt.getNodeType()) {
      case ASTNode.IF_STATEMENT :
         cond = ((IfStatement) stmt).getExpression();
         break;
      case ASTNode.WHILE_STATEMENT :
         cond = ((WhileStatement) stmt).getExpression();
         break;
      case ASTNode.DO_STATEMENT :
         cond = ((DoStatement) stmt).getExpression();
         break;
      case ASTNode.FOR_STATEMENT :
         cond = ((ForStatement) stmt).getExpression();
         break;
      default :
         break;
    }
   if (cond == null) return;
   RoseLog.logD("SEPAL","Check condition on " +  getLocation().getLineNumber() + " " + stmt);
   
   handleCondition(cond);
}



private void handleCondition(Expression cond) 
{
   if (cond == null) return;
   
   if (cond instanceof InfixExpression) {
      InfixExpression ifx = (InfixExpression) cond;
      if (ifx.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
         handleAndAndCondition(ifx);
       }
      else if (ifx.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
         handleOrOrCondition(ifx);
       }
      else {
         invertCondition(ifx);
       }
    }
   else invertCondition(cond);
}




@Override public double getFinderPriority()
{
   return 0.51;
}



/********************************************************************************/
/*                                                                              */
/*      Generate inverted simple condition                                      */
/*                                                                              */
/********************************************************************************/

private void invertCondition(Expression ex)
{
   Expression comp = null;
   
   switch (ex.getNodeType()) {
      case ASTNode.BOOLEAN_LITERAL :
         comp = invertBooleanLiteral((BooleanLiteral) ex);
         break;
      case ASTNode.INFIX_EXPRESSION :
         comp = invertInfixExpression((InfixExpression) ex,0);
         break;
      case ASTNode.PREFIX_EXPRESSION :
         break;
      default :
         comp = invertBooleanCondition(ex,false);
         break;
    }
   
   String logdata = getClass().getName();
   if (comp != null) {
      ASTRewrite rw = ASTRewrite.create(ex.getAST());
      rw.replace(ex,comp,null);
      String desc = "Use " + comp + " instead of " + ex;
      addRepair(rw,desc,logdata + "@CHANGEOP",0.25);
    }
   
   if (ex.getNodeType() == ASTNode.INFIX_EXPRESSION) {
      for (int i = 1; i <= 2; ++i) {
         comp = invertInfixExpression((InfixExpression) ex,i);
         if (comp != null) {
            ASTRewrite rw = ASTRewrite.create(ex.getAST());
            rw.replace(ex,comp,null);
            String desc = "Use " + comp + " in place of " + ex;
            addRepair(rw,desc,logdata + "@INVERT",0.25);
          }
       }
    }
}



private Expression invertBooleanLiteral(BooleanLiteral bl)
{
   boolean v = bl.booleanValue();
   AST ast = bl.getAST();
   return ast.newBooleanLiteral(!v);
}


private Expression invertInfixExpression(InfixExpression ifx,int check)
{
   InfixExpression.Operator op = ifx.getOperator();
   InfixExpression.Operator rop = null;
   if (op == InfixExpression.Operator.EQUALS) {
      if (check > 0) return null;
      rop = InfixExpression.Operator.NOT_EQUALS;
    }
   else if (op == InfixExpression.Operator.NOT_EQUALS) {
      if (check > 0) return null;
      rop = InfixExpression.Operator.EQUALS;
    }
   else if (op == InfixExpression.Operator.GREATER) {
      switch (check) {
         case 0 : rop = InfixExpression.Operator.LESS_EQUALS; break;
         case 1 : rop = InfixExpression.Operator.LESS; break;
         case 2 : rop = InfixExpression.Operator.GREATER_EQUALS; break;
       }
    }
   else if (op == InfixExpression.Operator.GREATER_EQUALS) {
      switch (check) {
         case 0 : rop = InfixExpression.Operator.LESS; break;
         case 1 : rop = InfixExpression.Operator.LESS_EQUALS; break;
         case 2 : rop = InfixExpression.Operator.GREATER; break;
       }
    }
   else if (op == InfixExpression.Operator.LESS) {
      switch (check) {
         case 0 : rop = InfixExpression.Operator.GREATER_EQUALS; break;
         case 1 : rop = InfixExpression.Operator.GREATER; break;
         case 2 : rop = InfixExpression.Operator.LESS_EQUALS; break;
       }
    }
   else if (op == InfixExpression.Operator.LESS_EQUALS) {
      switch (check) {
         case 0 : rop = InfixExpression.Operator.GREATER; break;
         case 1 : rop = InfixExpression.Operator.GREATER_EQUALS; break;
         case 2 : rop = InfixExpression.Operator.LESS; break;
       }
    }
   
   if (rop == null) {
      if (check != 0) return null;
      return invertBooleanCondition(ifx,true);
    }
   
   try {
      AST ast = ifx.getAST();
      InfixExpression rifx = ast.newInfixExpression();
      rifx.setLeftOperand((Expression) ASTNode.copySubtree(ast,ifx.getLeftOperand()));
      rifx.setRightOperand((Expression) ASTNode.copySubtree(ast,ifx.getRightOperand()));
      rifx.setOperator(rop);
      return rifx;
    }
   catch (Throwable t) {
      // copySubtree can fail under weird circumstances
      return null;
    }
}



private Expression invertBooleanCondition(Expression ex,boolean paren)
{
   AST ast = ex.getAST();
   Expression orig;
   try {
      orig = (Expression) ASTNode.copySubtree(ast,ex);
    }
   catch (Throwable t) {
      RoseLog.logE("Problem copying subtree " + ex,t);
      return null;
    }
   
   if (paren) {
      ParenthesizedExpression porig = ast.newParenthesizedExpression();
      porig.setExpression(orig);
      orig = porig;
    }
   PrefixExpression norig = ast.newPrefixExpression();
   norig.setOperand(orig);
   norig.setOperator(PrefixExpression.Operator.NOT);
   
   return norig;
}




/********************************************************************************/
/*                                                                              */
/*      Handle x && y                                                           */
/*                                                                              */
/********************************************************************************/

private void handleAndAndCondition(InfixExpression ex)
{
   handleCondition(ex.getLeftOperand());
   handleCondition(ex.getRightOperand());
   for (Object o : ex.extendedOperands()) {
      Expression e1 = (Expression) o;
      handleCondition(e1);
    }
}



private void handleOrOrCondition(InfixExpression ex)
{
   handleCondition(ex.getLeftOperand());
   handleCondition(ex.getRightOperand());
   for (Object o : ex.extendedOperands()) {
      Expression e1 = (Expression) o;
      handleCondition(e1);
    }
}




}       // end of class SepalIfCondition




/* end of SepalIfCondition.java */

