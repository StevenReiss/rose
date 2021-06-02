/********************************************************************************/
/*										*/
/*		StemMain.java							*/
/*										*/
/*	Main program for ROSE monitor and activities				*/
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



package edu.brown.cs.rose.stem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcomp.JcompAnnotation;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.leash.LeashIndex;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bract.BractFactory;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootMetrics;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.validate.ValidateFactory;

public class StemMain implements StemConstants, MintConstants, RootControl
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String ... args)
{
   StemMain sm = new StemMain(args);
   sm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum AnalysisState { NONE, PENDING, READY };

private MintControl	mint_control;
private String		workspace_path;
private boolean 	fait_started;
private boolean 	fait_running;
private boolean         seede_started;
private boolean         seede_running;
private String		session_id;
private Element 	last_analysis;
private boolean 	is_done;
private Set<File>	added_files;
private AnalysisState	analysis_state;
private boolean 	local_fait;
private boolean         local_seede;
private Map<String,EvalData> eval_handlers;
private StemCompiler    stem_compiler;
private Map<File,String> project_map;
private String          default_project;
private LeashIndex      project_index;
private LeashIndex      global_index;
private Set<File>       loaded_files;
private boolean         run_debug;


private static boolean	use_all_files = true;
// only loaded files will be reported

private static AtomicInteger id_counter = new AtomicInteger((int)(Math.random()*256000.0));




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private StemMain(String [] args)
{
   RoseLog.setup();

   mint_control = null;
   workspace_path = System.getProperty("user.home");
   fait_started = false;
   fait_running = false;
   seede_started = false;
   seede_running = false;
   session_id = null;
   is_done = false;
   local_fait = false;
   local_seede = false;
   session_id = "ROSE_" + IvyExecQuery.getProcessId() + "_" + id_counter.incrementAndGet();
   added_files = new HashSet<>();
   analysis_state = AnalysisState.NONE;
   eval_handlers = new HashMap<>();
   stem_compiler = null;
   loaded_files = new HashSet<>();
   run_debug = true;
   scanArgs(args);
}



/********************************************************************************/
/*										*/
/*	Argument parsing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {           // -m <msgid>
	    String mid = args[++i];
	    mint_control = edu.brown.cs.ivy.mint.MintControl.create(mid,
		  MintSyncMode.ONLY_REPLIES);
	  }
	 else if (args[i].startsWith("-w") && i+1 < args.length) {      // -w <workspace>
	    workspace_path = args[++i];
	  }
	 else if (args[i].startsWith("-L") && i+1 < args.length) {      // -L <logfile>
	    RoseLog.setLogFile(args[++i]);
	  }
	 else if (args[i].startsWith("-D")) {                           // -Debug
	    RoseLog.setLogLevel(RoseLog.LogLevel.DEBUG);
	  }
         else if (args[i].startsWith("-NoD")) {                         // -NoDebug
	    RoseLog.setLogLevel(RoseLog.LogLevel.INFO);
            run_debug = false;
	  }
         else if (args[i].startsWith("-NoS")) {                         // -NoSeedeDebug
            run_debug = false;
          }
	 else if (args[i].startsWith("-local")) {                       // -local
	    local_fait = true;
	  }
         else if (args[i].startsWith("-lseede")) {                       // -local seede
	    local_seede = true;
	  }
	 else badArgs();
       }
      else badArgs();
    }

   if (mint_control == null) badArgs();
}


private void badArgs()
{
   System.err.println("ROSE/STEM -m <message id>");
   System.exit(1);
}


/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   mint_control.register("<ROSE CMD='_VAR_0' />",new CommandHandler());
   mint_control.register("<FAITEXEC TYPE='_VAR_0' />",new UpdateHandler());
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='_VAR_0' />",new BubblesHandler());
   
   buildProjectMap();

   new WaitForExit().start();
}



/********************************************************************************/
/*										*/
/*	START: Ensure FAIT is running						*/
/*										*/
/********************************************************************************/

private void handleStartCommand(MintMessage msg) throws RoseException
{
   boolean sts = startFaitProcess();
   RoseLog.logD("STEM","START FAIT returned " + sts);
   if (!fait_running) return;
 
   Element rslt = sendFaitMessage("BEGIN",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) {
      analysis_state = AnalysisState.NONE;
      sts = false;
    }
   Element sess = IvyXml.getChild(rslt,"SESSION");
   String sid = IvyXml.getAttrString(sess,"ID");
   if (sid != null) session_id = sid;

   loadFilesIntoFait(null);

   if (sts) {
      analysis_state = AnalysisState.PENDING;
      msg.replyTo("<RESULT VALUE='true'/>");
    }
   else msg.replyTo("<RESULT VALUE='false' />");

   CommandArgs args = new CommandArgs("REPORT","SOURCE");
   BoardProperties props = BoardProperties.getProperties("Rose");
   int nth = props.getInt("Rose.fait.threads");
   if (nth > 0) args.put("THREADS",nth);
   rslt = sendFaitMessage("ANALYZE",args,null);
   if (!IvyXml.isElement(rslt,"RESULT")) {
      analysis_state = AnalysisState.NONE;
      throw new RoseException("ANALYZE for session failed");
    }
   
   sts = startSeede();
   RoseLog.logD("STEM","START SEEDE returned " + sts);
}									



private boolean startFaitProcess()
{
   RoseLog.logD("STEM","START FAIT " + fait_running + " " + fait_started);
   
   if (fait_running || fait_started) return false;      // quick, informal check
   
   IvyExec exec = null;
   File wd = new File(workspace_path);
   File logf = new File(wd,"fait.log");
   
   BoardProperties bp = BoardProperties.getProperties("Rose");
   String dbgargs = bp.getProperty("Rose.fait.jvm.args");
   List<String> args = new ArrayList<>();
   args.add(IvyExecQuery.getJavaPath());

   if (dbgargs != null && dbgargs.contains("###")) {
      int port = (int)(Math.random() * 1000 + 3000);
      RoseLog.logI("STEM","Fait debugging port " + port);
      dbgargs = dbgargs.replace("###",Integer.toString(port));
    }

   if (dbgargs != null) {
      StringTokenizer tok = new StringTokenizer(dbgargs);
      while (tok.hasMoreTokens()) {
	 args.add(tok.nextToken());
       }
    }

   BoardSetup setup = BoardSetup.getSetup();

   File f1 = setup.getRootDirectory();
   File f2 = new File(f1,"dropins");
   File faitjar = new File(f2,"fait.jar");
   File fjar = IvyFile.getJarFile(StemMain.class);
   if (fjar == null || fjar.getName().endsWith(".class")) {
      File f3 = new File("/Users/spr/Eclipse/fait/fait/bin");
      if (!f3.exists()) f3 = new File("/pro/fait/java");
      if (!f3.exists()) f3 = new File("/research/people/spr/fait/java");
      if (f3.exists()) faitjar = f3;
    }

   args.add("-cp");
   String xcp = bp.getProperty("Rose.fait.class.path");
   if (xcp == null) {
      xcp = System.getProperty("java.class.path");
      String ycp = bp.getProperty("Rose.fait.add.path");
      if (ycp != null) xcp = ycp + File.pathSeparator + xcp;
    }
   else {
      StringBuffer buf = new StringBuffer();
      StringTokenizer tok = new StringTokenizer(xcp,":;");
      while (tok.hasMoreTokens()) {
	 String elt = tok.nextToken();
	 if (!elt.startsWith("/") &&  !elt.startsWith("\\")) {
	    if (elt.equals("eclipsejar")) {
	       elt = setup.getEclipsePath();
	     }
	    else if (elt.equals("fait.jar") && faitjar != null) {
	       elt = faitjar.getPath();
	     }
	    else {
	       elt = setup.getLibraryPath(elt);
	     }
	  }
	 if (buf.length() > 0) buf.append(File.pathSeparator);
	 buf.append(elt);
       }
      xcp = buf.toString();
    }

   args.add(xcp);
   args.add("edu.brown.cs.fait.iface.FaitMain");
   args.add("-m");
   args.add(mint_control.getMintName());
   args.add("-L");
   args.add(logf.getPath());
   if (bp.getBoolean("Rose.fait.debug")) {
      args.add("-D");
      if (bp.getBoolean("Rose.fait.trace")) args.add("-T");
    }

   synchronized (this) {
      if (fait_started || fait_running) return false;
      fait_started = true;
    }

   boolean isnew = false;
   for (int i = 0; i < 100; ++i) {
      MintDefaultReply rply = new MintDefaultReply();
      mint_control.send("<FAIT DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String rslt = rply.waitForString(1000);
      if (rslt != null) {
	 fait_running = true;
	 break;
       }
      if (i == 0) {
	 if (local_fait) {
	    List<String> fargs = new ArrayList<>();
	    boolean use = false;
	    for (String s : args) {
	       if (s.equals("edu.brown.cs.fait.iface.FaitMain")) use = true;
	       else if (use) fargs.add(s);
	     }
	    String [] fargarr = new String[fargs.size()];
	    fargarr = fargs.toArray(fargarr);
	    try {
	       Class<?> faitcls = Class.forName("edu.brown.cs.fait.iface.FaitMain");
	       Method m = faitcls.getMethod("main",fargarr.getClass());
	       m.invoke(null,new Object [] { fargarr });
	     }
	    catch (Throwable t) {
	       RoseLog.logE("STEM","Couldn't start fait locally",t);
	       break;
	     }
	  }
	 else {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);     // make IGNORE_OUTPUT to clean up otuput
	       isnew = true;
               fait_started = true;
	       RoseLog.logD("STEM","Run " + exec.getCommand());
	     }
	    catch (IOException e) {
	       break;
	     }
	  }
       }
      else {
	 try {
	    if (exec != null && !local_fait) {
	       int sts = exec.exitValue();
	       RoseLog.logD("STEM","Fait server disappeared with status " + sts);
	       break;
	     }
	  }
	 catch (IllegalThreadStateException e) { }
       }

      try {
	 Thread.sleep(2000);
       }
      catch (InterruptedException e) { }
    }
   if (!fait_running) {
      RoseLog.logE("STEM","Unable to start fait server: " + args);
      return true;
    }

   return isnew;
}



