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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CastExpression;
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
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.bud.BudType;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootProblem;
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

StemQueryExceptionHistory(StemMain ctrl,RootProblem prob)
{
   super(ctrl,prob);
   
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
   if (expr == null)
      throw new RoseException("Can't find exception cause for " + exception_type);
   
   CommandArgs args = new CommandArgs("QTYPE","EXPRESSION");
   args = addCommandArgs(args);
   
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

ASTNode getExceptionNode() 
{
   try {
      ASTNode stmt = getSourceStatement();
      
      ExceptionChecker checker = null;
      switch (exception_type) {
         case "java.lang.NullPointerException" :
            checker = new NullPointerChecker();
            break;
         case "java.lang.ArrayIndexOutOfBoundsException" :
            checker = new ArrayIndexOutOfBoundsChecker();
            break;
         case "java.lang.IndexOutOfBoundsException" :
         case "java.util.NoSuchElementException" :
            checker = new IndexOutOfBoundsChecker();
            break;
         case "java.lang.StringIndexOutOfBoundsException" :
            checker = new StringIndexOutOfBoundsChecker();
            break;
         case "java.lang.StackOverflowError" :
            checker = new StackOverflowChecker();
            break;
         case "java.lang.ClassCastException" :
            checker = new ClassCastChecker();
            break;
       }
      
      if (checker != null && stmt != null) {
         checker.doCheck(stmt);
         return checker.getResult();
       }
    }
   catch (RoseException e) { }
   
   return null;
}



private String getExceptionCause() throws RoseException
{
  ASTNode stmt = getSourceStatement();
  
  if (exception_type == null || stmt == null) return null;
  
  ExceptionChecker checker = null;
  if (stmt instanceof ThrowStatement) {
     checker = new ThrowChecker();
   }
  else {
     switch (exception_type) {
        case "java.lang.NullPointerException" :
           checker = new NullPointerChecker();
           break;
        case "java.lang.ArrayIndexOutOfBoundsException" :
           checker = new ArrayIndexOutOfBoundsChecker();
           break;
        case "java.lang.IndexOutOfBoundsException" :
        case "java.util.NoSuchElementException" :
           checker = new IndexOutOfBoundsChecker();
           break;
        case "java.lang.StringIndexOutOfBoundsException" :
           checker = new StringIndexOutOfBoundsChecker();
           break;
        case "java.lang.StackOverflowError" :
           checker = new StackOverflowChecker();
           break;
        case "java.lang.ClassCastException" :
           checker = new ClassCastChecker();
           break;
      }
   }
  
  if (checker != null) {
     checker.doCheck(stmt);
     String loc = checker.generateResult();
     if (loc != null) return loc;
   }
  
  return null;
}


private abstract class ExceptionChecker extends ASTVisitor {
   
   private ASTNode use_node;
   private String orig_value;
   private String target_value;
   
   ExceptionChecker() {
      use_node = null;
      orig_value = null;
      target_value = null;
    }
   
   void doCheck(ASTNode n) {
      n.accept(this);
    }
   
   protected void useNode(ASTNode n,String orig,String tgt) {
      if (use_node == null) {
         use_node = n;
         orig_value = orig;
         target_value = tgt;
       }
    }
   
   protected boolean haveNode() {
      return use_node != null;
    }
   
   String generateResult() {
      if (use_node == null) return null;
      if (orig_value != null) for_problem.setOriginalValue(orig_value);
      if (target_value != null) for_problem.setTargetValue(target_value);
      return getXmlForLocation("EXPR",use_node,true);
    }
   
   ASTNode getResult() {
      return use_node; 
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
      if (bv == null) return false;
      if (bv.isNull()) {
         useNode(ex,"null","Non-Null");
         return false;
       }
      
      return bv.getBoolean();
    }
   
   private void checkForNull(Expression ex) {
      if (ex == null || haveNode()) return;
      BudValue bv = evaluate(ex.toString());
      if (bv != null && bv.isNull()) {
         useNode(ex,"null","Non-Null");
       }
    }
   
}       // end of inner class NullPointerChecker dc




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
      if (idx < 0 || idx >= bnd) useNode(aa,Long.toString(idx),null);
    }
   
}       // end of inner class ArrayIndexOutOfBoundsChecker



private class IndexOutOfBoundsChecker extends ExceptionChecker {

   @Override public void endVisit(MethodInvocation mi) {
      if (mi.getExpression() == null) return;
      JcompType jt = JcompAst.getExprType(mi.getExpression());
      String jtname = jt.getName();
      int idx0 = jtname.indexOf("<");
      if (idx0 > 0) jtname = jtname.substring(0,idx0);
      switch (jtname) {
         case "java.util.ArrayList" :
         case "java.util.List" :
         case "java.util.Vector" :
         case "java.util.Queue" :
         case "java.util.ArrayDeque" :
         case "java.util.LinkedList" :
         case "java.util.PriorityQueue" :
         case "java.util.Deque" :
            break;
         default :
            return;
       }
      switch (mi.getName().getIdentifier()) {
         case "get" :
         case "remove" :
         case "removeFirst" :
         case "removeLast" :
            break;
         default :
            return;
       }
      
      BudValue bv = evaluate("(" + mi.getExpression() + ").size()");
      if (bv == null) return;
      long bnd = bv.getInt();
      List<?> args = mi.arguments();
      long idx = 0;
      if (args.size() > 0) {
         Expression eidx = (Expression) args.get(0);
         BudValue bidx = evaluate(eidx.toString());
         if (bidx == null && bnd > 0) return;
         if (bidx != null) idx = bidx.getInt();
       }
      if (idx < 0 || idx >= bnd) useNode(mi,Long.toString(idx),null);
    }
   
}       // end of inner class IndexOutOfBoundsChecker


