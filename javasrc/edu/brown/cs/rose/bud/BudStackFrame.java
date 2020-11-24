/********************************************************************************/
/*                                                                              */
/*              BudStackFrame.java                                              */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.rose.bud;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;

public class BudStackFrame implements BudConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String frame_id;
private String class_name;
private String method_name;
private String method_signature;
private String format_signature;
private List<String> frame_variables;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BudStackFrame(Element xml)
{
   frame_id = IvyXml.getAttrString(xml,"ID");
   class_name = IvyXml.getAttrString(xml,"RECEIVER");
   method_name = IvyXml.getAttrString(xml,"METHOD");
   int idx = method_name.lastIndexOf(".");
   if (idx > 0) method_name = method_name.substring(idx+1);
   
   String sgn = IvyXml.getAttrString(xml,"SIGNATURE");
   method_signature = sgn;
   if (sgn != null) {
      int sidx = sgn.lastIndexOf(")");
      if (sidx > 0) sgn = sgn.substring(0,sidx+1);
      String fsgn = IvyFormat.formatTypeName(sgn);
      format_signature = fsgn;
    }
   else format_signature = null;
   
   frame_variables = new ArrayList<>();
   for (Element e : IvyXml.children(xml,"VALUE")) {
      String varnm = IvyXml.getAttrString(e,"NAME");
      frame_variables.add(varnm);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getFrameId()                      { return frame_id; }
public String getClassName()                    { return class_name; }
public String getMethodName()                   { return method_name; }
public String getMethodSignature()              { return method_signature; }
public String getFormatSignature()              { return format_signature; }
   


}       // end of class BudStackFrame




/* end of BudStackFrame.java */

