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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
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
      PicotTestCase test = createTestCase();
      if (test == null) args.put("STATUS","FAIL");
      else {
         args.put("STATUS",test.getStatus());
         if (test.getRunCode() != null) {
            IvyXmlWriter xw = new IvyXmlWriter();
            xw.begin("TESTCASE");
            xw.cdataElement("RUNCODE",test.getRunCode());
            xw.cdataElement("TESTCODE",test.getCode());
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
   
   ValidateFactory vf = ValidateFactory.getFactory(root_control);
   RootValidate rv = vf.createValidate(for_problem,bsf.getFrameId(),
         at_location,false,false,true);
   if (rv == null) return null;
   
   long start = getStartTime(rv);
   
   RootTrace rt = rv.getExecutionTrace();
   RootTraceCall rtc = rt.getRootContext();
   
   MethodDeclaration md = getMethod(rtc);
   if (md == null) return null;
   JcompTyper typer = JcompAst.getTyper(md);
   PicotValueBuilder pvb = new PicotValueBuilder(root_control,rv,start,typer);
   
   PicotValueContext runctx = buildCall(rt,rtc,pvb);
   
   TestCase rslt = null;
   if (runctx == null) {
      rslt = new TestCase(PicotTestStatus.FAIL,null,null);
    }
   else {
      RootTrace testtrace = runctx.getTrace();
      PicotTestStatus sts = validateTest(rv,testtrace);
      rslt = new TestCase(sts,runctx.getInitializationCode(),runctx.getCode());
    }
   
   pvb.finished();
   
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
      root_control.loadFilesIntoFait(tid,fileelt);
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
   RootTraceCall start0 = rv.getExecutionTrace().getRootContext();
   RootTraceCall ctx0 = rv.getExecutionTrace().getProblemContext();
   long t0 = rv.getExecutionTrace().getProblemTime();
   RootTraceValue exc0 = rv.getExecutionTrace().getException();
   RootProblem prob = rv.getProblem(); 
   
   RootTraceCall rtc = testtrace.getRootContext();
   RootTraceCall start1 = null;
   for (RootTraceCall rtc1 : rtc.getInnerTraceCalls()) {
      if (rtc1.getMethod().equals(start0.getMethod())) {
         start1 = rtc1;
         break;
       }
    }
   RootTraceValue exc1 = testtrace.getException();
   if (start1 == null) return PicotTestStatus.FAIL;
   
   if (exc0 != null) {
      if (exc1 == null) return PicotTestStatus.NO_DUP;
      else if (!exc0.getDataType().equals(exc1.getDataType())) return PicotTestStatus.NO_DUP;
    }
   
   // create a RootValidateMatch as interface to ValidateMatcher
   // create a method in RootValidate to return a ValidateMatch for a trace/context to match
   
   // match test trace with the execution trace 
   
   return PicotTestStatus.SUCCESS;
}



/********************************************************************************/
/*                                                                              */
/*      Test Case representation                                                */
/*                                                                              */
/********************************************************************************/

private static class TestCase implements PicotTestCase {
   
   private PicotTestStatus test_status;
   private PicotCodeFragment test_code;
   private PicotCodeFragment run_code;
   
   TestCase(PicotTestStatus sts,PicotCodeFragment tc,PicotCodeFragment rc) {
      test_status = sts;
      test_code = tc;
      run_code = rc;
    }
   
   @Override public PicotTestStatus getStatus()         { return test_status; }
   @Override public PicotCodeFragment getCode()         { return test_code; }
   @Override public PicotCodeFragment getRunCode()      { return run_code; }
   
}

}       // end of class PicotTestCreator




/* end of PicotTestCreator.java */

