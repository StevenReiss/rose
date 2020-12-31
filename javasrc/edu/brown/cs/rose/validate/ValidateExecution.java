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

import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootControl;
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
private String          exec_id;
private Element         seede_result;
private ExecState       exec_state;

private static AtomicInteger exec_counter = new AtomicInteger();


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateExecution(String sid)
{
   session_id = sid;
   seede_result = null;
   exec_state = ExecState.INITIAL;
   exec_id = "ROSE_EXEC_" + exec_counter.incrementAndGet();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getSessionId()                   { return session_id; }



/********************************************************************************/
/*                                                                              */
/*      Start method                                                    */
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
   
   CommandArgs args = new CommandArgs("EXECID",exec_id,
         "CONTINUOUS",false);
   rc.sendSeedeMessage(session_id,"EXEC",args,null);
   
   // for debugging only:
   Element xml = getSeedeResult();
   RoseLog.logD("VALIDATE","Execution returned: " + IvyXml.convertXmlToString(xml));
}




/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

synchronized void handleResult(Element xml)
{
   seede_result = xml;
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

Element getSeedeResult()
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


}       // end of class ValidateExecution




/* end of ValidateExecution.java */

