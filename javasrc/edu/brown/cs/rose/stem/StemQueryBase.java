/********************************************************************************/
/*                                                                              */
/*              StemQueryBase.java                                              */
/*                                                                              */
/*      Common functionality for all Stem queries                               */
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



package edu.brown.cs.rose.stem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootNodeContext;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RoseException;

abstract class StemQueryBase implements StemConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected String        thread_id;
protected String        frame_id;
protected File          for_file;
protected String        project_name;
protected int           line_number;
protected int           line_offset;
protected String        method_name;
protected BudLaunch     bud_launch;
protected RootNodeContext node_context;
protected StemMain      stem_control;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected StemQueryBase(StemMain ctrl,Element xml)
{
   thread_id = IvyXml.getAttrString(xml,"THREAD");
   frame_id = IvyXml.getAttrString(xml,"FRAME");
   for_file = new File(IvyXml.getAttrString(xml,"FILE"));
   project_name = IvyXml.getAttrString(xml,"PROJECT");
   line_number = IvyXml.getAttrInt(xml,"LINE");
   line_offset = IvyXml.getAttrInt(xml,"OFFSET");
   method_name = IvyXml.getAttrString(xml,"METHOD");
   stem_control = ctrl;
   
   bud_launch = new BudLaunch(ctrl,thread_id,frame_id,project_name);
}


protected StemQueryBase(StemMain ctrl,RootProblem prob)
{
   thread_id = prob.getThreadId();
   frame_id = prob.getFrameId();
   RootLocation loc = prob.getBugLocation();
   for_file = loc.getFile();
   project_name = loc.getProject();
   line_number = loc.getLineNumber();
   line_offset = loc.getStartOffset();
   method_name = loc.getMethod();
   node_context = prob.getNodeContext();
   
   bud_launch = new BudLaunch(ctrl,thread_id,frame_id,project_name);
}



/********************************************************************************/
/*                                                                              */
/*      Find last component of a tree                                           */
/*                                                                              */
/********************************************************************************/

protected static String getNodeTypeName(ASTNode n) 
{
   String typ = n.getClass().getName();
   int idx = typ.lastIndexOf(".");
   if (idx > 0) typ = typ.substring(idx+1);
   return typ;
}


/********************************************************************************/
/*                                                                              */
/*      Find statement of stopping point                                        */
/*                                                                              */
/********************************************************************************/

protected ASTNode getSourceStatement() throws RoseException
{
   try {
      String text = IvyFile.loadFile(for_file);
      CompilationUnit cu = JcompAst.parseSourceFile(text);
      return findNode(cu,text);
    }
   catch (IOException e) {
      throw new RoseException("Problem reading source",e);
    }
}


private ASTNode findNode(CompilationUnit cu,String text) throws RoseException
{
   if (cu == null) return null;
   if (line_offset <= 0 && line_number > 0) {
      line_offset = cu.getPosition(line_number,0);
      while (line_offset < text.length()) {
         char c = text.charAt(line_offset);
         if (!Character.isWhitespace(c)) break;
         ++line_offset;
       }
    }
   ASTNode node = JcompAst.findNodeAtOffset(cu,line_offset);   
   node = getStatementOf(node);
   if (node == null) throw new RoseException("Statement at line not found");
   return node;
}


protected ASTNode findNode(CompilationUnit cu,String text,int line) 
{
   if (cu == null) return null;
   int off = -1;
   if (line > 0) {
      off = cu.getPosition(line,0);
      while (off < text.length()) {
         char c = text.charAt(off);
         if (!Character.isWhitespace(c)) break;
         ++off;
       }
    }
   ASTNode node = JcompAst.findNodeAtOffset(cu,off);   
   return node;
}



protected ASTNode getResolvedSourceStatement() throws RoseException
{
   SourceFile sf = new SourceFile(for_file);
   String text = sf.getFileContents();
   if (text == null) throw new RoseException("Problem reading source");
   
   JcompProject jp = setupJcompProject(sf);
   jp.resolve();
   for (JcompSemantics js : jp.getSources()) {
      if (js.getFile() == sf) {
         CompilationUnit cu = (CompilationUnit) js.getAstNode();
         return findNode(cu,text);
       }
    }
   
   return null;
}



private JcompProject setupJcompProject(SourceFile sf)
{   
   JcodeFactory jf = stem_control.getJcodeFactory(project_name);
   
   JcompControl jc = new JcompControl();
   Collection<JcompSource> srcs = new ArrayList<>();
   srcs.add(sf);
   
   JcompProject proj = jc.getProject(jf,srcs);
   
   return proj;
}






