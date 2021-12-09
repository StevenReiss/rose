/********************************************************************************/
/*                                                                              */
/*              RoseEvalConstants.java                                          */
/*                                                                              */
/*      Constants for evaluation routines                                       */
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



package edu.brown.cs.rose.roseeval;

import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.rose.root.RootConstants;

public interface RoseEvalConstants extends MintConstants, RootConstants
{


enum TestType { ROSE, PICOT };


String ECLIPSE_PATH_MAC = "/vol/Developer/jee-2020-09/Eclipse.app/Contents/MacOS/eclipse";
String ECLIPSE_DIR_MAC = "/Users/spr/Eclipse/";



class LaunchData {
   
   private String project_name;
   private MintControl mint_control;
   private String lanuch_id;
   private String target_id;
   private String process_id;
   private String thread_id;
   
   LaunchData(MintControl mc,String proj,String launch,String target,
         String process,String thread) {
      mint_control = mc;
      project_name = proj;
      lanuch_id = launch;
      target_id = target;
      process_id = process;
      thread_id = thread;
    }
   
   MintControl getMintControl()                 { return mint_control; }
   String getProject()                          { return project_name; }
   String getLaunchId() 			{ return lanuch_id; }
   String getTargetId() 			{ return target_id; }
   String getProcessId()			{ return process_id; }
   String getThreadId() 			{ return thread_id; }
   
   void setThreadId(String id)			{ thread_id = id; }

}	// end of inner class LaunchData





}       // end of interface RoseEvalConstants




/* end of RoseEvalConstants.java */

