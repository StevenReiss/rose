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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootProcessor;
import edu.brown.cs.rose.root.RootRepair;
import edu.brown.cs.rose.root.RootThreadPool;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootRepairFinder;

public class BractControl extends Thread implements RootProcessor, BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl rose_control;
private String reply_id;
private RootProblem for_problem;
private RootLocation at_location;
private List<Class<?>> processor_classes;
private List<Class<?>> location_classes;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BractControl(RootControl ctrl,String id,RootProblem prob,RootLocation at)
{ 
   rose_control = ctrl;
   reply_id = id;
   for_problem = prob;
   at_location = at;
   processor_classes = new ArrayList<>();
   location_classes = new ArrayList<>();
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
      if (!RootRepairFinder.class.isAssignableFrom(cls)) {
         return false;
       }
      Constructor<?> cnst = cls.getConstructor();
      RootRepairFinder rrf = (RootRepairFinder) cnst.newInstance();
      boolean loc = rrf.requiresLocation();
      if (loc) location_classes.add(cls);
      else processor_classes.add(cls);
      return true;
    }
   catch (ClassNotFoundException e) { }
   catch (NoSuchMethodException e) { }
   catch (InvocationTargetException e) { }
   catch (IllegalAccessException e) { }
   catch (InstantiationException e) { }
   
   return false;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public RootControl getController()              { return rose_control; }



/********************************************************************************/
/*                                                                              */
/*      Main processing method                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void run()
{
   List<ProcessorTask> tasks = new ArrayList<>();
   
   List<RootLocation> uselocs = null;
   if (at_location == null && location_classes.size() > 0) {
     uselocs = getLocations(); 
    }
   
   if (for_problem != null) {
      for (Class<?> cls : processor_classes) {
         ProcessorTask pt = startTask(cls,for_problem,null);
         if (pt != null) tasks.add(pt);
       }
      for (Class<?> cls : location_classes) {
         for (RootLocation loc : uselocs) {
            ProcessorTask pt = startTask(cls,for_problem,loc);
            if (pt != null) tasks.add(pt);
          }
       }
    }
   
   for (ProcessorTask pt : tasks) {
      pt.waitForDone();
    }
   
   CommandArgs args = new CommandArgs("NAME",reply_id);
   rose_control.sendRoseMessage("ENDSUGGEST",args,null,-1);
}


private ProcessorTask startTask(Class<?> cls,RootProblem p,RootLocation l)
{
   Constructor<?> cnst = null;
   try {
      cnst = cls.getConstructor();
    }
   catch (NoSuchMethodException e) { }
   if (cnst == null) return null;
   RootRepairFinder rrf = null;
   try {
      rrf = (RootRepairFinder) cnst.newInstance();
      
      rrf.setup(this,for_problem,at_location);
    }
   catch (Throwable t) { }
   if (rrf == null) return null;
   ProcessorTask pt = new ProcessorTask(rrf);
   RootThreadPool.start(pt);
   return pt;
}


private List<RootLocation> getLocations()
{
   // compute the set of all locations -- use stem processing?
   
   return new ArrayList<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods for repair finders                                       */
/*                                                                              */
/********************************************************************************/

public void addRepair(RootRepair br)
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
   
   private RootRepairFinder repair_finder;
   private boolean is_done;
   
   ProcessorTask(RootRepairFinder brf) {
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


}       // end of class BractControl




/* end of BractControl.java */

