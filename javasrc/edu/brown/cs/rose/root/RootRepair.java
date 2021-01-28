/********************************************************************************/
/*                                                                              */
/*              RootRepair.java                                                 */
/*                                                                              */
/*      Generic description of a potential repair #x2c6;#x2c6;                              */
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

abstract public class RootRepair implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          repair_finder;
private String          repair_description;
private double          repair_priority;
private double          validate_score;
private RootEdit        repair_edit;
private RootLocation    repair_location;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/


protected RootRepair(RootRepairFinder finder,String desc,double pri,
      RootLocation loc,RootEdit edit)
{ 
   repair_finder = finder.getClass().getName();
   repair_description = desc;
   repair_priority = pri;
   repair_edit = edit;
   repair_location = loc;
   validate_score = 5;
}



protected RootRepair(Element xml,RootLocation loc)
{
   repair_finder = IvyXml.getAttrString(xml,"FINDER:");
   repair_priority = IvyXml.getAttrDouble(xml,"PRIORITY");
   repair_description = IvyXml.getTextElement(xml,"DESCRIPTION");
   repair_edit = new RootEdit(IvyXml.getChild(xml,"REPAIREDIT"));
   validate_score = IvyXml.getAttrDouble(xml,"VALIDATE",5.0);
   repair_location = loc;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getDescription()
{
   return repair_description;
}

public double getPriority()
{
   return repair_priority;
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


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("REPAIR");
   xw.field("PRIORITY",repair_priority);
   if (validate_score > 0) xw.field("VALIDATE",validate_score);
   xw.field("FINDER",repair_finder);
   repair_edit.outputXml(xw);
   xw.textElement("DESCRIPTION",repair_description);
   repair_location.outputXml(xw);
   xw.end("REPAIR");
}



protected void localOutputXml(IvyXmlWriter xw)
{ }



}       // end of class RootRepair




/* end of RootRepair.java */

