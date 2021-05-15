/********************************************************************************/
/*                                                                              */
/*              RoseEvalStem.java                                               */
/*                                                                              */
/*      description of class                                                    */
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



public class RoseEvalStem extends RoseEvalBase
{


/********************************************************************************/
/*                                                                              */
/*      Main Program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   RoseEvalStem res = new RoseEvalStem();
   res.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RoseEvalStem()
{ 
   super("rosetest","rosetest");
   run_local = false;
   run_debug = false;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   RoseEvalProblem p1 = RoseEvalProblem.createException("java.lang.NullPointerException");
   RoseEvalProblem p2 = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   RoseEvalProblem p3 = RoseEvalProblem.createVariable("rslt","java.lang.String \"The cow jumped over the moon.\"",
         "\"The calf jumped over the moon.\"");
   RoseEvalProblem p4 = RoseEvalProblem.createVariable("baby","null","Non-Null");
   RoseEvalProblem p5 = RoseEvalProblem.createLocation();
   RoseEvalProblem p6 = RoseEvalProblem.createVariable("baby","java.lang.String piglet","piglets");
   RoseEvalProblem p7 = RoseEvalProblem.createJunitAssertion();

   startEvaluations();
   try {
      // do a dummy run first
      runEvalaution("test01",p1,0,null);
      
      runEvalaution("test01",p1,0,null);
      runEvalaution("test02",p2,0,null);
      runEvalaution("test03",p3,0,null);
      runEvalaution("test04",p4,0,null);
      runEvalaution("test05",p5,0,null);
      runEvalaution("test06",p6,0,null);
      runEvalaution("test07",p7,0,null);
    }
   finally {
      finishEvaluations();
    }
   
// System.exit(0);
}




}       // end of class RoseEvalStem




/* end of RoseEvalStem.java */

