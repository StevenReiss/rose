/********************************************************************************/
/*										*/
/*		RoseEvalBase.java						*/
/*										*/
/*	Common code for doing evaluations					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.rose.roseeval;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.leash.LeashIndex;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractFactory;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootTestCase;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.stem.StemMain;


abstract class RoseEvalBase implements RoseEvalConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private PrintWriter  output_file;

private static String		stopped_thread = null;
private static String		end_process = null;

private static Map<String,SuggestionSet> suggest_set = new HashMap<>();
private static Map<String,RoseEvalTestResult> testresult_set = new HashMap<>();
private static Map<String,String> workspace_path = new HashMap<>();

private static Map<String,MintControl> mint_map = new HashMap<>();
private static Map<String,Integer>     mint_count = new HashMap<>();
private static String		source_id;
private static Random		random_gen = new Random();

protected static boolean	run_local = false;
protected static boolean	run_debug = false;
protected static boolean	seede_debug = false;

static {
   int rint = random_gen.nextInt(1000000);
   source_id = "STEM_" + rint;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected RoseEvalBase()
{
   output_file = null;
}



/********************************************************************************/
/*										*/
/*	TEST RUNNER								*/
/*										*/
/********************************************************************************/

protected void startEvaluations(String workspace,String project)
{
   setupBedrock(workspace,project);

   Date d = new Date();

   DateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmssSSS");
   String fnm = "ROSEEVAL_" + workspace + "_" + fmt.format(d) + ".csv";
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,"RoseEval");
   f2.mkdir();
   File f3 = new File(f2,fnm);
   try {
      output_file = new PrintWriter(f3);
      output_file.println("Name,# Results,# Displayed,Correct Rank,Total Time,Fix Time,Fix Count,Seede Count,# Checked");
    }
   catch (IOException e) {
      System.err.println("Can't create " + fnm);
      System.exit(1);
    }
}



protected void runEvaluation(RoseEvalTest test)
{
   RoseEvalSuite suite = test.getSuite();
   MintControl mc = setupBedrock(suite.getWorkspace(),suite.getProject());

   RoseEvalFrameData fd = setupTest(suite.getWorkspace(),suite.getProject(),
	 test.getName(),test.getSkipTimes(),test.getUseFiles());

   try {
      String cnts = test.getProblem().getDescription(fd);

      System.err.println("START " + test.getName());

      long starttime = System.currentTimeMillis();
      switch (test.getTestType()) {
	 case ROSE :
	    SuggestionSet ss = runTest(mc,fd,cnts,test);
	    long time = System.currentTimeMillis() - starttime;
	    processSuggestions(test.getName(),ss,test.getSolution(),time);
	    break;
	 case PICOT :
	    RoseEvalTestResult tc = runPicotTest(mc,fd,cnts,test);
	    long timep = System.currentTimeMillis() - starttime;
	    processTestCase(test.getName(),tc,timep);
	    break;
       }

    }
   catch (Throwable t) {
      RoseLog.logE("ROSEEVAL","Problem processing evaluation test",t);
    }
   finally {
      finishTest(fd);
      shutdownBedrock(suite.getWorkspace(),suite.getProject());
    }
}



protected void finishEvaluations(String workspace,String project)
{
   if (output_file != null) {
      output_file.close();
      output_file = null;
    }

   shutdownBedrock(workspace,project);
}



/********************************************************************************/
/*										*/
/*	Test Helper methods							*/
/*										*/
/********************************************************************************/

private RoseEvalFrameData setupTest(String workspace,String project,
      String launch,int cont,List<String> files)
{
   MintControl ctrl = mint_map.get(workspace);
   LaunchData ld = startLaunch(ctrl,project,launch);
   for (int i = 0; i < cont; ++i) {
      continueLaunch(ld);
    }
   RoseEvalFrameData fd = getTopFrame(ctrl,project,ld);
   setupStem(workspace);

   CommandArgs args = new CommandArgs("THREAD",ld.getThreadId(),
	 "LAUNCH",ld.getLaunchId());
   String cnts = null;
   if (files != null && !files.isEmpty()) {
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("FILES");
      for (String f : files) {
	 xw.begin("FILE");
	 xw.field("NAME",f);
	 xw.end("FILE");
       }
      xw.end("FILES");
      cnts = xw.toString();
      xw.close();
    }

   Element xml = sendStemReply(ctrl,"START",args,cnts);

   assert IvyXml.isElement(xml,"RESULT");

   return fd;
}




