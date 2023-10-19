/********************************************************************************/
/*										*/
/*		BushFactory.java						*/
/*										*/
/*	Bubbles interface for Repairing Obvious Semantic Errors 		*/
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



package edu.brown.cs.rose.bush;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bema.BemaMain;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootTestCase;
import edu.brown.cs.rose.root.RoseLog;


public class BushFactory implements BushConstants, BumpConstants, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BumpProcess,ProcessData>	process_map;
private boolean 	rose_running;
private boolean 	rose_started;
private boolean 	rose_failed;
private boolean 	rose_ready;
private MintControl	mint_control;
private Map<String,BushRepairAdder> current_repairs;
private static BushFactory the_factory = null;
private static AtomicInteger id_counter = new AtomicInteger();

private static boolean project_setup_required = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static synchronized BushFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BushFactory();
    }
   return the_factory;
}

private BushFactory()
{
   process_map = new HashMap<>();
   rose_running = false;
   rose_started = false;
   rose_failed = false;
   rose_ready = false;
   current_repairs = new HashMap<>();
   mint_control = BoardSetup.getSetup().getMintControl();
   mint_control.register("<ROSEREPLY DO='_VAR_0' />",new RoseHandler());
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   if (!BumpClient.getBump().getOptionBool("bubbles.useRose")) return;

   getFactory().setupHandlers();
}


public static void initialize(BudaRoot root)
{
   if (!BumpClient.getBump().getOptionBool("bubbles.useRose")) return;
   
   // start cocker
   CockerStarter cs = new CockerStarter();
   BoardThreadPool.start(cs);
}



