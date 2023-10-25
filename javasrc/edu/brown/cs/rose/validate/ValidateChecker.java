/********************************************************************************/
/*                                                                              */
/*              ValidateChecker.java                                            */
/*                                                                              */
/*      Check if a new execution is valid for given problem                     */
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

import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootRepair;
import edu.brown.cs.rose.root.RoseLog;

class ValidateChecker implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ValidateContext         validate_context;
private ValidateTrace           original_execution;
private ValidateTrace           check_execution;
private RootRepair              for_repair;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateChecker(ValidateContext ctx,ValidateTrace orig,ValidateTrace check,RootRepair repair)
{
   validate_context = ctx;
   original_execution = orig;
   check_execution = check;
   for_repair = repair;
}


/********************************************************************************/
/*                                                                              */
/*      Checking methods                                                        */
/*                                                                              */
/********************************************************************************/

double check()
{
   if (check_execution == null) return 0;
   if (original_execution == null) return 0.5;
   if (validate_context.getProblem() == null) return 0;
   if (check_execution.getRootContext() == null) return 0;
   
   ValidateMatcher matcher = new ValidateMatcher(original_execution,check_execution,for_repair,false);
   matcher.computeMatch();
   
   ValidateProblemChecker vpc = null;
   switch (validate_context.getProblem().getProblemType()) {
      case EXCEPTION :
      case ASSERTION :
         vpc = new ValidateCheckerException(matcher,false);
         break;
      case EXPRESSION :
         vpc = new ValidateCheckerExpression(matcher,false);
         break;
      case LOCATION :
         vpc = new ValidateCheckerLocation(matcher,false);
         break;
      case VARIABLE :
         vpc = new ValidateCheckerVariable(matcher,false);
         break;
      case NONE :
         return 0;
      default :
      case OTHER :
         break;
    }

   double v0 = DEFAULT_SCORE;
   if (vpc != null) v0 = vpc.validate();
   
   if (v0 != 0) {
      double v1 = matcher.getControlChangeTime();
      double v2 = matcher.getDataChangeTime();
      double v3 = matcher.getProblemTime();
      double v4 = (v1 == 0 ? v2 : (v2 == 0 ? v1 : Math.min(v1,v2)));
      double v6 = v4/v3;
      v0 = (v0 * 0.95) + (v6 * 0.05);
    }
   
   return v0;
}


