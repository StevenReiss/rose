/********************************************************************************/
/*                                                                              */
/*              RootTestCase.java                                               */
/*                                                                              */
/*      Test case for validating against                                        */
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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class RootTestCase implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum TestType { RETURNS, THROWS, LOOPS }

private String          entry_frame;
private String          entry_routine;
private Map<String,String> initial_values;
private TestType        test_type;
private String          return_value;
private Map<String,String> check_values;
private long            max_ticks;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RootTestCase(Element xml)
{
   entry_frame = IvyXml.getAttrString(xml,"FRAME");
   entry_routine = IvyXml.getAttrString(xml,"ROUTINE");
   test_type = IvyXml.getAttrEnum(xml,"ACTION",TestType.RETURNS);
   return_value = IvyXml.getTextElement(xml,"RETURNS");
   initial_values = loadVarMap(xml,"INITIALIZE");
   check_values = loadVarMap(xml,"CHECK");
   max_ticks = IvyXml.getAttrLong(xml,"MAXTIME");
}



public RootTestCase(String fid,String rtn)
{
   entry_frame = fid;
   entry_routine = rtn;
   initial_values = null;
   test_type = TestType.RETURNS;
   return_value = null;
   check_values = null;
   max_ticks = -1;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getFrameId()              { return entry_frame; }

public boolean getThrows()              { return test_type == TestType.THROWS; }

public String getReturnValue()         
{ 
   if (test_type != TestType.RETURNS) return null;
   return return_value;
}


public String getThrowType()
{
   if (test_type != TestType.THROWS) return null;
   return return_value;
}

public long getMaxTime()                { return max_ticks; }


public void setThrows(String exc)
{
   test_type = TestType.THROWS;
   if (exc != null && exc.trim().length() == 0) exc = null;
   return_value = exc;
}


public void setReturns(String val)
{
   test_type = TestType.RETURNS;
   if (val != null && val.trim().length() == 0) val = null;
   return_value = val;
}


public void setMaxTime(long t)
{
   max_ticks = t;
}


public void addCheckValue(String nm,String val)
{
   if (nm == null || val == null) return;
   if (check_values == null) check_values = new HashMap<>();
   check_values.put(nm,val);
}

public void setLoops()
{
   test_type = TestType.LOOPS;
   return_value = null;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("TESTCASE");
   xw.field("FRAME",entry_frame);
   xw.field("ROUTINE",entry_routine);
   xw.field("ACTION",test_type);
   if (max_ticks > 0) xw.field("MAXTIME",max_ticks);
   if (return_value != null) xw.textElement("RETURNS",return_value);
   outputVarMap(xw,"INITIALIZE",initial_values);
   outputVarMap(xw,"CHECK",check_values);
   xw.end("TESTCASE");
}



private void outputVarMap(IvyXmlWriter xw,String nm,Map<String,String> map)
{
   if (map != null) {
      xw.begin(nm);
      for (Map.Entry<String,String> ent : map.entrySet()) {
         xw.begin("VALUE");
         xw.field("VARIABLE",ent.getKey());
         xw.text(ent.getValue());
         xw.end("VALUE");
       }
      xw.end(nm);
    }
}


private Map<String,String> loadVarMap(Element xml,String nm)
{
   Element celt = IvyXml.getChild(xml,nm);
   if (celt == null) return null;
   Map<String,String> rslt = new HashMap<>();
   for (Element velt : IvyXml.children(celt,"VALUE")) {
      String key = IvyXml.getAttrString(velt,"VARIABLE");
      String val = IvyXml.getText(velt);
      rslt.put(key,val);
    }
   return rslt;
}









}       // end of class RootTestCase




/* end of RootTestCase.java */

