/********************************************************************************/
/*										*/
/*		ValidateContext.java				       	*/
/*										*/
/*	Implementation of a validation context					*/
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



package edu.brown.cs.rose.validate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootProcessor;
import edu.brown.cs.rose.root.RootRepair;
import edu.brown.cs.rose.root.RootThreadPool;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseLog;

class ValidateContext implements RootValidate, ValidateConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RootControl	root_control;
private RootProblem	for_problem;
private String          frame_id;
private BudLaunch	for_launch;
private String          base_session;
private ValidateExecution base_execution;
private List<ValidateAction> setup_actions;
private int             num_checked;
private long            seede_total;
private double          best_score;
private double          location_priority;
private double          repair_priority;
private double          finder_priority;
private Map<File,ValidateExecution> testfile_map;

private static final int        MAX_CHECKED_OK = 100;
private static final int        MIN_CHECKED_OK = 60;
private static final long       MAX_SEEDE_OK = 600000;
private static final long       MIN_SEEDE_OK = 50000;
private static final int        MAX_CHECKED = 300;
private static final long       MAX_SEEDE_TOTAL = 3000000;
private static final long       TIME_MULTIPLIER = 10;

private static final double     GOOD_SCORE = 0.7;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValidateContext(RootControl ctrl,RootProblem p,String fid)
{
   this(ctrl);
   
   for_problem = p;
   for_launch = new BudLaunch(root_control,for_problem);
   frame_id = fid;
   if (frame_id == null) frame_id = for_launch.getFrame();
   
   testfile_map = new HashMap<>();
}







private ValidateContext(RootControl ctrl)
{
   root_control = ctrl;
   for_problem = null;
   for_launch = null;
   frame_id = null;
   setup_actions = new ArrayList<>();
   num_checked = 0;
   seede_total = 0;
   best_score = 0;
   location_priority = 1.0;
   repair_priority = 1.0;
   finder_priority = 1.0;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public RootProblem getProblem()               { return for_problem; }



BudLaunch getLaunch()                                   { return for_launch; }



RootControl getControl()                                { return root_control; }

ValidateExecution getBaseExecution()                    { return base_execution; }

@Override public ValidateTrace getExecutionTrace()     
{
   return base_execution.getSeedeResult();
}

long getMaxTime()
{
   long dflt = 100000;
   
   if (for_problem.getCurrentTest() != null) {
      long ticks = for_problem.getCurrentTest().getMaxTime();
      if (ticks > 0) dflt = ticks;
    }
   if (base_execution == null) return dflt;
   
   long time = base_execution.getExecutionTime();
   if (time <= 0) time = dflt;
   else if (time < dflt) {
      time = Math.max(time,5000);
      time = TIME_MULTIPLIER*time;
      if (time > dflt*2) time = dflt*2;
    }   
   else time = dflt;

   return time;
}



@Override public boolean addLocalFile(File f,String src)
{
   // Add all the loaded files
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("FILE");
   xw.field("NAME",f.getPath());
   xw.cdataElement("CONTENTS",src);
   xw.end("FILE");
   String cnts = xw.toString();
   xw.close();
   root_control.sendSeedeMessage(base_session,"ADDFILE",null,cnts);
   return true;
}



@Override public boolean editLocalFile(File f,int start,int end,String cnts)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("EDIT");
   xw.field("FILE",f.getPath());
   xw.begin("EDIT");
   xw.field("FILE",f.getPath());
   xw.field("OFFSET",start);
   xw.field("LENGTH",end-start);
   xw.field("INCEND",end);
   xw.field("EXCEND",end);
   xw.field("ID",1);
   xw.field("COUNTER",1);
   if (cnts == null) {
      xw.field("TYPE","DELETE");
    }
   else if (start == end) {
      xw.field("TYPE","INSERT");
      xw.cdataElement("TEXT",cnts);
    }
   else { 
      xw.field("TYPE","REPLACE");
      xw.cdataElement("TEXT",cnts);
    }
   xw.end("EDIT");
   xw.end("EDIT");
   String edits = xw.toString();
   xw.close();
   
   ValidateExecution ve = testfile_map.get(f);
   String ssid = base_session;
   if (ve != null) ssid = ve.getSessionId();
   
   String sts = handleEdits(ssid,edits);
   
   return (!sts.equals("FAIL"));
}



