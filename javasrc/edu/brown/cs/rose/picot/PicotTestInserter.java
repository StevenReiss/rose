/********************************************************************************/
/*                                                                              */
/*              PicotTestInserter.java                                          */
/*                                                                              */
/*      Insert test code into class file                                        */
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



package edu.brown.cs.rose.picot;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RoseLog;

class PicotTestInserter extends Thread implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl     root_control;
private String          test_id;
private String          test_class;
private Element         test_case;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotTestInserter(RootControl rc,String rid,String cls,Element xml)
{
   super("PCIOT_TEST_INSERTER_" + rid);
   
   root_control = rc;
   test_id = rid;
   test_class = cls;
   test_case = xml;
}



/********************************************************************************/
/*                                                                              */
/*      Do the actual insertion                                                 */
/*                                                                              */
/********************************************************************************/

@Override public void run()
{
   RoseLog.logD("PICOT","Insert test " + test_id + " " + test_class + " " +
         IvyXml.convertXmlToString(test_case));
   
   File src = findSourceFile();
   RoseLog.logD("PICOT","Found source file " + src);
   if (src == null) return;
   
   CompilationUnit cu = getAstForSource(src);
   if (cu == null) return;
   
   String code = fixupCode(cu);
   insertCodeIntoFile(src,cu,code);
   fixImports(src,cu);
}



/********************************************************************************/
/*                                                                              */
/*      Find or create source file for test class                               */
/*                                                                              */
/********************************************************************************/

private File findSourceFile()
{
   File f = root_control.getFileForClass(test_class);
   if (f == null) return null;
   if (f.exists()) return f;
   
   String pkg =  IvyXml.getAttrString(test_case,"PACKAGE");
   String tcls = test_class;
   int idx = tcls.lastIndexOf(".");
   if (idx > 0) tcls = tcls.substring(idx+1);
   String proj = IvyXml.getAttrString(test_case,"PROJECT");
   
   CommandArgs arg1 = new CommandArgs("PROJECT",proj,"SAVE",true);
   root_control.sendBubblesMessage("COMMIT",arg1,null);
   
   StringBuffer cnts = new StringBuffer();
   String c = "Test cases for " + pkg;
   c += "\nGenerated by PICOT";
   cnts.append(generateComment(c));
   cnts.append("\n\n");
   cnts.append("package " + pkg + ";\n");
   cnts.append("\n\n");
   cnts.append("public class " + tcls + " {\n\n");
   cnts.append(generateComment("Constructors"));
   cnts.append("\npublic " + tcls + "() { }\n");
   cnts.append("\n\n");
   
   CommandArgs args = new CommandArgs("PROJECT",proj,"NAME",test_class,
         "FORCE",true);
   String data = "<CONTENTS>";
   data += "<![CDATA[" + cnts.toString() + "]]>";
   data += "</CONTENTS>";
   
   Element xml = root_control.sendBubblesMessage("CREATECLASS",args,data);
   if (!IvyXml.isElement(xml,"RESULT")) return null;
   Element felt = IvyXml.getChild(xml,"FILE");
   if (felt == null) return null;
   String path = IvyXml.getTextElement(felt,"PATH");
   if (path == null) return null;
   return new File(path);
}


private CompilationUnit getAstForSource(File f)
{
   String proj = IvyXml.getAttrString(test_case,"PROJECT");
   root_control.compileAll(proj,Collections.singleton(f));
   
   ASTNode n = root_control.getSourceNode(proj,f,0,1,true,false);
   
   return (CompilationUnit) n;
}



/********************************************************************************/
/*                                                                              */
/*      Methods for generating test case                                        */
/*                                                                              */
/********************************************************************************/

private String fixupCode(CompilationUnit cu)
{
   String code = IvyXml.getTextElement(test_case,"TESTCODE");
   String method = IvyXml.getAttrString(test_case,"METHOD");
   if (methodUsed(method,cu)) {
      String newnm = null;
      for (int i = 1; ; ++i) {
         String sfx = "";
         for (int j = i; j > 0; j = (j-1)/26) {
            sfx += Character.valueOf((char) j);
          }
         newnm = method + sfx;
         if (!methodUsed(newnm,cu)) break;
       }
      code = code.replace(method,newnm);
      String xmthd = method.replace("test","tester");
      String xnmthd = newnm.replace("test","tester");
      code = code.replace(xmthd,xnmthd);
    }
   
   return code;
}


