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

private String          entry_frame;
private String          entry_routine;
private Map<String,String> initial_values;
private boolean         is_throws;
private String          return_value;
private Map<String,String> check_values;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RootTestCase(Element xml)
{
   entry_frame = IvyXml.getAttrString(xml,"FRAME");
   entry_routine = IvyXml.getAttrString(xml,"ROUTINE");
   is_throws = IvyXml.getAttrBool(xml,"THROWS");
   return_value = IvyXml.getTextElement(xml,"RETURNS");
   initial_values = loadVarMap(xml,"INITIALIZE");
   check_values = loadVarMap(xml,"CHECK");
}



public RootTestCase(String fid,String rtn)
{
   entry_frame = fid;
   entry_routine = rtn;
   initial_values = null;
   is_throws = false;
   return_value = null;
   check_values = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getFrameId()              { return entry_frame; }

public boolean getThrows()              { return is_throws; }

public String getReturnValue()         
{ 
   if (is_throws) return null;
   return return_value;
}


public String getThrowType()
{
   if (!is_throws) return null;
   return return_value;
}



public void setThrows(String exc)
{
   is_throws = true;
   return_value = exc;
}


public void setReturns(String val)
{
   is_throws = false;
   return_value = val;
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
   xw.field("THROWS",is_throws);
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