private static class SourceFile implements JcompSource {
   
   private File for_file;
   private String file_body;
   
   SourceFile(File f) {
      for_file = f;
    }
   
   @Override public String getFileName()        { return for_file.getPath(); }
   
   @Override public String getFileContents() {
      if (file_body != null) return file_body;
      try {
         file_body = IvyFile.loadFile(for_file);
         return file_body;
       }
      catch (IOException e) { }
      return null;
    }
}


protected ASTNode getStatementOf(ASTNode node)
{
   while (node != null) {
      if (node instanceof Statement) break;
      node = node.getParent();
    }  
   
   return node;
}




/********************************************************************************/
/*                                                                              */
/*      Handle getting relevant information for query                           */
/*                                                                              */
/********************************************************************************/

protected String getXmlForLocation(String elt,ASTNode node,boolean next)
{
   if (node == null) return null;
   
   try (IvyXmlWriter xw = new IvyXmlWriter()) {
      addXmlForLocation(elt,node,next,xw);
      return xw.toString();
    }
}



protected String getXmlForStack()
{
   BudStack stk = bud_launch.getStack();
   if (stk == null) return null;
   
   try (IvyXmlWriter xw = new IvyXmlWriter()) {
      xw.begin("STACK");
      for (BudStackFrame bsf : stk.getFrames()) {
         xw.begin("FRAME");
         xw.field("CLASS",bsf.getClassName());
         xw.field("METHOD",bsf.getMethodName());
         xw.field("SIGNATURE",bsf.getMethodSignature());
         xw.field("FSIGN",bsf.getFormatSignature());
         xw.end("FRAME");
       }
      xw.end("STACK");
      return xw.toString();
    }
}




protected static void addXmlForLocation(String elt,ASTNode node,boolean next,IvyXmlWriter xw)
{
   if (node == null) return;
   
   CompilationUnit cu = (CompilationUnit) node.getRoot();
   
   ASTNode use = node;
   ASTNode after = null;
   
   if (next) {
      use = node.getParent();
      after = node;
    }
   else {
      after = getAfterNode(node);
    }
   
   xw.begin(elt);
   xw.field("START",use.getStartPosition());
   xw.field("END",use.getStartPosition() + node.getLength());
   xw.field("LINE",cu.getLineNumber(use.getStartPosition()));
   xw.field("NODETYPE",getNodeTypeName(use));
   xw.field("NODETYPEID",use.getNodeType());
   
   if (after != null) {
      StructuralPropertyDescriptor spd = after.getLocationInParent();
      xw.field("AFTER",spd.getId());
      xw.field("AFTERSTART",after.getStartPosition());
      xw.field("AFTEREND",after.getStartPosition() + after.getLength());
      xw.field("AFTERTYPE",getNodeTypeName(after));
      xw.field("AFTERTYPEID",after.getNodeType());
    }
   xw.textElement("TEXT",node.toString());
   xw.end(elt);
}



protected String getExecLocation() throws RoseException
{
   String rslt = null;
   ASTNode node = getSourceStatement();
   if (node == null) return null;
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("LOCATION");
   xw.field("FILE",for_file);
   xw.field("LINE",line_number);
   xw.field("START",node.getStartPosition());
   xw.field("END",node.getStartPosition() + node.getLength());
   xw.field("NODETYPE",getNodeTypeName(node));
   xw.field("NODETYPEID",node.getNodeType());
   xw.end("LOCATION");
   rslt = xw.toString();
   xw.close();
   
   return rslt;
}
/********************************************************************************/
/*                                                                              */
/*      Get after node for a tree node                                          */
/*                                                                              */
/********************************************************************************/

protected static ASTNode getAfterNode(ASTNode expr)
{
   if (expr == null) return null;
   
   AfterFinder af = new AfterFinder();
   expr.accept(af);
   return af.getAfterNode();
}



private static class AfterFinder extends ASTVisitor {

   private ASTNode start_node;
   private ASTNode last_node;
   
   AfterFinder() {
      start_node = null;
      last_node = null;
    }
   
   ASTNode getAfterNode()               { return last_node; }
   
   @Override public boolean preVisit2(ASTNode n) {
      if (start_node == null) {
         start_node = n;
         last_node = null;
       }
      return true;
    }
   
   @Override public void postVisit(ASTNode n) {
      if (n == start_node) {
         start_node = null;
       }
      else last_node = n;
    }
   
}




}       // end of class StemQueryBase




/* end of StemQueryBase.java */

