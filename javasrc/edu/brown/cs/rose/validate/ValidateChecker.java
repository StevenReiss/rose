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
   if (validate_context.getProblem() == null) return 0.5;
   if (check_execution.getRootContext() == null) return 0;
   
   ValidateMatcher matcher = new ValidateMatcher(original_execution,check_execution,for_repair);
   matcher.computeMatch();
   
   ValidateProblemChecker vpc = null;
   switch (validate_context.getProblem().getProblemType()) {
      case EXCEPTION :
      case ASSERTION :
         vpc = new ValidateCheckerException(matcher);
         break;
      case EXPRESSION :
         vpc = new ValidateCheckerExpression(matcher);
         break;
      case LOCATION :
         vpc = new ValidateCheckerLocation(matcher);
         break;
      case VARIABLE :
         vpc = new ValidateCheckerVariable(matcher);
         break;
      default :
      case OTHER :
         break;
    }

   if (vpc != null) return vpc.validate();
   
   return DEFAULT_SCORE;
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
   
   protected ValidateProblemChecker(ValidateMatcher m) {
      execution_matcher = m;
    }
   
   abstract double validate();
   
   protected boolean executionChanged() {
      if (!execution_matcher.repairExecuted()) 
         return false;
      long t0 = execution_matcher.getControlChangeTime();
      long t1 = execution_matcher.getDataChangeTime();
      long t2 = execution_matcher.getProblemAfterTime();
      if (Math.min(t0,t1) > t2) return false;
      return true;
    }
   
   protected boolean exceptionThrown() {
      long t0 = execution_matcher.getControlChangeTime();
      long t2 = execution_matcher.getProblemTime();
      long t3 = check_execution.getExceptionTime();
      long t4 = execution_matcher.getMatchProblemTime();
      if (t0 < t4 && t4 <= 0 && t3 > 0 && t3 < t2) return true;
      
      return false;
   }
   
}       // end of inner class ValidateProblemChecker



/********************************************************************************/
/*                                                                              */
/*      Checker for exception problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerException extends ValidateProblemChecker {
   
   ValidateCheckerException(ValidateMatcher m) {
      super(m);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0;
      if (exceptionThrown()) return 0;
      
      // note we are unlikely to match if execption not thrown
      ValidateValue origexc = original_execution.getException();
      if (origexc != null && execution_matcher.getMatchProblemContext() != null) {
         ValidateValue checkexc = check_execution.getException();
         if (checkexc == null) {
            if (execution_matcher.getMatchProblemAfterTime() > 0) return 1.0;
            return 0.8;
          }
         else if (execution_matcher.getMatchProblemAfterTime() > 0) {
            if (check_execution.getExceptionTime() > execution_matcher.getMatchProblemAfterTime()) {
               return 0.8;
             }
          }
         else if (origexc.getDataType().equals(checkexc.getDataType())) return 0;
         else return 0.1;   
       }
      else if (execution_matcher.getMatchProblemContext() != null) return 0.5;
     
      if (check_execution.isReturn()) return 0.75;
      
      return 0.2;
    }
   
}       // end of inner class ValidateCheckerException




/********************************************************************************/
/*                                                                              */
/*      Checker for variable  problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerVariable extends ValidateProblemChecker {

   ValidateCheckerVariable(ValidateMatcher m) {
      super(m);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0.1;
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
      // might need to separate oval into type and value
      String nval = prob.getTargetValue();
      // might need to change nval to null to indicate any other value
      oval = fixValue(oval,otyp);
      nval = fixValue(nval,otyp);
      
      ValidateCall vc = execution_matcher.getMatchChangeContext();
      if (vc == null) return 0.1;
      ValidateVariable vv = vc.getVariables().get(var);
      if (vv == null) return 0.5;
      
      long t0 = execution_matcher.getMatchProblemTime();
      if (t0 > 0) {
         ValidateValue vval = vv.getValueAtTime(check_execution,t0);
         if (vval != null) {
            // this needs to be more sophisticated, i.e.
            // it needs to differential "null" from null,
            // and handle non-primitives
            String vvalstr = vval.getValue();
            if (oval == null && vvalstr == null) return 0;
            if (oval != null && oval.equals(vvalstr)) return 0.0;
            if (nval == null) return 0.9;
            if (nval.equals(vvalstr)) return 1.0;
          }
       }
      boolean haveold = false;
      for (ValidateValue vval : vv.getValues(check_execution)) {
         String vvalstr = vval.getValue();
         if (oval == null && vvalstr == null) haveold = true;
         else if (oval != null && oval.equals(vvalstr)) haveold = true;
         else if (nval == null || nval.equals(vvalstr)) {
            if (haveold) return 0.60;
            return 0.75;
          }
       }
      
      return 0.5;
    }
   
}       // end of inner class ValidateCheckerException




/********************************************************************************/
/*                                                                              */
/*      Checker for exception problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerExpression extends ValidateProblemChecker {
   
   ValidateCheckerExpression(ValidateMatcher m) {
      super(m);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0.1;
      return 0.5;
    }
   
}       // end of inner class ValidateCheckerException




/********************************************************************************/
/*                                                                              */
/*      Checker for exception problems                                          */
/*                                                                              */
/********************************************************************************/

private class ValidateCheckerLocation extends ValidateProblemChecker {

   ValidateCheckerLocation(ValidateMatcher m) {
      super(m);
    }
   
   @Override double validate() {
      if (!executionChanged()) return 0.1;
      
      ValidateCall vc = execution_matcher.getMatchChangeContext();
      if (vc == null) return 0.7;
      long t0 = execution_matcher.getMatchProblemTime();
      if (t0 <= 0) return 0.7;
      ValidateVariable vv = vc.getLineNumbers();
      int lmatch = vv.getLineAtTime(t0);
      if (lmatch <= 0) return 0.7;
      
      ValidateVariable vv1 = execution_matcher.getProblemContext().getLineNumbers();
      int lorig = vv1.getLineAtTime(execution_matcher.getProblemTime());
      if (lorig == lmatch) return 0;
      
      return 0.5;
    }


}       // end of inner class ValidateCheckerException






}       // end of class ValidateChecker




/* end of ValidateChecker.java */

