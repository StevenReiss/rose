/********************************************************************************/
/*                                                                              */
/*              RootLocation.java                                               */
/*                                                                              */
/*      Representation of a location passed from front to back end              */
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

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class RootLocation implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File            for_file;
private String          project_name;
private int             start_offset;
private int             end_offset;
private int             line_number;
private double          location_priority;
private String          in_method;
private String          full_method;
private int             method_offset;
private int             method_length;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected RootLocation(RootControl ctrl,Element xml)
{
   String fnm = IvyXml.getAttrString(xml,"FILE");
   for_file = new File(fnm);
   start_offset = IvyXml.getAttrInt(xml,"OFFSET");
   end_offset = IvyXml.getAttrInt(xml,"ENDOFFSET");
   if (end_offset < 0) {
      int len = IvyXml.getAttrInt(xml,"LENGTH");
      if (len > 0) end_offset = start_offset+len;
      else end_offset = start_offset+1;
    }
   project_name = IvyXml.getAttrString(xml,"PROJECT");
   if (project_name == null && ctrl != null) {
      project_name = ctrl.getProjectForFile(for_file);
    }
    
   line_number = IvyXml.getAttrInt(xml,"LINE");
   location_priority = IvyXml.getAttrDouble(xml,"PRIORITY",DEFAULT_PRIORITY);
   full_method = null;
   method_offset = 0;
   method_length = 0;
   in_method = IvyXml.getAttrString(xml,"METHOD");
   Element itm = IvyXml.getChild(xml,"ITEM");
   if (itm != null) {
      String typ = IvyXml.getAttrString(itm,"TYPE");
      if (typ.equals("Function") && in_method == null) {
         in_method = IvyXml.getAttrString(itm,"QNAME");
       }
      full_method = IvyXml.getAttrString(itm,"HANDLE");
      if (full_method == null) full_method = IvyXml.getAttrString(itm,"KEY");
      method_offset = IvyXml.getAttrInt(itm,"STARTOFFSET");
      method_length = IvyXml.getAttrInt(itm,"LENGTH");
    }
}


protected RootLocation(File f,int start,int end,int line,String proj,String method,double pri)
{
   for_file = f;
   start_offset = start;
   end_offset = end;
   line_number = line;
   project_name = proj;
   if (pri <= 0) pri = DEFAULT_PRIORITY;
   location_priority = pri;
   full_method = method;
   in_method = method;
   if (method != null) {
      int idx = method.indexOf("(");
      if (idx > 0) in_method = method.substring(0,idx);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public File getFile()                           { return for_file; }
public int getStartOffset()                     { return start_offset; }
public int getEndOffset()                       { return end_offset; }
public String getProject()                      { return project_name; }

public double getPriority()                     { return location_priority; }
public void setPriority(double v)               { location_priority = v; }

public String getMethod()    
{
   if (in_method != null) return in_method;
   if (full_method != null) return full_method;
   
   return in_method; 
}

protected void setMethodData(String full,int off,int len)
{
   full_method = full;
   if (in_method == null) {
      int idx = full.indexOf("(");
      if (idx > 0) in_method = full.substring(0,idx);
      else in_method = full;
    }
   method_offset = off;
   method_length = len;
}

public int getLineNumber()
{
   if (line_number <= 0) {
      try {
         CompilationUnit cu = JcompAst.parseSourceFile(IvyFile.loadFile(for_file));
         if (cu != null) {
            line_number = cu.getLineNumber(start_offset);
          }
       }
      catch (IOException e) { }
    }
   return line_number;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

public void outputXml(IvyXmlWriter xw) 
{
   xw.begin("LOCATION");
   xw.field("PRIORITY",location_priority);
   xw.field("FILE",for_file);
   if (start_offset > 0) xw.field("OFFSET",start_offset);
   if (end_offset > 0) {
      xw.field("ENDOFFSET",end_offset);
      xw.field("LENGTH",end_offset - start_offset);
    }
   else if (start_offset > 0) {
      xw.field("LENGTH",1);
      xw.field("ENDOFFSET",start_offset+1);
    }
   if (line_number > 0) xw.field("LINE",line_number);
   if (project_name != null) xw.field("PROJECT",project_name);
   if (in_method != null) {
      xw.field("TYPE","Function");
      xw.field("METHOD",in_method);
      xw.begin("ITEM");
      int idx1 = in_method.lastIndexOf(".");
      if (idx1 > 0) xw.field("NAME",in_method.substring(idx1+1));
      else xw.field("NAME",in_method);
      xw.field("QNAME",in_method);
      xw.field("TYPE","Function");
      if (method_offset > 0) {
         xw.field("STARTOFFSET",method_offset);
         xw.field("LENGTH",method_length);
       }
      if (full_method != null) {
         xw.field("KEY",full_method);
         xw.field("HANDLE",full_method);
       }
      else {
         xw.field("KEY",in_method);
         xw.field("HANDLE",in_method);
       }
      xw.field("PROJECT",project_name);
      xw.end("ITEM");
    }
   xw.end("LOCATION");
}


@Override public String toString() 
{
   return "[" + for_file + "@" + line_number + "(" + start_offset + "-" + end_offset + ")]";
}



}       // end of class RootLocation




/* end of RootLocation.java */

