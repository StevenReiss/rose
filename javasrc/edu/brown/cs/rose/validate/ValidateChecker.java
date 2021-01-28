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



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateChecker(ValidateContext ctx,ValidateTrace orig,ValidateTrace check)
{
   validate_context = ctx;
   original_execution = orig;
   check_execution = check;
   
   original_execution.setupForLaunch(ctx.getLaunch());
}


/********************************************************************************/
/*                                                                              */
/*      Checking methods                                                        */
/*                                                                              */
/********************************************************************************/

double check()
{
   if (check_execution == null) return 0;
   if (original_execution == null) return 5;
   if (validate_context.getProblem() == null) return 5;
   
   ValidateMatcher matcher = new ValidateMatcher(original_execution,check_execution);
   matcher.computeMatch();
   
   ValidateProblemChecker vpc = null;
   switch (validate_context.getProblem().getProblemType()) {
      case EXCEPTION :
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
   
   return 5;
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
      long t0 = execution_matcher.getControlChangeTime();
      long t1 = execution_matcher.getDataChangeTime();
      long t2 = execution_matcher.getProblemAfterTime();
      if (Math.min(t0,t1) > t2) return false;
      return true;
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
      if (!executionChanged()) return 0;
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
      if (!executionChanged()) return 0;
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
      if (!executionChanged()) return 0;
      return 0.5;
    }

}       // end of inner class ValidateCheckerException






}       // end of class ValidateChecker




/* end of ValidateChecker.java */

