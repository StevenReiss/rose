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

import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootProblem;
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
private BudLaunch	for_launch;
private List<ValidateAction> setup_actions;
private String          base_session;
private ValidateExecution base_execution;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValidateContext(RootControl ctrl,RootProblem p,List<ValidateAction> acts)
{
   root_control = ctrl;
   for_problem = p;
   for_launch = new BudLaunch(root_control,for_problem);
   setup_actions = acts;
}





/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public RootProblem getProblem()               { return for_problem; }

BudLaunch getLaunch()                                   { return for_launch; }

List<ValidateAction> getActions()                       { return setup_actions; }



/********************************************************************************/
/*                                                                              */
/*      Get base-line seede execution                                           */
/*                                                                              */
/********************************************************************************/

void getBaseExecution()
{
   CommandArgs args = new CommandArgs("TYPE","LAUNCH",
         "PROJECT",for_problem.getBugLocation().getProject(),
         "LAUNCHID",for_launch.getLaunch(),
         "THREADID",for_launch.getThread(),
         "FRAMEID",for_launch.getFrame());
   
   IvyXmlWriter xw = new IvyXmlWriter();
   BudStack bs = for_launch.getStack();
   for (BudStackFrame bsf : bs.getFrames()) {
      xw.begin("LOCATION");
      xw.field("FILE",bsf.getSourceFile());
      xw.field("METHOD",bsf.getMethodName());
      xw.field("SIGNATURE",bsf.getMethodSignature());
      xw.field("CLASS",bsf.getClassName());
      xw.field("THREAD",for_launch.getThread());
      xw.field("ACTIVE",true);
      xw.field("LINE",bsf.getLineNumber());
      xw.end("LOCATION");
      if (bsf.getFrameId().equals(for_launch.getFrame())) break;
    }
   String cnts = xw.toString();
   xw.close();
   
   Element rslt = root_control.sendSeedeMessage(null,"BEGIN",args,cnts);
   if (!IvyXml.isElement(rslt,"RESULT")) return;
   Element sessxml = IvyXml.getChild(rslt,"SESSION");
   base_session = IvyXml.getAttrString(sessxml,"ID");
   if (base_session == null) return;
   
   for (ValidateAction va : setup_actions) {
      va.perform(root_control,base_session);
    }
   
   base_execution = new ValidateExecution(base_session);
   base_execution.start(root_control);
}


}	// end of class ValidateContext




/* end of ValidateContextImpl.java */

