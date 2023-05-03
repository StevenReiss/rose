/********************************************************************************/
/*										*/
/*		ValidateStartLocator.java					*/
/*										*/
/*	Find a starting point for validation					*/
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

import java.io.File;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;

class ValidateStartLocator implements ValidateConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RootControl  root_control;
private BudLaunch    for_launch;
private RootProblem  base_problem;
private RootLocation at_location;
private int          max_up;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValidateStartLocator(RootProblem rp,BudLaunch bl,RootLocation at)
{
   root_control = bl.getControl();
   for_launch = bl;
   base_problem = rp;
   at_location = at;
   max_up = rp.getMaxUp();
   if (max_up < 0) {
      BoardProperties bp = BoardProperties.getProperties("Rose");
      max_up = bp.getInt("Rose.max.up",5);
    }
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

String getStartingFrame(boolean usecur)
{
   String startframe = for_launch.getFrame();

   if (at_location != null) {
      startframe = updateFrameForLocation(startframe,at_location);
      // might want to move up stack anyway by calling findValidStart
    }
   else if (usecur) {
      // might want to check that startframe is a source frame
    }
   else if (base_problem.getMaxUp() >= 0) {
      int ct = 0;
      BudStackFrame last = null;
      for (BudStackFrame bsf : for_launch.getStack().getFrames()) {
         if (ct == base_problem.getMaxUp()) return bsf.getFrameId();
         last = bsf;
         ++ct;
       }
      if (last != null) startframe = last.getFrameId();
    }
   else {
      List<RootLocation> locs = root_control.getLocations(base_problem);
      if (locs != null) {
	 for (RootLocation loc : locs) {
	    startframe = updateFrameForLocation(startframe,loc);
	  }
       }
      startframe = findValidStart(startframe);
    }

   return startframe;
}




/********************************************************************************/
/*										*/
/*	Move up stack based on location 					*/
/*										*/
/********************************************************************************/

private String updateFrameForLocation(String fid,RootLocation loc)
{
   String m = loc.getMethod();

   boolean fnd = false;
   BudStack bs = for_launch.getStack();
   for (BudStackFrame bf : bs.getFrames()) {
      if (bf.getFrameId().equals(fid)) fnd = true;
      else if (fnd) {
	 String m1 = bf.getClassName() + "." + bf.getMethodName();
	 if (m1.equals(m)) {
	    fid = bf.getFrameId();
	  }
       }
    }

   return fid;
}



/********************************************************************************/
/*										*/
/*	Find a frame on stack for doing execution from				*/
/*										*/
/********************************************************************************/

private String findValidStart(String fid)
{
   boolean fnd = false;
   BudStack bs = for_launch.getStack();
   BudStackFrame prior = null;
   int ct = 0;
   for (BudStackFrame bf : bs.getFrames()) {
      if (bf.getFrameId().equals(fid)) fnd = true;
      else if (fnd) {
	 File f = bf.getSourceFile();
	 if (f != null && f.exists() && f.canRead()) {
	    String proj = null;
	    if (f != null) proj = root_control.getProjectForFile(f);
	    ASTNode n = root_control.getSourceNode(proj,f,-1,bf.getLineNumber(),true,false);
	    while (n != null) {
	       if (n.getNodeType() == ASTNode.METHOD_DECLARATION) break;
	       n = n.getParent();
	     }
	    // might want to check if method of n is not private
	    if (n != null) {
	       ++ct;
	       prior = bf;
	     }
	    else f = null;
	  }
	 else f = null;
	 if (f == null) {
	    break;
	  }
       }
    }
   
   if (prior != null && ct < max_up) {
      fid = prior.getFrameId();
    }

   return fid;
}




}	// end of class ValidateStartLocator




/* end of ValidateStartLocator.java */