/********************************************************************************/
/*										*/
/*	Handle expressions command to get local expressions			*/
/*										*/
/********************************************************************************/

private void handleExpressionsCommand(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   StemQueryExpressions q = new StemQueryExpressions(this,msgxml);
   IvyXmlWriter xw = new IvyXmlWriter();
   q.process(this,xw);
   msg.replyTo(xw.toString());
}



/********************************************************************************/
/*                                                                              */
/*      Handle finding items that might have changed since start of method      */
/*                                                                              */
/********************************************************************************/

private void handleChangedItemsCommand(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   StemQueryChangedItems q = new StemQueryChangedItems(this,msgxml);
   IvyXmlWriter xw = new IvyXmlWriter();
   q.process(this,xw);
   msg.replyTo(xw.toString());
}


private void handleParameterValuesCommand(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   StemQueryParameterValues q = new StemQueryParameterValues(this,msgxml);
   IvyXmlWriter xw = new IvyXmlWriter();
   q.process(this,xw);
   msg.replyTo(xw.toString());
}



/********************************************************************************/
/*										*/
/*	Handle history command to get possible locations			*/
/*										*/
/********************************************************************************/

@Override public List<RootLocation> getLocations(RootProblem prob)
{
   List<RootLocation> rslt = new ArrayList<>();
   StemQueryHistory histq = StemQueryHistory.createHistoryQuery(this,prob);
   if (histq != null) {
      IvyXmlWriter xw = new IvyXmlWriter();
      try {
         histq.process(this,xw);
       }
      catch (RoseException e) {
         RoseLog.logE("STEM","Problem finding locations for problem",e);
       }
      BractFactory bf = BractFactory.getFactory();
      Element e = IvyXml.convertStringToXml(xw.toString());
      Element nodes = IvyXml.getChild(e,"NODES");
      Map<String,RootLocation> done = new HashMap<>();
      for (Element n : IvyXml.children(nodes,"NODE")) {
         double p = IvyXml.getAttrDouble(n,"PRIORITY");
         String reason = IvyXml.getAttrString(n,"REASON");
         Element locelt = IvyXml.getChild(n,"LOCATION");
         RootLocation loc = bf.createLocation(this,locelt);
         double p1 = loc.getPriority();
         p1 = p1 * p;
         loc.setPriority(p1);
         loc.setReason(reason);
         if (!isLocationRelevant(loc,prob)) continue;
         String s = loc.getFile().getPath() + "@" + loc.getLineNumber();
         RootLocation oloc = done.putIfAbsent(s,loc);
         if (oloc != null) {
            double p2 = oloc.getPriority();
            if (p1 > p2) oloc.setPriority(p1);
          }
         else {
            rslt.add(loc);
          }
       }
    }
   
   return rslt;
}



