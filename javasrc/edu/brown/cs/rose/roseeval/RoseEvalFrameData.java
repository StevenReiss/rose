/********************************************************************************/
/*                                                                              */
/*              RoseEvalFrameData.java                                          */
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



package edu.brown.cs.rose.roseeval;

import java.io.File;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class RoseEvalFrameData implements RoseEvalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LaunchData   for_launch;
private String       frame_id;
private String       method_name;
private String       class_name;
private String       file_name;
private String       project_name;
private int          line_number;
private boolean      is_user;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RoseEvalFrameData(LaunchData ld,Element xml) 
{
   for_launch = ld;
   frame_id = IvyXml.getAttrString(xml,"ID");
   method_name = IvyXml.getAttrString(xml,"METHOD");
   line_number = IvyXml.getAttrInt(xml,"LINENO");
   class_name = IvyXml.getAttrString(xml,"CLASS");
   file_name = IvyXml.getAttrString(xml,"FILE");
   String sgn = IvyXml.getAttrString(xml,"SIGNATURE");
   if (sgn != null) method_name += sgn;
   project_name = null;
   is_user = true;
   if (file_name == null) is_user = false;
   else if (!(new File(file_name).exists())) is_user = false;
   else if (!IvyXml.getAttrString(xml,"FILETYPE").equals("JAVAFILE")) is_user = false;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getId()                          { return frame_id; }
String getMethod()                      { return method_name; }
String getClassName()                   { return class_name; }
int getLine()                           { return line_number; }
boolean isUserFrame()                   { return is_user; }

String getThreadId()                    { return for_launch.getThreadId(); }
String getLaunchId()                    { return for_launch.getLaunchId(); }
String getProject()                     { return for_launch.getProject(); }
String getTargetId()                    { return for_launch.getTargetId(); }
String getProcessId()                   { return for_launch.getProcessId(); }
MintControl getMintControl()            { return for_launch.getMintControl(); }



String getSourceFile(MintControl ctrl) 
{
   if (file_name == null) {
      getFileData(ctrl);
    }
   return file_name;
}


String getProject(MintControl ctrl) 
{
   if (project_name == null) getFileData(ctrl);
   return project_name;
}



/********************************************************************************/
/*                                                                              */
/*      Get file information from bedrock                                       */
/*                                                                              */
/********************************************************************************/

private void getFileData(MintControl ctrl)
{
   CommandArgs args = new CommandArgs("PATTERN",IvyXml.xmlSanitize(class_name),
         "DEFS",true,"REFS",false,"FOR","TYPE");
   Element cxml = RoseEvalBase.sendBubblesXmlReply(ctrl,"PATTERNSEARCH",null,args,null); 
   for (Element lxml : IvyXml.elementsByTag(cxml,"MATCH")) {
      file_name = IvyXml.getAttrString(lxml,"FILE");
      Element ielt = IvyXml.getChild(lxml,"ITEM");
      project_name = IvyXml.getAttrString(ielt,"PROJECT");
      break;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputLocation(IvyXmlWriter xw,String proj,double p,MintControl mc) 
{
   xw.begin("LOCATION");
   xw.field("FILE",getSourceFile(mc));
   xw.field("LINE",getLine());
   xw.field("METHOD",getMethod());
   xw.field("PROJECT",proj);
   // want signature appended to method?
   xw.field("PRIORITY",p);
   xw.end("LOCATION");
}









}       // end of class RoseEvalFrameData




/* end of RoseEvalFrameData.java */

