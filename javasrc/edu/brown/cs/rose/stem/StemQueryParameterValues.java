/********************************************************************************/
/*                                                                              */
/*              StemQueryParameterValues.java                                   */
/*                                                                              */
/*      Find original values of parameters for a call                           */
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

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RoseException;

class StemQueryParameterValues extends StemQueryBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<String>    parameter_set;
private String          method_name; 
private String          project_name;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryParameterValues(StemMain ctrl,Element xml)
{
   super(ctrl,xml);
   method_name = IvyXml.getAttrString(xml,"METHOD");
   parameter_set = new HashSet<>();
   project_name = null;
   for (Element p : IvyXml.children(xml,"PARAMETER")) {
      parameter_set.add(IvyXml.getAttrString(p,"NAME"));
    }
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process(StemMain sm,IvyXmlWriter xw) throws RoseException
{
   // first find the previous stack frame
   BudStack stk = bud_launch.getStack();
   boolean usenext = false;
   BudStackFrame prev = null;
   for (BudStackFrame frm : stk.getFrames()) {
      if (usenext) {
         prev = frm;
         break;
       }
      if (frm.getFrameId().equals(frame_id)) usenext = true;
    }
   if (prev == null) throw new RoseException("Can't find previous frame");
 
   // then find the method declaration of the caller
   ASTNode n = getSourceStatement();
   MethodDeclaration mthd = null;
   for (ASTNode m = n; m != null; m = m.getParent()) {
      if (m instanceof MethodDeclaration) {
         mthd = (MethodDeclaration) m;
         break;
       }
    }
   if (mthd == null) throw new RoseException("Can't find called method");
   
   // then get parameter numbers for each required parameter, -1 for this
   Map<Integer,String> parms = new HashMap<>();
   int idx = 1;
   if (parameter_set.contains("this")) parms.put(0,"this");
   for (Object o : mthd.parameters()) {
      SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
      SimpleName sn = svd.getName();
      String parmnm = sn.getIdentifier();
      if (parameter_set.contains(parmnm)) parms.put(idx,parmnm);
      ++idx;
    }
   if (parameter_set.size() != parms.size()) throw new RoseException("Missing parameters");
   
   // next find the AST for the caller
   ASTNode past = getAstForFrame(sm,prev,mthd);
   if (past == null) throw new RoseException("Can't find previous frame code"); 
   List<ASTNode> callargs = findMethodCallArgs(past);
   
   // then for each argument (or this), evaluate the corresponding expression
   
   BudLaunch pctx = new BudLaunch(sm,thread_id,prev.getFrameId(),project_name);
   Map<String,BudValue> pvals = new HashMap<>();
   for (int i = 0; i < callargs.size(); ++i) {
      String nm = parms.get(i);
      if (nm == null) continue;
      String expr = callargs.get(i).toString();
      BudValue bv = pctx.evaluate(expr);
      pvals.put(nm,bv);
    }
   
   // then output the results
   xw.begin("RESULT");
   for (Map.Entry<String,BudValue> ent : pvals.entrySet()) {
      String nm = ent.getKey();
      BudValue bv = ent.getValue();
      xw.begin("PARAMETER");
      xw.field("NAME",nm);
      bv.outputXml(xw);
      xw.end("PARAMETER");
    }
   xw.end("RESULT");
}



private ASTNode getAstForFrame(StemMain sm,BudStackFrame frm,ASTNode base) 
{
   CommandArgs args = new CommandArgs("PATTERN",IvyXml.xmlSanitize(frm.getClassName()),
         "DEFS",true,"REFS",false,"FOR","TYPE");
   Element cxml = sm.sendBubblesMessage("PATTERNSEARCH",args,null);
   File fnm = null;
   String pnm = null;
   for (Element lxml : IvyXml.elementsByTag(cxml,"MATCH")) {
      fnm = new File(IvyXml.getAttrString(lxml,"FILE"));
      Element ielt = IvyXml.getChild(lxml,"ITEM");
      pnm = IvyXml.getAttrString(ielt,"PROJECT");
    }
   if (fnm == null || pnm == null) return null;
   
   project_name = pnm;
   
   try {
      String text = IvyFile.loadFile(fnm);
      CompilationUnit cu;
      if (fnm.equals(for_file)) {
         cu = (CompilationUnit) base.getRoot();
       }
      else {
         cu = JcompAst.parseSourceFile(text);
       }
      return findNode(cu,text,frm.getLineNumber());
    }
   catch (IOException e) {
      return null;
    }
}



private List<ASTNode> findMethodCallArgs(ASTNode n)
{
   String mnm = method_name;
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




}       // end of class StemQueryParameterValues




/* end of StemQueryParameterValues.java */

