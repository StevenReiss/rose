/********************************************************************************/
/*                                                                              */
/*              RoseEvalRunner.java                                             */
/*                                                                              */
/*      Run experiment from test suites                                         */
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootThreadPool;
import edu.brown.cs.rose.root.RoseLog;

public class RoseEvalRunner extends RoseEvalBase
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   RoseEvalRunner runner = new RoseEvalRunner(args);
   
   runner.process();
}

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<RoseEvalSuite>     test_suites;
private List<RoseEvalTest>      run_tests;
private boolean                 run_setup;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RoseEvalRunner(String [] args)
{
   test_suites = new ArrayList<>();
   run_tests = new ArrayList<>();
   run_setup = false;
   
   run_local = true;
   run_debug = true;
   seede_debug = true;
   
   loadSuites(args);
   loadTests(args);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   List<RoseEvalSuite> suites = new ArrayList<>();
   for (RoseEvalTest t : run_tests) {
      RoseEvalSuite s = t.getSuite();
      if (suites.contains(s)) continue;
      suites.add(s);
    }

   for (RoseEvalSuite suite : suites) {
      startEvaluations(suite.getWorkspace(),suite.getProject());
      try {
         int ctr = 0;
         for (RoseEvalTest t : run_tests) {
            if (t.getSuite() == suite) {
               if (ctr++ == 0 && run_setup) {
                  RoseEvalTest dt = t.getDummyTest();
                  runEvaluation(dt);
                }
               runEvaluation(t);
             }
          }
       }
      catch (Throwable t) {
         RoseLog.logE("ROSEEVAL","Problem running evaluation",t);
       }
      finally {
         finishEvaluations(suite.getWorkspace(),suite.getProject());
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void loadSuites(String [] args)
{
   boolean fnd = false;
   for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("-use") && i+1 < args.length) {          // -use <description file>
         Element xml = IvyXml.loadXmlFromFile(args[i+1]);
         loadSuite(xml);
         fnd = true;
       }
    }
   
   if (!fnd) {
      InputStream ins = getClass().getClassLoader().getResourceAsStream("roseeval.xml");
      if (ins == null) {
         System.err.println("Can't load test suite descriptions");
         System.exit(1);
       }
      Element xml = IvyXml.loadXmlFromStream(ins);
      loadSuite(xml);
    }
}



private void loadSuite(Element xml)
{
   if (IvyXml.isElement(xml,"SUITE")) {
      addSuite(xml);
    }
   else {
      for (Element sxml : IvyXml.children(xml,"SUITE")) {
         addSuite(sxml);
       }
    }
}


private void addSuite(Element xml)
{
   if (IvyXml.getAttrBool(xml,"IGNORE")) return;
   RoseEvalSuite suite = new RoseEvalSuite(xml);
   test_suites.add(suite);
}


private void loadTests(String [] args)
{
   RootThreadPool.setMaxThreads(1);
   
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-u")) ++i;
      else if (args[i].startsWith("-RS")) {                             // -RSEEDE
         run_debug = false;
         seede_debug = true;
         run_setup = false;
       }
      else if (args[i].startsWith("-RX")) {                              // -RX
         run_debug = true;
         seede_debug = true;
         run_setup = false;
         RootThreadPool.setMaxThreads(4);
       }
      else if (args[i].startsWith("-R")) {                              // -RUN
         run_debug = false;
         seede_debug = false;
         run_setup = true;
         RootThreadPool.setMaxThreads(4);
       }
      else if (args[i].startsWith("-s") && i+1 < args.length) {         // -suite <name>
         ++i;
         for (RoseEvalSuite suite : test_suites) {
            if (suite.getName().equals(args[i]) ||
                  suite.getWorkspace().equals(args[i]) ||
                  suite.getProject().equals(args[i])) {
               run_tests.addAll(suite.getTests());
             }
          }
       }
      else {
         RoseEvalTest test = null;
         for (RoseEvalSuite suite : test_suites) {
            test = suite.findTest(args[i]);
            if (test != null) break;
          }
         if (test != null) run_tests.add(test);
         else {
            System.err.println("Can't find test " + args[i]);
            System.exit(1);
          }
       }
    }
   if (run_tests.size() == 0) {
      run_setup = true;
      for (RoseEvalSuite suite : test_suites) {
         run_tests.addAll(suite.getTests());
       }
    }
}


}       // end of class RoseEvalRunner




/* end of RoseEvalRunner.java */