private SuggestionSet runTest(MintControl ctrl,RoseEvalFrameData fd,String oprob,
      RoseEvalTest test)
{
   boolean usecur = test.getUpFrames() == 0;
   long max = test.getTime();
   String startloc = (usecur ? "*" : null);
   String prob = getStartFrame(ctrl,oprob,startloc,max);

   SuggestionSet ss = getSuggestionsFor(ctrl,fd,prob);

   sendStemReply(ctrl,"FINISHED",null,null);

   return ss;
}


private RoseEvalTestResult runPicotTest(MintControl ctrl,RoseEvalFrameData fd,String prob,
      RoseEvalTest test)
{
   String id = "PICOT_" + source_id + "_" + random_gen.nextInt(100000);  CommandArgs args = new CommandArgs("REPLYID",id);
   if (test.getUpFrames() >= 0) args.put("UPFRAMES",test.getUpFrames());

   RoseEvalTestResult tr = new RoseEvalTestResult();
   testresult_set.put(id,tr);
	
   Element xml = sendStemReply(ctrl,"CREATETEST",args,prob);
   assert IvyXml.isElement(xml,"RESULT");

   tr.waitForResult();
   testresult_set.remove(id);

   return tr;
}

private void finishTest(RoseEvalFrameData fd)
{
   finishLaunch(fd);
}





protected void getChangedVariables(MintControl ctrl,RoseEvalFrameData fd,String prob)
{
   CommandArgs args = new CommandArgs("TYPE","EXCEPTION",
	 "METHOD",fd.getMethod(),
	 "LINE",fd.getLine(),
	 "CLASS",fd.getClassName(),
	 "FILE",fd.getSourceFile(ctrl),
	 "PROJECT",fd.getProject(ctrl),
	 "LAUNCH",fd.getLaunchId(),
	 "FRAME",fd.getId(),
	 "THREAD",fd.getThreadId() );
   Element xml = sendStemReply(ctrl,"CHANGEDITEMS",args,prob);
   assert IvyXml.isElement(xml,"RESULT");

   String pvals = null;
   for (Element cve : IvyXml.children(xml,"VARIABLE")) {
      String typ = IvyXml.getAttrString(cve,"TYPE");
      String nam = IvyXml.getAttrString(cve,"NAME");
      if (typ == null || nam == null) continue;
      switch (typ) {
	 case "PARAMETER" :
	    String pt = "<PARAMETER NAME='" + nam + "'/>";
	    if (pvals == null) pvals = pt;
	    else pvals += pt;
	    break;
       }
    }

   if (pvals != null) {
      args = new CommandArgs("METHOD",fd.getMethod(),
	    "CLASS",fd.getClassName(),
	    "LINE",fd.getLine(),
	    "FILE",fd.getSourceFile(ctrl),
	    "PROJECT",fd.getProject(ctrl),
	    "FRAME",fd.getId(),
	    "THREAD",fd.getThreadId());
      Element pxml = sendStemReply(ctrl,"PARAMETERVALUES",args,pvals);
      assert IvyXml.isElement(pxml,"RESULT");
    }
}


