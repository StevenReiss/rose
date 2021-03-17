/********************************************************************************/
/*                                                                              */
/*              ValidateExecution.java                                          */
/*                                                                              */
/*      Representation of a SEEDE execution                                     */
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



package edu.brown.cs.rose.validate;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootRepair;
import edu.brown.cs.rose.root.RootTestCase;
import edu.brown.cs.rose.root.RoseLog;

class ValidateExecution implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum ExecState { INITIAL, PENDING, READY };

private String          session_id;
private ValidateTrace   seede_result;
private ExecState       exec_state;
private ValidateContext for_context;
private RootRepair      for_repair;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateExecution(String sid,ValidateContext ctx,RootRepair repair)
{
   session_id = sid;
   for_context = ctx;
   seede_result = null;
   exec_state = ExecState.INITIAL;
   for_repair = repair;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getSessionId()                   { return session_id; }

RootRepair getRepair()                  { return for_repair; }



/********************************************************************************/
/*                                                                              */
/*      Start method                                                            */
/*                                                                              */
/********************************************************************************/

void start(RootControl rc)
{
   synchronized(this) {
      seede_result = null;
      exec_state = ExecState.PENDING;
    }
   
   ValidateFactory vfac = ValidateFactory.getFactory(rc);
   vfac.register(this);
   
   CommandArgs args = new CommandArgs("EXECID",session_id,
         "CONTINUOUS",false,"MAXTIME",1000000,"MAXDEPTH",100);
   Element r1 = rc.sendSeedeMessage(session_id,"EXEC",args,null);
   if (!IvyXml.isElement(r1,"RESULT")) {
      RoseLog.logD("VALIDATE","Exec setup returned: " + IvyXml.convertXmlToString(r1));     exec_state = ExecState.READY;
      return;
    }
}




/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

synchronized void handleResult(Element xml)
{
   seede_result = new ValidateTrace(xml,for_context.getLaunch().getThread());
   exec_state = ExecState.READY;
   notifyAll();
}


synchronized void handleReset()
{
   seede_result = null;
   exec_state = ExecState.PENDING;
}


synchronized String handleInput(String file)
{
   return null; 
}


synchronized String handleInitialValue(String what)
{
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Get result                                                              */
/*                                                                              */
/********************************************************************************/

ValidateTrace getSeedeResult()
{
   synchronized (this) {
      while (exec_state != ExecState.READY) {
         try {
            wait(3000);
          }
         catch (InterruptedException e) { }
       }
      return seede_result;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle test cases                                                       */
/*                                                                              */
/********************************************************************************/

double checkTest(RootTestCase rtc)
{
   if (rtc == null) return 1;
   
   return seede_result.checkTest(rtc);
}



}       // end of class ValidateExecution




/* end of ValidateExecution.java */

