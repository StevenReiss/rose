/********************************************************************************/
/*                                                                              */
/*              ValidateValue.java                                              */
/*                                                                              */
/*      Representation of a value (that can change over time)                   */
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



package edu.brown.cs.rose.validate;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RootValidate.RootTrace;

class ValidateValue implements ValidateConstants, RootValidate.RootTraceValue
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         value_element;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateValue(Element v)
{
   value_element = v;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public long getStartTime()                     
{
   return IvyXml.getAttrLong(value_element,"TIME");
}

@Override public boolean isNull()
{
   return IvyXml.getAttrBool(value_element,"NULL");
}


@Override public String getDataType()
{
   return IvyXml.getAttrString(value_element,"TYPE");
}


@Override public Long getNumericValue()
{
   try {
      return Long.parseLong(IvyXml.getText(value_element));
    }
   catch (NumberFormatException e) { }
    
   return null;
}





@Override public int getLineValue()
{
   try {
      return Integer.parseInt(IvyXml.getText(value_element));
    }
   catch (NumberFormatException e) { }
   
   return 0;
}



@Override public String getValue()
{
   if (IvyXml.getAttrBool(value_element,"NULL")) return null;
   else if (IvyXml.getAttrBool(value_element,"OBJECT")) {
      String cls = IvyXml.getAttrString(value_element,"CLASS");
      if (cls != null) return cls;
      String fil = IvyXml.getAttrString(value_element,"FILE");
      if (fil != null) return fil;
      // getting more detailed info requires handling REFS
      return "{" + getDataType() + "}";
    }
   else if (IvyXml.getAttrBool(value_element,"ARRAY")) {
      // getting more detailed info requires handling REFS
      return "[" + getDataType() + "]";
    }
   else if (IvyXml.getAttrBool(value_element,"CHARS")) {
      int len = IvyXml.getAttrInt(value_element,"LENGTH");
      String txt = IvyXml.getText(value_element);
      return IvyXml.decodeCharacters(txt,len);
    }
   
   return IvyXml.getText(value_element);
}


@Override public String getEnum()
{
   return IvyXml.getAttrString(value_element,"ENUM");
}


@Override public ValidateValue getFieldValue(RootTrace rvtr,String fld,long when)
{
   ValidateTrace vtr = (ValidateTrace) rvtr;
   Element use = null;
   
   for (Element flde : IvyXml.children(value_element,"FIELD")) {
      String nm = IvyXml.getAttrString(flde,"NAME");
      if (nm.equals(fld)) {
         use = flde;
         break;
       }
      else if (nm.endsWith("." + fld)) use = flde;
    }
   
   if (use != null) {
      ValidateVariable vvar = new ValidateVariable(use);
      return vvar.getValueAtTime(vtr,when);
    }
   
   return null;
}



@Override public ValidateValue getIndexValue(RootTrace rvtr,int idx,long when)
{
   ValidateTrace vtr = (ValidateTrace) rvtr;
   Element use = null;
   for (Element flde : IvyXml.children(value_element,"ELEMENT")) {
      int eidx = IvyXml.getAttrInt(flde,"INDEX");
      if (eidx == idx) {
         use = flde;
         break;
       }
    }
   
   if (use != null) {
      ValidateVariable vvar = new ValidateVariable(use);
      return vvar.getValueAtTime(vtr,when);
    }
   
   return null;
}


@Override public String getId()
{
   return IvyXml.getAttrString(value_element,"ID");
}


@Override public int getArrayLength()
{
   return IvyXml.getAttrInt(value_element,"SIZE");
}



/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o) 
{
   if (o instanceof ValidateValue) {
      ValidateValue vv = (ValidateValue) o;
      return value_element.equals(vv.value_element);
    }
   return false;
}


@Override public int hashCode()
{
   return value_element.hashCode();
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString() 
{
   return IvyXml.convertXmlToString(value_element);
}



}       // end of class ValidateValue




/* end of ValidateValue.java */

