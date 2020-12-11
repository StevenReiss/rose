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
private int             location_priority;
private String          in_method;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected RootLocation(Element xml)
{
   String fnm = IvyXml.getAttrString(xml,"FILE");
   for_file = new File(fnm);
   start_offset = IvyXml.getAttrInt(xml,"OFFSET");
   end_offset = IvyXml.getAttrInt(xml,"ENDOFFSET");
   project_name = IvyXml.getAttrString(xml,"PROJECT");
   line_number = IvyXml.getAttrInt(xml,"LINE");
   location_priority = IvyXml.getAttrInt(xml,"PRIORITY",DEFAULT_PRIORITY);
   in_method = IvyXml.getAttrString(xml,"METHOD");
}


protected RootLocation(File f,int start,int end,int line,String proj,String method,int pri)
{
   for_file = f;
   start_offset = start;
   end_offset = end;
   line_number = line;
   project_name = proj;
   if (pri <= 0) pri = DEFAULT_PRIORITY;
   location_priority = pri;
   in_method = method;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public File getFile()                           { return for_file; }
public int getStartOffset()                     { return start_offset; }
public int getEndOffset()                      { return end_offset; }
public String getProject()                      { return project_name; }
public int getPriority()                        { return location_priority; }

public String getMethod()                       { return in_method; }

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
   xw.field("FILE",for_file);
   if (start_offset > 0) xw.field("OFFSET",start_offset);
   if (end_offset > 0) xw.field("ENDOFFSET",end_offset);
   if (line_number > 0) xw.field("LINE",line_number);
   if (project_name != null) xw.field("PROJECT",project_name);
   if (in_method != null) xw.field("METHOD",in_method);
   xw.field("PRIORITY",location_priority);
   xw.end("LOCATION");
}


@Override public String toString() 
{
   return "[" + for_file + "@" + line_number + "(" + start_offset + "-" + end_offset + ")]";
}



}       // end of class RootLocation




/* end of RootLocation.java */

