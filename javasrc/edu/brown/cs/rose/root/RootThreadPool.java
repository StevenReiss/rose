/********************************************************************************/
/*                                                                              */
/*              RootThreadPool.java                                             */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.rose.root;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import edu.brown.cs.ivy.file.IvyLog.LoggerThread;

public class RootThreadPool implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ThreadPoolExecutor      thread_pool;

private static RootThreadPool   the_pool = null;

private static AtomicInteger    thread_counter = new AtomicInteger();

private static int max_threads = 4;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RootThreadPool()
{
   BlockingQueue<Runnable> bq = new PriorityBlockingQueue<>(10,new TaskComparator());
   if (max_threads <= 1) {
      thread_pool = new ThreadPoolExecutor(1,1,30000,TimeUnit.MILLISECONDS,
            bq,new OurThreadFactory());
    }
   else {
      thread_pool = new ThreadPoolExecutor(max_threads,max_threads,
            30000,TimeUnit.MILLISECONDS,
            bq,new OurThreadFactory());
    }
}


/********************************************************************************/
/*                                                                              */
/*      Static processing methods                                               */
/*                                                                              */
/********************************************************************************/

public synchronized static void start(Runnable r) 
{
   if (the_pool == null) {
      the_pool = new RootThreadPool();
    }
   if (r != null) {
      the_pool.thread_pool.execute(r);
      RoseLog.logD("Start task " + r + " " + the_pool.thread_pool.getQueue().size() + " " +
            the_pool.thread_pool.getPoolSize() + " " +
            the_pool.thread_pool.getLargestPoolSize());
    }
}

public static void setMaxThreads(int max)
{
   max_threads = max;
}



/********************************************************************************/
/*                                                                              */
/*      Factory for creating threads                                            */
/*                                                                              */
/********************************************************************************/

private static class OurThreadFactory implements ThreadFactory {
   
   @Override public Thread newThread(Runnable r) {
      Thread t = new ProcThread(r,thread_counter.incrementAndGet());
      return t;
    }
   
}       // end of inner class OurThreadFactory


private static class TaskComparator implements Comparator<Runnable> {
   
   @Override public int compare(Runnable r1,Runnable r2) {
      double p1 = 0;
      double p2 = 0;
      if (r1 instanceof PriorityTask) {
         p1 = ((PriorityTask) r1).getTaskPriority();
       }
      if (r2 instanceof PriorityTask) {
         p2 = ((PriorityTask) r2).getTaskPriority();
       }
      if (p1 < p2) return 1;
      if (p1 > p2) return -1;
      return 0;
    }
   
}       // end of inner class TaskComparator


private static class ProcThread extends Thread implements LoggerThread {
   
   private int thread_id;
   
   ProcThread(Runnable r,int i) {
      super(r,"RootProcessor_" + i);
    }
    
   @Override public int getLogId()                      { return thread_id; }
   
}       // end of inner class ProcThread



}       // end of class RootThreadPool




/* end of RootThreadPool.java */

