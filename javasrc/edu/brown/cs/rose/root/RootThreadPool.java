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

import java.util.concurrent.LinkedBlockingQueue;
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

private static RootThreadPool   the_pool = new RootThreadPool();

private static AtomicInteger    thread_counter = new AtomicInteger();

private boolean do_debug = true;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RootThreadPool()
{
   if (do_debug) {
      thread_pool = new ThreadPoolExecutor(1,1,30000,TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),new OurThreadFactory());
    }
   else {
      thread_pool = new ThreadPoolExecutor(2,12,30000,TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),new OurThreadFactory());
    }
}


/********************************************************************************/
/*                                                                              */
/*      Static processing methods                                               */
/*                                                                              */
/********************************************************************************/

public static void start(Runnable r) 
{
   if (r != null) {
      the_pool.thread_pool.execute(r);
    }
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


private static class ProcThread extends Thread implements LoggerThread {
   
   private int thread_id;
   
   ProcThread(Runnable r,int i) {
      super(r,"RootProcessor_" + i);
    }
    
   @Override public int getLogId()                      { return thread_id; }
   
}       // end of inner class ProcThread



}       // end of class RootThreadPool




/* end of RootThreadPool.java */

