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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class ValidateMatcher implements ValidateConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ValidateTrace original_trace;
private ValidateTrace match_trace;

private Element problem_context;             // context of problem
private long problem_time;                   // time of problem in original context
private long problem_after_time;             // time of statement after problem in original 

private long control_change;                 // time of first control change
private Element original_change_context;     // context of first control change
private Element match_change_context;        // matching context of control change

private long data_change;                    // time of first data change
private Element original_data_context;       // context of first data change 
private Element match_data_context;          // matching context of first data change

private Element match_problem_context;       // matching context of problem change
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
   for (Element varelt : IvyXml.children(problem_context,"VARIABLE")) {
      if (IvyXml.getAttrString(varelt,"NAME").equals("*LINE*")) {
         boolean fnd = false;
         for (Element valelt : IvyXml.children(varelt,"VALUE")) {
            Long t = IvyXml.getAttrLong(valelt,"TIME");
            if (t == problem_time) {
               fnd = true;
             }
            else if (fnd) {
               problem_after_time = t;
               break;
             }
          }
         if (fnd && problem_after_time == 0) {
            problem_after_time = IvyXml.getAttrLong(problem_context,"END");
          }
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
/*      Compute the match                                                       */
/*                                                                              */
/********************************************************************************/

void computeMatch()
{
   // walk through the contexts, checking for matches
}




}       // end of class ValidateMatcher




/* end of ValidateMatcher.java */