private boolean methodUsed(String nm,CompilationUnit cu)
{
   for (Object o : cu.types()) {
      if (o instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) o;
         for (Object o1 : td.bodyDeclarations()) {
            if (o1 instanceof MethodDeclaration) {
               MethodDeclaration md = (MethodDeclaration) o1;
               String mnm = md.getName().getIdentifier();
               if (mnm.equals(nm)) return true;
             }
          }
       }
    }
   return false;
}


private void insertCodeIntoFile(File src,CompilationUnit cu,String code)
{
   String c = "Test for " + IvyXml.getTextElement(test_case,"DESCRIPTION");
   String cmmt = generateComment(c);
   String ins = "\n\n" + cmmt + "\n\n" + code + "\n\n";
   
   int pos = -1;
   for (Object o : cu.types()) {
      if (o instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) o;
         for (Object o1 : td.bodyDeclarations()) {
            BodyDeclaration bd = (BodyDeclaration) o1;
            pos = cu.getExtendedStartPosition(bd) + cu.getExtendedLength(bd);
          }
         if (pos < 0) {
            pos = td.getStartPosition() + td.getLength() - 2;
          }
       }
    }
   if (pos < 0) return;
   
   doInsertion(src,pos,ins);
}




/********************************************************************************/
/*                                                                              */
/*      Methods for updating imports                                            */
/*                                                                              */
/********************************************************************************/

private void fixImports(File src,CompilationUnit cu)
{
   Set<String> present = new HashSet<>();
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      if (id.isStatic() || id.isOnDemand()) continue;
      present.add(id.getName().getFullyQualifiedName());
    }
   Set<String> toadd = new HashSet<>();
   for (Element ielt : IvyXml.children(test_case,"IMPORT")) {
      String ivl = IvyXml.getText(ielt);
      if (!present.contains(ivl)) toadd.add(ivl);
    }
   if (toadd.isEmpty()) return;
   
   StringBuffer buf = new StringBuffer();
   buf.append("\n");
   for (String s : toadd) {
      buf.append("import " + s + ";\n");
    }
   String ins = buf.toString();
   int pos = -1;
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      pos = id.getStartPosition() + id.getLength();
    }
   if (pos < 0) {
       PackageDeclaration pd = cu.getPackage();
       pos = pd.getStartPosition() + pd.getLength();
       ins = "\n\n" + ins + "\n\n";
    }
   if (pos < 0) pos = 0;
   
   doInsertion(src,pos,ins);
}
   


/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private String generateComment(String text)
{
   StringBuffer buf = new StringBuffer();
   buf.append("/****************\n");
   buf.append(" *\n");
   StringTokenizer tok = new StringTokenizer(text,"\r\n");
   while (tok.hasMoreTokens()) {
      String c = tok.nextToken();
      c = c.replace("*/","**");
      buf.append(" *\t" + c + "\n");
    }
   buf.append(" *\n");
   buf.append("***************/\n");
   return buf.toString();
}


private void doInsertion(File src,int pos,String ins)
{
   String proj = IvyXml.getAttrString(test_case,"PROJECT");
   CommandArgs args = new CommandArgs("FILE",src.getPath(),
         "NEWLINE",true,"PROJECT",proj);
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("EDIT");
   xw.field("START",pos);
   xw.field("END",pos);
   if (ins.contains("@@@]@@@]@@@>")) {
      xw.field("ENCODE",true);
      xw.text(IvyXml.byteArrayToString(ins.getBytes()));
    }
   else xw.cdata(ins);
   xw.end("EDIT");
   String edit = xw.toString();
   xw.close();
   
   root_control.sendBubblesMessage("EDITFILE",args,edit);
}


}       // end of class PicotTestInserter




/* end of PicotTestInserter.java */

