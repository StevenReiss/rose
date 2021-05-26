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
   String sol1 = "105@if (baby != null) {";
   RoseEvalProblem p2 = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   String sol2 = "136@i <= baby.length-1";
   RoseEvalProblem p3 = RoseEvalProblem.createVariable("rslt","java.lang.String \"The cow jumped over the moon.\"",
         "\"The calf jumped over the moon.\"");
   String sol3 = "181@wd' with `baby'";
   RoseEvalProblem p4 = RoseEvalProblem.createVariable("baby","null","Non-Null");
   String sol4 = "197@getBabyName' with `getBabyWord";
   RoseEvalProblem p5 = RoseEvalProblem.createLocation();
   String sol5 = "147@index i with i-1";
   RoseEvalProblem p6 = RoseEvalProblem.createVariable("baby","java.lang.String piglet","piglets");
   String sol6 = "215@ + \"s\"";
   RoseEvalProblem p7 = RoseEvalProblem.createJunitAssertion();
   String sol7 = "119@baby' with `newbaby'";

   startEvaluations();
   try {
      // do a dummy run first
      runEvaluation("test01",p1,0,null);
      
      runEvaluation("test01",p1,0,sol1);
      runEvaluation("test02",p2,0,sol2);
      runEvaluation("test03",p3,0,sol3);
      runEvaluation("test04",p4,0,sol4);
      runEvaluation("test05",p5,0,sol5);
      runEvaluation("test06",p6,0,sol6);
      runEvaluation("test07",p7,0,sol7);
    }
   finally {
      finishEvaluations();
    }
   
// System.exit(0);
}




}       // end of class RoseEvalStem




/* end of RoseEvalStem.java */

