/********************************************************************************/
/*                                                                              */
/*              RootNodeContext.java                                            */
/*                                                                              */
/*      Node context showing the execution location of an expression            */
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

public class RootNodeContext implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int     start_offset;
private int     end_offset;
private String  node_type;
private int     node_typeid;
private String  after_location;
private int     after_start;
private int     after_end;
private String  after_type;
private int     after_typeid;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RootNodeContext(Element xml)
{
   start_offset = IvyXml.getAttrInt(xml,"START");
   end_offset = IvyXml.getAttrInt(xml,"END");
   node_type = IvyXml.getAttrString(xml,"NODETYPE");
   node_typeid = IvyXml.getAttrInt(xml,"NODETYPEID");
   after_location = IvyXml.getAttrString(xml,"AFTER");
   after_start = IvyXml.getAttrInt(xml,"AFTERSTART");
   after_end = IvyXml.getAttrInt(xml,"AFTEREND");
   after_type = IvyXml.getAttrString(xml,"AFTERTYPE");
   after_typeid = IvyXml.getAttrInt(xml,"AFTERTYPEID");
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   outputXml("CONTEXT",xw);
}



public void outputXml(String elt,IvyXmlWriter xw)
{
   if (elt != null) xw.begin(elt);
   outputXmlFields(xw);
   if (elt != null) xw.end(elt);
}




public void outputXmlFields(IvyXmlWriter xw)
{
   xw.field("START",start_offset);
   xw.field("END",end_offset);
   xw.field("NODETYPE",node_type);
   xw.field("NODETYPEID",node_typeid);
   if (after_location != null) {
      xw.field("AFTER",after_location);
      xw.field("AFTERSTART",after_start);
      xw.field("AFTEREND",after_end);
      xw.field("AFTERTYPE",after_type);
      xw.field("AFTERTYPEID",after_typeid);
    }
}



}       // end of class RootNodeContext




/* end of RootNodeContext.java */

