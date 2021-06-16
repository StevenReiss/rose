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
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootProcessor;
import edu.brown.cs.rose.root.RootRepair;
import edu.brown.cs.rose.root.RootThreadPool;
import edu.brown.cs.rose.root.RootValidate;

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



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValidateContext(RootControl ctrl,RootProblem p,String fid)
{
   root_control = ctrl;
   for_problem = p;
   for_launch = new BudLaunch(root_control,for_problem);
   frame_id = fid;
   if (frame_id == null) frame_id = for_launch.getFrame();
   setup_actions = new ArrayList<>();
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
      time = 10*time;
      if (time > dflt*2) time = dflt*2;
    }   
   else time = dflt;

   return time;
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




/********************************************************************************/
/*                                                                              */
/*      Get base-line seede execution                                           */
/*                                                                              */
/********************************************************************************/

void setupBaseExecution()
{
   // set up the base session in SEEDE
   CommandArgs args = new CommandArgs("TYPE","LAUNCH",
         "PROJECT",for_problem.getBugLocation().getProject(),
         "LAUNCHID",for_launch.getLaunch(),
         "THREADID",for_launch.getThread(),
//       "SHOWALL",true,
         "FRAMEID",frame_id); 
   
   Element rslt = root_control.sendSeedeMessage(null,"BEGIN",args,null);
   if (!IvyXml.isElement(rslt,"RESULT")) return;
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   base_session = IvyXml.getAttrString(sessxml,"ID");
   if (base_session == null) return;

   // Add all the loaded files
   IvyXmlWriter xw = new IvyXmlWriter();
   for (File f : root_control.getLoadedFiles()) {
      xw.begin("FILE");
      xw.field("NAME",f.getPath());
      xw.end("FILE");
    }
   String cnts = xw.toString();
   xw.close();
   root_control.sendSeedeMessage(base_session,"ADDFILE",null,cnts);
   
   ValidateChangedItems valuechanges = new ValidateChangedItems(for_launch,frame_id,for_problem);
   runBaseExecution(null);
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
}



private boolean checkBaseExecution(ValidateAction va)
{
   Element rslt = root_control.sendSeedeMessage(base_session,"SUBSESSION",null,null);
   if (!IvyXml.isElement(rslt,"RESULT")) return true;
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   String ssid = IvyXml.getAttrString(sessxml,"ID");
   boolean first = true;
   for (ValidateAction vs : setup_actions) {
      vs.perform(root_control,ssid,for_launch.getThread(),first);
      first = false;
    }
   if (va != null) {
      va.perform(root_control,ssid,for_launch.getThread(),first);
      first = false;
    }
   runBaseExecution(ssid);
   if (base_execution.getSeedeResult().getProblemTime() >= 0) return true;
   return false;
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
   
   ValidateChecker checker = new ValidateChecker(this,e2,e1,ve.getRepair());
   
   return checker.check();
}

}	// end of class ValidateContext




/* end of ValidateContextImpl.java */

