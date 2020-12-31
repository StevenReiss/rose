/********************************************************************************/
/*										*/
/*		ValidateChangedItems.java					*/
/*										*/
/*	Handle finding changed items for a stack frame				*/
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.thorn.ThornFactory;
import edu.brown.cs.rose.thorn.ThornConstants.ThornVariable;
import edu.brown.cs.rose.thorn.ThornConstants.ThornVariableType;

class ValidateChangedItems implements ValidateConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RootControl	root_control;
private BudLaunch	for_launch;
private String		for_frame;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValidateChangedItems(BudLaunch bl,String frame)
{
   root_control = bl.getControl();
   for_launch = bl;
   for_frame = frame;
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

List<ValidateAction> getChangeActions()
{
   Set<ThornVariable> items = findChangedItems();

   if (items == null) return null;

   List<ValidateAction> rslt = new ArrayList<>();

   Set<ThornVariable> params = new HashSet<>();
   for (ThornVariable tv : items) {
      if (tv.getVariableType() == ThornVariableType.PARAMETER) {
	 params.add(tv);
       }
    }
   if (!params.isEmpty()) {
      Map<String,BudValue> pvals = for_launch.getParameterValues();
      if (pvals != null) {
         for (ThornVariable tv : params) {
            BudValue bv = pvals.get(tv.getName());
            if (bv != null) rslt.add(ValidateAction.createSetAction(tv.getName(),bv));
          }
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Find the set of items that have changed 				*/
/*										*/
/********************************************************************************/

private Set<ThornVariable> findChangedItems()
{
   Set<ThornVariable> rslt = new HashSet<>();
   BudStack bs = for_launch.getStack();
   ThornFactory tf = new ThornFactory(root_control);

   for (BudStackFrame bf : bs.getFrames()) {
      File f = bf.getSourceFile();
      if (f != null && f.exists() && f.canRead()) {
	 String proj = null;
	 if (f != null) proj = root_control.getProjectForFile(f);
	 ASTNode n = root_control.getSourceNode(proj,f,-1,bf.getLineNumber(),true,true);
	 List<ThornVariable> fnd = tf.getChangedVariables(n);
	 if (fnd != null && !fnd.isEmpty()) {
	    boolean top = for_frame.equals(bf.getFrameId());
	    for (ThornVariable tv : fnd) {
	       if (!top && tv.getVariableType() == ThornVariableType.PARAMETER) continue;
	       rslt.add(tv);
	     }
	  }
	 // this needs to track what is internal and removed by outer method
	 // This should probably be done in THORN, with getChangedVariables() taking
	 // the current set as arguments and returning the resultant set
       }
    }

   return rslt;
}




}	// end of class ValidateChangedItems




/* end of ValidateChangedItems.java */

