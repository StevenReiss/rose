/********************************************************************************/
/*										*/
/*		RoseEvalQuixBugs.java						*/
/*										*/
/*	Evaluation suite for QuixBugs programs					*/
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



package edu.brown.cs.rose.roseeval;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.rose.root.RoseLog;

public class RoseEvalQuixBugs extends RoseEvalBase
{



/********************************************************************************/
/*										*/
/*	Main Program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   RoseEvalQuixBugs res = new RoseEvalQuixBugs(args);

   res.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final int	TEST_COUNT = 40;
private static final int	TEST_TIME = 10000;


private String []		test_names;
private RoseEvalProblem []	test_problem;
private String []		test_solution;
private int []			test_skip;
private int []			test_time;
private List<Integer>		run_tests;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private RoseEvalQuixBugs(String [] args)
{
   super("quixspr","quixbugs");
   run_local = true;
   run_debug = true;
   seede_debug = true;

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

   test_names[1] = "BitCount";
   test_problem[1] = RoseEvalProblem.createJunitAssertion();
   test_solution[1] = "25@n & (n - 1)";
   test_time[1] = TEST_TIME;
   test_skip[1] = 0;

   test_names[2] = "BreadthFirstSearch";
   test_problem[2] = RoseEvalProblem.createException("java.util.NoSuchElementException");
   test_solution[2] = "33@if (queue.size() == 0) return ";
   test_time[2] = TEST_TIME;
   test_skip[2] = 0;

   test_names[3] = "BucketSort";
   test_problem[3] = RoseEvalProblem.createAssertion();
   test_solution[3] = "29@`arr' with `counts'";
   test_time[3] = TEST_TIME;
   test_skip[3] = 0;

   test_names[4] = "DepthFirstSearch";
   test_problem[4] = RoseEvalProblem.createLocation();
   test_solution[4] = "44@nodes_visited.add";
   test_time[4] = TEST_TIME;;
   test_skip[4] = 3;

   test_names[5] = "DetectCycle";
   test_problem[5] = RoseEvalProblem.createException("java.lang.NullPointerException");
   test_solution[5] = "26@(hare == null) ||";
   test_time[5] = TEST_TIME;
   test_skip[5] = 0;

   test_names[6] = "FindFirstInSorted";
   test_problem[6] = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   test_solution[6] = "24@lo < hi in place";
   test_time[6] = TEST_TIME;
   test_skip[6] = 0;

   test_names[7] = "FindInSorted";
   test_problem[7] = RoseEvalProblem.createVariable("start","3","4");
   test_solution[7] = "30@mid + 1";
   test_time[7] = TEST_TIME;
   test_skip[7] = 4;

   test_names[8] = "Flatten";
   test_problem[8] = RoseEvalProblem.createLocation();
   test_solution[8] = "29@flatten(x)";
   test_time[8] = TEST_TIME;
   test_skip[8] = 1;

   test_names[9] = "Gcd";
   test_problem[9] = RoseEvalProblem.createVariable("b","600",null);
   test_solution[9] = "26@gcd(b,";
   test_time[9] = TEST_TIME;
   test_skip[9] = 2;

   test_names[10] = "GetFactors";
   test_problem[10] = RoseEvalProblem.createJunitAssertion();
   test_solution[10] = "34@asList(n)";
   test_time[10] = TEST_TIME;
   test_skip[10] = 0;

   test_names[11] = "Hanoi";
   test_problem[11] = RoseEvalProblem.createAssertion();
   test_solution[11] = "35@`helper' with `end'";
   test_time[11] = TEST_TIME;
   test_skip[11] = 0;

   test_names[12] = "IsValidParenthesization";
   test_problem[12] = RoseEvalProblem.createAssertion();
   test_solution[12] = "33@depth == 0";
   test_time[12] = TEST_TIME;
   test_skip[12] = 0;

   test_names[13] = "KHeapSort";
   test_problem[13] = RoseEvalProblem.createAssertion();
   test_solution[13] = "37@sublist(k,arr.size())";
   test_time[13] = TEST_TIME;
   test_skip[13] = 0;

   test_names[14] = "Knapsack";
   test_problem[14] = RoseEvalProblem.createAssertion();
   test_solution[14] = "37@<=";
   test_time[14] = TEST_TIME;
   test_skip[14] = 0;

   test_names[15] = "Kth";
   test_problem[15] = RoseEvalProblem.createException("java.lang.IndexOutOfBoundsException");
   test_solution[15] = "39@k - num_lessoreq";
   test_time[15] = TEST_TIME;
   test_skip[15] = 0;

   test_names[16] = "LcsLength";
   test_problem[16] = RoseEvalProblem.createAssertion();
   test_solution[16] = "46@dp.get(i-1).containsKey(j-1)";
   test_time[16] = TEST_TIME;
   test_skip[16] = 0;

   test_names[17] = "Levenshtein";
   test_problem[17] = RoseEvalProblem.createVariable("result","8","3");
   test_solution[17] = "26@- 1";
   test_time[17] = 500000;
   test_skip[17] = 0;

   test_names[18] = "Lis";
   test_problem[18] = RoseEvalProblem.createAssertion();
   test_solution[18] = "42@Math.max";
   test_time[18] = TEST_TIME;
   test_skip[18] = 0;

   test_names[19] = "LongestCommonSubsequence";
   test_problem[19] = RoseEvalProblem.createAssertion();
   test_solution[19] = "25@b.substring(1)";
   test_time[19] = 50000;
   test_skip[19] = 0;

   test_names[20] = "MaxSublistSum";
   test_problem[20] = RoseEvalProblem.createAssertion();
   test_solution[20] = "25@Math.max(0";
   test_time[20] = TEST_TIME;
   test_skip[20] = 0;

   test_names[21] = "MergeSort";
   test_problem[21] = RoseEvalProblem.createVariable("arr",null,null);
   test_solution[21] = "56@<= 1";
   test_time[21] = TEST_TIME;
   test_skip[21] = 2;

   test_names[22] = "MinimumSpanningTree";
   test_problem[22] = RoseEvalProblem.createAssertion();
   test_solution[22] = "46@groupByNode.put(node,groupByNode.get(vertex_u)";
   test_time[22] = TEST_TIME;
   test_skip[22] = 0;

   test_names[23] = "NextPalindrome";
   test_problem[23] = RoseEvalProblem.createAssertion();
   test_solution[23] = "44@length - 1";
   test_time[23] = TEST_TIME;
   test_skip[23] = 0;

   test_names[24] = "NextPermutation";
   test_problem[24] = RoseEvalProblem.createAssertion();
   test_solution[24] = "26@> ";
   test_time[24] = TEST_TIME;
   test_skip[24] = 0;

   test_names[25] = "Pascal";
   test_problem[25] = RoseEvalProblem.createException("java.lang.IndexOutOfBoundsException");
   test_solution[25] = "28@c <= r";
   test_time[25] = TEST_TIME;
   test_skip[25] = 0;

   test_names[26] = "PossibleChange";
   test_problem[26] = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   test_solution[26] = "28@.length";
   test_time[26] = TEST_TIME;
   test_skip[26] = 0;

   test_names[27] = "PowerSet";
   test_problem[27] = RoseEvalProblem.createAssertion();
   test_solution[27] = "34@to_add.add(";
   test_time[27] = TEST_TIME;
   test_skip[27] = 0;

   test_names[28] = "QuickSort";
   test_problem[28] = RoseEvalProblem.createAssertion();
   test_solution[28] = "35@>=";
   test_time[28] = TEST_TIME;
   test_skip[28] = 0;

   test_names[29] = "ReverseLinkedList";
   test_problem[29] = RoseEvalProblem.createException("java.lang.NullPointerException");
   test_solution[29] = "26@prevnode = node";
   test_time[29] = TEST_TIME;
   test_skip[29] = 0;

   test_names[30] = "RpnEval";
   test_problem[30] = RoseEvalProblem.createAssertion();
   test_solution[30] = "43@apply(b,a)";
   test_time[30] = TEST_TIME;
   test_skip[30] = 0;

   test_names[31] = "ShortestPathLength";
   test_problem[31] = RoseEvalProblem.createAssertion();
   test_solution[31] = "53@distance";
   test_time[31] = TEST_TIME;
   test_skip[31] = 0;

   test_names[32] = "ShortestPathLengths";
   test_problem[32] = RoseEvalProblem.createAssertion();
   test_solution[32] = "46@Arrays.asList(k,j)";
   test_time[32] = 500000;
   test_skip[32] = 0;

   test_names[33] = "ShortestPaths";
   test_problem[33] = RoseEvalProblem.createAssertion();
   test_solution[33] = "40@edge.get(i)";
   test_time[33] = 500000;
   test_skip[33] = 0;

   test_names[34] = "ShuntingYard";
   test_problem[34] = RoseEvalProblem.createAssertion();
   test_solution[34] = "48@opstack.push(token)";
   test_time[34] = TEST_TIME;
   test_skip[34] = 0;

   test_names[35] = "Sieve";
   test_problem[35] = RoseEvalProblem.createAssertion();
   test_solution[35] = "47@`any' with `all'";
   test_time[35] = TEST_TIME;
   test_skip[35] = 0;

   test_names[36] = "Sqrt";
   test_problem[36] = RoseEvalProblem.createLocation();
   test_solution[36] = "23@approx * approx";
   test_time[36] = TEST_TIME;
   test_skip[36] = 4;

   test_names[37] = "Subsequences";
   test_problem[37] = RoseEvalProblem.createAssertion();
   test_solution[37] = "23@.add(";
   test_time[37] = TEST_TIME;
   test_skip[37] = 0;

   test_names[38] = "ToBase";
   test_problem[38] = RoseEvalProblem.createAssertion();
   test_solution[38] = "26@+ result";
   test_time[38] = TEST_TIME;
   test_skip[38] = 0;

   test_names[39] = "TopologicalOrdering";
   test_problem[39] = RoseEvalProblem.createAssertion();
   test_solution[39] = "31@getPredecessors";
   test_time[39] = TEST_TIME;
   test_skip[39] = 0;

   test_names[40] = "Wrap";
   test_problem[40] = RoseEvalProblem.createAssertion();
   test_solution[40] = "36@lines.add(test)";
   test_time[40] = TEST_TIME;
   test_skip[40] = 0;

   // dummy run

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
	 runEvaluation(test_names[tn],test_problem[tn],test_skip[tn],
	       test_solution[tn],test_time[tn]);
       }
    }
   catch (Throwable t) {
      RoseLog.logE("ROSEEVAL","Problem running evaluation",t);
    }
   finally {
      finishEvaluations();
    }
}




}	// end of class RoseEvalQuixBugs




/* end of RoseEvalQuixBugs.java */
































































