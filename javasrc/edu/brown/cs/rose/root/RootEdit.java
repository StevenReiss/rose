/********************************************************************************/
/*										*/
/*		RootEdit.java							*/
/*										*/
/*	Representation of a text edit						*/
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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.CopyingRangeMarker;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class RootEdit implements RootConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Element 	complete_edit;
private File		base_file;
private TextEdit	text_edit;

private static AtomicInteger edit_counter = new AtomicInteger(1);


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public RootEdit(Element xml)
{
   complete_edit = xml;
   String f = IvyXml.getAttrString(xml,"FILE");
   if (f == null) base_file = null;
   else base_file = new File(f);
}


public RootEdit(File f,TextEdit te)
{
   complete_edit = null;
   base_file = f;
   text_edit = te;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public File getFile()                           { return base_file; }

public Element getTextEditXml()
{
   if (complete_edit != null) return complete_edit;
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("EDIT");
   xw.field("FILE",base_file);
   xw.field("TYPE","NONE");
   outputTextEdit(text_edit,xw);
   xw.end("EDIT");
   Element rslt = IvyXml.convertStringToXml(xw.toString());
   xw.close();
   return rslt;
}

public TextEdit getTextEdit()
{
   if (text_edit != null) return text_edit;
   if (complete_edit == null) return null;
   text_edit = computeTextEdit(complete_edit);
   
   return text_edit;
}
   
   
   
public int getEditStartOffset()
{
   if (text_edit != null) {
      return text_edit.getOffset();
    }
   else if (complete_edit != null) {
      int off = -1;
      for (Element ed : IvyXml.elementsByTag(complete_edit,"EDIT")) {
         int o = IvyXml.getAttrInt(ed,"OFFSET");
         if (o >= 0) {
            if (off < 0 || off > o) off = o;
          }
       }
      if (off > 0) return off;
    }
   
   return 0;
}


public int getEditEndOffset()
{
   if (text_edit != null) {
      return text_edit.getExclusiveEnd();
    }
   else if (complete_edit != null) {
      int off = -1;
      for (Element ed : IvyXml.elementsByTag(complete_edit,"EDIT")) {
         int o = IvyXml.getAttrInt(ed,"EXCEND");
         if (o >= 0) {
            if (off < 0 || off < o) off = o;
          }
       }
      if (off > 0) return off;
    }
   
   return 0;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   if (complete_edit != null) {
      xw.writeXml(complete_edit);
    }
   else {
      xw.begin("REPAIREDIT");
      xw.field("FILE",base_file);
      outputTextEdit(text_edit,xw);
      xw.end("REPAIREDIT");
    }
}



private static void outputTextEdit(TextEdit te,IvyXmlWriter xw)
{
   xw.begin("EDIT");
   xw.field("OFFSET",te.getOffset());
   xw.field("LENGTH",te.getLength());
   xw.field("INCEND",te.getInclusiveEnd());
   xw.field("EXCEND",te.getExclusiveEnd());
   xw.field("ID",te.hashCode());
   xw.field("COUNTER",edit_counter.incrementAndGet());

   if (te instanceof CopyingRangeMarker) {
      xw.field("TYPE","COPYRANGE");
    }
   else if (te instanceof CopySourceEdit) {
      CopySourceEdit cse = (CopySourceEdit) te;
      xw.field("TYPE","COPYSOURCE");
      xw.field("TARGET",cse.getTargetEdit().hashCode());
    }
   else if (te instanceof CopyTargetEdit) {
      CopyTargetEdit cte = (CopyTargetEdit) te;
      xw.field("TYPE","COPYTARGET");
      xw.field("SOURCE",cte.getSourceEdit().hashCode());
    }
   else if (te instanceof DeleteEdit) {
      xw.field("TYPE","DELETE");
    }
   else if (te instanceof InsertEdit) {
      InsertEdit ite = (InsertEdit) te;
      xw.field("TYPE","INSERT");
      xw.cdataElement("TEXT",ite.getText());
    }
   else if (te instanceof MoveSourceEdit) {
      MoveSourceEdit mse = (MoveSourceEdit) te;
      xw.field("TYPE","MOVESOURCE");
      xw.field("TARGET",mse.getTargetEdit().hashCode());
    }
   else if (te instanceof MoveTargetEdit) {
      xw.field("TYPE","MOVETARGET");
    }
   else if (te instanceof MultiTextEdit) {
      xw.field("TYPE","MULTI");
    }
   else if (te instanceof RangeMarker) {
      xw.field("TYPE","RANGEMARKER");
    }
   else if (te instanceof ReplaceEdit) {
      ReplaceEdit rte = (ReplaceEdit) te;
      xw.field("TYPE","REPLACE");
      xw.cdataElement("TEXT",rte.getText());
    }
   else if (te instanceof UndoEdit) {
      xw.field("TYPE","UNDO");
    }

   if (te.hasChildren()) {
      for (TextEdit cte : te.getChildren()) {
	 outputTextEdit(cte,xw);
       }
    }
   xw.end("EDIT");
}



/********************************************************************************/
/*                                                                              */
/*      Convert XML to an actual EDIT                                           */
/*                                                                              */
/********************************************************************************/

TextEdit computeTextEdit(Element xml)
{ 
   int off = IvyXml.getAttrInt(xml,"OFFSET");
   int len = IvyXml.getAttrInt(xml,"LENGTH");
   String type = IvyXml.getAttrString(xml,"TYPE");
   String cnts = IvyXml.getTextElement(xml,"TEXT");
   TextEdit te = null;
   switch (type) {
      case "MULTI" :
         te = new MultiTextEdit(off,len);
         break;
      case "REPLACE" :
         te = new ReplaceEdit(off,len,cnts);
         break;
      case "DELETE" :
         te = new DeleteEdit(off,len);
         break;
      case "INSERT" :
         te = new InsertEdit(off,cnts);
         break;
      case "NONE" :
         break;
      default :
         RoseLog.logE("ROOT","Edit type " + type + " not found");
         return null;
    }
   
   for (Element celt : IvyXml.children(xml,"EDIT")) {
      TextEdit te1 = computeTextEdit(celt);
      if (te == null) te = te1;
      else if (te1 != null) te.addChild(te1);
    }
   
   return te;
}

}	// end of class RootEdit




/* end of RootEdit.java */