private String getStartFrame(MintControl ctrl,String prob,String loc,long max)
{
   CommandArgs args = new CommandArgs();
   String cnts = prob;
   if (loc != null && loc.equals("*")) {
      loc = null;
      args.put("CURRENT",true);
    }
   if (loc != null) cnts += loc;
   Element rslt = sendStemReply(ctrl,"STARTFRAME",args,cnts);
   String startframe = IvyXml.getAttrString(rslt,"STARTFRAME");
   String startrtn = IvyXml.getAttrString(rslt,"CLASS");
   startrtn += "." + IvyXml.getAttrString(rslt,"METHOD");
   startrtn += IvyXml.getAttrString(rslt,"SIGNATURE");
   RootTestCase rtc = new RootTestCase(startframe,startrtn);
   rtc.setMaxTime(max);
   Element xml = IvyXml.convertStringToXml(prob);
   RootProblem rp = BractFactory.getFactory().createProblemDescription(null,xml);
   rp.setCurrentTest(rtc);
   IvyXmlWriter xw = new IvyXmlWriter();
   rp.outputXml(xw);
   String nprob = xw.toString();
   xw.close();

   return nprob;
}


private SuggestionSet getSuggestionsFor(MintControl ctrl,RoseEvalFrameData fd,String prob)
{
   String id = "SUGGEST_" + source_id + "_" + random_gen.nextInt(100000);
   CommandArgs args = new CommandArgs("NAME",id);

   String cnts = prob;

   SuggestionSet ss = new SuggestionSet();
   suggest_set.put(id,ss);

   Element xml = sendStemReply(ctrl,"SUGGEST",args,cnts);
   assert IvyXml.isElement(xml,"RESULT");
   List<RoseEvalSuggestion> rslt = ss.getSuggestions();
   assert rslt != null;

   suggest_set.remove(id);

   return ss;
}




private void processSuggestions(String name,SuggestionSet ss,RoseEvalSolution sol,long time)
{
   if (sol == null) return;

   int ctr = 0;
   int showctr = 0;
   int fnd = -1;
   long fixtime = 0;
   int fixcnt = 0;
   long fixseede = 0;
   double max = 0;
   for (RoseEvalSuggestion sug : ss.getSuggestions()) {
      if (max == 0) max = sug.getPriority()*0.1;
      if (fnd < 0) {
	 System.err.println("CHECK " + sug.getPriority() + " " + sug.getLine() + " " + sug.getDescription() +
	       " " + sug.getTime() + " " + sug.getCount());
       }
      if (sol.match(sug) && fnd < 0) {
	 fnd = ctr+1;
	 fixtime = sug.getTime();
	 fixcnt = sug.getCount();
	 fixseede = sug.getSeedeCount();
       }
      ++ctr;
      if (sug.getPriority() >= max) ++showctr;
    }

   int ct = ss.getNumChecked();

   if (output_file != null) {
      output_file.println(name + "," + ctr + "," + showctr + "," + fnd + "," +
	    time + "," + fixtime + "," + fixcnt + "," + fixseede + "," + ct);
    }

   System.err.println("PROCESS SUGGESTIONS: " + name +": " + ctr + " " + showctr + " " +
	 fnd + " " + time + " " + fixtime + " " + fixcnt + " " + fixseede + " " + ct);
}



private void processTestCase(String name,RoseEvalTestResult tc,long time)
{
   if (output_file != null) {
      output_file.println(name + "," + (tc != null) + "," + time);
    }
   System.err.println("PROCESS TEST CASE: " + name + ": " + tc + " " + time);
}




/********************************************************************************/
/*										*/
/*	Stem setup methods							*/
/*										*/
/********************************************************************************/