private boolean isLocationRelevant(RootLocation loc,RootProblem prob)
{
   for (String s : prob.ignorePatterns()) {
      String nm = loc.getMethod();
      if (nm.matches(s)) return false;
    }
   
   if (prob.ignoreMain() || prob.ignoreTests()) {
      ASTNode n = getSourceNode(loc.getProject(),loc.getFile(),loc.getStartOffset(),
            loc.getLineNumber(),true,false);
      while (n != null) {
         if (n instanceof MethodDeclaration) break;
         n = n.getParent();
       }
      if (n != null) {
         JcompSymbol js = JcompAst.getDefinition(n);
         if (js != null) {
            if (prob.ignoreMain()) {
               if (js.getName().equals("main") && js.isStatic() &&
                     js.getType().getBaseType().isVoidType()) 
                  return false;
             }
            if (prob.ignoreTests() && js.getAnnotations() != null) {
               for (JcompAnnotation ja : js.getAnnotations()) {
                  if (ja.getAnnotationType().getName().equals("org.junit.Test"))
                     return false;
                }
               if (js.isPublic() && js.getName().startsWith("test")) 
                  return false;
             }
          }
       }
    }
   
   return true;
}


/********************************************************************************/
/*                                                                              */
/*      Handle finding exception cause for a problem                            */
/*                                                                              */
/********************************************************************************/

@Override public ASTNode getExceptionNode(RootProblem rp)
{
   if (rp.getProblemType() != RoseProblemType.EXCEPTION) return null;
   
   StemQueryExceptionHistory query = new StemQueryExceptionHistory(this,rp);
   
   return query.getExceptionNode();
}


@Override public AssertionData getAssertionData(RootProblem rp)
{
   if (rp.getProblemType() == RoseProblemType.ASSERTION) {
      StemQueryAssertionHistory query = new StemQueryAssertionHistory(this,rp);
      return query.getAssertionData();
    }
   else if (rp.getProblemType() == RoseProblemType.LOCATION) {
      // check if previous statement is if (x == y) or (x.equals(y))
    }
   return null;
}



private void handleHistoryCommand(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   String tid = IvyXml.getAttrString(msgxml,"THREAD");
   if (tid != null) loadFilesIntoFait(tid);
   waitForAnalysis();
   long start = System.currentTimeMillis();
   Element probxml = IvyXml.getChild(msgxml,"PROBLEM");
   RootProblem prob = BractFactory.getFactory().createProblemDescription(this,probxml);
   StemQueryHistory histq = StemQueryHistory.createHistoryQuery(this,prob);
   if (histq != null) {
      IvyXmlWriter xw = new IvyXmlWriter();
      histq.process(this,xw);
      msg.replyTo(xw.toString());
    }
   else {
      msg.replyTo();
    }
   RootMetrics.noteCommand("STEM","HISTORYTIME",System.currentTimeMillis()-start);
}