public static void postLoad()
{
   BoardProperties bp = BoardProperties.getProperties("Rose");
   BumpClient bc = BumpClient.getBump();

   Set<String> skipproj = bp.getStringSet("Rose.no.autostart");
   boolean nostart = false;
   
   Element xml = bc.getAllProjects(5000);
   if (xml != null) {
      for (Element pe : IvyXml.children(xml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
         if (skipproj != null && skipproj.contains(pnm)) nostart = true;
       }
    }
   if (nostart) return;
   
   try {
      Class<?> bsean = BemaMain.findClass("edu.brown.cs.faitbb.bsean.BseanFactory");
      if (bsean == null) {
	 RoseLog.logE("BUSH","Can't find BseanFactory");
	 return;
       }
      Method m = bsean.getMethod("beginAnalysis");
      m.invoke(null);
    }
   catch (Throwable t) {
      RoseLog.logE("BUSH","Problem starting fait",t);
    }
}



private void setupHandlers()
{
   BumpClient bc = BumpClient.getBump();

   bc.getRunModel().addRunEventHandler(new RoseRunEventHandler());

   if (project_setup_required) fixupProjects();
}



/********************************************************************************/
/*										*/
/*	Metrics commands							*/
/*										*/
/********************************************************************************/

static void metrics(String cmd,Object ... args)
{
   CommandArgs cmdargs = new CommandArgs("WHO","BUSH","WHAT",cmd);
   IvyXmlWriter xw = new IvyXmlWriter();
   for (int i = 0; i < args.length; ++i) {
      if (args[i] != null) {
	 xw.textElement("ARG",String.valueOf(args[i]));
       }
    }
   getFactory().sendRoseMessage("METRICS",cmdargs,xw.toString());
   xw.close();
}


/********************************************************************************/
/*										*/
/*	Add fix annotations							*/
/*										*/
/********************************************************************************/

void addFixAnnotations(BushProblem prob,List<BushLocation> locs,String mid)
{
   BumpThread bt = prob.getThread();
   if (bt == null) return;
   BumpProcess bp = bt.getProcess();
   if (bp == null) return;
   ProcessData pd = process_map.get(bp);
   if (pd == null) return;
   ThreadData td = pd.findThread(bt);
   if (td == null) return;

   List<BushLocation> uselocs = new ArrayList<>();
   for (BushLocation bl : locs) {
      int ln = bl.getLineNumber();
      boolean fnd = false;
      for (BushLocation ubl : uselocs) {
	 if (!ubl.getFile().equals(bl.getFile())) continue;
	 int uln = ubl.getLineNumber();
	 if (uln != ln) continue;
	 fnd = true;
	 break;
       }
      if (!fnd) uselocs.add(bl);
    }
   if (uselocs.isEmpty()) return;

   for (BushLocation bl : uselocs) {
      td.addFixAnnotation(prob,bl,mid);
    }
}



/********************************************************************************/
/*										*/
/*	Handle repair suggestions						*/
/*										*/
/********************************************************************************/

void startRepairSuggestor(BushProblem prob,BushLocation loc,BushRepairAdder adder)
{
   String id = "ROSESUGGEST_" + IvyExecQuery.getProcessId() + "_" + id_counter.incrementAndGet();
   CommandArgs args = new CommandArgs("NAME",id);
   current_repairs.put(id,adder);

   IvyXmlWriter xw = new IvyXmlWriter();
   prob.outputXml(xw);
   if (loc != null) {
      loc.outputXml(xw);
    }
   String body = xw.toString();
   xw.close();

   Element rply = sendRoseMessage("SUGGEST",args,body);
   if (IvyXml.isElement(rply,"RESULT")) {
      String name = IvyXml.getAttrString(rply,"NAME");
      current_repairs.put(name,adder);
      if (!name.equals(id)) current_repairs.remove(id);
    }
   else {
      BoardLog.logE("BUSH","suggest command failed: " + IvyXml.convertXmlToString(rply));
      adder.doneRepairs();
      current_repairs.remove(id);
    }
}


AbstractAction getSuggestAction(BushProblem p,BushLocation l,Component c,
      BushProblemPanel pnl,String mid)
{
   return new RoseSuggestAction(p,l,c,pnl,mid);
}



private void handleSuggestion(Element xml)
{
   String who = IvyXml.getAttrString(xml,"NAME");
   BoardLog.logD("BUSH","Handle suggestion for " + who);
   BushRepairAdder ra = current_repairs.get(who);
   if (ra == null) {
      BoardLog.logE("BUSH","No repair bubble to add to");
      return;
    }
   Element repairxml = IvyXml.getChild(xml,"REPAIR");
   Element locxml = IvyXml.getChild(repairxml,"LOCATION");
   BumpLocation bumploc = BumpLocation.getLocationFromXml(locxml);
   double pri = IvyXml.getAttrDouble(locxml,"PRIORITY");
   if (pri < 0) pri = 0.5;
   BushLocation bloc = new BushLocation(bumploc,pri);
   BushRepair br = new BushRepair(repairxml,bloc);
   ra.addRepair(br);
}


private void handleEndSuggestion(Element xml)
{
   String who = IvyXml.getAttrString(xml,"NAME");
   BushRepairAdder ra = current_repairs.get(who);
   if (ra == null) return;
   ra.doneRepairs();
   current_repairs.remove(who);
}




/********************************************************************************/
/*										*/
/*	Start server								*/
/*										*/
/********************************************************************************/


private class RoseStarter extends Thread {

   private String panel_id;
   private BumpThread for_thread;

   RoseStarter(BumpThread bt) {
      super("Rose_Starter");
      panel_id = null;
      for_thread = bt;
    }

   @Override public void run() {
      BoardLog.logD("BUSH","Start rose");
      waitForRose();
      BoardLog.logD("BUSH","Rose running " + rose_started + " " + rose_running + " " +
	    rose_failed + " " + rose_ready);
      startRoseAnalysis(for_thread);
      BoardLog.logD("BUSH","Rose START returned " + rose_ready);

      synchronized (this) {
	 while (panel_id == null) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
       }
    }

   synchronized void setPanelId(String id) {
      panel_id = id;
      notifyAll();
    }

}	// end of inner class RoseStarter




private boolean startRoseServer()
{
   if (rose_running || rose_started) return false;

   BoardSetup setup = BoardSetup.getSetup();
   IvyExec exec = null;
   File wd = new File(setup.getDefaultWorkspace());
   File logf = new File(wd,"rose.log");

   BoardProperties bp = BoardProperties.getProperties("Rose");
   String dbgargs = bp.getProperty("Rose.rose.jvm.args");
   List<String> args = new ArrayList<>();
   args.add(IvyExecQuery.getJavaPath());

   if (dbgargs != null && dbgargs.contains("###")) {
      int port = (int)(Math.random() * 1000 + 3000);
      BoardLog.logI("BUSH","Fait debugging port " + port);
      dbgargs = dbgargs.replace("###",Integer.toString(port));
    }

   if (dbgargs != null) {
      StringTokenizer tok = new StringTokenizer(dbgargs);
      while (tok.hasMoreTokens()) {
	 args.add(tok.nextToken());
       }
    }

   File jarfile = IvyFile.getJarFile(BushFactory.class);

   args.add("-cp");
   String xcp = bp.getProperty("Rose.rose.class.path");
   if (xcp == null) {
      xcp = System.getProperty("java.class.path");
      String ycp = bp.getProperty("Rose.rose.add.path");
      if (ycp != null) xcp = decodeClassPath(ycp,jarfile) + File.pathSeparator + xcp;
    }
   else {
      xcp = decodeClassPath(xcp,jarfile);
    }

   args.add(xcp);
   args.add("edu.brown.cs.rose.stem.StemMain");
   args.add("-m");
   args.add(setup.getMintName());
   args.add("-w");
   args.add(setup.getDefaultWorkspace());
   args.add("-L");
   args.add(logf.getPath());
   if (bp.getBoolean("Rose.rose.debug")) {
      args.add("-D");
    }

   synchronized (this) {
      if (rose_started || rose_running) return false;
      rose_started = true;
    }

   boolean isnew = false;
   for (int i = 0; i < 100; ++i) {
      MintDefaultReply rply = new MintDefaultReply();
      mint_control.send("<ROSE CMD='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
      String rslt = rply.waitForString(1000);
      if (rslt != null) {
	 synchronized (this) {
	    rose_running = true;
	    notifyAll();
	  }
	 break;
       }
      if (i == 0) {
	 try {
	    exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);     // make IGNORE_OUTPUT to clean up output
	    isnew = true;
	    BoardLog.logD("BUSH","Run " + exec.getCommand());
	  }
	 catch (IOException e) {
	    break;
	  }
       }
      else {
	 try {
	    if (exec != null) {
	       int sts = exec.exitValue();
	       BoardLog.logD("BUSH","Rose server disappeared with status " + sts);
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
   if (!rose_running) {
      BoardLog.logE("BUSH","Unable to start rose server: " + args);
      synchronized (this) {
	 rose_failed = true;
	 notifyAll();
       }
      return true;
    }

   return isnew;
}


synchronized boolean waitForRose()
{
   while (!rose_running && !rose_failed) {
      if (!rose_started) startRoseServer();
      try {
	 wait(1000);
       }
      catch (InterruptedException e) { }
    }

   return rose_running;
}



synchronized boolean waitForRoseReady()
{
   waitForRose();

   while (rose_running && !rose_ready) {
      try {
	 wait(1000);
       }
      catch (InterruptedException e) { }
    }

   return rose_ready;
}



private String decodeClassPath(String xcp,File jarfile)
{
   if (xcp == null) return null;
   StringBuffer buf = new StringBuffer();

   BoardSetup setup = BoardSetup.getSetup();
   StringTokenizer tok = new StringTokenizer(xcp,":;");
   while (tok.hasMoreTokens()) {
      String elt = tok.nextToken();
      if (!elt.startsWith("/") &&  !elt.startsWith("\\")) {
	 if (elt.equals("eclipsejar")) {
	    elt = setup.getEclipsePath();
	  }
	 else if (elt.equals("rose.jar") && jarfile != null) {
	    elt = jarfile.getPath();
	  }
	 else {
	    elt = setup.getLibraryPath(elt);
	  }
       }
      if (buf.length() > 0) buf.append(File.pathSeparator);
      buf.append(elt);
    }

   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Start analysis for rose 						*/
/*										*/
/********************************************************************************/

private void startRoseAnalysis(BumpThread bt)
{
   CommandArgs args = null;
   if (bt != null) args = new CommandArgs("THREAD",bt.getId());


   sendRoseMessage("START",args,null);

   synchronized (this) {
      rose_ready = true;
      notifyAll();
    }
}



/********************************************************************************/
/*										*/
/*	Ensure Projects setup for SEEDE 					*/
/*										*/
/********************************************************************************/

private void fixupProjects()
{
   BumpClient bc = BumpClient.getBump();

   Element xml = bc.getAllProjects();
   if (xml != null) {
      for (Element pe : IvyXml.children(xml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 boolean havepoppy = false;
	 boolean haveseede = false;
	 Element delpath = null;
	 Element opxml = bc.getProjectData(pnm,false,true,false,false,false);
	 if (opxml != null) {
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
		  else {
		     delpath = rpe;
		   }
		}
	       if (bn.contains("seede")) haveseede = true;
	     }

	    if (haveseede && !havepoppy) {
	       opxml = bc.getProjectData(pnm,false,false,true,false,false);
	       Element cp = IvyXml.getChild(cpe,"CLASSES");
	       for (Element fe : IvyXml.children(cp,"TYPE")) {
		  String cnm = IvyXml.getAttrString(fe,"NAME");
		  if (cnm.startsWith("edu.brown.cs.seede.poppy.")) {
		     havepoppy = true;
		     break;
		   }
		}
	     }
	    if (delpath != null) {
	       IvyXmlWriter xwp = new IvyXmlWriter();
	       xwp.begin("PROJECT");
	       xwp.field("NAME",pnm);
	       xwp.begin("PATH");
	       xwp.field("TYPE","LIBRARY");
	       xwp.field("DELETE",true);
	       String src = IvyXml.getTextElement(delpath,"SOURCE");
	       if (src != null) xwp.textElement("SOURCE",src);
	       src = IvyXml.getTextElement(delpath,"OUTPUT");
	       if (src != null) xwp.textElement("OUTPUT",src);
	       src = IvyXml.getTextElement(delpath,"BINARY");
	       if (src != null) xwp.textElement("BINARY",src);
	       xwp.end("PATH");
	       xwp.end("PROJECT");
	       String cnts = xwp.toString();
	       xwp.close();
	       bc.editProject(pnm,cnts);
	     }
	    BoardLog.logD("BUSH","Fixup project " + pnm + " " + havepoppy);
	    if (!havepoppy) {
	       File poppyjar = new File(findPoppyJar());
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
		  bc.editProject(pnm,cnts);
		}
	     }
	  }
       }
    }
}


private String findPoppyJar()
{
   String path = BoardSetup.getSetup().getLibraryPath("poppy.jar");

   BoardLog.logD("BUSH","POPPY = " + path);
   return path;
}



/********************************************************************************/
/*										*/
/*	Send message to FAIT							*/
/*										*/
/********************************************************************************/

Element sendRoseMessage(String cmd,CommandArgs args,String cnts)
{
   if (!waitForRose()) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();

   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("ROSE");
   xw.field("CMD",cmd);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("ROSE");
   String msg = xw.toString();
   xw.close();

   BoardLog.logD("BUSH","Send to ROSE: " + msg);

   mc.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element rslt = rply.waitForXml(0);

   BoardLog.logD("BUSH","Reply from ROSE: " + IvyXml.convertXmlToString(rslt));
   if (rslt == null && (cmd.equals("START") || cmd.equals("BEGIN"))) {
      MintDefaultReply prply = new MintDefaultReply();
      mc.send("<FAIT DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String prslt = prply.waitForString(3000);
      if (prslt == null) {
	 rose_running = false;
	 rose_started = false;
	 startRoseServer();
	 rply = new MintDefaultReply();
	 mc.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
	 rslt = rply.waitForXml(0);
       }
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Handler for run events							*/
/*										*/
/********************************************************************************/

private void handleRunEvent(BumpRunEvent evt)
{
   BumpProcess proc = evt.getProcess();
   BumpThread bth = evt.getThread();
   ProcessData pd = process_map.get(proc);

   switch (evt.getEventType()) {
      case PROCESS_ADD :
	 for (ProcessData pd1 : process_map.values()) {
	    pd1.clear();
	  }
	 process_map.clear();
	 pd = new ProcessData(proc);
	 process_map.put(proc,pd);
	 break;
      case PROCESS_REMOVE :
	 if (pd != null) {
	    pd.clear();
	    process_map.remove(proc);
	  }
	 break;
      case THREAD_REMOVE :
	 if (pd != null) pd.removeThread(bth);
	 break;
      case THREAD_CHANGE :
	 BumpThreadState state = bth.getThreadState();
	 if (state.isStopped()) {
	    if (pd != null) {
	       pd.clearThread(bth);
	       pd.setupThread(bth);
	     }
	    // handle stopped
	  }
	 else if (state.isRunning()) {
	    if (pd != null) pd.clearThread(bth);
	  }
	 else {
	    if (pd != null) pd.removeThread(bth);
	  }
	 break;
      default :
	 break;
    }
}



private class RoseRunEventHandler implements BumpRunEventHandler {

   @Override public void handleProcessEvent(BumpRunEvent evt) {
      handleRunEvent(evt);
    }

   @Override public void handleThreadEvent(BumpRunEvent evt) {
      handleRunEvent(evt);
    }

}	// end of inner class RoseRunEventHandler




/********************************************************************************/
/*										*/
/*	Starting data								*/
/*										*/
/********************************************************************************/

private class ProcessData {

   private Map<BumpThread,ThreadData> thread_map;

   ProcessData(BumpProcess bp) {
      thread_map = new HashMap<>();
    }

   void clear() {
      for (ThreadData td : thread_map.values()) {
	 td.clear();
       }
      thread_map.clear();
    }

   void removeThread(BumpThread bth) {
      ThreadData td = thread_map.get(bth);
      if (td != null) {
	 td.clear();
	 thread_map.remove(bth);
       }
    }

   void clearThread(BumpThread bth) {
      ThreadData td = thread_map.get(bth);
      if (td != null) {
	 td.clear();
       }
    }

   void setupThread(BumpThread bth) {
      ThreadData td = thread_map.get(bth);
      if (td == null) {
	 td = new ThreadData(bth);
	 thread_map.put(bth,td);
       }
      td.setup();
    }

   ThreadData findThread(BumpThread bth) {
      return thread_map.get(bth);
    }

}	// end of inner class ProcessData




private class ThreadData {

   private BumpThread thread_id;
   private List<RoseAnnotation> annot_list;
   private List<RoseFixAnnotation> fix_annots;

   ThreadData(BumpThread th) {
      thread_id = th;
      annot_list = new ArrayList<>();
      fix_annots = new ArrayList<>();
    }

   synchronized void addFixAnnotation(BushProblem bp,BushLocation loc,String mid) {
      RoseFixAnnotation rfa = new RoseFixAnnotation(bp,loc,mid);
      fix_annots.add(rfa);
      BaleFactory.getFactory().addAnnotation(rfa);
    }

   synchronized void clear() {
      BaleFactory bf = BaleFactory.getFactory();
      for (RoseAnnotation annot : annot_list) {
	 bf.removeAnnotation(annot);
       }
      annot_list.clear();
      for (RoseFixAnnotation fannot : fix_annots) {
	 bf.removeAnnotation(fannot);
       }
      fix_annots.clear();
    }

   synchronized void setup() {
      clear();
      BaleFactory bf = BaleFactory.getFactory();
      BumpThreadStack stk = thread_id.getStack();
      if (stk == null) return;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 BumpStackFrame frame = stk.getFrame(i);
	 if (frame.isSystem() || frame.isSynthetic()) continue;
	 RoseAnnotation ra = new RoseAnnotation(thread_id,frame);
	 annot_list.add(ra);
	 bf.addAnnotation(ra);
       }
    }

}	// end of inner class ThreadData




/********************************************************************************/
/*										*/
/*	Annotation for triggering ROSE						*/
/*										*/
/********************************************************************************/

private class RoseAnnotation implements BaleAnnotation {

   private BumpThread for_thread;
   private BumpStackFrame stack_frame;
   private BaleFileOverview for_document;
   private Position execute_pos;

   RoseAnnotation(BumpThread th,BumpStackFrame fr) {
      for_thread = th;
      stack_frame = fr;
      for_document = BaleFactory.getFactory().getFileOverview(null,getFile());
      execute_pos = null;
      if (for_document == null) return;
      int off = for_document.findLineOffset(fr.getLineNumber());
      try {
	 execute_pos = for_document.createPosition(off);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BUSH","Bad execution position",e);
       }
    }

   @Override public File getFile() {
      return stack_frame.getFile();
    }

   @Override public int getDocumentOffset() {
      if (execute_pos == null) return -1;
      return execute_pos.getOffset();
    }

   @Override public Icon getIcon(BudaBubble bb) 		{ return null; }

   @Override public String getToolTip() 			{ return null; }

   @Override public Color getLineColor(BudaBubble bb)		{ return null; }

   @Override public Color getBackgroundColor()			{ return null; }

   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }

   @Override public int getPriority()				{ return 0; }

   @Override public void addPopupButtons(Component base,JPopupMenu menu) {
      if (for_thread != null && execute_pos != null) {
	 menu.add(new AskRoseAction(for_thread,stack_frame,base,for_document));
       }
    }

}	// end of inner class RoseAnnotation



/********************************************************************************/
/*										*/
/*	Annotation for showing a Rose potential fix location			*/
/*										*/
/********************************************************************************/

private class RoseFixAnnotation implements BaleAnnotation {

   private BushProblem for_problem;
   private BushLocation for_location;
   private String metric_id;

   RoseFixAnnotation(BushProblem bp,BushLocation loc,String mid) {
      for_problem = bp;
      for_location = loc;
      metric_id = mid;
    }

   @Override public File getFile() {
      return for_location.getFile();
    }

   @Override public int getDocumentOffset() {
      return for_location.getStartOffset();
    }

   @Override public Icon getIcon(BudaBubble bb) {
      return BoardImage.getIcon("rosefix");
    }

   @Override public String getToolTip() {
      return "<html>Potential repair location for " + for_problem.getDescription();
    }

   @Override public Color getLineColor(BudaBubble bb)		{ return null; }
   @Override public Color getBackgroundColor()			{ return null; }
   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }

   @Override public int getPriority()				{ return 10; }

   @Override public void addPopupButtons(Component base,JPopupMenu menu) {
      menu.add(new RoseSuggestAction(for_problem,for_location,base,null,metric_id));
    }

}	// end of inner class RoseFixAnnotation



/********************************************************************************/
/*										*/
/*	Trigger action								*/
/*										*/
/********************************************************************************/

private class AskRoseAction extends AbstractAction implements Runnable {

   private transient BumpThread for_thread;
   private transient BumpStackFrame for_frame;
   private Component base_editor;
   private transient BaleFileOverview bale_file;

   private static final long serialVersionUID = 1;

   AskRoseAction(BumpThread thread,BumpStackFrame frame,Component base,BaleFileOverview doc) {
      super("Ask ROSE to help debug");
      for_thread = thread;
      for_frame = frame;
      base_editor = base;
      bale_file = doc;
    }

   @Override public void actionPerformed(ActionEvent evt) {
//    BoardThreadPool.start(this);
      SwingUtilities.invokeLater(this);
    }

   @Override public void run() {
      RoseStarter starter = new RoseStarter(for_thread);
      starter.start();

      BushProblemPanel pnl = new BushProblemPanel(for_thread,for_frame,base_editor,bale_file);
      starter.setPanelId(pnl.getMetricId());

      pnl.createBubble(base_editor);
   }

}	// end of inner class AskRoseAction



/********************************************************************************/
/*										*/
/*	Find suggestions action 						*/
/*										*/
/********************************************************************************/

private static class RoseSuggestAction extends AbstractAction implements Runnable {

   private transient BushProblem for_problem;
   private transient BushLocation for_location;
   private Component from_component;
   private String metric_id;
   private boolean create_bubble;
   private transient BushProblemPanel problem_panel;

   private static final long serialVersionUID = 1;

   RoseSuggestAction(BushProblem p,BushLocation l,Component c,BushProblemPanel pnl,String mid) {
      super("Suggest Repairs for " + p.getDescription() +
	    (l != null ? " here" : ""));
      for_problem = p;
      for_location = l;
      from_component = c;
      metric_id = mid;
      create_bubble = false;
      problem_panel = pnl;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (for_problem.getCurrentTest() == null) {
	 BoardThreadPool.start(this);
       }
      else {
	 create_bubble = true;
	 SwingUtilities.invokeLater(this);
       }
    }

   @Override public void run() {
      if (create_bubble) {
	 metrics("SUGGEST",metric_id,for_problem.getProblemType(),for_problem.getProblemDetail(),
	       for_problem.getOriginalValue(),for_problem.getTargetValue());
	 // add metrics here
	 BushSuggestPanel pnl = new BushSuggestPanel(from_component,for_problem,for_location,metric_id);
	 pnl.createBubble();
	 BushFactory.getFactory().startRepairSuggestor(for_problem,for_location,pnl);
	 if (problem_panel != null) problem_panel.noteWorking(false);
       }
      else {
	 setupDefaultTest();
       }
    }

   private void setupDefaultTest() {
      IvyXmlWriter xw = new IvyXmlWriter();
      for_problem.outputXml(xw);
      String body = xw.toString();
      xw.close();
      if (for_location != null) {
	 xw = new IvyXmlWriter();
	 for_location.outputXml(xw);
	 body += xw.toString();
	 xw.close();
       }
      CommandArgs args = new CommandArgs();
      Element rslt = BushFactory.getFactory().sendRoseMessage("STARTFRAME",args,body);
      String startframe = IvyXml.getAttrString(rslt,"STARTFRAME");
      String startrtn = IvyXml.getAttrString(rslt,"CLASS") + "." +
	    IvyXml.getAttrString(rslt,"METHOD") + IvyXml.getAttrString(rslt,"SIGNATURE");
      RootTestCase rtc = new RootTestCase(startframe,startrtn);
      for_problem.setCurrentTest(rtc);
      create_bubble = true;
      SwingUtilities.invokeLater(this);
    }

}	// end of inner class RoseSuggestAction








/********************************************************************************/
/*										*/
/*	Handle messages from ROSE						*/
/*										*/
/********************************************************************************/

private class RoseHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      BoardLog.logD("BUSH","ROSE message : " +  cmd + " " + msg.getText());
      switch (cmd) {
	 case "SUGGEST" :
	    handleSuggestion(msg.getXml());
	    msg.replyTo("<OK/>");
	    break;
	 case "ENDSUGGEST" :
	    handleEndSuggestion(msg.getXml());
	    msg.replyTo("<OK/>");
	    break;
	 case "TESTCREATE" :
	    BushTestGenerator.handleTestGenerated(msg.getXml());
	    msg.replyTo("<OK/>");
	    break;
       }
    }

}	// end of inner class RoseHandler




/********************************************************************************/
/*										*/
/*	Background task to start up cocker					*/
/*										*/
/********************************************************************************/

private static class CockerStarter implements Runnable {

   @Override public void run() {
      BushIndex idx = new BushIndex();
      idx.start();
    }

}	// end of inner class CockerStarter



}	// end of class BushFactory




/* end of BushFactory.java */

