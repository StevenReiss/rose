/********************************************************************************/
/*                                                                              */
/*              RoseEvalTest.java                                               */
/*                                                                              */
/*      Single test to run evaluation on                                        */
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
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class RoseEvalTest implements RoseEvalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RoseEvalSuite   test_suite;
private String          test_name;
private RoseEvalProblem test_problem;
private long            test_time;
private int             test_skip;
private RoseEvalSolution test_solution;
private boolean         local_test;
private int             up_frames;
private List<String>    use_files;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RoseEvalTest(RoseEvalSuite suite,Element xml)
{
   test_suite = suite;
   test_name = IvyXml.getAttrString(xml,"NAME");
   test_problem = RoseEvalProblem.createProblem(IvyXml.getChild(xml,"PROBLEM"));
   test_time = IvyXml.getAttrLong(xml,"TIME",suite.getDefaultTime());
   test_skip = IvyXml.getAttrInt(xml,"SKIP",0);
   test_solution = new RoseEvalSolution(IvyXml.getChild(xml,"SOLUTION"));
   local_test = IvyXml.getAttrBool(xml,"LOCAL");
   up_frames = IvyXml.getAttrInt(xml,"FRAMES");
   if (local_test && up_frames < 0) up_frames = 0;
   use_files = new ArrayList<>();
   for (Element felt : IvyXml.children(xml,"FILE")) {
      String fnm = IvyXml.getAttrString(felt,"NAME");
      if (fnm == null) fnm = IvyXml.getText(felt);
      if (fnm != null) use_files.add(fnm);
    }
   if (use_files.isEmpty()) use_files = null;
}


private RoseEvalTest(RoseEvalTest t)
{
   test_suite = t.test_suite;
   test_name = t.test_name;
   test_problem = t.test_problem;
   test_time = t.test_time;
   test_skip = t.test_skip;
   test_solution = t.test_solution;
   local_test = t.local_test;
   up_frames = t.up_frames;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getName()                                { return test_name; }

RoseEvalSuite getSuite()                        { return test_suite; }

long getTime()                                  { return test_time; }

int getSkipTimes()                               { return test_skip; }

boolean isLocalTest()                           { return local_test; }

int getUpFrames()                               { return up_frames; }

RoseEvalProblem getProblem()                    { return test_problem; }

RoseEvalSolution getSolution()                  { return test_solution; }

List<String> getUseFiles()                      { return use_files; }

RoseEvalTest getDummyTest()
{
   RoseEvalTest dummy = new RoseEvalTest(this);
   dummy.test_solution = null;
   return dummy;
}


}       // end of class RoseEvalTest




/* end of RoseEvalTest.java */

