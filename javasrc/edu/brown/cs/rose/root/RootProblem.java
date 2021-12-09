/********************************************************************************/
/*                                                                              */
/*              RootProblem.java                                                */
/*                                                                              */
/*      Generic description of a problem                                        */
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



package edu.brown.cs.rose.root;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class RootProblem implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RoseProblemType problem_type;
private String          problem_item;
private String          original_value;
private String          target_value;
private String          launch_id;
private String          thread_id;
private String          frame_id;
private double          target_precision;
private RootLocation    bug_location;
private RootNodeContext node_context;
private RootTestCase    current_test;
private List<RootTestCase> other_tests; 
private boolean         ignore_main;
private boolean         ignore_tests;
private List<String>    ignore_patterns;
private int             max_up;

private static double DEFAULT_PRECISION = 1e-6;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected RootProblem(RoseProblemType typ,String item,String orig,String tgt,
      RootNodeContext ctx)
{
   problem_type = typ;
   problem_item = item;
   original_value = orig;
   target_value = tgt;
   launch_id = null;
   thread_id = null;
   frame_id = null;
   bug_location = null;
   node_context = ctx;
   current_test = null;
   other_tests = null;
   ignore_main = false;
   ignore_tests = false;
   ignore_patterns = new ArrayList<>();
   target_precision = DEFAULT_PRECISION;
   max_up = 5;
}


protected RootProblem(RootControl ctrl,Element xml)
{
   problem_type = IvyXml.getAttrEnum(xml,"TYPE",RoseProblemType.OTHER);
   problem_item = IvyXml.getTextElement(xml,"ITEM");
   original_value = IvyXml.getTextElement(xml,"ORIGINAL");
   target_value = IvyXml.getTextElement(xml,"TARGET");
   target_precision = IvyXml.getAttrDouble(xml,"PRECISION",DEFAULT_PRECISION);
   launch_id = IvyXml.getAttrString(xml,"LAUNCH");
   thread_id = IvyXml.getAttrString(xml,"THREAD");
   frame_id = IvyXml.getAttrString(xml,"FRAME");
   
   Element loc = IvyXml.getChild(xml,"LOCATION");
   if (loc != null) bug_location = new RootLocation(ctrl,loc);
   
   Element ctx = IvyXml.getChild(xml,"CONTEXT");
   if (ctx == null) ctx = IvyXml.getChild(xml,"EXPRESSION");
   if (ctx != null) node_context = new RootNodeContext(ctx);
   
   Element test = IvyXml.getChild(xml,"TESTCASE");
   if (test == null) current_test = null;
   else current_test = new RootTestCase(test);
   Element checks = IvyXml.getChild(xml,"CHECKS");
   if (checks == null) other_tests = null;
   else {
      other_tests = new ArrayList<>();
      for (Element telt : IvyXml.children(checks,"TESTCASE")) {
         other_tests.add(new RootTestCase(telt));
       }
    }
   
   ignore_main = IvyXml.getAttrBool(xml,"IGNOREMAIN");
   ignore_tests = IvyXml.getAttrBool(xml,"IGNORETESTS");
   ignore_patterns = new ArrayList<>();
   for (Element telt : IvyXml.children(xml,"IGNORE")) {
      ignore_patterns.add(IvyXml.getText(telt));
    }
   
   max_up = IvyXml.getAttrInt(xml,"MAXUP",-1);
}



protected void setBugFrame(String lid,String tid,String fid)
{
   launch_id = lid;
   thread_id = tid;
   frame_id = fid;
}



protected void setBugLocation(RootLocation loc)
{
   bug_location = loc;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public RoseProblemType getProblemType()
{ 
   return problem_type;
}


public String getProblemDetail()
{
   return problem_item;
}



public String getOriginalValue()
{
   return original_value;
}



public String getTargetValue()
{
   return target_value;
}

public double getTargetPrecision()             { return target_precision; }

public void setOriginalValue(String v)          { original_value = v; }
public void setTargetValue(String v)            { target_value = v; }
public void setTargetPrecision(double v)        { target_precision = v; }

public String getLaunchId()                     { return launch_id; }

public String getThreadId()                     { return thread_id; }

public String getFrameId()                      { return frame_id; }

public RootLocation getBugLocation()            { return bug_location; }

public RootNodeContext getNodeContext()         { return node_context; }

public RootTestCase getCurrentTest()            { return current_test; }
public void setCurrentTest(RootTestCase rtc)    { current_test = rtc; }

public List<RootTestCase> getOtherTests()       { return other_tests; }

public boolean ignoreMain()                     { return ignore_main; }
public boolean ignoreTests()                    { return ignore_tests; }
public List<String> ignorePatterns()            { return ignore_patterns; }
public void setIgnoreMain(boolean fg)           { ignore_main = fg; }
public void setIgnoreTests(boolean fg)          { ignore_tests = fg; }
public void addIgnorePattern(String pat)        { ignore_patterns.add(pat); }

public int getMaxUp()                           { return max_up; }
public void setMaxUp(int mup)                   { max_up = mup; }

 



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getDescription()
{
   switch (problem_type) {
      case EXCEPTION :
         return problem_item + " shouldn't be thrown";
      case ASSERTION :
         return "Assertion should not have failed";
      case EXPRESSION :
         return "Expression " + problem_item + " has the wrong value";
      case VARIABLE :
         return "Variable " + problem_item + " has the wrong value";
      case LOCATION :
         return "Execution shouldn't be here";
      case OTHER :
         return problem_item;
      default :
         return "Current debugging problem";
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw) 
{
   xw.begin("PROBLEM");
   xw.field("TYPE",problem_type);
   if (launch_id != null) xw.field("LAUNCH",launch_id);
   if (frame_id != null) xw.field("FRAME",frame_id);
   if (thread_id != null) xw.field("THREAD",thread_id);
   if (ignore_main) xw.field("IGNOREMAIN",ignore_main);
   if (ignore_tests) xw.field("IGNORETESTS",ignore_tests);
   if (target_precision != DEFAULT_PRECISION) xw.field("PRECISION",target_precision);
   if (max_up >= 0) xw.field("MAX_UP",max_up);
   
   if (problem_item != null) xw.textElement("ITEM",problem_item);
   if (original_value != null) xw.textElement("ORIGINAL",original_value);
   if (target_value != null) xw.textElement("TARGET",target_value);
   
   if (bug_location != null) bug_location.outputXml(xw);
   if (node_context != null) node_context.outputXml(xw);
   if (current_test != null) current_test.outputXml(xw);
   if (other_tests != null) {
      xw.begin("CHECKS");
      for (RootTestCase rtc : other_tests) {
         rtc.outputXml(xw);
       }
      xw.end("CHECKS");
    }
   for (String s : ignore_patterns) {
      xw.textElement("IGNORE",s);
    }
   xw.end("PROBLEM");
}



}       // end of class RootProblem




/* end of RootProblem.java */

