/********************************************************************************/
/*                                                                              */
/*              StemQueryExceptionHistory.java                                  */
/*                                                                              */
/*      Handle exception shouldn't be thrown history                            */
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



package edu.brown.cs.rose.stem;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractProblem;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RoseException;


class StemQueryExceptionHistory extends StemQueryHistory
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  exception_type;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryExceptionHistory(StemMain ctrl,BractProblem prob,Element xml)
{
   super(ctrl,prob,xml);
   
   exception_type = prob.getProblemDetail();
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override void process(StemMain stem,IvyXmlWriter xw) throws RoseException
{
   stem.waitForAnalysis();
   
   String expr = getExceptionCause();
   if (expr == null) throw new RoseException("Can't find exception cause");
   
   CommandArgs args = new CommandArgs("FILE",for_file.getAbsolutePath(),
         "QTYPE","EXPRESSION",
         "LINE",line_number,
         "METHOD",method_name);
   
   String sxml = getXmlForStack();
   if (sxml != null) expr += sxml;
   
   Element rslt = stem.sendFaitMessage("FLOWQUERY",args,expr);
   outputGraph(rslt,xw);
}




/********************************************************************************/
/*                                                                              */
/*      Get expression causing the exception                                    */
/*                                                                              */
/********************************************************************************/

private String getExceptionCause() throws RoseException
{
  ASTNode stmt = getSourceStatement();
  
  ExceptionChecker checker = null;
  switch (exception_type) {
     case "java.lang.NullPointerException" :
        checker = new NullPointerChecker();
        break;
     case "java.lang.ArrayIndexOutOfBoundsException" :
        checker = new ArrayIndexOutOfBoundsChecker();
        break;
   }
  
  if (checker != null) {
     stmt.accept(checker);
     String loc = checker.generateResult();
     if (loc == null) return null;
     return loc;
   }
  
  return null;
}


private abstract class ExceptionChecker extends ASTVisitor {
   
   private ASTNode use_node;
   
   ExceptionChecker() {
      use_node = null;
    }
   
   protected void useNode(ASTNode n) {
      if (use_node == null) use_node = n;
    }
   
   protected boolean haveNode() {
      return use_node != null;
    }
   
   String generateResult() {
      if (use_node == null) return null;
      return getXmlForLocation("EXPR",use_node,true);
    }
   
}       // end of inner class ExceptionChecker




/********************************************************************************/
/*                                                                              */
/*      Checker for null pointer exceptions                                     */
/*                                                                              */
/********************************************************************************/

private class NullPointerChecker extends ExceptionChecker {
   
   @Override public void endVisit(ArrayAccess aa) {
      checkForNull(aa.getIndex());
      checkForNull(aa.getArray());
    }
   
   @Override public void endVisit(FieldAccess fa) {
      checkForNull(fa.getExpression());
    }
   
   @Override public void endVisit(MethodInvocation mi) {
      checkForNull(mi.getExpression());
    }
   
   @Override public boolean visit(InfixExpression ex) {
      if (haveNode()) return false;
      if (ex.getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
         checkAndAnd(ex);
         return false;
       }
      else if (ex.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
         checkOrOr(ex);
         return false;
       } 
      return true;
    }
   
   @Override public void endVisit(InfixExpression ex) {
      if (ex.getOperator() == InfixExpression.Operator.PLUS) {
         checkPlus(ex);
       }
      else if (ex.getOperator() == InfixExpression.Operator.EQUALS) { }
      else if (ex.getOperator() == InfixExpression.Operator.NOT_EQUALS) { }
      else {
         checkForNull(ex.getLeftOperand());
         checkForNull(ex.getRightOperand());
         for (Object o : ex.extendedOperands()) {
            Expression ope = (Expression) o;
            checkForNull(ope);
          }
       }
    }
   
   @Override public void endVisit(SwitchStatement ss) {
      checkForNull(ss.getExpression());
    }
   
   @Override public void endVisit(WhileStatement ws) {
      checkForNull(ws.getExpression());
    }
   
   @Override public void endVisit(ForStatement fs) {
      checkForNull(fs.getExpression());
    }
   
   @Override public void endVisit(EnhancedForStatement fs) {
      checkForNull(fs.getExpression());
    }
   
   @Override public boolean visit(IfStatement is) {
      if (haveNode()) return false;
      boolean fg = checkBoolean(is.getExpression());
      if (haveNode()) return false;
      if (fg) {
         is.getThenStatement().accept(this);
       }
      else if (is.getElseStatement() != null) {
         is.getElseStatement().accept(this);
       }
      return false;
    }
   
   @Override public void endVisit(DoStatement ds) {
       checkForNull(ds.getExpression()); 
    }
   
   @Override public boolean visit(ConditionalExpression ce) {
      if (haveNode()) return false;
      boolean fg = checkBoolean(ce.getExpression());
      if (haveNode()) return false;
      if (fg) {
         ce.getThenExpression().accept(this);
       }
      else {
         ce.getElseExpression().accept(this);
       }
      return false;
    }
   
   
   private void checkPlus(InfixExpression ex) { }
   
   private void checkAndAnd(InfixExpression ex) { 
      if (haveNode()) return;
      if (!checkBoolean(ex.getLeftOperand())) return;
      if (haveNode()) return;
      if (!checkBoolean(ex.getRightOperand())) return;
      if (haveNode()) return;
      for (Object o : ex.extendedOperands()) {
         Expression eop = (Expression) o;
         if (haveNode()) return;
         if (!checkBoolean(eop)) return;
       }
    }
   
   private void checkOrOr(InfixExpression ex) { 
      if (haveNode()) return;
      if (checkBoolean(ex.getLeftOperand())) return;
      if (haveNode()) return;
      if (checkBoolean(ex.getRightOperand())) return;
      for (Object o : ex.extendedOperands()) {
         Expression eop = (Expression) o;
         if (haveNode()) return;
         if (checkBoolean(eop)) return;
       }
    }
   
   private boolean checkBoolean(Expression ex) {
      ex.accept(this);
      if (haveNode()) return false;
      BudValue bv = evaluate(ex.toString());
      if (bv.isNull()) {
         useNode(ex);
         return false;
       }
      
      return bv.getBoolean();
    }
   
   private void checkForNull(Expression ex) {
      if (ex == null || haveNode()) return;
      BudValue bv = evaluate(ex.toString());
      if (bv != null && bv.isNull()) {
         useNode(ex);
       }
    }
   
}       // end of inner class NullPointerChecker




/********************************************************************************/
/*                                                                              */
/*      Checker for index out of bounds                                         */
/*                                                                              */
/********************************************************************************/

private class ArrayIndexOutOfBoundsChecker extends ExceptionChecker {
   
   @Override public void endVisit(ArrayAccess aa) {
      BudValue bv = evaluate("(" + aa.getArray().toString() + ").length");
      if (bv == null) return;
      long bnd = bv.getInt();
      BudValue abv = evaluate(aa.getIndex().toString());
      if (abv == null) return;
      long idx = abv.getInt();
      if (idx < 0 || idx >= bnd) useNode(aa);
    }
   
}       // end of inner class IndexOutOfBoundsChecker




/********************************************************************************/
/*                                                                              */
/*      Evaluate an expression in the current frame                             */
/*                                                                              */
/********************************************************************************/

private BudValue evaluate(String expr) {
   return bud_launch.evaluate(expr);
}

}       // end of class StemQueryExceptionHistory




/* end of StemQueryExceptionHistory.java */

