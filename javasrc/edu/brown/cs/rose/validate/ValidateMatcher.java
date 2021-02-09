/********************************************************************************/
/*                                                                              */
/*              ValidateMatcher.java                                            */
/*                                                                              */
/*      Handle matching of an edited run versus the original run                */
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.brown.cs.rose.root.RoseLog;

class ValidateMatcher implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ValidateTrace original_trace;
private ValidateTrace match_trace;

private ValidateCall problem_context;        // context of problem
private long problem_time;                   // time of problem in original context
private long problem_after_time;             // time of statement after problem in original 

private long control_change;                 // time of first control change
private ValidateCall original_change_context;     // context of first control change
private ValidateCall match_change_context;        // matching context of control change

private long data_change;                    // time of first data change
private ValidateCall original_data_context;       // context of first data change 
private ValidateCall match_data_context;          // matching context of first data change

private ValidateCall match_problem_context;       // matching context of problem change
private long match_time;                     // matching time of problem change
private long match_after_time;               // matching time at end of statement




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateMatcher(ValidateTrace orig,ValidateTrace match)
{
   original_trace = orig;
   match_trace = match;
   
   problem_context = orig.getProblemContext();
   problem_time = orig.getProblemTime();
   
   problem_after_time = 0;
   if (problem_context != null) {
      ValidateVariable plines = problem_context.getLineNumbers();
      boolean fnd = false;
      for (ValidateValue vv : plines.getValues(orig)) {
         long t = vv.getStartTime();
         if (t == problem_time) fnd = true;
         else if (fnd) {
            problem_after_time = t;
            break;
          }
       }
      if (fnd && problem_after_time == 0) {
         problem_after_time = problem_context.getEndTime();
       }
    }
  
   control_change = 0;
   original_change_context = null;
   match_change_context = null;
   
   data_change = 0;
   original_data_context = null;
   match_data_context = null;
   
   match_problem_context = null;
   match_time = 0;
   match_after_time = 0;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

ValidateCall getProblemContext()        { return problem_context; }
long getProblemTime()                   { return problem_time; }
long getProblemAfterTime()              { return problem_after_time; }

ValidateCall getMatchProblemContext()        { return match_problem_context; }
long getMatchProblemTime()              { return match_time; }
long getMatchProblemAfterTime()         { return match_after_time; }

ValidateCall getOriginalChangeContext()      { return original_change_context; }
ValidateCall getMatchChangeContext()         { return match_change_context; }
long getControlChangeTime()             { return control_change; }

ValidateCall getOriginalDataContext()        { return original_data_context; }
ValidateCall getMatchDataContext()           { return match_data_context; }
long getDataChangeTime()                { return data_change; }




/********************************************************************************/
/*                                                                              */
/*      Compute the match                                                       */
/*                                                                              */
/********************************************************************************/

void computeMatch()
{
   ValidateCall origctx = original_trace.getRootContext();
   ValidateCall matchctx = match_trace.getRootContext();
   
   try {
      matchContexts(origctx,matchctx);
    }
   catch (Throwable e) {
      RoseLog.logE("Problem matching contexts",e);
      e.printStackTrace();
      System.err.println("Problem matching contexts");
    }
}



private void matchContexts(ValidateCall origctx,ValidateCall matchctx)
{
   if (origctx.sameAs(problem_context)) { 
      match_problem_context = matchctx;
    }
   
   matchLines(origctx,matchctx);
   matchVariables(origctx,matchctx);
   matchInnerContexts(origctx,matchctx);
}



private void matchLines(ValidateCall origctx,ValidateCall matchctx) 
{
   ValidateVariable origline = origctx.getLineNumbers();
   ValidateVariable matchline = matchctx.getLineNumbers();
   
   if (origline == null || matchline == null) return;
   
   long lasttime = origctx.getStartTime();
   long matchtime = matchctx.getStartTime();
   long lastmatch = matchtime;
   if (lasttime == matchtime) {
      Iterator<ValidateValue> it1 = origline.getValues(origctx.getTrace()).iterator();
      Iterator<ValidateValue> it2 = matchline.getValues(matchctx.getTrace()).iterator();
      boolean fnd = false;
      while (it1.hasNext() && it2.hasNext()) {
         ValidateValue origval = it1.next();
         ValidateValue matchval = it2.next();
         long thistime = origval.getStartTime();
         if (match_problem_context == matchctx) {
            if (lasttime <= problem_time && thistime > problem_time) {
               match_time = lastmatch;
               match_after_time = matchval.getStartTime();
             }
          }
         if (!fnd && 
               (origval.getStartTime() != matchval.getStartTime() ||
                     origval.getNumericValue() != matchval.getNumericValue())) {
            noteChange(origctx,matchctx,lasttime);
            fnd = true;
          }
         lasttime = thistime;
         lastmatch = matchval.getStartTime();
       }
      if (!fnd && (it1.hasNext() || it2.hasNext())) {
         noteChange(origctx,matchctx,lasttime);
       }
    }
}




private void matchInnerContexts(ValidateCall origctx,ValidateCall matchctx)
{
   DiffStruct diffs = computeContextDiffs(origctx,matchctx);
   Iterator<ValidateCall> cit1 = origctx.getInnerCalls().iterator();
   Iterator<ValidateCall> cit2 = matchctx.getInnerCalls().iterator();
   
   int count = 0;
   for (DiffStruct ds = diffs; ds != null; ds = ds.getNext()) {
      while (ds.getIndex() > count) {
         ValidateCall c1 = cit1.next();
         ValidateCall c2 = cit2.next();
         matchInnerContext(origctx,matchctx,c1,c2);
         ++count;
       }
      int ndel = ds.getNumDelete();
      if (ndel == 0) {
         matchInnerContext(origctx,matchctx,null,ds.getData());
       }
      else count += ndel;
   }
   while (cit1.hasNext()) {
      ValidateCall c1 = cit1.next();
      ValidateCall c2 = null;
      if (cit2.hasNext()) c2 = cit2.next();
      matchInnerContext(origctx,matchctx,c1,c2);
      ++count;
    }
}



private void matchInnerContext(ValidateCall origctx,ValidateCall matchctx,
      ValidateCall octx,ValidateCall mctx)
{
   if (octx != null && mctx != null) {
      long ostart = octx.getStartTime();
      long mstart = mctx.getStartTime();
      if (!octx.getMethod().equals(mctx.getMethod()) || ostart != mstart) {
         noteChange(origctx,matchctx,Math.min(ostart,mstart));
       }
      matchContexts(octx,mctx);
    }
   else if (octx != null) {
      long ostart = octx.getStartTime();
      noteChange(origctx,matchctx,ostart);
    }
   else if (mctx != null) {
      long mstart = mctx.getStartTime();   
      noteChange(origctx,matchctx,mstart);
    }
}



private void matchVariables(ValidateCall origctx,ValidateCall matchctx)
{
   Map<String,ValidateVariable> matchelts = matchctx.getVariables();
   
   for (ValidateVariable oval : origctx.getVariables().values()) {
      String nm = oval.getName();
      ValidateVariable mval = matchelts.remove(nm);
      matchVariable(origctx,matchctx,oval,mval);
    }
   for (ValidateVariable mval : matchelts.values()) {
      matchVariable(origctx,matchctx,null,mval);
    }
}



private void matchVariable(ValidateCall origctx,ValidateCall matchctx,
      ValidateVariable ovar,ValidateVariable mvar)
{
   List<ValidateValue> ovals = getVariableValues(origctx,ovar);
   List<ValidateValue> mvals = getVariableValues(matchctx,mvar);
   int sz = Math.max(ovals.size(),mvals.size());
   long difftime = -1;
   long lastdiff = origctx.getStartTime();
   for (int i = 0; i < sz; ++i) {
      ValidateValue oval = null;
      if (i < ovals.size()) oval = ovals.get(i);
      ValidateValue mval = null;
      if (i < mvals.size()) mval = mvals.get(i);
      if (oval == null) {
         difftime = lastdiff;
         break;
       }
      else if (mval == null) {
         difftime = oval.getStartTime();
         if (difftime < 0) difftime = origctx.getStartTime();
         break;
       }
      else if (oval.getValue() == null) {
         if (mval.getValue() != null) {
            difftime = oval.getStartTime();
            if (difftime < 0) difftime = origctx.getStartTime();
            break;
          }
       }
      else if (!oval.getValue().equals(mval.getValue())) {
         difftime = oval.getStartTime();
         if (difftime < 0) difftime = origctx.getStartTime();
         break;
       }
      if (oval != null) lastdiff = oval.getStartTime();
    }
   
   if (difftime > 0) {
      if (data_change < 0 || data_change > difftime) {
         data_change = difftime;
         original_data_context = origctx;
         match_data_context = matchctx;
       }
    }
}



private List<ValidateValue> getVariableValues(ValidateCall ctx,ValidateVariable var)
{
   if (var == null) return new ArrayList<>();
   
   return var.getValues(ctx.getTrace()); 
}


private void noteChange(ValidateCall origctx,ValidateCall matchctx,long when)
{
   if (control_change > 0 && when > control_change) return;
   control_change = when;
   original_change_context = origctx;
   match_change_context = matchctx;
}








/********************************************************************************/
/*                                                                              */
/*      Match two contexts                                                      */
/*                                                                              */
/********************************************************************************/

private DiffStruct computeContextDiffs(ValidateCall origctx,ValidateCall matchctx)
{
   List<ValidateCall> a = origctx.getInnerCalls();
   List<ValidateCall> b = matchctx.getInnerCalls();
   
   int m = a.size();
   int n = b.size();
   int maxd = m + n;
   int origin = maxd;
   int [] lastd = new int[2*maxd+2];
   DiffStruct [] script = new DiffStruct [2*maxd+2];
   DiffStruct rslt = null;
   
   int row = 0;
   while (row < m && row < n && matchContext(a.get(row),b.get(row))) row++;
   
   int col = 0;
   lastd[0+origin] = row;
   script[0+origin] = null;
   
   int lower = (row == m ? origin+1 : origin-1);
   int upper = (row == n ? origin-1 : origin+1);
   if (lower > upper) return null;
   
   for (int d = 1; d <= maxd; ++d) {
      for (int k = lower; k <= upper; k+= 2) {
	 if (k == origin-d || (k != origin+d && lastd[k+1] >= lastd[k-1])) {
	    row = lastd[k+1] + 1;
	    script[k] = new DiffStruct(script[k+1],true,null,row-1);
	  }
	 else {
	    row = lastd[k-1];
	    script[k] = new DiffStruct(script[k-1],false,b.get(row+k-origin-1),row);
	  }
	 col = row + k - origin;
	 while (row < m && col < n && matchContext(a.get(row),b.get(col))) {
	    ++row;
	    ++col;
	  }
	 lastd[k] = row;
	 if (row == m && col == n) {
	    rslt = script[k].createEdits();
	    return rslt;
	  }
	 if (row == m) lower = k+2;
	 if (col == n) upper = k-2;
       }
      lower = lower-1;
      upper = upper+1;
    }
   
   return rslt;
}  
  


private static boolean matchContext(ValidateCall octx,ValidateCall mctx)
{
   String omthd = octx.getMethod();
   String mmthd = mctx.getMethod();
   
   return omthd.equals(mmthd);
}


private static class DiffStruct {

   private int delete_count;
   private ValidateCall replace_data;
   private int line_index;
   private DiffStruct next_edit;
   
   public DiffStruct(DiffStruct prior,boolean del,ValidateCall dat, int i) {
      next_edit = prior;
      delete_count = (del ? 1 : 0);
      replace_data = dat;
      line_index = i;
    }
   
   public int getNumDelete()		{ return delete_count; }
   
   public ValidateCall getData()		{ return replace_data; }
   
   public int getIndex()		{ return line_index; }
   
   public DiffStruct getNext()		{ return next_edit; }
   
   DiffStruct createEdits() {
      DiffStruct shead = this;
      DiffStruct ep = null;
      DiffStruct behind = null;
      while (shead != null) {
         behind = ep;
         if (ep != null && ep.delete_count > 0 && shead.delete_count > 0 &&
               ep.line_index == shead.line_index + 1) {
            shead.delete_count += ep.delete_count;
            behind = ep.next_edit;
          }
         ep = shead;
         shead = shead.next_edit;
         ep.next_edit = behind;
       }
      return ep;
    }
   
}	// end of inner class DiffStruct


}       // end of class ValidateMatcher




/* end of ValidateMatcher.java */