private class StringIndexOutOfBoundsChecker extends ExceptionChecker {
   
   @Override public void endVisit(MethodInvocation mi) {
      JcompType jt = JcompAst.getExprType(mi.getExpression());
      String jtname = jt.getName();
      int idx0 = jtname.indexOf("<");
      if (idx0 > 0) jtname = jtname.substring(0,idx0);
      String ex = null;
      switch (jtname) {
         case "java.lang.String" :
            ex = mi.getExpression().toString();
            switch (mi.getName().getIdentifier()) {
               case "charAt" :
               case "codePointAt" :
               case "codePointBefore" :
               case "codePointCount" :
               case "offsetByCodeePoints" :
               case "getBytes" :
               case "substring" :
               case "subSequence" :
                  checkIndex(mi,ex,0);
                  break;
               case "getChars" :
                  useNode(mi,null,null);
                  break;
               default :
                  return;
             }
            break;
         case "java.lang.Character" :
            switch (mi.getName().getIdentifier()) {
               case "codePointAt" :
                  ex = mi.arguments().get(0).toString();
                  if (ex != null) checkIndex(mi,ex,1);
                  break;
               default :
                  return;
             }
            break;
         default :
            return;
       }
    }
   
   private boolean checkIndex(MethodInvocation mi,String ex,int arg) {
      BudValue bv = evaluate("(" + ex + ").length()");
      if (bv == null) bv = evaluate("(" + ex + ").length");
      if (bv == null) return false;
      long bnd = bv.getInt();
      List<?> args = mi.arguments();
      long idx = 0;
      if (args.size() > arg) {
         Expression eidx = (Expression) args.get(arg);
         BudValue bidx = evaluate(eidx.toString());
         if (bidx == null && bnd > 0) return false;
         if (bidx != null) idx = bidx.getInt();
       }
      if (idx < 0 || idx >= bnd) {
         useNode(mi,Long.toString(idx),null);
         return true;
       }
      return false;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Class Cast Exception checker                                            */
/*                                                                              */
/********************************************************************************/

private class ClassCastChecker extends ExceptionChecker {
   
   @Override public void endVisit(CastExpression c) {
      BudValue cbv = evaluate(c.getExpression().toString());
      if (cbv == null) return;
      BudType bt = cbv.getDataType();
      JcompType jt = JcompAst.getJavaType(c.getType());
      if (jt.getName().equals(bt.getName())) return;
      useNode(c.getExpression(),bt.toString(),null);   
    }

}       // end of inner class ClassCastChecker





/********************************************************************************/
/*                                                                              */
/*      Stack Overflow handler                                                  */
/*                                                                              */
/********************************************************************************/

private class StackOverflowChecker extends ExceptionChecker {

   private String find_method;
   private String find_signature;
   
   StackOverflowChecker() {
      find_method = null;
      find_signature = null;
    }
   
   @Override void doCheck(ASTNode stmt) {
      BudStack stk = bud_launch.getStack();
      Map<String,Integer> cnts = new HashMap<>();
      for (BudStackFrame frm : stk.getFrames()) {
         File f = frm.getSourceFile();
         int lno = frm.getLineNumber();
         String mthd = frm.getMethodName();
         String sgn = frm.getMethodSignature();
         String key = f.getPath() + "@" + lno + "@" + mthd + "@" + sgn;
         Integer v = cnts.get(key);
         if (v == null) v = 0;
         cnts.put(key,v+1);
       }
      String most = null;
      int mostv = 0;
      for (Map.Entry<String,Integer> ent : cnts.entrySet()) {
         if (ent.getValue() > mostv) {
            most = ent.getKey();
            mostv = ent.getValue();
          }
       }
      String [] items = most.split("@");
      Integer lno = Integer.parseInt(items[1]);
      ASTNode n = stem_control.getSourceNode(project_name,new File(items[0]),-1,lno,true,true);
      find_method = items[2];
      find_signature = items[3];
      n.accept(this);     
    }
   
   @Override public boolean visit(MethodInvocation mi) {
      if (mi.getName().getIdentifier().equals(find_method)) {
         JcompSymbol js = JcompAst.getReference(mi.getName());
         String ftynm = js.getType().getJavaTypeName();
         if (find_signature == null || find_signature.equals(ftynm)) {
            useNode(mi,null,null);
          }
       }
      return true;
    }
   
}       // end of inner class StackOverflowChecker



/********************************************************************************/
/*                                                                              */
/*      Throw checker -- handle explicit throws                                 */
/*                                                                              */
/********************************************************************************/

private class ThrowChecker extends ExceptionChecker {
   
   ThrowChecker() {
    }
   
   @Override void doCheck(ASTNode stmt) {
      ASTNode prev = stmt;
      for (ASTNode p = stmt.getParent(); p != null; p = p.getParent()) {
         if (p instanceof IfStatement) {
            IfStatement ifstmt = (IfStatement) p;
            Boolean val = null;
            if (prev == ifstmt.getThenStatement()) val = true;
            else if (prev == ifstmt.getElseStatement()) val = false;
            String oval = (prev == null ? null : Boolean.toString(val));
            String tval = (prev == null ? null : Boolean.toString(!val));
            useNode(ifstmt.getExpression(),oval,tval);
            break;
          }
         // TODO: should check for previous statement with break/continue in a block
         prev = p;
       }
    }

}       // end of inner class StackOverflowChecker




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