private void setupStem(String workspace)
{
   MintControl mc = mint_map.get(workspace);

   Element rply = sendStemReply(mc,"PING",null,null);
   if (rply != null) return;

   List<String> args = new ArrayList<>();
   args.add("-m");
   args.add(mc.getMintName());
   args.add("-w");
   args.add(workspace_path.get(workspace));
   if (seede_debug) args.add("-DD");
   else if (run_debug) args.add("-D");
   else args.add("-NoD");
   if (run_local) args.add("-local");


   if (run_debug || seede_debug || run_local) {
      String [] argarr = new String[args.size()];
      argarr = args.toArray(argarr);
      StemMain.main(argarr);
      for (int i = 0; i < 100; ++i) {
	 Element ping = sendStemReply(mc,"PING",null,null);
	 if (ping != null) break;
	 try {
	    Thread.sleep(2000);
	  }
	 catch (InterruptedException e) { }
       }
    }
   else {
      List<String> callargs = new ArrayList<>();
      callargs.add(IvyExecQuery.getJavaPath());
      callargs.add("-cp");
      callargs.add(System.getProperty("java.class.path"));
      callargs.add("edu.brown.cs.rose.stem.StemMain");
      callargs.addAll(args);
      IvyExec exec = null;
      for (int i = 0; i < 100; ++i) {
	 Element ping = sendStemReply(mc,"PING",null,null);
	 if (ping != null) break;
	 if (i == 0) {
	    try {
	       exec = new IvyExec(callargs,null,IvyExec.ERROR_OUTPUT);
	       RoseLog.logD("EVAL","Run " + exec.getCommand());
	     }
	    catch (IOException e) {
	       RoseLog.logE("EVAL","Problem running STEM",e);
	       System.exit(1);
	     }
	  }
	 try {
	    Thread.sleep(2000);
	  }
	 catch (InterruptedException e) { }
       }
    }

   mc.register("<ROSEREPLY DO='_VAR_0' />",new SuggestHandler());
}



/********************************************************************************/
/*										*/
/*	Handle sending messages to STEM 					*/
/*										*/
/********************************************************************************/

private Element sendStemReply(MintControl mc,String cmd,CommandArgs args,String xml)
{
   MintDefaultReply rply = new MintDefaultReply();
   sendStem(mc,cmd,args,xml,rply);
   Element rslt = rply.waitForXml();
   RoseLog.logD("STEM REPLY: " + IvyXml.convertXmlToString(rslt));
   return rslt;
}



private void sendStem(MintControl mc,String cmd,CommandArgs args,String xml,MintReply rply)
{
   IvyXmlWriter msg = new IvyXmlWriter();
   msg.begin("ROSE");
   msg.field("CMD",cmd);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 if (ent.getValue() == null) continue;
	 msg.field(ent.getKey(),ent.getValue().toString());
       }
    }
   if (xml != null) msg.xmlText(xml);
   msg.end("ROSE");
   String msgt = msg.toString();
   msg.close();

   if (rply == null) {
      mc.send(msgt,rply,MINT_MSG_NO_REPLY);
    }
   else {
      mc.send(msgt,rply,MINT_MSG_FIRST_NON_NULL);
    }
}



protected void stopSeede(MintControl mc)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("SEEDE");
   xw.field("DO","EXIT");
   xw.field("SID","*");
   xw.end("SEEDE");
   String msg = xw.toString();
   xw.close();

   RoseLog.logD("STEM","Send to SEEDE: " + msg);

   mc.send(msg);
}



/********************************************************************************/
/*										*/
/*	Bedrock setup / shutdown methods					*/
/*										*/
/********************************************************************************/

private MintControl setupBedrock(String workspace,String project)
{
   MintControl mc = mint_map.get(workspace);
   if (mc == null) {
      int rint = random_gen.nextInt(1000000);
      String mint = "STEM_TEST_" + workspace.toUpperCase() + "_" + rint;
      mc = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
      mint_map.put(workspace,mc);
      mint_count.put(workspace,1);
      mc.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new IDEHandler());
    }
   else {
      int ct = mint_count.get(workspace);
      mint_count.put(workspace,ct+1);
    }

   RoseLog.logI("STEM","SETTING UP BEDROCK");
   File ec1 = new File("/u/spr/eclipse-oxygenx/eclipse/eclipse");
   File ec2 = new File("/u/spr/Eclipse/" + workspace);
   if (!ec1.exists()) {
      ec1 = new File(ECLIPSE_PATH_MAC);
      ec2 = new File(ECLIPSE_DIR_MAC + workspace);
    }
   if (!ec1.exists()) {
      System.err.println("Can't find bubbles version of eclipse to run");
      System.exit(1);
    }
   String path = ec2.getAbsolutePath();
   workspace_path.put(workspace,path);

   String cmd = ec1.getAbsolutePath();
   cmd += " -application edu.brown.cs.bubbles.bedrock.application";
   cmd += " -data " + ec2.getAbsolutePath();
