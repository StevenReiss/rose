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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudLocalVariable;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.thorn.ThornChangeData;
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
private RootProblem     for_problem;
private ThornChangeData change_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ValidateChangedItems(BudLaunch bl,String frame,RootProblem prob)
{
   root_control = bl.getControl();
   for_launch = bl;
   for_frame = frame;
   for_problem = prob;
   
   ThornFactory tf = new ThornFactory(root_control);
   change_data = tf.getChangedVariables(for_launch,for_problem,for_frame);
}



/********************************************************************************/
/*										*/
/*	Processing methods: Simple parameters           			*/
/*										*/
/********************************************************************************/

List<ValidateAction> getParameterActions()
{
   Collection<ThornVariable> items = change_data.getTopParameters();

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
/*                                                                              */
/*      Processing methods -- all possible actions                              */
/*                                                                              */
/********************************************************************************/

List<ValidateAction> getResetActions(ValidateContext ctx)
{
   BudStack bs = for_launch.getStack();
   BudStackFrame startframe = null;
   for (BudStackFrame bsf : bs.getFrames()) {
      if (bsf.getFrameId().equals(for_frame)) {
         startframe = bsf;
         break;
       }
    }
   
   
   ValidateExecution ve = ctx.getBaseExecution();
   ValidateTrace vt = ve.getSeedeResult();
   ValidateCall vc = vt.getRootContext();
   
   ValidateSetup vs = new ValidateSetup(ctx,change_data,startframe,vc);

   List<ValidateAction> rslt = vs.findResets();
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Find actions for a particular frame and call                            */
/*                                                                              */
/********************************************************************************/

private List<ValidateAction> findResetActions(ValidateTrace vt,
      BudStackFrame bsf,ValidateCall vc,int fct)
{
   ValidateVariable linev = vc.getLineNumbers();
   long time = -1;
   for (ValidateValue vv : linev.getValues(vt)) {
      long lno = vv.getNumericValue();
      if (lno == bsf.getLineNumber()) {
         time = vv.getStartTime();
       }
      else if (time >= 0) {
         long endt = vv.getStartTime();
         List<ValidateAction> rstl = findResetActions(vt,bsf,vc,fct,time,endt);
         if (rstl != null) return rstl;
         time = -1;
       }
    }
   if (time >= 0) {
      long endt = vc.getEndTime();
      return findResetActions(vt,bsf,vc,fct,time,endt);
    }
   return null;
}




private List<ValidateAction> findResetActions(ValidateTrace vt,
      BudStackFrame bsf,ValidateCall vc,int fct,long time,long endt)
{
   if (fct != 0) {
      BudStackFrame bsf1 = for_launch.getStack().getFrames().get(fct-1);
      for (ValidateCall innercall : vc.getInnerCalls()) {
         if (innercall.getStartTime() >= time && innercall.getStartTime() < endt) {
            // this check might not work
            if (innercall.getMethod().equals(bsf1.getMethodName())) {
               if (checkVariables(bsf,vt,vc,vc.getStartTime())) {
                  System.err.println("CHECK NEXT LEVEL");
                }
               // might want to handle mulitple instances
               break;
             }
          }
       }
    }
   else {
      // check top-frame here
    }
   
   return null;
}

/********************************************************************************/
/*                                                                              */
/*      Check variable values                                                   */
/*                                                                              */
/********************************************************************************/

private boolean checkVariables(BudStackFrame frame,ValidateTrace vt,ValidateCall vc,long t0)
{
   for (Map.Entry<String,ValidateVariable> ent : vc.getVariables().entrySet()) {
      String vname = ent.getKey();
      BudLocalVariable bvar = frame.getLocal(vname);
      if (bvar == null) continue;
      ValidateVariable vv = ent.getValue();
      ValidateValue vval = vv.getValueAtTime(vt,t0);
      if (compareVariable(bvar,vval,t0) == Boolean.FALSE) return false;
    }
   
   return true;
}


private Boolean compareVariable(BudLocalVariable local,ValidateValue vv,long t0)
{
   switch (local.getKind()) {
      case "PRIMITIVE" :
      case "STRING" :
         String valtxt = vv.getValue();
         return local.getValue().equals(valtxt);
      case "ARRAY" :
         return compareArray(local,vv,t0);
      case "OBJECT" :
         return compareObject(local,vv,t0);
      default :
         break;
    }
   
   return null;
}



private Boolean compareArray(BudLocalVariable local,ValidateValue vv,long t0)
{
   if (local.getType().equals("null")) {
      if (vv.isNull()) return true;
      return false;
    }   
   
   if (!local.getType().equals(vv.getDataType())) return false;
   
   // check size and elements here
      
   return null;
}



private Boolean compareObject(BudLocalVariable local,ValidateValue vv,long t0)
{
   if (local.getType().equals("null")) {
      if (vv.isNull()) return true;
      return false;
    }
   
   if (!local.getType().equals(vv.getDataType())) return false;
   
   // check fields here
   
   return null;
}



}	// end of class ValidateChangedItems




/* end of ValidateChangedItems.java */

