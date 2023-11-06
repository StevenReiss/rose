/********************************************************************************/
/*										*/
/*		ValidateFactory.java						*/
/*										*/
/*	Seede Access for Verification of Edit-Based Repairs access class	*/
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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootTestCase;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseLog;
public class ValidateFactory implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

public synchronized static  ValidateFactory getFactory(RootControl rc)
{
   ValidateFactory vf = factory_map.get(rc);
   if (vf == null) {
      vf = new ValidateFactory(rc);
      factory_map.put(rc,vf);
    }
   
   return vf;
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RootControl     root_control;
private Map<String,ValidateExecution> exec_map;

private static Map<RootControl,ValidateFactory> factory_map = new HashMap<>();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private ValidateFactory(RootControl ctrl)
{
   root_control = ctrl;
   exec_map = new HashMap<>();
   
   MintControl mc = root_control.getMintControl();
   mc.register("<SEEDEXEC TYPE='_VAR_0' ID='_VAR_1' />",new SeedeHandler());
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public RootControl getControl()                 { return root_control; }




/********************************************************************************/
/*										*/
/*	Create a validate structure to handle validation data			*/
/*										*/
/********************************************************************************/

public RootValidate createValidate(RootProblem prob,String frameid,RootLocation atloc,boolean showall,boolean tostring,boolean toarray)
{
   BudLaunch bl = new BudLaunch(root_control,prob);
   
   if (frameid == null) {
      RootTestCase rtc = prob.getCurrentTest();
      if (rtc != null) {
         frameid = rtc.getFrameId();
       }
      RoseLog.logD("VALIDATE","Use start from test case " + frameid);
    }

   if (frameid == null) {
      RoseLog.logD("VALIDATE","Create start location");
      ValidateStartLocator ssl = new ValidateStartLocator(prob,bl,atloc);
      frameid = ssl.getStartingFrame(false);
    }
   if (frameid == null) return null;

   ValidateContext ctx = new ValidateContext(root_control,prob,frameid);
   
   ctx.setupBaseExecution(showall,tostring,toarray);
   
   return ctx;
}



/********************************************************************************/
/*                                                                              */
/*      Just compute the starting point                                         */
/*                                                                              */
/********************************************************************************/

public BudStackFrame getStartingFrame(RootProblem prob,RootLocation atloc,boolean usecur)
{
   BudLaunch bl = new BudLaunch(root_control,prob);
   ValidateStartLocator ssl = new ValidateStartLocator(prob,bl,atloc);
   String frameid = ssl.getStartingFrame(usecur);
   for (BudStackFrame bsf : bl.getStack().getFrames()) {
      if (bsf.getFrameId().equals(frameid)) return bsf;
    }
   return null;
}



void register(ValidateExecution ve)
{
   exec_map.put(ve.getSessionId(),ve);
}


void unregister(String ssid)
{
   exec_map.remove(ssid);
}



/********************************************************************************/
/*                                                                              */
/*      Handle seede returns                                                    */
/*                                                                              */
/********************************************************************************/

private class SeedeHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      String txt = msg.getText();
      RoseLog.logD("VALIDATE","SEEDE message: " + txt);
      
      if (txt.length() > 102400000) {
         RoseLog.logW("VALIDATE","Seede message length " + txt.length());
       }
      
      String typ = args.getArgument(0);
      String id = args.getArgument(1);
      Element xml = msg.getXml();
      ValidateExecution ve = exec_map.get(id);
      if (ve == null) {
         msg.replyTo();
         return;
       }
      
      String rslt = null;
      switch (typ) {
         case "EXEC" :
            ve.handleResult(xml);
            break;
         case "RESET" :
            ve.handleReset();
            break;
         case "INPUT" :
            rslt = ve.handleInput(IvyXml.getAttrString(xml,"FILE"));
            break;
         case "INITIALVALUE" :
            rslt = ve.handleInitialValue(IvyXml.getAttrString(xml,"WHAT"));
            break;
         default :
            RoseLog.logE("VALIDATE","Unknown seede command " + typ);
            break;
       }
      
      msg.replyTo(rslt);
    }
   
}       // end of inner class SeedeHandler



}	// end of class ValidateFactory




/* end of ValidateFactory.java */

