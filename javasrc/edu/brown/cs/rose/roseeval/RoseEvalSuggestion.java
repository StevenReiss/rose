/********************************************************************************/
/*                                                                              */
/*              RoseEvalSuggestion.java                                         */
/*                                                                              */
/*      Data about a suggestion returned by ROSE                                */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class RoseEvalSuggestion implements RoseEvalConstants, Comparable<RoseEvalSuggestion>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int line_number;
private double fix_priority;
private String fix_description;
private long fix_time;
private int fix_count;
private long seede_count;
private String fix_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RoseEvalSuggestion(Element xml)
{
   fix_description = IvyXml.getTextElement(xml,"DESCRIPTION");
   Element loc = IvyXml.getChild(xml,"LOCATION");
   fix_file = IvyXml.getAttrString(loc,"FILE");
   line_number = IvyXml.getAttrInt(loc,"LINE");
   fix_time = IvyXml.getAttrLong(xml,"TIME");
   fix_count = IvyXml.getAttrInt(xml,"COUNT",0);
   seede_count = IvyXml.getAttrLong(xml,"SEEDE",0);
   double reppri = IvyXml.getAttrDouble(xml,"PRIORITY");
   double valpri = IvyXml.getAttrDouble(xml,"VALIDATE");
   fix_priority = reppri * valpri;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

int getLine()                        { return line_number; }
String getDescription()              { return fix_description; }
double getPriority()                 { return fix_priority; }
long getTime()                       { return fix_time; }
int getCount()                       { return fix_count; }
long getSeedeCount()                 { return seede_count; }
String getFile()                     { return fix_file; }



/********************************************************************************/
/*                                                                              */
/*      Comparison methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(RoseEvalSuggestion s) 
{
   return Double.compare(s.fix_priority,fix_priority);
}



}       // end of class RoseEvalSuggestion




/* end of RoseEvalSuggestion.java */