// cmd += " -bhide";
   cmd += " -nosplash";
   cmd += " -vmargs -Dedu.brown.cs.bubbles.MINT=" + mc.getMintName();
   cmd += " -vmargs -XstartOnFirstThread";

   try {
      for (int i = 0; i < 250; ++i) {
	 if (pingEclipse(mc)) {
	    CommandArgs args = new CommandArgs("LEVEL","DEBUG");
	    sendBubblesXmlReply(mc,"ENTER",null,null,null);
	    sendBubblesXmlReply(mc,"LOGLEVEL",null,args,null);
	    sendBubblesXmlReply(mc,"GETHOST",null,null,null);
	    sendBubblesXmlReply(mc,"PREFERENCES",null,null,null);
	    sendBubblesXmlReply(mc,"PROJECTS",null,null,null);
	    args = new CommandArgs("WS",workspace);
	    sendBubblesXmlReply(mc,"PROJECTS",null,args,null);
	    args = new CommandArgs("PATHS",true);
	    args = new CommandArgs("REFRESH",false,"CLEAN",false,"FULL",false,"WS",workspace);
	    sendBubblesXmlReply(mc,"BUILDPROJECT",project,args,null);
	    args = new CommandArgs("PATHS",true);
	    Element pxml = sendBubblesXmlReply(mc,"OPENPROJECT",project,args,null);
	    if (!IvyXml.isElement(pxml,"PROJECT")) pxml = IvyXml.getChild(pxml,"PROJECT");
	    if (i != 0) checkProject(mc,pxml);
	    return mc;
	  }
	 if (i == 0) {
	    RoseLog.logI("STEM","RUN: " + cmd);
	    new IvyExec(cmd);
	  }
	 else {
	    try { Thread.sleep(100); } catch (InterruptedException e) { }
	  }
       }
    }
   catch (IOException e) { }

   throw new Error("Problem running Eclispe: " + cmd);
}



private void checkProject(MintControl mc,Element opxml)
{
   boolean havepoppy = false;

   String pnm = IvyXml.getAttrString(opxml,"NAME");
   Element cpe = IvyXml.getChild(opxml,"CLASSPATH");
   for (Element rpe : IvyXml.children(cpe,"PATH")) {
      String bn = null;
      String ptyp = IvyXml.getAttrString(rpe,"TYPE");
      if (ptyp != null && ptyp.equals("SOURCE")) {
	 bn = IvyXml.getTextElement(rpe,"OUTPUT");
       }
      else {
	 bn = IvyXml.getTextElement(rpe,"BINARY");
       }
      if (bn == null) continue;
      if (bn.contains("poppy.jar")) {
	 File bfn = new File(bn);
	 if (bfn.exists()) {
	    havepoppy = true;
	  }
       }
    }

   if (!havepoppy) {
      RoseLog.logD("ROSEEVAL","Add poppy to class path");
      File poppyjar = IvyFile.expandFile("$(PRO)/bubbles/lib/poppy.jar");
      if (poppyjar.exists()) {
	 IvyXmlWriter xwp = new IvyXmlWriter();
	 xwp.begin("PROJECT");
	 xwp.field("NAME",pnm);
	 xwp.begin("PATH");
	 xwp.field("TYPE","LIBRARY");
	 xwp.field("NEW",true);
	 xwp.field("BINARY",poppyjar.getPath());
	 xwp.field("EXPORTED",false);
	 xwp.field("OPTIONAL",true);
	 xwp.end("PATH");
	 xwp.end("PROJECT");
	 String cnts = xwp.toString();
	 xwp.close();
	 CommandArgs args = new CommandArgs("LOCAL",true);
	 sendBubblesMessage(mc,"EDITPROJECT",pnm,args,cnts);
       }
    }
}



