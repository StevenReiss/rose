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
private RootLocation    bug_location;
private RootNodeContext node_context;



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
}


protected RootProblem(RootControl ctrl,Element xml)
{
   problem_type = IvyXml.getAttrEnum(xml,"TYPE",RoseProblemType.OTHER);
   problem_item = IvyXml.getTextElement(xml,"ITEM");
   original_value = IvyXml.getTextElement(xml,"ORIGINAL");
   target_value = IvyXml.getTextElement(xml,"TARGET");
   launch_id = IvyXml.getAttrString(xml,"LAUNCH");
   thread_id = IvyXml.getAttrString(xml,"THREAD");
   frame_id = IvyXml.getAttrString(xml,"FRAME");
   
   Element loc = IvyXml.getChild(xml,"LOCATION");
   if (loc != null) bug_location = new RootLocation(ctrl,loc);
   
   Element ctx = IvyXml.getChild(xml,"CONTEXT");
   if (ctx == null) ctx = IvyXml.getChild(xml,"EXPRESSION");
   if (ctx != null) node_context = new RootNodeContext(ctx);
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

public void setOriginalValue(String v)          { original_value = v; }
public void setTargetValue(String v)            { target_value = v; }

public String getLaunchId()                     { return launch_id; }

public String getThreadId()                     { return thread_id; }

public String getFrameId()                      { return frame_id; }

public RootLocation getBugLocation()            { return bug_location; }

public RootNodeContext getNodeContext()         { return node_context; }




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



public void outputXml(IvyXmlWriter xw) 
{
   xw.begin("PROBLEM");
   xw.field("TYPE",problem_type);
   if (launch_id != null) xw.field("LAUNCH",launch_id);
   if (frame_id != null) xw.field("FRAME",frame_id);
   if (thread_id != null) xw.field("THREAD",thread_id);
   if (problem_item != null) xw.textElement("ITEM",problem_item);
   if (original_value != null) xw.textElement("ORIGINAL",original_value);
   if (target_value != null) xw.textElement("TARGET",target_value);
   if (bug_location != null) bug_location.outputXml(xw);
   if (node_context != null) node_context.outputXml(xw);
   xw.end("PROBLEM");
}



}       // end of class RootProblem




/* end of RootProblem.java */

