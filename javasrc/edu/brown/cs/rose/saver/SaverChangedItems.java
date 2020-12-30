/********************************************************************************/
/*                                                                              */
/*              ServerChangedItems.java                                         */
/*                                                                              */
/*      Handle finding changed items for a stack frame                          */
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



package edu.brown.cs.rose.saver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.w3c.dom.Element;

import edu.brown.cs.fait.server.ServerConstants;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.thorn.ThornFactory;
import edu.brown.cs.rose.thorn.ThornConstants.ThornVariable;
import edu.brown.cs.rose.thorn.ThornConstants.ThornVariableType;

class SaverChangedItems implements ServerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl     root_control;
private BudLaunch       for_launch;
private String          for_frame;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SaverChangedItems(BudLaunch bl,String frame)
{
   root_control = bl.getControl();
   for_launch = bl;
   for_frame = frame;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

Map<ThornVariable,BudValue> getChangedItems()
{
   Set<ThornVariable> items = findChangedItems();
   
   if (items == null) return null;
   
   Map<ThornVariable,BudValue> rslt = new HashMap<>();
   
   Set<String> params = new HashSet<>();
   for (ThornVariable tv : items) {
      if (tv.getVariableType() == ThornVariableType.PARAMETER) {
         params.add(tv.getName());
       }
    }
   Map<String,BudValue> pvals = getParameterValues(params);
   if (pvals != null) {
      for (ThornVariable tv : items) {
         if (tv.getVariableType() == ThornVariableType.PARAMETER) {
            BudValue bv = pvals.get(tv.getName());
            rslt.put(tv,bv);
          }
       }
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Find the set of items that have changed                                 */
/*                                                                              */
/********************************************************************************/

private Set<ThornVariable> findChangedItems()
{
   Set<ThornVariable> rslt = new HashSet<>();
   BudStack bs = for_launch.getStack();
   ThornFactory tf = new ThornFactory(root_control);
   
   for (BudStackFrame bf : bs.getFrames()) {
      File f = bf.getSourceFile();
      if (f != null && f.exists() && f.canRead()) {
         String proj = null;
         if (f != null) proj = root_control.getProjectForFile(f);
         ASTNode n = root_control.getSourceNode(proj,f,-1,bf.getLineNumber(),true,true);
         List<ThornVariable> fnd = tf.getChangedVariables(n);
         if (fnd != null && !fnd.isEmpty()) {
            boolean top = for_frame.equals(bf.getFrameId());
            for (ThornVariable tv : fnd) {
               if (!top && tv.getVariableType() == ThornVariableType.PARAMETER) continue;
               rslt.add(tv);
             }
          }
         // this needs to track what is internal and removed by outer method
         // This should probably be done in THORN, with getChangedVariables() taking
         // the current set as arguments and returning the resultant set
       }
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Get values for parameters of a call                                     */
/*                                                                              */
/********************************************************************************/

Map<String,BudValue> getParameterValues(Set<String> params)
{
   Map<String,BudValue> rslt = new HashMap<>();
   if (params == null || params.isEmpty()) return rslt;
   
   BudStack stk = for_launch.getStack();
   boolean usenext = false;
   BudStackFrame prev = null;
   BudStackFrame cur = null;
   for (BudStackFrame frm : stk.getFrames()) {
      if (usenext) {
         prev = frm;
         break;
       }
      if (frm.getFrameId().equals(for_frame)) {
         cur = frm;
         usenext = true;
       }
    }
   if (prev == null) return null;
   
   // then find the method declaration of the caller
   File f = cur.getSourceFile();
   String proj = root_control.getProjectForFile(f);
   
   
   ASTNode n = root_control.getSourceStatement(proj,f,-1,cur.getLineNumber(),false);
   MethodDeclaration mthd = null;
   for (ASTNode m = n; m != null; m = m.getParent()) {
      if (m instanceof MethodDeclaration) {
         mthd = (MethodDeclaration) m;
         break;
       }
    }
   if (mthd == null) return null;
   
   // then get parameter numbers for each required parameter, -1 for this
   Map<Integer,String> parms = new HashMap<>();
   int idx = 1;
   if (params.contains("this")) parms.put(0,"this");
   for (Object o : mthd.parameters()) {
      SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
      SimpleName sn = svd.getName();
      String parmnm = sn.getIdentifier();
      if (params.contains(parmnm)) parms.put(idx,parmnm);
      ++idx;
    }
   if (params.size() != parms.size()) return null;
   
   // next find the AST for the caller
   ASTNode past = getAstForFrame(prev,mthd);
   if (past == null) return null; 
   List<ASTNode> callargs = findMethodCallArgs(past,cur.getMethodName());
   
   // then for each argument (or this), evaluate the corresponding expression
   
   BudLaunch pctx = new BudLaunch(root_control,for_launch.getThread(),
         prev.getFrameId(),proj);
   Map<String,BudValue> pvals = new HashMap<>();
   for (int i = 0; i < callargs.size(); ++i) {
      String nm = parms.get(i);
      if (nm == null) continue;
      String expr = callargs.get(i).toString();
      BudValue bv = pctx.evaluate(expr);
      pvals.put(nm,bv);
    }
   
   return rslt;
}




private ASTNode getAstForFrame(BudStackFrame frm,ASTNode base) 
{
   CommandArgs args = new CommandArgs("PATTERN",IvyXml.xmlSanitize(frm.getClassName()),
         "DEFS",true,"REFS",false,"FOR","TYPE");
   Element cxml = root_control.sendBubblesMessage("PATTERNSEARCH",args,null);
   File fnm = null;
   String pnm = null;
   for (Element lxml : IvyXml.elementsByTag(cxml,"MATCH")) {
      fnm = new File(IvyXml.getAttrString(lxml,"FILE"));
      Element ielt = IvyXml.getChild(lxml,"ITEM");
      pnm = IvyXml.getAttrString(ielt,"PROJECT");
    }
   if (fnm == null || pnm == null) return null;
   
// value_project_name = pnm;
   
   try {
      String text = IvyFile.loadFile(fnm);
      CompilationUnit cu = JcompAst.parseSourceFile(text);
      return findNode(cu,text,frm.getLineNumber());
    }
   catch (IOException e) {
      return null;
    }
}



protected ASTNode findNode(CompilationUnit cu,String text,int line) 
{
   if (cu == null) return null;
   int off = -1;
   if (line > 0) {
      off = cu.getPosition(line,0);
      while (off < text.length()) {
         char c = text.charAt(off);
         if (!Character.isWhitespace(c)) break;
         ++off;
       }
    }
   ASTNode node = JcompAst.findNodeAtOffset(cu,off);   
   return node;
}



private List<ASTNode> findMethodCallArgs(ASTNode n,String mthd)
{
   String mnm = mthd;
   int idx = mnm.indexOf("(");
   if (idx > 0) mnm = mnm.substring(0,idx);
   idx = mnm.lastIndexOf(".");
   if (idx > 0) mnm = mnm.substring(idx+1);
   
   CallFinder cf = new CallFinder(mnm);
   for (ASTNode p = n; p != null; p = p.getParent()) {
      p.accept(cf);
      List<ASTNode> args = cf.getCallArgs();
      if (args != null) return args;
      if (p instanceof MethodDeclaration) return null;
    }
   
   return null;
}

private class CallFinder extends ASTVisitor {

   private String called_method;
   private List<?> call_args;
   private ASTNode this_arg;
   
   CallFinder(String cm) {
      call_args = null;
      called_method = cm;
      this_arg = null;
    }
   
   List<ASTNode> getCallArgs() {
      if (call_args == null) return null;
      List<ASTNode> rslt = new ArrayList<>();
      rslt.add(this_arg);
      for (Object o : call_args) {
         rslt.add((ASTNode) o);
       }
      return rslt;
    }
   
   @Override public void endVisit(MethodInvocation mi) {
      if (mi.getName().getIdentifier().equals(called_method)) {
         call_args = mi.arguments();
         this_arg = mi.getExpression();
       }
    }
   
   @Override public void endVisit(ConstructorInvocation ci) { }
   
   @Override public void endVisit(SuperConstructorInvocation ci) { }
   
   @Override public void endVisit(ClassInstanceCreation ci) {
      if (ci.getType().toString().equals(called_method)) {
         call_args = ci.arguments();
         this_arg = ci.getExpression();
       }
    }

}       // end of inner class CallFinder


}       // end of class ServerChangedItems




/* end of ServerChangedItems.java */