private void handleLocationCommand(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   String tid = IvyXml.getAttrString(msgxml,"THREAD");
   if (tid != null) loadFilesIntoFait(tid);
   waitForAnalysis();
   long start = System.currentTimeMillis();
   Element probxml = IvyXml.getChild(msgxml,"PROBLEM");
   RootProblem prob = BractFactory.getFactory().createProblemDescription(this,probxml);
   List<RootLocation> locs = getLocations(prob);
   if (locs == null) {
      msg.replyTo();
    }
   else {
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("RESULT");
      for (RootLocation loc : locs) {
         loc.outputXml(xw);
       }
      xw.end("RESULT");
      msg.replyTo(xw.toString());
    }
   RootMetrics.noteCommand("STEM","LCOATIONTIME",System.currentTimeMillis()-start);
}



private void handleStartFrame(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   Element probxml = IvyXml.getChild(msgxml,"PROBLEM");
   RootProblem prob = BractFactory.getFactory().createProblemDescription(this,probxml);
   Element locxml = IvyXml.getChild(msgxml,"LOCATION");
   RootLocation loc = BractFactory.getFactory().createLocation(this,locxml);
   BudStackFrame bsf = ValidateFactory.getFactory(this).getStartingFrame(prob,loc);
   if (bsf != null) {
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("RESULT");
      xw.field("STARTFRAME",bsf.getFrameId());
      xw.field("METHOD",bsf.getMethodName());
      xw.field("SIGNATURE",bsf.getMethodSignature());
      xw.field("CLASS",bsf.getClassName());
      xw.end("RESULT");
      msg.replyTo(xw.toString());
      xw.close();
    }
   else msg.replyTo();
}




/********************************************************************************/
/*                                                                              */
/*      Handle suggest command to suggest repairs                               */
/*                                                                              */
/********************************************************************************/

private void handleSuggestCommand(MintMessage msg) throws RoseException
{
   Element msgxml = msg.getXml();
   BractFactory bract = BractFactory.getFactory();
   Element probxml = IvyXml.getChild(msgxml,"PROBLEM");
   RootProblem problem = bract.createProblemDescription(this,probxml);
   String id = IvyXml.getAttrString(msgxml,"NAME");
   if (id == null) {
      id = "ROSESUGGEST_" + IvyExecQuery.getProcessId() + "_" + 
         id_counter.incrementAndGet();
    }
   Element locxml = IvyXml.getChild(msgxml,"LOCATION");
   RootLocation loc = bract.createLocation(this,locxml);
   
   ValidateFactory vfac = ValidateFactory.getFactory(this);
   String fid = IvyXml.getAttrString(msgxml,"VALIDATE");
   if (fid != null && fid.equals("*")) fid = null;
   RootValidate validate = vfac.createValidate(problem,fid,loc);
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");
   xw.field("NAME",id);
   xw.end();
   msg.replyTo(xw.toString());
   xw.close();
   
   bract.startSuggestions(this,id,problem,loc,validate);
}





/********************************************************************************/
/*										*/
/*	Handle metrics recording request					*/
/*										*/
/********************************************************************************/

private void handleMetricsCommand(MintMessage msg)
{
   Element msgxml = msg.getXml();
   String who = IvyXml.getAttrString(msgxml,"WHO");
   if (who == null) who = "BUSH";
   String cmd = IvyXml.getAttrString(msgxml,"WHAT");
   if (cmd == null) return;

   List<String> args = new ArrayList<>();
   String arg0 = IvyXml.getTextElement(msgxml,"ARG0");
   if (arg0 != null) args.add(arg0.trim());
   for (int i = 1; ; ++i) {
      String argi = "ARG" + i;
      String argv = IvyXml.getTextElement(msgxml,argi);
      if (argv == null) break;
      args.add(argv.trim());
    }
   for (Element argelt : IvyXml.children(msgxml,"ARG")) {
      String txt = IvyXml.getText(argelt);
      if (txt != null) args.add(txt.trim());
    }
   Object [] argarr = args.toArray();

   RootMetrics.noteCommand(who,cmd,argarr);
}



/********************************************************************************/
/*                                                                              */
/*      Start seede process                                                     */
/*                                                                              */
/********************************************************************************/

