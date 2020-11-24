/********************************************************************************/
/*                                                                              */
/*              BractControl.java                                               */
/*                                                                              */
/*      Semantic quick fix (quickrepair) controller                             */
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



package edu.brown.cs.rose.bract;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootThreadPool;
import edu.brown.cs.rose.root.RoseLog;

public class BractControl extends Thread implements BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl rose_control;
private String reply_id;
private BractProblem for_problem;
private List<Class<?>> processor_classes;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BractControl(RootControl ctrl,String id,BractProblem prob)
{ 
   rose_control = ctrl;
   reply_id = id;
   for_problem = prob;
   processor_classes = new ArrayList<>();
}


/********************************************************************************/
/*                                                                              */
/*      Register a processor                                                    */
/*                                                                              */
/********************************************************************************/

public boolean registerProcessor(String clsnm)
{
   try {
      Class<?> cls = (Class<?>) Class.forName(clsnm);
      if (BractRepairFinder.class.isAssignableFrom(cls)) {
         processor_classes.add(cls);
         return true;
       }
    }
   catch (ClassNotFoundException e) { }
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Main processing method                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void run()
{
   List<ProcessorTask> tasks = new ArrayList<>();
   
   if (for_problem != null) {
      for (Class<?> cls : processor_classes) {
         Constructor<?> cnst = null;
         try {
            cnst = cls.getConstructor(BractControl.class,BractProblem.class);
          }
         catch (NoSuchMethodException e) { }
         if (cnst == null) continue;
         BractRepairFinder brf = null;
         try {
            brf = (BractRepairFinder) cnst.newInstance(this,for_problem);
          }
         catch (Throwable t) { }
         if (brf == null) continue;
         ProcessorTask pt = new ProcessorTask(brf);
         RootThreadPool.start(pt);
       }
    }
   
   for (ProcessorTask pt : tasks) {
      pt.waitForDone();
    }
   
   CommandArgs args = new CommandArgs("NAME",reply_id);
   rose_control.sendRoseMessage("ENDSUGGEST",args,null,-1);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods for repair finders                                       */
/*                                                                              */
/********************************************************************************/

public void addRepair(BractRepair br)
{
   CommandArgs args = new CommandArgs("NAME",reply_id);
   String body = null; // build from repair
   
   rose_control.sendRoseMessage("SUGGEST",args,body,-1);
}



/********************************************************************************/
/*                                                                              */
/*      Processor task                                                          */
/*                                                                              */
/********************************************************************************/

private static class ProcessorTask implements Runnable {
   
   private BractRepairFinder repair_finder;
   private boolean is_done;
   
   ProcessorTask(BractRepairFinder brf) {
      repair_finder = brf;
      is_done = false;
    }
   
   @Override public void run() {
      try {
         repair_finder.process();
       }
      catch (Throwable t) {
         RoseLog.logE("BRACT","Problem in repair finder",t);
       }
      finally {
         synchronized (this) {
            is_done = true; 
            notifyAll();
          }
       }
    }
   
   synchronized void waitForDone() {
      while (!is_done) {
         try {
            wait(4000);
          }
         catch (InterruptedException e) { }
       }
    }
   
}       // end of inner class ProcessorTask


}       // end of class BractProcessor




/* end of BractProcessor.java */

