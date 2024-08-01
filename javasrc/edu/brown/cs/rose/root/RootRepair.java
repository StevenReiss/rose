/********************************************************************************/
/*										*/
/*		RootRepair.java 						*/
/*										*/
/*	Generic description of a potential repair #x2c6;#x2c6;				    */
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



package edu.brown.cs.rose.root;


import java.io.File;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract public class RootRepair implements RootConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		repair_finder;
private double		finder_priority;
private String		repair_description;
private String		repair_logdata;
private String		repair_id;
private double		repair_priority;
private double		validate_score;
private RootEdit	repair_edit;
private RootLocation	repair_location;
private RootLineMap	line_map;
private long		repair_time;
private int		repair_count;
private long		repair_seede;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/


protected RootRepair(RootRepairFinder finder,String desc,double pri,
      RootLocation loc,RootEdit edit,RootLineMap linemap,String logdata)
{
   repair_finder = finder.getClass().getName();
   finder_priority = finder.getFinderPriority();
   repair_description = desc;
   repair_priority = pri;
   repair_edit = edit;
   repair_location = loc;
   validate_score = 0.5;
   repair_time = 0;
   repair_count = 0;
   line_map = linemap;
   if (logdata == null) logdata = repair_finder;
   else repair_logdata = logdata;
   repair_id = "R" + hashCode();
}



protected RootRepair(Element xml,RootLocation loc)
{
   repair_finder = IvyXml.getAttrString(xml,"FINDER");
   finder_priority = IvyXml.getAttrDouble(xml,"FINDERPRIORITY",0.5);
   repair_priority = IvyXml.getAttrDouble(xml,"PRIORITY");
   repair_description = IvyXml.getTextElement(xml,"DESCRIPTION");
   repair_edit = new RootEdit(IvyXml.getChild(xml,"REPAIREDIT"));
   validate_score = IvyXml.getAttrDouble(xml,"VALIDATE",0.5);
   repair_logdata = IvyXml.getTextElement(xml,"LOGDATA");
   repair_id = IvyXml.getAttrString(xml,"ID");
   repair_time = IvyXml.getAttrLong(xml,"TIME",0);
   repair_count = IvyXml.getAttrInt(xml,"COUNT",0);
   repair_seede = IvyXml.getAttrLong(xml,"SEEDE",0);
   repair_location = loc;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getDescription()
{
   return repair_description;
}

public double getPriority()
{
   return repair_priority;
}

public double getValidatedPriority()
{
   return validate_score*repair_priority;
}

public double getFinderPriority()
{
   return finder_priority;
}


public void noteValidateScore(double v)
{
   validate_score = v;
}


public RootLocation getLocation()
{
   return repair_location;
}


public RootEdit getEdit()
{
   return repair_edit;
}


public long getMappedLine(File file,long line)
{
   if (line_map == null) return line;
   return line_map.getEditedLine(file,(int) line);
}


public String getLogData()
{
   return repair_logdata;
}


public String getId()
{
   return repair_id;
}


public void setTime(long time)
{
   repair_time = time;
}

public void setCount(int ct)
{
   repair_count = ct;
}


public void setSeedeCount(long ct)
{
   repair_seede = ct;
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("REPAIR");
   xw.field("PRIORITY",repair_priority);
   if (validate_score > 0) xw.field("VALIDATE",validate_score);
   xw.field("FINDER",repair_finder);
   xw.field("FINDERPRIORITY",finder_priority);
   xw.field("ID",repair_id);
   if (repair_time > 0) xw.field("TIME",repair_time);
   if (repair_count > 0) xw.field("COUNT",repair_count);
   if (repair_seede > 0) xw.field("SEEDE",repair_seede);
   localOutputXml(xw);
   repair_edit.outputXml(xw);
   xw.cdataElement("DESCRIPTION",repair_description);
   xw.textElement("LOGDATA",repair_logdata);
   repair_location.outputXml(xw);
   xw.end("REPAIR");
}



protected void localOutputXml(IvyXmlWriter xw)
{ }



}	// end of class RootRepair




/* end of RootRepair.java */