private void shutdownBedrock(String workspace,String project)
{
   MintControl mc = mint_map.get(workspace);
   if (mc == null) return;
   int ctr = mint_count.get(workspace);
   if (ctr > 1) {
      mint_count.put(workspace,ctr-1);
      return;
    }
   else {
      mint_count.remove(workspace);
      mint_map.remove(workspace);
    }

   RoseLog.logI("STEM","Shut down bedrock");

   String path = workspace_path.get(workspace);
   File bdir = new File(path);
   File bbdir = new File(bdir,".bubbles");
   File cdir = new File(bbdir,"CockerIndex");
   LeashIndex idx = new LeashIndex(ROSE_PROJECT_INDEX_TYPE,cdir);
   idx.stop();

   if (!run_local) sendStem(mc,"EXIT",null,null,null);

   stopSeede(mc);

   sendBubblesMessage(mc,"EXIT");
}



/********************************************************************************/
/*										*/
/*	Handle launching execution						*/
/*										*/
/********************************************************************************/

private LaunchData startLaunch(MintControl mc,String proj,String name)
{
   stopped_thread = null;

   CommandArgs args = new CommandArgs("NAME",name,"MODE","debug","BUILD","TRUE",
	 "REGISTER","TRUE");
   MintDefaultReply rply = new MintDefaultReply();
   sendBubblesMessage(mc,"START",proj,args,null,rply);
   Element xml = rply.waitForXml();
   Element ldata = IvyXml.getChild(xml,"LAUNCH");
   assert ldata != null;
   String launchid = IvyXml.getAttrString(ldata,"ID");
   assert launchid != null;
   String targetid = IvyXml.getAttrString(ldata,"TARGET");
   assert targetid != null;
   String processid = IvyXml.getAttrString(ldata,"PROCESS");
   assert processid != null;
   String threadid = waitForStop();
   assert threadid != null;

   return new LaunchData(mc,proj,launchid,targetid,processid,threadid);
}


private void continueLaunch(LaunchData ld)
{
   stopped_thread = null;

   MintControl mc = ld.getMintControl();
   String project = ld.getProject();

   CommandArgs args = new CommandArgs("LAUNCH",ld.getLaunchId(),
	 "TARGET",ld.getTargetId(),
	 "PROCESS",ld.getProcessId(),"ACTION","RESUME");
   MintDefaultReply rply = new MintDefaultReply();
   sendBubblesMessage(mc,"DEBUGACTION",project,args,null,rply);
   String x = rply.waitForString();
   assert x != null;
   String threadid = waitForStop();
   assert threadid != null;

   ld.setThreadId(threadid);
}


private void finishLaunch(RoseEvalFrameData fd)
{
   stopped_thread = null;

   MintControl mc = fd.getMintControl();

   CommandArgs args = new CommandArgs("LAUNCH",fd.getLaunchId(),
	 "TARGET",fd.getTargetId(),
	 "PROCESS",fd.getProcessId(),"ACTION","TERMINATE");
   MintDefaultReply rply = new MintDefaultReply();
   sendBubblesMessage(mc,"DEBUGACTION",fd.getProject(),args,null,rply);
   String x = rply.waitForString();
   assert x != null;
   waitForTerminate();
}




private RoseEvalFrameData getTopFrame(MintControl mc,String project,LaunchData ld)
{
   List<RoseEvalFrameData> frames = getFrames(mc,project,ld);
   if (frames == null || frames.size() == 0) return null;
   for (RoseEvalFrameData fd : frames) {
      if (fd.isUserFrame()) return fd;
    }
   return null;
}



private List<RoseEvalFrameData> getFrames(MintControl mc,String project,LaunchData ld)
{
   List<RoseEvalFrameData> frames = new ArrayList<>();
   CommandArgs args = new CommandArgs("LAUNCH",ld.getLaunchId(),"THREAD",ld.getThreadId());
   Element frameresult = sendBubblesXmlReply(mc,"GETSTACKFRAMES",project,args,null);
   Element framesrslt = IvyXml.getChild(frameresult,"STACKFRAMES");
   for (Element thrslt : IvyXml.children(framesrslt,"THREAD")) {
      if (ld.getThreadId().equals(IvyXml.getAttrString(thrslt,"ID"))) {
	 for (Element frslt : IvyXml.children(thrslt,"STACKFRAME")) {
	    frames.add(new RoseEvalFrameData(ld,frslt));
	  }
       }
    }

   return frames;
}