@Override public void createTestSession(File f,RootLocation loc)
{
   if (base_session == null) return;
   if (testfile_map.get(f) != null) return;
   
   IvyXmlWriter xw = new IvyXmlWriter();
   loc.outputXml(xw);
   
   String cnts = xw.toString();
   xw.close();
   
   
   Element rslt = root_control.sendSeedeMessage(base_session,"SUBSESSION",null,cnts);
   if (!IvyXml.isElement(rslt,"RESULT")) return;
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   String ssid = IvyXml.getAttrString(sessxml,"ID");  
   if (ssid == null) return;
   if (setup_actions != null) {
      boolean first = true;
      for (ValidateAction va : setup_actions) {
         va.perform(root_control,ssid,for_launch.getThread(),first);
         first = false;
       }
    }
   
   ValidateExecution ve = new ValidateExecution(ssid,this,null);
   
   testfile_map.put(f,ve);
}


@Override public RootTrace getTestTrace(File f)
{
   ValidateExecution ve = testfile_map.get(f);
   if (ve == null) return null;
   ve.start(root_control);
   
   return ve.getSeedeResult();
}



@Override public void finishTestSession(File f)
{
   ValidateExecution ve = testfile_map.remove(f);
   if (ve == null) return;
   
   removeSubsession(ve.getSessionId());
}



/********************************************************************************/
/*                                                                              */
/*      Validation methods                                                      */
/*                                                                              */
/********************************************************************************/

public void validateAndSend(RootProcessor rp,RootRepair rr)
{
   ValidateRunner vr = new ValidateRunner(this,rp,rr);
   RootThreadPool.start(vr);
   rp.addSubtask(vr);
}



public void runTestSession(ValidateExecution ve)
{
   ValidateTestSetupRunner vr = new ValidateTestSetupRunner(ve);
   RootThreadPool.start(vr);
}





/********************************************************************************/
/*                                                                              */
/*      Get base-line seede execution                                           */
/*                                                                              */
/********************************************************************************/

void setupBaseExecution(boolean showall,boolean tostring,boolean toarray)
{
   // set up the base session in SEEDE
   CommandArgs args = new CommandArgs("TYPE","LAUNCH",
         "PROJECT",for_problem.getBugLocation().getProject(),
         "LAUNCHID",for_launch.getLaunch(),
         "THREADID",for_launch.getThread(),
         "FRAMEID",frame_id); 
   if (showall) args.put("SHOWALL",true);
   if (tostring) args.put("TOSTRING",true);
   if (toarray) args.put("TOARRAY",true);
   
   Element rslt = root_control.sendSeedeMessage(null,"BEGIN",args,null);
   if (!IvyXml.isElement(rslt,"RESULT")) {
      RoseLog.logE("VALIDATE","No result from BEGIN message");
      return;
    }
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   base_session = IvyXml.getAttrString(sessxml,"ID");
   if (base_session == null) return;

   // Add all the loaded files
   IvyXmlWriter xw = new IvyXmlWriter();
   root_control.useFaitFilesForSeede();
   for (File f : root_control.getSeedeFiles(for_launch.getThread())) {
      xw.begin("FILE");
      xw.field("NAME",f.getPath());
      xw.end("FILE");
    }
   String cnts = xw.toString();
   xw.close();
   root_control.sendSeedeMessage(base_session,"ADDFILE",null,cnts);
   
   ValidateChangedItems valuechanges = new ValidateChangedItems(for_launch,frame_id,for_problem);
   runBaseExecution(null);
   RoseLog.logD("VALIDATE","Problem time " + base_execution.getSeedeResult().getProblemTime());
   if (base_execution.getSeedeResult().getProblemTime() >= 0) return;
   
   List<ValidateAction> pchanges = valuechanges.getParameterActions();
   if (pchanges != null) setup_actions.addAll(pchanges);
   if (setup_actions.size() > 0 && checkBaseExecution(null)) return;
   
   // this needs to be more sophisticated to try multiple changes in series
   List<ValidateAction> changes = valuechanges.getResetActions(this);
   if (changes != null) {
      for (ValidateAction va : changes) {
         if (checkBaseExecution(va)) {
            setup_actions.add(va);
            return;
          }
       }
    }
   RoseLog.logE("VALIDATE","BAD BASE EXECUTION");
}


@Override public void setOutputOptions(boolean showall,boolean tostring,boolean toarray)
{
   CommandArgs args = new CommandArgs("SHOWALL",showall,
         "TOSTRING",tostring,
         "TOARRAY",toarray);
   root_control.sendSeedeMessage(base_session,"SHOW",args,null);
}



