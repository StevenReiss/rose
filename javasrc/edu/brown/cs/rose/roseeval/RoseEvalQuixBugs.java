/********************************************************************************/
/*                                                                              */
/*              RoseEvalQuixBugs.java                                           */
/*                                                                              */
/*      Evaluation suite for QuixBugs programs                                  */
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

import edu.brown.cs.rose.root.RoseLog;

public class RoseEvalQuixBugs extends RoseEvalBase
{


/********************************************************************************/
/*                                                                              */
/*      Main Program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   RoseEvalQuixBugs res = new RoseEvalQuixBugs();
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

private RoseEvalQuixBugs()
{ 
   super("quixspr","quixbugs");
   run_local = true;
   run_debug = true;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   RoseEvalProblem p1 = RoseEvalProblem.createJunitAssertion();
   String sol1 = "25@n & (n - 1)";
   RoseEvalProblem p2 = RoseEvalProblem.createException("java.util.NoSuchElementException");
   String sol2 = "33@if (queue.size() == 0) return ";
   RoseEvalProblem p3 = RoseEvalProblem.createAssertion();
   String sol3 = "29@`arr' with `counts'";
   RoseEvalProblem p4 = RoseEvalProblem.createLocation();
   String sol4 = "48@nodesvisitied.add";
   RoseEvalProblem p5 = RoseEvalProblem.createException("java.lang.NullPointerException");
   String sol5 = "26@hare == null ||";
   RoseEvalProblem p6 = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   String sol6 = "24@lo < hi in place";
   RoseEvalProblem p7 = RoseEvalProblem.createVariable("start","3","4");
   String sol7 = "30@mid + 1"; 
   RoseEvalProblem p8 = RoseEvalProblem.createLocation();
   String sol8 = "29@flatten(x)";
   RoseEvalProblem p9 = RoseEvalProblem.createVariable("b","600",null);
   String sol9 = "26@Change argument order";
   RoseEvalProblem p10 = RoseEvalProblem.createJunitAssertion();
   String sol10 = "34@asList(n)"; 
   RoseEvalProblem p11 = RoseEvalProblem.createAssertion();
   String sol11 = "35@`helper' with `end'";
   RoseEvalProblem p12 = RoseEvalProblem.createAssertion();
   String sol12 = "33@depth == 0";
   RoseEvalProblem p13 = RoseEvalProblem.createAssertion();
   String sol13 = "37@sublist(k,arr.size())";
   RoseEvalProblem p14 = RoseEvalProblem.createAssertion();
   String sol14 = "37@<=";
   RoseEvalProblem p15 = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   String sol15 = "39@k - num_lessoreq";
   RoseEvalProblem p16 = RoseEvalProblem.createAssertion();
   String sol16 = "46@dp.get(i-1).containsKey(j-1)"; 
   RoseEvalProblem p17 = RoseEvalProblem.createVariable("result","8","3");
   String sol17 = "26@- 1"; 
   RoseEvalProblem p18 = RoseEvalProblem.createAssertion();
   String sol18 = "42@Math.max"; 
   RoseEvalProblem p19 = RoseEvalProblem.createAssertion();
   String sol19 = "25@b.substring(1)"; 
   RoseEvalProblem p20 = RoseEvalProblem.createAssertion(); 
   String sol20 = "25@Math.max(0";
   RoseEvalProblem p21 = RoseEvalProblem.createVariable("arr",null,null);
   String sol21 = "56@<= 1";
   RoseEvalProblem p22 = RoseEvalProblem.createAssertion(); 
   String sol22 = "46@groupByNode.put(node,groupByNode.get(vertex_u)";
   RoseEvalProblem p23 = RoseEvalProblem.createAssertion();
   String sol23 = "44@length - 1";
   RoseEvalProblem p24 = RoseEvalProblem.createAssertion();
   String sol24 = "26@> ";
   // Pascal
   RoseEvalProblem p25 = RoseEvalProblem.createException("java.lang.IndexOutOfBoundsException");
   String sol25 = "28@c <= r";
   // PossibleChange
   RoseEvalProblem p26 = RoseEvalProblem.createException("java.lang.ArrayIndexOutOfBoundsException");
   String sol26 = "28@.length";    
   RoseEvalProblem p27 = RoseEvalProblem.createAssertion();
   String sol27 = "34@to_add.add(";
   // Quicksort
   RoseEvalProblem p28 = RoseEvalProblem.createAssertion();
   String sol28 = "35@>=";
   RoseEvalProblem p29 = RoseEvalProblem.createException("java.lang.NullPointerException");
   String sol29 = "26@prevnode = node";
   // RpnEval
   RoseEvalProblem p30 = RoseEvalProblem.createAssertion();                     
   String sol30 = "43@Change argument order";
   RoseEvalProblem p31 = RoseEvalProblem.createAssertion();
   String sol31 = "53@distance";
   RoseEvalProblem p32 = RoseEvalProblem.createAssertion();
   String sol32 = "46@Change argument order";
   RoseEvalProblem p33 = RoseEvalProblem.createAssertion();
   String sol33 = "40@edge.get(i)";
   RoseEvalProblem p34 = RoseEvalProblem.createAssertion();
   String sol34 = "48@opstack.push(token)";
   RoseEvalProblem p35 = RoseEvalProblem.createAssertion();
   String sol35 = "47@`any' with `all'";
   RoseEvalProblem p36 = RoseEvalProblem.createLocation();
   String sol36 = "23@approx * approx";
   RoseEvalProblem p37 = RoseEvalProblem.createAssertion();
   String sol37 = "23@.add(";
   RoseEvalProblem p38 = RoseEvalProblem.createAssertion();
   String sol38 = "26@+ result";
   RoseEvalProblem p39 = RoseEvalProblem.createAssertion();
   String sol39 = "31@getPredecessors";
   RoseEvalProblem p40 = RoseEvalProblem.createAssertion();
   String sol40 = "36@lines.add(test)";
   
   startEvaluations();
   
   try {
      // do a dummy run first
      runEvaluation("BitCount",p1,0,null);
      
      runEvaluation("BitCount",p1,0,sol1);
      runEvaluation("BreadthFirstSearch",p2,0,sol2);
      runEvaluation("BucketSort",p3,0,sol3);
      runEvaluation("DepthFirstSearch",p4,3,sol4);
      runEvaluation("DetectCycle",p5,0,sol5);
      runEvaluation("FindFirstInSorted",p6,0,sol6);
      runEvaluation("FindInSorted",p7,4,sol7);
      runEvaluation("Flatten",p8,1,sol8);
      runEvaluation("Gcd",p9,2,sol9);
      runEvaluation("GetFactors",p10,0,sol10);
      runEvaluation("Hanoi",p11,0,sol11);
      runEvaluation("IsValidParenthesization",p12,0,sol12);
      runEvaluation("KHeapSort",p13,0,sol13);
      runEvaluation("Knapsack",p14,0,sol14);
      runEvaluation("Kth",p15,0,sol15);
      runEvaluation("LcsLength",p16,0,sol16);
      runEvaluation("Levenshtein",p17,0,sol17);
      runEvaluation("Lis",p18,0,sol18);
      runEvaluation("LongestCommonSubsequence",p19,0,sol19);
      runEvaluation("MaxSublistSum",p20,0,sol20);
      runEvaluation("MergeSort",p21,2,sol21);
      runEvaluation("MinimumSpanningTree",p22,0,sol22);
      runEvaluation("NextPalindrome",p23,0,sol23);
      runEvaluation("NextPermutation",p24,0,sol24);
      runEvaluation("Pascal",p25,0,sol25);
      runEvaluation("PossibleChange",p26,0,sol26);
      runEvaluation("PowerSet",p27,0,sol27);
      runEvaluation("QuickSort",p28,0,sol28);
      runEvaluation("ReverseLinkedList",p29,0,sol29);
      runEvaluation("RpnEval",p30,0,sol30);
      runEvaluation("ShortestPathLength",p31,0,sol31);
      runEvaluation("ShortestPathLengths",p32,0,sol32);
      runEvaluation("ShortestPaths",p33,0,sol33);
      runEvaluation("ShuntingYard",p34,0,sol34);
      runEvaluation("Sieve",p35,0,sol35);
      runEvaluation("Sqrt",p36,4,sol36);
      runEvaluation("Subsequences",p37,0,sol37);
      runEvaluation("ToBase",p38,0,sol38);
      runEvaluation("TopologicalOrdering",p39,0,sol39);
      runEvaluation("Wrap",p40,0,sol40);
    }
   catch (Throwable t) {
      RoseLog.logE("ROSEEVAL","Problem running evaluation",t);
    }
   finally {
      finishEvaluations();
    }
}




}       // end of class RoseEvalQuixBugs




/* end of RoseEvalQuixBugs.java */