boolean checkTest()
{
   if (check_execution == null) return false;
   if (original_execution == null) return false;
   if (validate_context.getProblem() == null) return false;
   if (check_execution.getRootContext() == null) return false;
   
   ValidateMatcher matcher = new ValidateMatcher(original_execution,check_execution,null,true);
   matcher.computeMatch();
   
   ValidateProblemChecker vpc = null;
   switch (validate_context.getProblem().getProblemType()) {
      case EXCEPTION :
      case ASSERTION :
         vpc = new ValidateCheckerException(matcher,true);
         break;
      case EXPRESSION :
         vpc = new ValidateCheckerExpression(matcher,true);
         break;
      case LOCATION :
         vpc = new ValidateCheckerLocation(matcher,true);
         break;
      case VARIABLE :
         vpc = new ValidateCheckerVariable(matcher,true);
         break;
      case NONE :
         vpc = new ValidateCheckerNone(matcher,true);
         break;
      default :
      case OTHER :
         return false;
    }
   
   boolean fg = vpc.validateTest();
   
   return fg;
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

String fixValue(String val,String typ)
{
   if (val == null) return null;
   String rslt = val;
   int len = val.length();
   if (typ != null && typ.equals("java.lang.String") && 
         val.startsWith("\"") && val.endsWith("\"") && len >= 2) {
      rslt = val.substring(1,len-1);
    }
   
   if (val.equalsIgnoreCase("Non-Null")) return null;   // anything should work then
   
   return rslt;
}

/********************************************************************************/
/*                                                                              */
/*      General problem-specific checker                                        */
/*                                                                              */
/********************************************************************************/

private abstract class ValidateProblemChecker {
   
   protected ValidateMatcher execution_matcher;
   
   
   protected ValidateProblemChecker(ValidateMatcher m,boolean test) {
      execution_matcher = m;
    }
   
   abstract double validate();
   
   boolean validateTest() {
      long t0 = execution_matcher.getControlChangeTime();
      long t1 = execution_matcher.getDataChangeTime();
      long t2 = execution_matcher.getProblemTime();
      if (t0 > 0 && t0 < t2) return false;
      if (t1 > 0 && t1 < t2) return false;
      boolean fg = validateTestLocal();
      return fg;
    }
   abstract boolean validateTestLocal();
   
   protected boolean executionChanged() {
      long t0 = execution_matcher.getControlChangeTime();
      long t1 = execution_matcher.getDataChangeTime();
      long t2 = execution_matcher.getProblemAfterTime();
      if (!execution_matcher.repairExecuted()) return false;
      if (Math.min(t0,t1) > t2) return false;
      return true;
    }
   
   protected boolean exceptionThrown() {
      long t0 = execution_matcher.getControlChangeTime();
      long t2 = execution_matcher.getProblemTime();
      long t3 = check_execution.getExceptionTime();
      long t4 = execution_matcher.getMatchProblemTime();
      if (t0 < t2 && t4 <= 0 && t3 > 0 && t3 < t2) return true;
      
      return false;
   }
   
}       // end of inner class ValidateProblemChecker



/********************************************************************************/
/*                                                                              */
/*      Checker for exception problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerException extends ValidateProblemChecker {
   
   ValidateCheckerException(ValidateMatcher m,boolean test) {
      super(m,test);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0;
      if (exceptionThrown()) return 0;
      
      ValidateValue origexc = original_execution.getException();
      if (origexc != null && execution_matcher.getMatchProblemContext() != null) {
         ValidateValue checkexc = check_execution.getException();
         if (checkexc == null) {
            if (execution_matcher.getMatchProblemAfterTime() > 0) return 1.0;
            return 0.8;
          }
         else if (execution_matcher.getMatchProblemAfterTime() > 0) {
            if (check_execution.getExceptionTime() > execution_matcher.getMatchProblemAfterTime()) {
               return 0.5;
             }
          }
         else if (origexc.getDataType().equals(checkexc.getDataType())) return 0;
         else return 0.1;   
       }
      else if (execution_matcher.getMatchProblemContext() != null) return 0.5;
     
      if (check_execution.isReturn()) return 0.75;
      
      return 0.2;
    }
   
   @Override boolean validateTestLocal() {
      ValidateValue origexc = original_execution.getException();
      if (origexc != null && execution_matcher.getMatchProblemContext() != null) {
         ValidateValue checkexc = check_execution.getException();
         if (checkexc == null) return false;
         else if (origexc.getDataType().equals(checkexc.getDataType())) return true;
         else return false;
       }
      return false;
    }
   
}       // end of inner class ValidateCheckerException




/********************************************************************************/
/*                                                                              */
/*      Checker for variable  problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerVariable extends ValidateProblemChecker {

   ValidateCheckerVariable(ValidateMatcher m,boolean test) {
      super(m,test);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0;
      if (exceptionThrown()) return 0;
      
      RootProblem prob = validate_context.getProblem();
      String var = prob.getProblemDetail();
      String oval = prob.getOriginalValue();
      String otyp = null;
      int idx = -1;
      if (oval != null) idx = oval.indexOf(" ");
      if (idx > 0) {
         otyp = oval.substring(0,idx);
         oval = oval.substring(idx+1);
       }
      // might need to separate oval into type and v`alue
      String nval = prob.getTargetValue();
      // might need to change nval to null to indicate any other value
      oval = fixValue(oval,otyp);
      nval = fixValue(nval,otyp);
      RoseLog.logD("VALIDATE","Check variable values " + oval + " " + nval);
      
      ValidateCall vc = execution_matcher.getMatchProblemContext();
      if (vc == null) {
         RoseLog.logD("VALIDATE","No change context for variable");
         return 0.0;
       }   
      ValidateVariable vv = vc.getVariables().get(var);
      if (vv == null) {
         RoseLog.logD("VALIDATE","Variable not found in change context");
         return 0.5;
       }
      
      long t0 = execution_matcher.getMatchProblemTime();
      RoseLog.logD("VALIDATE","Match problem time " + t0);
      if (t0 > 0) {
         ValidateValue vval = vv.getValueAtTime(check_execution,t0);
         RoseLog.logD("VALIDATE","Value at time : " + vval);
         if (vval != null) {
            String vvalstr = vval.getValue();
            if (oval == null && vvalstr == null) return 0;
            if (oval != null && oval.equals(vvalstr)) return 0.0;
            
            return matchValue(vval,vvalstr,nval);
          }
       }
      boolean haveold = false;
      boolean haveother = false;
      for (ValidateValue vval : vv.getValues(check_execution)) {
         String vvalstr = vval.getValue();
         if (oval == null && vvalstr == null) {
            haveold = true;
            haveother = false;
          }     
         else if (oval != null && oval.equals(vvalstr)) haveold = true;
         else if (nval != null && !nval.equals(vvalstr)) {
            if (haveold) return 0.60;
            return 0.75;
          }
         else if (nval == null && vval.getStartTime() > 0) haveother = true;
       }
      
      if (!haveold) return 0.6;
      if (haveother) return 0.5;
      
      return 0.0;
    }
   
   @Override boolean validateTestLocal() {
      long t1 = execution_matcher.getDataChangeTime();
      if (t1 <= 0 || t1 > execution_matcher.getProblemAfterTime()) return true;
      return false;
    }
   
   private double matchValue(ValidateValue vval,String vvalstr,String nval)
   {
      if (nval == null) return 0.9;
      
      RoseLog.logD("VALIDATE","Match values " + vvalstr + " " + nval + " " + vval.getDataType());
         
      RootProblem prob = validate_context.getProblem();
      
      if (nval.equals(vvalstr)) return 1.0;
      if (vval.getDataType().equals("float") || vval.getDataType().equals("double")) {
         try {
            double v1 = Double.valueOf(vvalstr);
            double v2 = Double.valueOf(nval);
            double diff = Math.abs(v1-v2);
            if (diff <= prob.getTargetPrecision()) return 1.0;
          }
         catch (NumberFormatException e) {
            // should handle > x, < x, ...
            return 0.6;
          }
       }
      else if (vval.getDataType().equals("int") || vval.getDataType().equals("long")) {
         try {
            long v1 = Long.valueOf(vvalstr);
            long v2 = Long.valueOf(nval);
            if (v1 == v2) return 1.0;
          }
         catch (NumberFormatException e) {
            // should handle > x, < x, ...
            return 0.6;
          }
         // handle > x , < x , ...
       }
      else if (vval.getDataType().equals("boolean")) {
         Boolean v1 = getBoolean(vvalstr);
         Boolean v2 = getBoolean(nval);
         if (v1 != null && v2 != null) {
            if (v1.equals(v2)) return 1.0;
          }
       }
      else {
         // handle non-null, etc.
       }
         
      return 0.0;
   }
   
   private Boolean getBoolean(String s)
   {
      if (s == null || s.length() == 0) return null;
      return s.startsWith("tT1Yy");
   }
   
}       // end of inner class ValidateCheckerVariable



/********************************************************************************/
/*                                                                              */
/*      Check for NO Problems                                                   */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerNone extends ValidateProblemChecker {
   
   ValidateCheckerNone(ValidateMatcher m,boolean test) {
      super(m,test);
    }
   
   @Override double validate()                  { return 0.0; }
   
   @Override boolean validateTestLocal() {
      long t1 = execution_matcher.getDataChangeTime();
      if (t1 <= 0 || t1 > execution_matcher.getProblemAfterTime()) return true;
      // should check problem variables at the problem time rather than all variables
      return false;
    }
   
}       // end of inner class ValidateCheckerNone
      



/********************************************************************************/
/*                                                                              */
/*      Checker for exception problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerExpression extends ValidateProblemChecker {
   
   ValidateCheckerExpression(ValidateMatcher m,boolean test) {
      super(m,test);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0;
      if (exceptionThrown()) return 0;
      long t0 = execution_matcher.getMatchProblemTime();
      if (t0 < 0) return 0.2;
      return 0.5;
    }
   
   @Override boolean validateTestLocal() {
      return true;
    }
   
}       // end of inner class ValidateCheckerException




/********************************************************************************/
/*                                                                              */
/*      Checker for exception problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerLocation extends ValidateProblemChecker {

   ValidateCheckerLocation(ValidateMatcher m,boolean test) {
      super(m,test);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0;
      
      ValidateCall vc = execution_matcher.getMatchChangeContext();
      long t0 = execution_matcher.getMatchProblemTime();
      if  (vc != null) {
         if (t0 <= 0) {
            if (exceptionThrown()) return 0.2;
            return 0.8;
          }  
       }
     
      while (vc != null && t0 > vc.getEndTime()) {
         vc = vc.getParentCall();
       }
      
      if (vc == null) return 0.5;
      
      ValidateVariable vv = vc.getLineNumbers();
      int lmatch = vv.getLineAtTime(t0);
      if (lmatch <= 0) return 0.8;
      
      ValidateCall ovc = execution_matcher.getProblemContext();
      ValidateCall mvc = execution_matcher.getMatchProblemContext();
      if (ovc == null) ovc = vc;
      if (mvc == null) return 0.5;
      
      long t1 = execution_matcher.getProblemTime();
      ValidateVariable ovv = ovc.getLineNumbers();
      ValidateVariable mvv = mvc.getLineNumbers();
      int olno = ovv.getLineAtTime(t1);
      int mlno = mvv.getLineAtTime(t0);
      
      if (mlno == olno) return 0;
      
      boolean fnd = false;
      for (ValidateValue mv : mvv.getValues(check_execution)) {
         int elno = mv.getLineValue();
         if (elno == olno) fnd = true;
       }
      if (fnd) return 0.6;
      
      return 0.9;
    }

   @Override boolean validateTestLocal() {
      long t0 = execution_matcher.getMatchProblemTime();
      ValidateCall vc = execution_matcher.getMatchChangeContext();
      ValidateVariable vv = vc.getLineNumbers();
      int lmatch = vv.getLineAtTime(t0);
      if (lmatch <= 0) return false;
      return true;
    }

}       // end of inner class ValidateCheckerException






}       // end of class ValidateChecker




/* end of ValidateChecker.java */

