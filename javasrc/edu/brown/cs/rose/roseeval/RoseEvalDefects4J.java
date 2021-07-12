/********************************************************************************/
/*                                                                              */
/*              RoseEvalDefects4J.java                                          */
/*                                                                              */
/*      Run ROSE tests on Defects4J examples                                    */
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.brown.cs.rose.root.RoseLog;

public class RoseEvalDefects4J extends RoseEvalBase
{


/********************************************************************************/
/*										*/
/*	Main Program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   RoseEvalDefects4J res = new RoseEvalDefects4J(args);
   
   res.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


private static final int	TEST_COUNT = 15;
private static final int	TEST_TIME = 100000;


private String []		test_names;
private RoseEvalProblem []	test_problem;
private String []		test_solution;
private int []			test_skip;
private int []			test_time;
private List<Integer>		run_tests;
private Set<String>             local_tests;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RoseEvalDefects4J(String [] args)
{
   super("d4jspr","d4jspr");
   run_local = true;
   run_debug = true;
   seede_debug = true;
   local_tests = new HashSet<>();
   
   setupTests();
   
   run_tests = new ArrayList<>();
   
   for (int i = 0; i < args.length; ++i) {
      int v = -1;
      try {
	 v = Integer.parseInt(args[i]);
       }
      catch (NumberFormatException e) { }
      if (v < 0) {
	 for (int j = 1; j <= TEST_COUNT; ++j) {
	    if (test_names[j].equals(args[i])) {
	       v = j;
	       break;
	     }
	  }
       }
      if (v < 0) {
	 System.err.println("RoseEval: Unknown Test: " + args[i]);
	 System.exit(1);
       }
      run_tests.add(v);
    }
   
   if (run_tests.isEmpty()) {
      run_debug = false;
      seede_debug = false;
      for (int i = 0; i <= TEST_COUNT; ++i) run_tests.add(i);
    }
}



/********************************************************************************/
/*										*/
/*	Initialize tests							*/
/*										*/
/********************************************************************************/

private void setupTests()
{
   test_names = new String[TEST_COUNT+1];
   test_problem = new RoseEvalProblem[TEST_COUNT+1];
   test_solution = new String[TEST_COUNT+1];
   test_skip = new int[TEST_COUNT+1];
   test_time = new int[TEST_COUNT+1];
   
   test_names[1] = "Math02";
   test_problem[1] = RoseEvalProblem.createAssertion();
   test_solution[1] = "271@getSampleSize() * (@HypergeometricDistribution.java";
   test_time[1] = TEST_TIME;
   test_skip[1] = 0;
   
   test_names[2] = "Math05";
   test_problem[2] = RoseEvalProblem.createAssertion();
   test_solution[2] = "307@INF@Complex.java";
   test_time[2] = TEST_TIME;
   test_skip[2] = 0;
   
   test_names[3] = "Math09";
   test_problem[3] = RoseEvalProblem.createAssertion();
   test_solution[3] = "91@reverted.direction = @Line.java";
   test_time[3] = TEST_TIME;
   test_skip[3] = 0;
   
   test_names[4] = "Math10";
   test_problem[4] = RoseEvalProblem.createAssertion();
   test_solution[4] = "1427-1433@FastMath.atan2@DSCompiler.java";
   test_time[4] = TEST_TIME;
   test_skip[4] = 0;
   
   test_names[5] = "Math11";
   test_problem[5] = RoseEvalProblem.createAssertion();
   test_solution[5] = "185@2.0@MultivariateNormalDistribution.java";
   test_time[5] = TEST_TIME;
   test_skip[5] = 0;
  
   test_names[6] = "Math25";
   test_problem[6] = RoseEvalProblem.createLocation();
   test_solution[6] = "341-347@c2 == 0@ParameterGuesser.java";
   test_time[6] = TEST_TIME;
   test_skip[6] = 0;
   
   test_names[7] = "Math27";
   test_problem[7] = RoseEvalProblem.createAssertion();
   test_solution[7] = "599@100 * doubleValue@Fraction.java";
   test_time[7] = TEST_TIME;
   test_skip[7] = 0;
   
   test_names[8] = "Math30";
   test_problem[8] = RoseEvalProblem.createVariable("r","NaN","> 0");
   test_solution[8] = "180@double@MannWhitneyUTest.java";
   test_time[8] = TEST_TIME;
   test_skip[8] = 0;
   local_tests.add("Math30");
   
   test_names[9] = "Math32";
   test_problem[9] = RoseEvalProblem.createException("java.lang.ClassCastException");
   test_solution[9] = "529@tree.getCut() == null@PolygonsSet.java";
   test_time[9] = TEST_TIME;
   test_skip[9] = 0;
   
   test_names[10] = "Math34";
   test_problem[10] = RoseEvalProblem.createLocation();
   test_solution[10] = "223@getChromosomes()@ListPopulation.java";
   test_time[10] = TEST_TIME;
   test_skip[10] = 0;
   
   test_names[11] = "Chart01";
   test_problem[11] = RoseEvalProblem.createAssertion();
   test_solution[11] = "1799@==@AbstractCategeoryItemRenderer.java";
   test_time[11] = TEST_TIME;
   test_skip[11] = 0;
   
   test_names[12] = "Chart03";
   test_problem[12] = RoseEvalProblem.createAssertion();
   test_solution[12] = "1059@NaN@TimeSeries.java";
   test_time[12] = TEST_TIME;
   test_skip[12] = 0;
   
   test_names[13] = "Chart11";
   test_problem[13] = RoseEvalProblem.createAssertion();
   test_solution[13] = "278@p2.getPathIterator@ShapeUtilities.java";
   test_time[13] = TEST_TIME;
   test_skip[13] = 0;
   
   test_names[14] = "Chart12";
   test_problem[14] = RoseEvalProblem.createAssertion();
   test_solution[14] = "1648@setDataset@MultiplePiePlot.java";
   test_time[14] = TEST_TIME;
   test_skip[14] = 0;
   
   test_names[15] = "Chart20";
   test_problem[15] = RoseEvalProblem.createAssertion();
   test_solution[15] = "97@outlinePaint, outlineStroke@ValueMarker.java";
   test_time[15] = TEST_TIME;
   test_skip[15] = 0;
   
   test_names[0] = test_names[1];
   test_problem[0] = test_problem[1];
   test_solution[0] = null;
   test_skip[0] = test_skip[1];
   test_time[0] = test_time[1];
} 



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   startEvaluations();
   
   try {
      for (int tn : run_tests) {
         boolean usecur = local_tests.contains(test_names[tn]);
	 runEvaluation(test_names[tn],test_problem[tn],test_skip[tn],
	       test_solution[tn],test_time[tn],usecur);
       }
    }
   catch (Throwable t) {
      RoseLog.logE("ROSEEVAL","Problem running evaluation",t);
    }
   finally {
      finishEvaluations();
    }
}



}       // end of class RoseEvalDefects4J




/* end of RoseEvalDefects4J.java */