private boolean startSeede()
{
   RoseLog.logD("STEM","START SEEDE " + seede_running + " " + seede_started);
   
   synchronized (this) {
      if (seede_running || seede_started) return false;      // quick, informal check
      seede_started = true;
    }
   
   IvyExec exec = null;
   File wd = new File(workspace_path);
   File logf = new File(wd,"seede.log");
   boolean isnew = false;
   
   BoardProperties bp = BoardProperties.getProperties("Rose");
   String dbgargs = bp.getProperty("Rose.seede.jvm.args");
   List<String> args = new ArrayList<String>();
   args.add(IvyExecQuery.getJavaPath());
   
   if (dbgargs != null && dbgargs.contains("###")) {
      int port = (int)(Math.random() * 1000 + 3000);
      RoseLog.logI("STEM","Seede debugging port " + port);
      dbgargs = dbgargs.replace("###",Integer.toString(port));
    }
   
   if (dbgargs != null) {
      StringTokenizer tok = new StringTokenizer(dbgargs);
      while (tok.hasMoreTokens()) {
         args.add(tok.nextToken());
       }
    }
   
   BoardSetup setup = BoardSetup.getSetup();
   File f1 = setup.getRootDirectory();
   File f2 = new File(f1,"dropins");
   File seedejar = new File(f2,"seede.jar");
   File fjar = IvyFile.getJarFile(StemMain.class);
   if (fjar == null || fjar.getName().endsWith(".class")) {
      File f3 = new File("/Users/spr/Eclipse/seede/seede/bin");
      if (!f3.exists()) f3 = new File("/pro/seede/java");
      if (!f3.exists()) f3 = new File("/research/people/spr/seede/java");
      if (f3.exists()) seedejar = f3;
    }
   
   args.add("-cp");
   String xcp = bp.getProperty("Rose.seede.class.path");
   if (xcp == null) {
      xcp = System.getProperty("java.class.path");
      String ycp = bp.getProperty("Rose.seede.add.path");
      if (ycp != null) xcp = ycp + File.pathSeparator + xcp;
    }
   else {
      StringBuffer buf = new StringBuffer();
      StringTokenizer tok = new StringTokenizer(xcp,":;");
      while (tok.hasMoreTokens()) {
         String elt = tok.nextToken();
         if (!elt.startsWith("/") &&  !elt.startsWith("\\")) {
            if (elt.equals("eclipsejar")) {
               elt = setup.getEclipsePath();
             }
            else if (elt.equals("seede.jar") && seedejar != null) {
               elt = seedejar.getPath();
             }
            else {
               elt = setup.getLibraryPath(elt);
             }
          }
         if (buf.length() > 0) buf.append(File.pathSeparator);
         buf.append(elt);
       }
      xcp = buf.toString();
    }
   
   args.add(xcp);
   args.add("edu.brown.cs.seede.sesame.SeedeMain");
   args.add("-m");
   args.add(mint_control.getMintName());
   args.add("-L");
   args.add(logf.getPath());
   if (run_debug && bp.getBoolean("Rose.seede.debug")) args.add("-D");
   if (run_debug && bp.getBoolean("Rose.seede.trace")) args.add("-T");
   
   for (int i = 0; i < 100; ++i) {
      MintDefaultReply rply = new MintDefaultReply();
      mint_control.send("<SEEDE DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String rslt = rply.waitForString(1000);
      if (rslt != null) {
         seede_running = true;
         break;
       }
      if (i == 0) {
         if (local_seede) {
            List<String> fargs = new ArrayList<>();
            boolean use = false;
            for (String s : args) {
               if (s.equals("edu.brown.cs.seede.sesame.SeedeMain")) use = true;
               else if (use) fargs.add(s);
             }
            String [] fargarr = new String[fargs.size()];
            fargarr = fargs.toArray(fargarr);
            try {
               Class<?> faitcls = Class.forName("edu.brown.cs.seede.sesame.SeedeMain");
               Method m = faitcls.getMethod("main",fargarr.getClass());
               m.invoke(null,new Object [] { fargarr });
             }
            catch (Throwable t) {
               RoseLog.logE("STEM","Couldn't start seede locally",t);
               break;
             }
          }
         else {   
            try {
               exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);     // make IGNORE_OUTPUT to clean up otuput
               isnew = true;
               seede_started = true;
               RoseLog.logD("STEM","Run " + exec.getCommand());
             }
            catch (IOException e) {
               break;
             }
          }
       }
      else {
         try {
            if (exec != null && !local_seede) {
               int sts = exec.exitValue();
               RoseLog.logD("STEM","Seede server disappeared with status " + sts);
               break;
             }
          }
         catch (IllegalThreadStateException e) { }
       }
      synchronized (this) {
         try {
            wait(2000);
          }
         catch (InterruptedException e) { }
       }
    }
   
   if (!seede_running) {
      RoseLog.logE("STEM","Unable to start seede server: " + args);
    }
   
   return isnew;
}



/********************************************************************************/
/*										*/
/*	Handle messaging							*/
/*										*/
/********************************************************************************/

@Override public MintControl getMintControl()           { return mint_control; }


