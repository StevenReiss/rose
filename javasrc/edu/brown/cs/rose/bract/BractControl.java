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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootMetrics;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootProcessor;
import edu.brown.cs.rose.root.RootRepair;
import edu.brown.cs.rose.root.RootThreadPool;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootRepairFinder;
import edu.brown.cs.rose.root.RootTask;

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
private RootValidate base_validator;
private List<RootTask> sub_tasks;
private long    start_time;
private int     num_checked;

private static boolean sort_locations = false;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BractControl(RootControl ctrl,String id,RootProblem prob,RootLocation at,
        List<Class<?>> pclass,List<Class<?>> lclass,RootValidate validator)
{ 
   super("BractControl_" + id);
   rose_control = ctrl;
   reply_id = id;
   for_problem = prob;
   at_location = at;
   processor_classes = pclass;
   location_classes = lclass;
   base_validator = validator;
   sub_tasks = new ArrayList<>();
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
   try {
      work();
    }
   catch (Throwable t) {
      RoseLog.logE("BRACT","Problem executing control task",t);
    }
}



private void work()
{
   List<ProcessorTask> tasks = new ArrayList<>();
   start_time = System.currentTimeMillis();
   num_checked = 0;
   
   List<RootLocation> uselocs = null;
   if (at_location == null && location_classes.size() > 0) {
      uselocs = getLocations(); 
    }
   else {
      uselocs = new ArrayList<>();
      uselocs.add(at_location);
    }
   
   if (sort_locations) {
      Collections.sort(uselocs,new LocationSorter());
    }
   
   RoseLog.logI("BRACT","Start processing " + uselocs.size() + " " +
         location_classes.size());
   
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
   
   while (!sub_tasks.isEmpty()) {
      List<RootTask> t = null;
      synchronized (sub_tasks) {
         t = new ArrayList<>(sub_tasks);
         sub_tasks.clear();
       }
      for (RootTask rt : t) rt.waitForDone();
    }
   
   long t1 = System.currentTimeMillis();
   RootMetrics.noteCommand("BRACT","ENDREPAIR",t1-start_time,num_checked);
   CommandArgs args = new CommandArgs("NAME",reply_id,"CHECKED",num_checked);
   rose_control.sendRoseMessage("ENDSUGGEST",args,null,-1);
}



private class LocationSorter implements Comparator<RootLocation> {
   
   @Override public int compare(RootLocation l1,RootLocation l2) {
      return Double.compare(l2.getPriority(),l1.getPriority());
    }
   
}


@Override public void addSubtask(RootTask rt)
{
   synchronized (sub_tasks) {
      sub_tasks.add(rt);
    }
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
      rrf.setup(this,for_problem,l);
    }
   catch (Throwable t) { }
   if (rrf == null) return null;
   ProcessorTask pt = new ProcessorTask(rrf,base_validator);
   RoseLog.logD("BRACT","Queue finder " + rrf.getClass() + " " + l.getLineNumber());
   RootThreadPool.start(pt);
   return pt;
}




private List<RootLocation> getLocations()
{
   // compute the set of all locations -- use stem processing?
   return rose_control.getLocations(for_problem);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods for repair finders                                       */
/*                                                                              */
/********************************************************************************/

@Override public void validateRepair(RootRepair br)
{
   ++num_checked;
   
   if (base_validator != null) {
      base_validator.validateAndSend(this,br);
    }
   else {
      sendRepair(br);
    }
}



@Override public void sendRepair(RootRepair br)
{
   long time = System.currentTimeMillis() - start_time;
   br.setTime(time);
   
   CommandArgs args = new CommandArgs("NAME",reply_id);
   
   IvyXmlWriter xw = new IvyXmlWriter();
   br.outputXml(xw);
   String body = xw.toString();
   
   long t1 = System.currentTimeMillis();
   RootMetrics.noteCommand("BRACT","SENDREPAIR",
         br.getPriority(),br.getValidatedPriority(),t1-start_time,br.getId(),br.getLogData(),br.getDescription());
   
   rose_control.sendRoseMessage("SUGGEST",args,body,0);
}


@Override public boolean haveGoodResult()
{
   if (base_validator == null) return false;
   return base_validator.haveGoodResult();
}

/********************************************************************************/
/*                                                                              */
/*      Processor task                                                          */
/*                                                                              */
/********************************************************************************/

private static class ProcessorTask extends RootTask implements PriorityTask {
   
   private RootRepairFinder repair_finder;
   private RootValidate repair_validate;
   
   ProcessorTask(RootRepairFinder brf,RootValidate rv) {
      repair_finder = brf;
      repair_validate = rv;
    }
   
   @Override public void run() {
      RootLocation loc = repair_finder.getLocation();
      if (loc == null) {
         RoseLog.logD("BRACT","Start repair finder " + repair_finder.getClass());
       }
      else {
         RoseLog.logD("BRACT","Start repair finder " + repair_finder.getClass() + 
               " at " + loc.getFile() + " " + loc.getLineNumber());
       }
   
      try {
         if (repair_validate.canCheckResult(loc.getPriority(),repair_finder.getFinderPriority())) {
            repair_finder.process();
          }
         else {
            RoseLog.logD("BRACT","Skipping repair");
          }
       }
      catch (Throwable t) {
         RoseLog.logE("BRACT","Problem in repair finder",t);
       }
      finally {
         RoseLog.logD("BRACT","Finish repair finder " + repair_finder.getClass());
         synchronized (this) {
            noteDone();
          }
       }
    }
   
   
   
   @Override public double getTaskPriority() {
      double v = repair_finder.getFinderPriority() * 0.5;
      if (repair_finder.getLocation() != null) {
         double lv = repair_finder.getLocation().getPriority();
         v += lv * 0.01;
       }
      
      return v;
    }
   
}       // end of inner class ProcessorTask


}       // end of class BractControl




/* end of BractControl.java */

