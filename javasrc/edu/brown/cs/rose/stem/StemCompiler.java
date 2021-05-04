/********************************************************************************/
/*                                                                              */
/*              StemCompiler.java                                               */
/*                                                                              */
/*      Handle getting ASTs and resolved ASTs related to problems               */
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcode.JcodeFactory;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;

class StemCompiler implements StemConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private StemMain                stem_main;
private Map<SourceFile,JcompProject> project_map;
private Map<String,JcodeFactory> binary_map;
private Map<File,SourceFile>    file_map;
private JcompControl            jcomp_control;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemCompiler(StemMain sm)
{
   stem_main = sm;
   file_map = new HashMap<>();
   project_map = new HashMap<>();
   binary_map = new HashMap<>();
   jcomp_control = new JcompControl();
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

ASTNode getSourceNode(String proj,File f,int offset,int line,boolean resolve)
{
   SourceFile sf = getSourceFile(f);
   CompilationUnit cu = getAstForFile(proj,sf,resolve);
   
   if (offset <= 0 && line <= 0) return cu;
   if (offset < 0) {
      offset = getLineOffset(cu,sf,line);
    }
   
   ASTNode n = findNode(cu,offset);
   
   return n;
}



ASTNode getStatementOfNode(ASTNode node)
{
   while (node != null) {
      if (node instanceof Statement) break;
      node = node.getParent();
    }  
   
   return node; 
}



IDocument getSourceDocument(String proj,File file)
{
   SourceFile sf = getSourceFile(file);
   return sf.getDocument();
}


/********************************************************************************/
/*                                                                              */
/*      Handle getting Jcode factory for project                                */
/*                                                                              */
/********************************************************************************/

private JcodeFactory getJcodeFactory(String proj)
{
   JcodeFactory jf = binary_map.get(proj);
   if (jf != null) return jf;
   
   CommandArgs cargs = new CommandArgs("PATHS",true,"PROJECT",proj);
   Element pxml = stem_main.sendBubblesMessage("OPENPROJECT",cargs,null);
   Element cp = IvyXml.getChild(pxml,"CLASSPATH");
   List<File> sourcepaths = new ArrayList<>();
   List<String> classpaths = new ArrayList<>();
   String ignore = null;
   for (Element rpe : IvyXml.children(cp,"PATH")) {
      String bn = null;
      String ptyp = IvyXml.getAttrString(rpe,"TYPE");
      if (ptyp != null && ptyp.equals("SOURCE")) {
	 bn = IvyXml.getTextElement(rpe,"OUTPUT");
	 String sdir = IvyXml.getTextElement(rpe,"SOURCE");
	 if (sdir != null) {
	    File sdirf = new File(sdir);
	    sourcepaths.add(sdirf);
	  }
       }
      else {
	 bn = IvyXml.getTextElement(rpe,"BINARY");
       }
      if (bn == null) continue;
      if (bn.endsWith("/lib/rt.jar")) {
	 int idx = bn.lastIndexOf("rt.jar");
	 ignore = bn.substring(0,idx);
       }
      if (bn.endsWith("/lib/jrt-fs.jar")) {
	 int idx = bn.lastIndexOf("/lib/jrt-fs.jar");
	 ignore = bn.substring(0,idx);
       }
      if (IvyXml.getAttrBool(rpe,"SYSTEM")) continue;
      if (!classpaths.contains(bn)) {
	 classpaths.add(bn);
       }
    }
   if (ignore != null) {
      for (Iterator<String> it = classpaths.iterator(); it.hasNext(); ) {
	 String nm = it.next();
	 if (nm.startsWith(ignore)) it.remove();
       }
    }  
   
   int ct = Runtime.getRuntime().availableProcessors();
   ct = Math.max(1,ct/2);
   jf = new JcodeFactory(ct);
   
   for (String s : classpaths) {
      jf.addToClassPath(s);
    }
   jf.load();
   
   synchronized (this) {
      JcodeFactory njf = binary_map.putIfAbsent(proj,jf);
      if (njf != null) jf = njf;
   }
   
   return jf;  
}


/********************************************************************************/
/*                                                                              */
/*      Handle getting file information                                         */
/*                                                                              */
/********************************************************************************/

private synchronized SourceFile getSourceFile(File f)
{
   SourceFile sf = file_map.get(f);
   if (sf == null) {
      sf = new SourceFile(f);
      file_map.put(f,sf);
    }
   
   return sf;
}



/********************************************************************************/
/*                                                                              */
/*      Handle getting AST for file                                             */
/*                                                                              */
/********************************************************************************/

private CompilationUnit getAstForFile(String proj,SourceFile file,boolean resolve)
{
   JcompProject jproj = getJcompProject(proj,file);
   
   if (resolve && !jproj.isResolved()) {
      jproj.resolve();
    }
   
   for (JcompSemantics js : jproj.getSources()) {
      if (js.getFile() == file) {
         return (CompilationUnit) js.getAstNode();
       }
    }
   
   return null;
}




private JcompProject getJcompProject(String proj,SourceFile file)
{
   JcompProject jp = project_map.get(file);
   if (jp != null) return jp;
   
   JcodeFactory jf = getJcodeFactory(proj);
   List<JcompSource> srcs = new ArrayList<>();
   srcs.add(file);
   addRelatedSources(srcs);
   jp = jcomp_control.getProject(jf,srcs);
   if (jp == null) return null;
   
   synchronized (this) {
      JcompProject njp = project_map.putIfAbsent(file,jp);
      if (njp != null) jp = njp;
      for (JcompSource src : srcs) {
         project_map.putIfAbsent((SourceFile) src,njp);
       }
    }
   
   return jp;
}




/********************************************************************************/
/*                                                                              */
/*      Find node for given offset\\\                                           */
/*                                                                              */
/********************************************************************************/

private int getLineOffset(CompilationUnit cu,SourceFile sf,int line) 
{
   if (line <= 0) return 0;
   String text = sf.getFileContents();
   int off = cu.getPosition(line,0);
   while (off < text.length()) {
      char c = text.charAt(off);
      if (!Character.isWhitespace(c)) break;
      ++off;
    }
   return off;
}

private ASTNode findNode(CompilationUnit cu,int offset) 
{
   if (cu == null) return null;
   
   ASTNode node = JcompAst.findNodeAtOffset(cu,offset);   
   
   return node;
}




/********************************************************************************/
/*                                                                              */
/*      Augment sources as needed                                               */
/*                                                                              */
/********************************************************************************/

private void addRelatedSources(List<JcompSource> srcs)
{
   Set<String> used = new HashSet<>();
   for (JcompSource jcs : srcs) {
      used.add(jcs.getFileName());
    }
   
   List<JcompSource> add = new ArrayList<>();
   for (JcompSource jcs : srcs) {
      SourceFile sf = (SourceFile) jcs;
      File f = sf.getFile();
      File dir = f.getParentFile();
      for (File srcf : dir.listFiles()) {
         if (srcf.getName().endsWith(".java")) {
            if (used.contains(srcf.getPath())) continue;
            SourceFile sf1 = getSourceFile(srcf);
            used.add(sf1.getFileName());
            add.add(sf1);
          }
       }
    }
   
   srcs.addAll(add);
}

/********************************************************************************/
/*                                                                              */
/*      File representation                                                     */
/*                                                                              */
/********************************************************************************/

private static class SourceFile implements JcompSource {

   private File for_file;
   private String file_body;
   private Document file_document;
   
   SourceFile(File f) {
      for_file = f;
      file_body = null;
      file_document = null;
    }
   
   File getFile()                               { return for_file; }
   
   @Override public String getFileName()        { return for_file.getPath(); }
   
   synchronized IDocument getDocument() {
      if (file_document == null) {
         file_document = new Document(getFileContents());
       }
      return file_document;
    }
   
   @Override public String getFileContents() {
      if (file_body != null) return file_body;
      try {
         file_body = IvyFile.loadFile(for_file);
         return file_body;
       }
      catch (IOException e) { }
      return null;
    }
   
}       // end of inner class SourceFile

}       // end of class StemCompiler




/* end of StemCompiler.java */