@Override public Element sendFaitMessage(String cmd,CommandArgs args,String cnts)
{
   if (!fait_running) return null;

   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("FAIT");
   xw.field("DO",cmd);
   if (session_id != null) xw.field("SID",session_id);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("FAIT");
   String msg = xw.toString();
   xw.close();

   RoseLog.logD("STEM","Send to FAIT: " + msg);

   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element rslt = rply.waitForXml(0);

   RoseLog.logD("STEM","Reply from FAIT: " + IvyXml.convertXmlToString(rslt));
   if (rslt == null && (cmd.equals("START") || cmd.equals("BEGIN"))) {
      MintDefaultReply prply = new MintDefaultReply();
      mint_control.send("<FAIT DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String prslt = prply.waitForString(3000);
      if (prslt == null) {
	 fait_running = false;
	 fait_started = false;
	 startFaitProcess();
	 rply = new MintDefaultReply();
	 mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
	 rslt = rply.waitForXml(0);
       }
    }

   return rslt;
}


@Override public Element sendBubblesMessage(String cmd,CommandArgs args,String cnts)
{
   return sendBubblesXmlReply(cmd,null,args,cnts);
}


@Override public Element sendSeedeMessage(String id,String cmd,CommandArgs args,String cnts)
{
   if (!seede_running) startSeede();
   
   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("SEEDE");
   xw.field("DO",cmd);
   if (id != null) xw.field("SID",id);
   else xw.field("SID","*");
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("SEEDE");
   String msg = xw.toString();
   xw.close();
   
   RoseLog.logD("STEM","Send to SEEDE: " + msg);
   
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
   
   Element rslt = rply.waitForXml(300000);
   
   RoseLog.logD("STEM","Reply from SEEDE: " + IvyXml.convertXmlToString(rslt));
   if (rslt == null && (cmd.equals("START") || cmd.equals("BEGIN"))) {
      MintDefaultReply prply = new MintDefaultReply();
      mint_control.send("<SEEDE DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String prslt = prply.waitForString(3000);
      if (prslt == null) {
	 seede_running = false;
	 seede_started = false;
	 startSeede();
	 rply = new MintDefaultReply();
	 mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
	 rslt = rply.waitForXml(0);
       }
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Handle COCKER databases                                                 */
/*                                                                              */
/********************************************************************************/

@Override public LeashIndex getProjectIndex()
{
   if (project_index == null) {
      File bdir = new File(workspace_path);
      File bbdir = new File(bdir,".bubbles");
      File cdir = new File(bbdir,"CockerIndex");
      LeashIndex idx = new LeashIndex(ROSE_PROJECT_INDEX_TYPE,cdir);
      idx.start();
      if (idx.isActive()) project_index = idx;
    }
   return project_index;
}



@Override public LeashIndex getGlobalIndex()
{
   return global_index;
}



/********************************************************************************/
/*										*/
/*	Keep track of FAIT state						*/
/*										*/
/********************************************************************************/

private class UpdateHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      RoseLog.logD("STEM","Fait message: " + msg.getText());
      String type = args.getArgument(0);
      Element xml = msg.getXml();
      String id = IvyXml.getAttrString(xml,"ID");
      if (session_id == null || !session_id.equals(id)) return;
      String rslt = null;
      try {
         switch (type) {
            case "ANALYSIS" :
               handleAnalysis(xml);
               break;
            default :
               RoseLog.logE("STEM","Unknown command " + type + " from Fait");
               break;
            case "ERROR" :
               throw new Error("Fait error: " + msg.getText());
          }
       }
      catch (Throwable e) {
         RoseLog.logE("STEM","Error processing command",e);
       }
      msg.replyTo(rslt);
    }

}	// end of inner class UpdateHandler


synchronized void handleAnalysis(Element xml)
{
   RoseLog.logD("STEM","Analysis received: " + IvyXml.convertXmlToString(last_analysis));

   boolean started = IvyXml.getAttrBool(xml,"STARTED");
   boolean aborted = IvyXml.getAttrBool(xml,"ABORTED");

   if (started || aborted) {
      analysis_state = AnalysisState.PENDING;
      last_analysis = null;
    }
   else {
      analysis_state = AnalysisState.READY;
      last_analysis = xml;
    }

   notifyAll();
}


synchronized Element waitForAnalysis()
{
   long start = System.currentTimeMillis();
   try {
      for ( ; ; ) {
	 if (!fait_running) return null;

	 switch (analysis_state) {
	    case NONE :
	       return null;
	    case PENDING :
	       break;
	    case READY :
	       return last_analysis;
	  }
	 try {
	    wait(10000);
	  }
	 catch (InterruptedException e) { }
       }
    }
   finally {
      long wait = System.currentTimeMillis() - start;
      if (wait > 100) {
	 RootMetrics.noteCommand("STEM","WAIT",wait);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Inner class to handle command messages					*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      RoseLog.logD("STEM","Handle command " + msg.getText());
      try {
         switch (cmd) {
            case "START" :
               handleStartCommand(msg);
               break;
            case "PING" :
               msg.replyTo("<PONG/>");
               break;
            case "HISTORY" :
               handleHistoryCommand(msg);
               break;
            case "LOCATIONS" :
               handleLocationCommand(msg);
               break;
            case "EXPRESSIONS" :
               handleExpressionsCommand(msg);
               break;
            case "CHANGEDITEMS" :
               handleChangedItemsCommand(msg);
               break;
            case "PARAMETERVALUES" :
               handleParameterValuesCommand(msg);
               break;
            case "SUGGEST": 
               handleSuggestCommand(msg);
               break;
            case "METRIC" :
            case "METRICS" :
               handleMetricsCommand(msg);
               msg.replyTo();
               break;
            case "STARTFRAME" :
               handleStartFrame(msg);
               break;
            case "EXIT" :
               serverDone();
               msg.replyTo();
               break;
          }
       }
      catch (Throwable t) {
         RoseLog.logE("STEM","Problem processing command",t);
         msg.replyTo("<ERROR>" + t + "</ERROR>");
       }
    }

}	// end of inner class CommandHandler



/********************************************************************************/
/*										*/
/*	Handle Eclipse messages 						*/
/*										*/
/********************************************************************************/

private class EclipseHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
   
      switch (cmd) {
         case "ELISION" :
            return;
       }
   
      RoseLog.logD("STEM","Message from eclipse: " + cmd + " " + msg.getText());
   
      Element e = msg.getXml();
   
      switch (cmd) {
         case "PING" :
         case "PING1" :
         case "PING2" :
         case "PING3" :
            msg.replyTo("<PONG/>");
            break;
         case "EDITERROR" :
         case "FILEERROR" :
         case "EDIT" :
         case "RUNEVENT" :
         case "RESOURCE" :
         case "LAUNCHCONFIGEVENT" :
         case "NAMES" :
         case "ENDNAMES" :
         case "PROGRESS" :
         case "BUILDDONE" :
         case "FILECHANGE" :
         case "PROJECTDATA" :
         case "PROJECTOPEN" :
         case "PRIVATEERROR" :
            break;
         case "EVALUATION" :
            String id = IvyXml.getAttrString(e,"ID");
            if (id != null && id.startsWith("ROSE")) {
               EvalData ed = new EvalData(e);
               synchronized (eval_handlers) {
                  eval_handlers.put(id,ed);
                  eval_handlers.notifyAll();
                }
             }
            msg.replyTo();
            break;
         case "CONSOLE" :
         case "BREAKEVENT" :
            msg.replyTo();
            break;
         case "STOP" :
            serverDone();
            break;
         default :
            RoseLog.logE("STEM","Unknown Eclipse Message: " + msg.getText());
            break;
       }
    }

}	// end of inner class EclipseHandler




/********************************************************************************/
/*										*/
/*	Handle bubbles messages 						*/
/*										*/
/********************************************************************************/

private class BubblesHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      RoseLog.logD("STEM","BUBBLES COMMAND: " + cmd);
      // Element e = msg.getXml();
      switch (cmd) {
	 case "EXIT" :
	    serverDone();
	    break;
       }
      msg.replyTo();
    }

}	// end of inner class BubblesHandler



