/********************************************************************************/
/*                                                                              */
/*              PicotTestCreator.java                                           */
/*                                                                              */
/*      Actually create a test case for current run                             */
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



package edu.brown.cs.rose.picot;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractFactory;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootValidate.RootTrace;
import edu.brown.cs.rose.root.RootValidate.RootTraceCall;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;
import edu.brown.cs.rose.root.RootValidate.RootTraceVariable;
import edu.brown.cs.rose.validate.ValidateFactory;

class PicotTestCreator extends Thread implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl     root_control;
private String          reply_id;
private Element         test_description;
private RootProblem     for_problem;
private RootLocation    at_location;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotTestCreator(RootControl rc,String rid,Element xml)
{
   super("PICOT_TEST_CREATOR_" + rid);
   
   root_control = rc;
   reply_id = rid;
   test_description = xml;
   
   BractFactory bf = BractFactory.getFactory();
   Element pxml = IvyXml.getChild(xml,"PROBLEM");
   for_problem = bf.createProblemDescription(rc,pxml);
   int upframes = IvyXml.getAttrInt(xml,"UPFRAMES");
   if (upframes >= 0) for_problem.setMaxUp(upframes);
   at_location = for_problem.getBugLocation();
   if (at_location == null) {
      Element locxml = IvyXml.getChild(xml,"LOCATION");
      if (locxml == null) locxml = IvyXml.getChild(pxml,"LOCATION");
      at_location = bf.createLocation(rc,IvyXml.getChild(xml,"LOCATION"));
    }
}



/********************************************************************************/
/*                                                                              */
/*      Main processing method                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void run()
{
   CommandArgs args = new CommandArgs("NAME",reply_id);
   String cnts = null;

   if (test_description == null) {
      args.put("STATUS","NOTEST");
    }
   else {
      PicotTestCase test = null;
      try {
         test = createTestCase();
       }
      catch (Throwable t) {
         RoseLog.logE("Problem creating test case",t);
       }
      RoseLog.logD("PICOT","TEST CREATION RESULT: " + test);
      if (test == null) args.put("STATUS","FAIL");
      else {
         args.put("STATUS",test.getStatus());
         if (test.getRunCode() != null) {
            Set<String> imps = fixImports(test);
            IvyXmlWriter xw = new IvyXmlWriter();
            xw.begin("TESTCASE");
            xw.field("PROJECT",test.getTestProject());
            xw.field("PACKAGE",test.getTestPackageName());
            xw.field("CLASS",test.getTestClassName());
            xw.field("METHOD",test.getTestMethodName());
            xw.cdataElement("RUNCODE",test.getRunCode().getCode());
            xw.cdataElement("TESTCODE",test.getTestCode());
            xw.textElement("DESCRIPTION",for_problem.getDescription());
            for (String s : imps) {
               xw.textElement("IMPORT",s);
             }
            xw.end("TESTCASE");
            cnts = xw.toString();
            xw.close();
          }
       }
    }
   
   root_control.sendRoseMessage("TESTCREATE",args,cnts,0);
}



/********************************************************************************/
/*                                                                              */
/*      Logic for creating a test case                                          */
/*                                                                              */
/********************************************************************************/

