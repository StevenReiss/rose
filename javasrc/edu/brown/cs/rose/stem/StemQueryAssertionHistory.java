/********************************************************************************/
/*                                                                              */
/*              StemQueryAssertionHistory.java                                  */
/*                                                                              */
/*      Handle assertion failure problems                                       */
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
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.rose.root.RootControl.AssertionData;

class StemQueryAssertionHistory extends StemQueryHistory
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static int THIS_INDEX = 999999;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryAssertionHistory(StemMain ctrl,RootProblem prob)
{
   super(ctrl,prob);
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override void process(StemMain stem,IvyXmlWriter xw) throws RoseException
{
   stem.waitForAnalysis();
   
   ASTNode stmt = getSourceStatement();
   AssertionChecker checker = new AssertionChecker();
   stmt.accept(checker);
   
   String expr = checker.generateResult();
   
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
/*      Get informationn about the assertion                                    */
/*                                                                              */
/********************************************************************************/

AssertionData getAssertionData()
{
   stem_control.waitForAnalysis();
   
   try {
      ASTNode stmt = getSourceStatement();
      AssertionChecker checker = new AssertionChecker();
      stmt.accept(checker);
      if (checker.getExpression() == null) return null;
      return checker;
    }
   catch (RoseException e) { }
   
   return null;
}







/********************************************************************************/
/*                                                                              */
/*      Assertion checker                                                       */
/*                                                                              */
/********************************************************************************/

private class AssertionChecker extends ASTVisitor implements AssertionData {
   
   private ASTNode use_node;
   private String orig_value;
   private String target_value;
   private boolean is_location;
   
   AssertionChecker() {
      use_node = null;
      orig_value = null;
      target_value = null;
      is_location = false;
    }
   
   String generateResult() {
      if (use_node == null) return null;
      if (orig_value != null) for_problem.setOriginalValue(orig_value);
      if (target_value != null) for_problem.setTargetValue(target_value);
      // should set precision
      return getXmlForLocation("EXPR",use_node,true);
    }
   
   @Override public ASTNode getExpression()     { return use_node; }
   @Override public String getOriginalValue()   { return orig_value; }
   @Override public String getTargetValue()     { return target_value; }
   @Override public boolean isLocation()        { return is_location; }
   
   private void useNode(ASTNode n,String orig,String tgt) {
      if (use_node == null) {
         use_node = n;
         orig_value = orig;
         target_value = tgt;
       }
    }
   
   @Override public boolean visit(MethodInvocation mi) {
      String nm = mi.getName().getIdentifier();
      int ct = mi.arguments().size();
      int givenidx = -1;
      int targetidx = -1;
      switch (nm) {
         case "equals" :
            givenidx = THIS_INDEX;
            targetidx = 0;
            break;
         case "assertArrayEquals" :
            break;
         case "assertEquals" :
         case "assertSame" :
         case "assertNotEquals" :
         case "assertNotSame" :
            if (ct == 2) { 
               givenidx = 0;
               targetidx = 1;
             }
            else if (ct == 3) {
               ASTNode arg1 = (ASTNode) mi.arguments().get(1);
               JcompType t1 = JcompAst.getExprType(arg1);
               if (t1.isFloatingType()) {
                  givenidx = 0;
                  targetidx = 1;
                }
               else {
                  givenidx = 1;
                  targetidx = 2;
                }
             }
            else if (ct == 4) {
               givenidx = 1;
               targetidx = 2;
             }
            break;
         case "assertNull" :
         case "assertNotNull" :
         case "assertTrue" :
         case "assertFalse" :
         case "assertThrows" :
            if (ct == 1) givenidx = 0;
            else givenidx = 1;
            break;
         case "assertThat" :
            if (ct == 2) {
               ASTNode arg1 = getArgument(1,mi); 
               JcompType t1 = JcompAst.getExprType(arg1);
               if (t1.isBooleanType()) givenidx = 1;
               else givenidx = 0;
             }
            else givenidx = 1;
            break;
         case "fail" :
            break;
         default :
            return false;
       }
      
      if (givenidx >= 0 && targetidx >= 0) {
         ASTNode ng = getArgument(givenidx,mi);
         switch (ng.getNodeType()) {
            case ASTNode.NUMBER_LITERAL :
            case ASTNode.STRING_LITERAL :
            case ASTNode.NULL_LITERAL :
            case ASTNode.TEXT_BLOCK :
               int idx = givenidx;
               givenidx = targetidx;
               targetidx = idx;
               break;
          }
       }
      
      if (givenidx < 0) return false;
      
      String given = getSourceValue(mi,givenidx);
      String target = null;
      
      switch (nm) {
         case "assertArrayEquals" :
            break;
         case "assertEquals" :
         case "assertSame" :
            target = getTargetValue(mi,targetidx);
            break;
         case "assertNotEquals" :
         case "assertNotSame" :
            break;
         case "assertNull" :
            target = "null";
            break;
         case "assertNotNull" :
            target = "Non-Null";
            break;
         case "assertThat" :
         case "assertTrue" :
            target = "true";
            break;
         case "assertFalse" :
            target = "false";
            break;
         case "assertThrows" :
            break;
         case "fail" :
            is_location = true;
            break;
         default :
            break;
       }
      Expression ex = getArgument(givenidx,mi);
      useNode(ex,given,target);
      
      return false;
    }
   
   private Expression getArgument(int idx,MethodInvocation mi) {
      if (idx == THIS_INDEX) return mi.getExpression();
      return (Expression) mi.arguments().get(idx);
    }
   
   @Override public boolean visit(AssertStatement stmt) {
      useNode(stmt.getExpression(),"false","true");
      return false;
    }
   
   private String getSourceValue(MethodInvocation mi,int idx) {
      BudValue bv = getBudValue(mi,idx);
      if (bv == null) return null;
      return bv.getDataType().getName() + " " + bv.toString();
    }
   
   private String getTargetValue(MethodInvocation mi,int idx) {
      BudValue bv = getBudValue(mi,idx);
      if (bv == null) return null;
      return bv.toString();
    }
   
   private BudValue getBudValue(MethodInvocation mi,int idx) {
      if (idx < 0) return null;
      Expression ex = getArgument(idx,mi);
      BudValue bv = bud_launch.evaluate(ex.toString());
      return bv;
    }
   
}       // end of inner class AssertionChecker



}       // end of class StemQueryAssertionHistory




/* end of StemQueryAssertionHistory.java */