/********************************************************************************/
/*										*/
/*	Wait for exit								*/
/*										*/
/********************************************************************************/

private synchronized void serverDone()
{
   is_done = true;
   notifyAll();
}



private boolean checkEclipse()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PING' />";
   mint_control.send(msg,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
   String r = rply.waitForString(300000);
   RoseLog.logD("STEM","BUBBLES PING " + r);
   if (r == null) return false;
   return true;
}



private class WaitForExit extends Thread {

   WaitForExit() {
      super("RoseWaitForExit");
    }

   @Override public void run() {
      StemMain mon = StemMain.this;
      synchronized (mon) {
	 for ( ; ; ) {
	    if (checkEclipse()) break;
	    try {
	       mon.wait(30000l);
	     }
	    catch (InterruptedException e) { }
	  }

	 while (!is_done) {
	    if (!checkEclipse()) is_done = true;
	    else {
	       try {
		  mon.wait(30000l);
		}
	       catch (InterruptedException e) { }
	     }
	  }
       }

      System.exit(0);
}

}	// end of inner class WaitForExit



/********************************************************************************/
/*										*/
/*	Load source files into fait						*/
/*										*/
/********************************************************************************/

@Override public Set<File> getLoadedFiles()
{
   return loaded_files;
}



private void loadFilesIntoFait(String tid)
{
   Set<File> files = new HashSet<>();
   if (use_all_files) files = findAllSourceFiles();
   else files = findStackFiles(tid);

   StringBuffer buf = new StringBuffer();
   int ct = 0;
   for (File f : files) {
      if (added_files.add(f)) {
	 buf.append("<FILE NAME='");
	 buf.append(f.getAbsolutePath());
	 buf.append("' />");
	 ++ct;
       }
    }
   if (ct > 0) {
      loaded_files.addAll(files);
      Element xw = sendFaitMessage("ADDFILE",null,buf.toString());
      if (IvyXml.isElement(xw,"RESULT")) {
	 if (IvyXml.getAttrBool(xw,"ADDED")) {
	    analysis_state = AnalysisState.PENDING;
	  }
       }
    }
   else {
      RoseLog.logD("STEM","No files to add for " + tid + " " + use_all_files);
    }
}



private Set<File> findStackFiles(String threadid)
{
   Set<File> rslt = new HashSet<>();
   if (threadid == null) return rslt;

   CommandArgs args = new CommandArgs("THREAD",threadid);
   Element r = sendBubblesXmlReply("GETSTACKFRAMES",null,args,null);
   Element sf = IvyXml.getChild(r,"STACKFRAMES");
   for (Element th : IvyXml.children(sf,"THREAD")) {
      String id = IvyXml.getAttrString(th,"ID");
      if (threadid == null || threadid.equals(id)) {
	 for (Element frm : IvyXml.children(th,"STACKFRAME")) {
	    String fnm = IvyXml.getAttrString(frm,"FILE");
	    if (fnm != null) {
	       File f = new File(fnm);
	       if (f.exists()) {
		  rslt.add(f);
		}
	     }
	  }
       }
    }

   return rslt;
}