/********************************************************************************/
/*										*/
/*	Handle bubbles messaging						*/
/*										*/
/********************************************************************************/

private static boolean pingEclipse(MintControl mc)
{
   MintDefaultReply mdr = new MintDefaultReply();
   sendBubblesMessage(mc,"PING",null,null,null,mdr);
   String r = mdr.waitForString(500);
   return r != null;
}


static Element sendBubblesXmlReply(MintControl mc,String cmd,String proj,Map<String,Object> flds,String cnts)
{
   MintDefaultReply mdr = new MintDefaultReply();
   sendBubblesMessage(mc,cmd,proj,flds,cnts,mdr);
   Element pxml = mdr.waitForXml();
   RoseLog.logD("STEM","RECEIVE from BUBBLES: " + IvyXml.convertXmlToString(pxml));
   return pxml;
}



private static void sendBubblesMessage(MintControl mc,String cmd)
{
   sendBubblesMessage(mc,cmd,null,null,null,null);
}


private static void sendBubblesMessage(MintControl mc,String cmd,String proj,Map<String,Object> flds,String cnts)
{
   sendBubblesMessage(mc,cmd,proj,flds,cnts,null);
}



private static void sendBubblesMessage(MintControl mc,String cmd,String proj,Map<String,Object> flds,String cnts,
      MintReply rply)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBBLES");
   xw.field("DO",cmd);
   xw.field("BID",source_id);
   if (proj != null && proj.length() > 0) xw.field("PROJECT",proj);
   if (flds != null) {
      for (Map.Entry<String,Object> ent : flds.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   xw.field("LANG","eclipse");
   if (cnts != null) xw.xmlText(cnts);
   xw.end("BUBBLES");

   String xml = xw.toString();
   xw.close();

   RoseLog.logD("STEM","SEND to BUBBLES: " + xml);

   int fgs = MINT_MSG_NO_REPLY;
   if (rply != null) fgs = MINT_MSG_FIRST_NON_NULL;
   mc.send(xml,rply,fgs);
}



/********************************************************************************/
/*										*/
/*	Monitor executions							*/
/*										*/
/********************************************************************************/

private class IDEHandler implements MintHandler {

@Override public void receive(MintMessage msg,MintArguments args) {
   String cmd = args.getArgument(0);
   Element e = msg.getXml();
   if (cmd == null) return;

   switch (cmd) {
      case "ELISIION" :
	 break;
      case "EDITERROR" :
	 break;
      case "FILEERROR" :
	 break;
      case "PRIVATEERROR" :
	 break;
      case "EDIT" :
	 break;
      case "BREAKEVENT" :
	 break;
      case "LAUNCHCONFIGEVENT" :
	 break;
      case "RUNEVENT" :
	 long when = IvyXml.getAttrLong(e,"TIME");
	 for (Element re : IvyXml.children(e,"RUNEVENT")) {
	    handleRunEvent(re,when);
	  }
	 msg.replyTo("<OK/>");
	 break;
      case "NAMES" :
      case "ENDNAMES" :
	 break;
      case "PING" :
	 msg.replyTo("<PONG/>");
	 break;
      case "PROGRESS" :
	 msg.replyTo("<OK/>");
	 break;
      case "RESOURCE" :
	 break;
      case "CONSOLE" :
	 msg.replyTo("<OK/>");
	 break;
      case "OPENEDITOR" :
	 break;
      case "EVALUATION" :
	 msg.replyTo("<OK/>");
	 break;
      case "BUILDDONE" :
      case "FILECHANGE" :
      case "PROJECTDATA" :
      case "PROJECTOPEN" :
	 break;
      case "STOP" :
	 break;
      default :
	 break;
    }
}

}	// end of innerclass IDEHandler



private void handleRunEvent(Element xml,long when)
{
   String type = IvyXml.getAttrString(xml,"TYPE");
   if (type == null) return;
   switch (type) {
      case "PROCESS" :
	 handleProcessEvent(xml,when);
	 break;
      case "THREAD" :
	 handleThreadEvent(xml,when);
	 break;
      case "TARGET" :
	 break;
      default :
	 break;
    }
}


private void handleThreadEvent(Element xml,long when)
{
   String kind = IvyXml.getAttrString(xml,"KIND");
   Element thread = IvyXml.getChild(xml,"THREAD");
   if (thread == null) return;
   switch (kind) {
      case "SUSPEND" :
	 synchronized (this) {
	    stopped_thread = IvyXml.getAttrString(thread,"ID");
	    notifyAll();
	  }
	 break;
    }
}



private void handleProcessEvent(Element xml,long when)
{
   String kind = IvyXml.getAttrString(xml,"KIND");
   Element process = IvyXml.getChild(xml,"PROCESS");
   if (process == null) return;
   switch (kind) {
      case "TERMINATE" :
	 synchronized (this) {
	    end_process = IvyXml.getAttrString(process,"PID");
	    notifyAll();
	  }
	 break;
    }
}



private String waitForStop()
{
   synchronized (this) {
      for (int i = 0; i < 100; ++i) {
	 if (stopped_thread != null) break;
	 try {
	    wait(3000);
	  }
	 catch (InterruptedException e) { }
       }
      String ret = stopped_thread;
      stopped_thread = null;
      return ret;
    }
}



private void waitForTerminate()
{
   synchronized (this) {
      for (int i = 0; i < 100; ++i) {
	 if (end_process != null) break;
	 try {
	    wait(3000);
	  }
	 catch (InterruptedException e) { }
       }
      end_process = null;
    }
}



/********************************************************************************/
/*										*/
/*	Monitor Suggestiongs							*/
/*										*/
/********************************************************************************/

private class SuggestHandler implements MintHandler {

@Override public void receive(MintMessage msg,MintArguments args) {
   String cmd = args.getArgument(0);
   Element e = msg.getXml();
   String name = IvyXml.getAttrString(e,"NAME");
   SuggestionSet ss = null;
   RoseEvalTestResult tr = null;
   if (name != null) {
      ss = suggest_set.get(name);
      tr = testresult_set.get(name);
    }
   switch (cmd) {
      case "SUGGEST" :
	 if (ss != null) ss.addSuggestion(e);
	 break;
      case "ENDSUGGEST" :
	 if (ss != null) ss.endSuggestions(IvyXml.getAttrInt(e,"CHECKED"));
	 break;
      case "TESTCREATE" :
	 if (tr != null) tr.handleTestResult(e);
	 break;
    }
   msg.replyTo("<OK/>");
}

}	// end of inner class SuggestHandler



private static class SuggestionSet {

   private List<Element> suggest_nodes;
   private boolean is_done;
   private int num_checked;

   SuggestionSet() {
      suggest_nodes = new ArrayList<>();
      is_done = false;
    }

   synchronized void addSuggestion(Element xml) {
      for (Element r : IvyXml.children(xml,"REPAIR")) {
	 suggest_nodes.add(r);
       }
    }

   synchronized void endSuggestions(int ct) {
      num_checked = ct;
      is_done = true;
      notifyAll();
    }

   synchronized List<RoseEvalSuggestion> getSuggestions() {
      while (!is_done) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      List<RoseEvalSuggestion> rslt = new ArrayList<>();
      for (Element e : suggest_nodes) {
         rslt.add(new RoseEvalSuggestion(e));
       }
      Collections.sort(rslt);
      return rslt;
    }

   int getNumChecked()		{ return num_checked; }

}	// end of inner class SuggestionSet




}	// end of class RoseEvalBase




/* end of RoseEvalBase.java */