PicotTestCase createTestCase()
{
   loadFilesIntoFait();
   Set<File> f = root_control.getLoadedFiles();
   root_control.compileAll(f);
   
   PicotStartFinder fndr = new PicotStartFinder(this);
   BudStackFrame bsf = fndr.findStartingPoint();
   if (bsf == null) return null;
   RoseLog.logD("PICOT","Found starting frame " + bsf.getMethodName());
   
   ValidateFactory vf = ValidateFactory.getFactory(root_control);
   
   RootValidate rv = vf.createValidate(for_problem,bsf.getFrameId(),
         at_location,false,false,true);
   if (rv == null) return null;
   RoseLog.logD("PICOT","PROBLEM TIME: " + rv.getExecutionTrace().getProblemTime()); 
   
   long start = getStartTime(rv);
   
   RootTrace rt = rv.getExecutionTrace();
   RootTraceCall rtc = rt.getRootContext();
   RoseLog.logD("PICOT","START BUILDING " + rt.getProblemTime() + " " + rtc.getMethod());
   
   MethodDeclaration md = getMethod(rtc);
   if (md == null) return null;
   JcompTyper typer = JcompAst.getTyper(md);
   PicotValueBuilder pvb = new PicotValueBuilder(root_control,rv,start,typer);
   
   PicotValueContext runctx = buildCall(rt,rtc,pvb);
   
   TestCase rslt = null;
   if (runctx == null) {
      rslt = new TestCase(PicotTestStatus.FAIL);
    }
   else {
      RootTrace testtrace = runctx.getTrace();
      PicotTestStatus sts = validateTest(rv,testtrace);
      rslt = new TestCase(sts,runctx);
    }
   
   pvb.finished();
   
   RoseLog.logD("PICOT","FINISH BUILDING " + rslt);
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Create a call                                                           */
/*                                                                              */
/********************************************************************************/

private PicotValueContext buildCall(RootTrace rt,RootTraceCall rtc,PicotValueBuilder pvb)
{
   MethodDeclaration md = getMethod(rtc);
   if (md == null) return null;
   JcompSymbol js = JcompAst.getDefinition(md);
   if (js == null) return null;
   
   PicotCodeFragment thisfrag = null;
   List<PicotCodeFragment> args = new ArrayList<>();
   
   if (!js.isStatic()) {
      RootTraceVariable thisvar = rtc.getTraceVariables().get("this");
      pvb.computeValue(thisvar);
      RootTraceVariable this0var = rtc.getTraceVariables().get("this$0");
      pvb.computeValue(this0var);
    }
   // handle this$0 if needed
   for (Object o : md.parameters()) {
      SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
      String nm = svd.getName().getIdentifier();
      RootTraceVariable pvar = rtc.getTraceVariables().get(nm);
      pvb.computeValue(pvar);
    }
   Map<String,RootTraceVariable> glbls = rt.getGlobalVariables();
   for (String vnm : glbls.keySet()) {
      int idx = vnm.lastIndexOf(".");
      if (idx < 0) continue;
      String cnm = vnm.substring(0,idx);
      JcompType jty = pvb.getJcompTyper().findType(cnm);
      if (jty == null) continue;
      if (jty.isCompiledType()) {
         pvb.computeValue(glbls.get(vnm));
       }
    }
 
   PicotValueContext initctx = pvb.getInitializationContext();
   if (initctx == null) return null;
   
   if (!js.isStatic()) {
      RootTraceVariable thisvar = rtc.getTraceVariables().get("this");
      thisfrag = pvb.buildSimpleValue(thisvar);
      if (thisfrag == null) return null;
    }
   else {
      String cnm = js.getClassType().getName();
      thisfrag = new PicotCodeFragment(cnm);
    }
   // handle this$0 if needed
   for (Object o : md.parameters()) {
      SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
      String nm = svd.getName().getIdentifier();
      RootTraceVariable pvar = rtc.getTraceVariables().get(nm);
      PicotCodeFragment arg = pvb.buildSimpleValue(pvar);
      if (arg == null) return null;
      args.add(arg);
    }

// look for static fields accessed by code in the trace
   
// initctx = pvb.getInitializationContext();
   
// should clean up initcode by removing unneeded items
   
   String call = "";
   if (!js.getType().getBaseType().isVoidType()) {
      call = js.getType().getBaseType().getName() + " result = ";
    }
   if (thisfrag != null) call += thisfrag.getCode() + ".";
   call += md.getName() + "(";
   for (int i = 0; i < args.size(); ++i) {
      if (i > 0) call += ",";
      call += args.get(i).getCode();
    }
   call += ");\n";
   
   PicotCodeFragment callfrag = new PicotCodeFragment(call);
   PicotValueContext runctx = new PicotValueContext(initctx,callfrag);
   
   return runctx;
}



/********************************************************************************/
/*                                                                              */
/*      Support methods                                                         */
/*                                                                              */
/********************************************************************************/

RootProblem getProblem()
{
   return for_problem;
}



RootControl getRootControl()
{
   return root_control;
}



void loadFilesIntoFait()
{
   String tid = IvyXml.getAttrString(test_description,"THREAD");
   Element fileelt = IvyXml.getChild(test_description,"FILES");
   try {
      root_control.loadFilesIntoFait(tid,fileelt,true);
    }
   catch (RoseException e) {
      RoseLog.logE("PICOT","Problem finding fait files",e);
      return;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Compute start time (after initializations)                              */
/*                                                                              */
/********************************************************************************/

long getStartTime(RootValidate rv)
{
   RootTrace rt = rv.getExecutionTrace();
   RootTraceCall rtc = rt.getRootContext();
   
   long start = rtc.getStartTime();
   
   MethodDeclaration md = getMethod(rtc);
   if (md != null) {
      RootTraceVariable lns = rtc.getLineNumbers();
      Block blk = md.getBody();
      if (blk.statements().size() > 0) {
         Statement s0 = (Statement) blk.statements().get(0);
         CompilationUnit cu = (CompilationUnit) blk.getRoot();
         int ln1 = cu.getLineNumber(s0.getStartPosition());
         for (RootTraceValue rtv : lns.getTraceValues(rt)) {
            int ln2 = rtv.getLineValue();
            if (ln2 == ln1) {
               start = rtv.getStartTime();
               break;
             }
          }
       }
    }
   
   
   return start;
}


private MethodDeclaration getMethod(RootTraceCall rtc)
{
   File f = rtc.getFile();
   RootTraceVariable lns = rtc.getLineNumbers();
   int lno = lns.getLineAtTime(rtc.getStartTime());
   ASTNode n0 = root_control.getSourceNode(null,f,-1,lno,true,false);
   MethodDeclaration md = null;
   for (ASTNode p = n0; p != null; p = p.getParent()) {
      if (p instanceof MethodDeclaration) {
         md = (MethodDeclaration) p;
         break;
       }
    }
   
   return md;
}



/********************************************************************************/
/*                                                                              */
/*      Validate the test                                                       */
/*                                                                              */
/********************************************************************************/

private PicotTestStatus validateTest(RootValidate rv,RootTrace testtrace)
{
   boolean fg = rv.checkTestResult(testtrace);
   if (!fg) return PicotTestStatus.FAIL;
   
   return PicotTestStatus.SUCCESS;
}



/********************************************************************************/
/*                                                                              */
/*      Find imports and replace names                                          */
/*                                                                              */
/********************************************************************************/

private Set<String> fixImports(PicotTestCase tc)
{
   String src = tc.getRunCode().getCode();
   CompilationUnit cu = root_control.compileSource(at_location,src);
   CheckImportVisitor civ = new CheckImportVisitor();
   cu.accept(civ);
   String rslt = civ.doRewrite(src,cu);
   if (rslt != null) tc.updateRunCode(rslt);
   
   return civ.getImports();
}






private static class CheckImportVisitor extends ASTVisitor {
   
   private Set<String> import_set;
   private Set<ASTNode> to_fix;
   private String package_name;
   private Set<String> used_imports;
   
   CheckImportVisitor() {
      import_set = new HashSet<>();
      to_fix = new HashSet<>();
      used_imports = new LinkedHashSet<>();
      package_name = null;
    }
   
   String doRewrite(String src,CompilationUnit cu) {
      ASTRewrite rw = ASTRewrite.create(cu.getAST());
      for (ASTNode n : to_fix) {
         switch (n.getNodeType()) {
            case ASTNode.QUALIFIED_NAME :
               rewriteQualifiedName((QualifiedName) n,rw);
               break;
            case ASTNode.QUALIFIED_TYPE :
               rewriteQualifiedType((QualifiedType) n,rw);
               break;
          }
       }
      if (used_imports != null) {
         ListRewrite lrw = rw.getListRewrite(cu,CompilationUnit.IMPORTS_PROPERTY);
         for (Object o : cu.imports()) {
            ASTNode n = (ASTNode) o;
            lrw.remove(n,null);
          }
         AST ast = cu.getAST();
         for (String s : used_imports) {
            Name nm = JcompAst.getQualifiedName(ast,s);
            ImportDeclaration id = ast.newImportDeclaration();
            id.setName(nm);
            id.setOnDemand(false);
            id.setStatic(false);
            lrw.insertLast(id,null);
          }
       }
      try {
         IDocument doc = new Document(src);
         TextEdit te = rw.rewriteAST(doc,null);
         te.apply(doc);
         return doc.get();
         // apply te to cu.toString()
         // return the result
       }
      catch (Exception e) { 
         return null;
       }
    }
   
   Set<String> getImports()             { return used_imports; }
   
   private void rewriteQualifiedName(QualifiedName qn,ASTRewrite rw) {
      String nm = qn.getFullyQualifiedName();
      int idx = nm.lastIndexOf(".");
      String pfx = nm.substring(0,idx);
      String sfx = nm.substring(idx+1);
      if (import_set.contains(nm) || pfx.equals(package_name) || pfx.equals("java.lang")) {
         if (import_set.contains(nm)) used_imports.add(nm);
         ASTNode an = JcompAst.getSimpleName(qn.getAST(),sfx);
         rw.replace(qn,an,null);
       }
   }
   
   private void rewriteQualifiedType(QualifiedType qt,ASTRewrite rw) {
      
   }
   
   @Override public boolean visit(QualifiedName qn) {
      // get type name and add to import set
      String qnm = qn.getFullyQualifiedName();
      int idx = qnm.lastIndexOf(".");
      String pfx = qnm.substring(0,idx);
      JcompType jt = JcompAst.getJavaType(qn);
      if (jt != null && jt.getName().equals(qnm)) {
         if (!pfx.equals("java.lang") && !pfx.equals(package_name)) import_set.add(qnm);
         to_fix.add(qn);
         return false;
       }
      if (package_name != null && pfx.equals(package_name)) {
         to_fix.add(qn);
         return false;
       }
      return true;
    }
   
   @Override public boolean visit(QualifiedType qt) {
      String typ = qt.getQualifier().toString();
      typ += "." + qt.getName().getFullyQualifiedName();
      to_fix.add(qt);
      import_set.add(typ);
      return false;
    }
   
   @Override public boolean visit(PackageDeclaration pd) {
      package_name = pd.getName().getFullyQualifiedName();
      return false;
    }
   
   @Override public boolean visit(ImportDeclaration id) {
      String nm = id.getName().getFullyQualifiedName();
      if (!id.isOnDemand()) import_set.add(nm);
      return false;
    }
   
}       // end of inner class CheckImportVisitor




/********************************************************************************/
/*                                                                              */
/*      Test Case representation                                                */
/*                                                                              */
/********************************************************************************/

private static class TestCase implements PicotTestCase {
   
   private PicotTestStatus test_status;
   private PicotCodeFragment run_code;
   private String package_name;
   private String class_name;
   private String test_method;
   private String test_project;
   
   TestCase(PicotTestStatus sts,PicotValueContext ctx) {
      test_status = sts;
      run_code = ctx.getCode();
      package_name = ctx.getPackageName();
      class_name = ctx.getTestClassName();
      test_method = ctx.getTestMethodName();
      test_project = ctx.getTestProject();
    }
   
   TestCase(PicotTestStatus sts) {
      test_status = sts;
      run_code = null;
      package_name = null;
      class_name = null;
      test_project = null;
      test_method = null;
    }
   
   @Override public PicotTestStatus getStatus()         { return test_status; }
   @Override public PicotCodeFragment getRunCode()      { return run_code; }
   
   @Override public String getTestCode()
   {
      String code = run_code.getCode();
      int start = code.indexOf(TEST_START);
      start += TEST_START.length();
      int end = code.indexOf(TEST_END);
      return code.substring(start,end);
   }
   @Override public String getTestClassName()           { return class_name; }
   @Override public String getTestPackageName()         { return package_name; }
   @Override public String getTestMethodName()          { return test_method; }
   @Override public String getTestProject()             { return test_project; }
   
   @Override public void updateRunCode(String code) {
      run_code = new PicotCodeFragment(code);
    }
   
}

}       // end of class PicotTestCreator




/* end of PicotTestCreator.java */

