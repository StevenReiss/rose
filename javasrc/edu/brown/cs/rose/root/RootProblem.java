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




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected RootProblem(RoseProblemType typ,String item,String orig,String tgt)
{
   problem_type = typ;
   problem_item = item;
   original_value = orig;
   target_value = tgt;
}


protected RootProblem(Element xml)
{
   problem_type = IvyXml.getAttrEnum(xml,"TYPE",RoseProblemType.OTHER);
   problem_item = IvyXml.getTextElement(xml,"ITEM");
   original_value = IvyXml.getTextElement(xml,"ORIGINAL");
   target_value = IvyXml.getTextElement(xml,"TARGET");
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


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getDescription()
{
   switch (problem_type) {
      case EXCEPTION :
         return problem_item + "shouldn't be thrown";
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
   localOutputXml(xw);
   if (problem_item != null) xw.textElement("ITEM",problem_item);
   if (original_value != null) xw.textElement("ORIGINAL",original_value);
   if (target_value != null) xw.textElement("TARGET",target_value);
   xw.end("END");
}


protected void localOutputXml(IvyXmlWriter xw)                  { }





}       // end of class RootProblem




/* end of RootProblem.java */