private boolean checkBaseExecution(ValidateAction va)
{
   Element rslt = root_control.sendSeedeMessage(base_session,"SUBSESSION",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) return true;
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   String ssid = IvyXml.getAttrString(sessxml,"ID");
   try {
      boolean first = true;
      for (ValidateAction vs : setup_actions) {
         vs.perform(root_control,ssid,for_launch.getThread(),first);
         first = false;
       }
      if (va != null) {
         va.perform(root_control,ssid,for_launch.getThread(),first);
         first = false;
       }
      ValidateExecution oexec = base_execution;
      runBaseExecution(ssid);
      if (base_execution.getSeedeResult().getProblemTime() >= 0) return true;
      if (oexec != null) base_execution = oexec;           // else ignore
      return false;
    }
   finally {
      removeSubsession(ssid);
    }
}



void runBaseExecution(String sid)
{
   if (sid == null) sid = base_session;
   base_execution = new ValidateExecution(sid,this,null);
   base_execution.start(root_control);
   
   base_execution.getSeedeResult().setupForLaunch(getLaunch());
}


ValidateExecution getSubsession(RootRepair repair) 
{
   if (base_session == null) return null;
   
   Element rslt = root_control.sendSeedeMessage(base_session,"SUBSESSION",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) return null;
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   String ssid = IvyXml.getAttrString(sessxml,"ID");
   if (ssid == null) return null;
   if (setup_actions != null) {
      boolean first = true;
      for (ValidateAction va : setup_actions) {
         va.perform(root_control,ssid,for_launch.getThread(),first);
         first = false;
       }
    }
   
   ValidateExecution ve = new ValidateExecution(ssid,this,repair);
   
   return ve;
}


void removeSubsession(String ssid)
{
   root_control.sendSeedeMessage(ssid,"REMOVE",null,null);
   ValidateFactory vf = ValidateFactory.getFactory(root_control);
   vf.unregister(ssid);
}


String handleEdits(String ssid,String edits)
{
   Element rslt = root_control.sendSeedeMessage(ssid,"EDITFILE",null,edits);
   
   String sts = IvyXml.getAttrString(rslt,"STATUS");
   if (sts == null) sts = "FAIL";
   return sts;
}



/********************************************************************************/
/*                                                                              */
/*      Execution comparison methods                                            */
/*                                                                              */
/********************************************************************************/

double checkValidResult(ValidateExecution ve)
{
   ValidateTrace e2 = base_execution.getSeedeResult();
   ValidateTrace e1 = ve.getSeedeResult();
   if (e1 == null || e1.isCompilerError()) return 0;
   if (ve.getRepair() == null) return 1;
   
   ValidateChecker checker = new ValidateChecker(this,e2,e1,ve.getRepair());
   
   return checker.check();
}


@Override public boolean checkTestResult(RootTrace testtrace)
{
   ValidateTrace testtr = (ValidateTrace) testtrace;
   ValidateTrace origtrace = base_execution.getSeedeResult();
   if (testtr.isCompilerError()) return false;
   ValidateChecker checker = new ValidateChecker(this,origtrace,testtr,null);
   boolean fg = checker.checkTest();
   return fg;
}



@Override public synchronized boolean canCheckResult(double locpri,double findpri)
{
   if (base_execution.getSeedeResult().getProblemTime() < 0) 
      return false;
   
   boolean rslt = true;
   if (haveGoodResult()) {
      if (num_checked > MAX_CHECKED_OK) rslt = false;
      if (num_checked < MIN_CHECKED_OK && seede_total > MAX_SEEDE_OK) rslt = false;
      if (!rslt && forceCheck(locpri,findpri)) rslt = true;
    }
   if (num_checked > MAX_CHECKED) rslt = false;
   else if (seede_total < MIN_SEEDE_OK) rslt = true;
   else if (seede_total > MAX_SEEDE_TOTAL) rslt = false;
   
   return rslt;
}


@Override public boolean haveGoodResult()
{
   return best_score >= GOOD_SCORE;
}


public boolean forceCheck(double locpri,double findpri)
{
   if (locpri < location_priority * 0.8) return false;

   double usepri = 0.95;
   double bestrepair = (locpri/4.0 + 0.75 +  1.0  + usepri)/3.0 * findpri;
   if (bestrepair < repair_priority) return false;
   
   RoseLog.logD("VALDIATE","Force check at this point");
   
   return true;
}




synchronized void noteSeedeLength(long t,RootRepair repair,double score) 
{
   num_checked++;
   seede_total += t;
   if (score > best_score) {
      best_score = score;
      double p = repair.getLocation().getPriority();
      if (p < location_priority) location_priority = p;
      double p1 = repair.getPriority();
      if (p1 < repair_priority) repair_priority = p1;
      double p2 = repair.getFinderPriority();
      if (p2 < finder_priority) finder_priority = p2;
    }
   repair.setCount(num_checked);
   repair.setSeedeCount(seede_total);
}



}	// end of class ValidateContext




/* end of ValidateContextImpl.java */

