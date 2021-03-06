/********************************************************************************/
/*                                                                              */
/*              RoseEvalProblem.java                                            */
/*                                                                              */
/*      Problem description for Rose Evaluations                                */
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

import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class RoseEvalProblem implements RoseEvalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String problem_type;
private String problem_item;
private String original_value;
private String target_value;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RoseEvalProblem(String typ,String item,String oval,String tval)
{
   problem_type = typ;
   problem_item = item;
   original_value = oval;
   target_value = tval;
}


static RoseEvalProblem createException(String typ)
{
   return new RoseEvalProblem("EXCEPTION",typ,null,null);
}


static RoseEvalProblem createVariable(String var,String oval,String tval)
{
   return new RoseEvalProblem("VARIABLE",var,oval,tval);
}


static RoseEvalProblem createExpression(String var,String oval,String tval)
{
   return new RoseEvalProblem("EXPRESSION",var,oval,tval);
}

static RoseEvalProblem createLocation()
{
   return new RoseEvalProblem("LOCATION",null,null,null);
}


static RoseEvalProblem createAssertion()
{
   return new RoseEvalProblem("ASSERTION","java.lang.AssertionError",null,null);
}


static RoseEvalProblem createJunitAssertion()
{
   return new RoseEvalProblem("ASSERTION","org.junit.ComparisonFailure",null,null);
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

String getDescription(RoseEvalFrameData fd)
{
   MintControl mc = fd.getMintControl();
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PROBLEM");
   xw.field("LAUNCH",fd.getLaunchId());     
   xw.field("FRAME",fd.getId());
   xw.field("THREAD",fd.getThreadId());
   xw.field("TYPE",problem_type);
   xw.field("IGNOREMAIN",true);
   xw.field("IGNORETESTS",true);
   if (problem_item != null) xw.textElement("ITEM",problem_item);
   if (original_value != null) xw.textElement("ORIGINAL",original_value);
   if (target_value != null) xw.textElement("TARGET",target_value);
   fd.outputLocation(xw,fd.getProject(),0.5,mc);
   xw.end("PROBLEM");
   
   return xw.toString();
}



}       // end of class RoseEvalProblem




/* end of RoseEvalProblem.java */