private Set<File> findAllSourceFiles()
{
   Element r = sendBubblesXmlReply("PROJECTS",null,null,null);
   if (!IvyXml.isElement(r,"RESULT")) {
      RoseLog.logE("STEM","Problem getting project information: " + IvyXml.convertXmlToString(r));
      return null;
    }

   Set<File> allfiles = new HashSet<>();
   for (Element pe : IvyXml.children(r,"PROJECT")) {
      String pnm = IvyXml.getAttrString(pe,"NAME");
      allfiles.addAll(getProjectSourceFiles(pnm));
    }

   return allfiles;
}




private Set<File> getProjectSourceFiles(String proj)
{
   Set<File> rslt = new HashSet<>();
   CommandArgs cargs = new CommandArgs("FILES",true);
   Element pxml = sendBubblesXmlReply("OPENPROJECT",proj,cargs,null);
   Element p1 = IvyXml.getChild(IvyXml.getChild(pxml,"PROJECT"),"FILES");

   for (Element fe : IvyXml.children(p1,"FILE")) {
      if (IvyXml.getAttrBool(fe,"SOURCE")) {
	 File f2 = new File(IvyXml.getText(fe));
	 if (f2.exists() && f2.getName().endsWith(".java")) {
	    try {
	       f2 = f2.getCanonicalFile();
	     }
	    catch (IOException e) {
	       continue;
	     }
	    rslt.add(f2);
	  }
       }
    }

   return rslt;
}



private Element sendBubblesXmlReply(String cmd,String proj,CommandArgs args,String cnts)
{
   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBBLES");
   xw.field("DO",cmd);
   if (proj != null) xw.field("PROJECT",proj);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("BUBBLES");

   String msg = xw.toString();
   xw.close();

   RoseLog.logD("STEM","Send to BUBBLES: " + msg);

   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element rslt = rply.waitForXml(60000);

   RoseLog.logD("STEM","Reply from BUBBLES: " + IvyXml.convertXmlToString(rslt));

   return rslt;
}



public Element sendRoseMessage(String cmd,CommandArgs args,String cnts,long wait)
{
   MintDefaultReply rply = null;
   if (wait >= 0) rply = new MintDefaultReply();
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("ROSEREPLY");
   xw.field("DO",cmd);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("ROSEREPLY");
   
   String msg = xw.toString();
   xw.close();
   
   RoseLog.logD("STEM","Send message: " + msg);
   
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
   
   if (rply == null) return null;
   
   Element rslt = rply.waitForXml(wait);
   
   RoseLog.logD("STEM","Reply for message: " + IvyXml.convertXmlToString(rslt));
   
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Handle evaluations							*/
/*										*/
/********************************************************************************/

@Override public Element waitForEvaluation(String id)
{
   synchronized (eval_handlers) {
      for ( ; ; ) {
	 EvalData ed = eval_handlers.remove(id);
	 if (ed != null) {
	    return ed.getResult();
	  }
	 try {
	    eval_handlers.wait(5000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}




private static class EvalData {

   private Element eval_result;

   EvalData(Element rslt) {
      eval_result = rslt;
    }

   Element getResult() {
      return eval_result;
    }

}	// end of inner class EvalData




/********************************************************************************/
/*                                                                              */
/*      Handle compilation                                                      */
/*                                                                              */
/********************************************************************************/

@Override public ASTNode getSourceNode(String proj,File f,int offset,int line,
      boolean resolve,boolean stmt)
{
   synchronized (this) {
      if (stem_compiler == null) stem_compiler = new StemCompiler(this);
    }
   
   ASTNode n = stem_compiler.getSourceNode(proj,f,offset,line,resolve);
   
   if (stmt) n = stem_compiler.getStatementOfNode(n);
   
   return n;
}


@Override public IDocument getSourceDocument(String proj,File f)
{
   synchronized (this) {
      if (stem_compiler == null) stem_compiler = new StemCompiler(this);
    }
   
   return stem_compiler.getSourceDocument(proj,f);
}





/********************************************************************************/
/*                                                                              */
/*      Handle finding relevant project for file                                */
/*                                                                              */
/********************************************************************************/

@Override public String getProjectForFile(File f)
{
   if (f == null) return default_project;
   
   String p = project_map.get(f);
   if (p == null) p = default_project;
   
   return p;
}



private void buildProjectMap()
{
   project_map = new HashMap<>();
   default_project = null;
   
   Element xml = sendBubblesMessage("PROJECTS",null,null);
   for (Element p : IvyXml.children(xml,"PROJECT")) {
      String nm = IvyXml.getAttrString(p,"NAME");
      CommandArgs args = new CommandArgs("PROJECT",nm,"FILES",true);
      Element pxml = sendBubblesMessage("OPENPROJECT",args,null);
      Element rxml = IvyXml.getChild(pxml,"PROJECT");
      Element files = IvyXml.getChild(rxml,"FILES");
      for (Element fxml : IvyXml.children(files,"FILE")) {
         if (IvyXml.getAttrBool(fxml,"SOURCE")) {
            String fnm = IvyXml.getAttrString(fxml,"PATH");
            File f = new File(fnm);
            project_map.putIfAbsent(f,nm);
            if (default_project != null) default_project = nm;
          }
       }
    }
}

   
   
   
}	// end of class StemMain




/* end of StemMain.java */

