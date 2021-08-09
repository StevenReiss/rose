/********************************************************************************/
/*                                                                              */
/*              RoseEvalCounter.java                                            */
/*                                                                              */
/*      Count # methods and tests in a set of files                             */
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompType;

public class RoseEvalCounter implements RoseEvalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main Program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   RoseEvalCounter rec = new RoseEvalCounter(args);
   rec.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<File>              work_files;
private int                     total_methods;
private int                     total_tests;
private int                     total_support;
private int                     total_possible;
private int                     file_methods;
private int                     file_tests;
private int                     file_support;
private int                     file_possible;
private boolean                 totals_only;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RoseEvalCounter(String [] args)
{
   work_files = new ArrayList<>();
   total_methods = 0;
   total_tests = 0;
   total_support = 0;
   total_possible = 0;
   totals_only = false;
   
   scanArgs(args);
}


/********************************************************************************/
/*                                                                              */
/*      Scan arguments                                                          */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (args[i].startsWith("-t")) {
            totals_only = true;
          }
         else badArgs();
       }
      else {
         File f = new File(args[i]);
         if (f.exists() && f.canRead()) work_files.add(f);
         else {
            System.err.println("ROSE: File " + f + " not accessible");
            System.exit(1);
          }
       }
    }
   if (work_files.isEmpty()) work_files.add(new File("."));
}


private void badArgs()
{
   System.err.println("ROSE: counter <file> ...");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   for (File f : work_files) {
      int sm = total_methods;
      int st = total_tests;
      int ss = total_support;
      int sp = total_possible;
      processFile(f,false);
      if (f.isDirectory()) {
         if (total_methods != sm || total_tests != st || total_support != ss ||
               total_possible != sp) {
            System.out.println((total_methods-sm) + "\t" +
                  (total_tests-st) + "\t" + 
                  (total_support-ss) + "\t" +
                  (total_possible-sp) + "\t" +
                  "TOTAL for " + f);
          }
       }
    }
   
   if (work_files.size() > 1) {
      System.out.println(total_methods + "\t" + 
            total_tests + "\t" + 
            total_support + "\t" +
            total_possible + "\t" +
            "TOTAL");
    }
}



private void processFile(File f,boolean test)
{
   if (Files.isSymbolicLink(f.toPath())) return;
   if (f.isDirectory() && f.canRead()) {
      if (f.getName().equalsIgnoreCase("test")) test = true;
      File [] subs = f.listFiles();
      if (subs != null) {
         for (File df : subs) {
            processFile(df,test);
          }
       }
    }
   else if (f.getName().endsWith(".java")) {
      processSource(f,test);
    }
}



private void processSource(File f,boolean test)
{
   CompilationUnit cu = compile(f);
   if (cu == null) return;
   
   file_methods = 0;
   file_tests = 0;
   file_support = 0;
   file_possible = 0;
   boolean junit = false;
   for (Object o2 : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o2;
      String nm = id.getName().getFullyQualifiedName();
      if (nm.contains("junit")) junit = true;
    }
   for (Object o : cu.types()) {
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
      processType(atd,false,junit);
    }
   
   if (file_tests > 0) {
      file_support = file_methods;
      file_methods = 0;
    }
   if (file_tests == 0 && file_methods == 0 && file_support == 0) return;
   if (test && !junit && file_tests == 0) {
      file_possible = file_methods;
      file_methods = 0;
    }
   if (!totals_only) {
      System.out.println(file_methods + "\t" + 
            file_tests + "\t" + 
            file_support + "\t" +
            file_possible + "\t" +
            f.getPath());
    }
   total_tests += file_tests;
   total_methods += file_methods;
   total_support += file_support;
   total_possible += file_possible;
}



private void processType(AbstractTypeDeclaration atd,boolean testclass,boolean junit)
{
   if (atd instanceof TypeDeclaration) {
      TypeDeclaration td = (TypeDeclaration) atd;
      if (td.isInterface()) return;
      JcompType sty = JcompAst.getJavaType(td.getSuperclassType());
      if (sty != null && sty.getName().contains("Test")) testclass = true;
    }
   for (Object o1 : atd.bodyDeclarations()) {
      if (o1 instanceof MethodDeclaration) {
         MethodDeclaration mtd = (MethodDeclaration) o1;
         if (Modifier.isAbstract(mtd.getModifiers())) continue;
         if (isTestMethod(mtd,testclass,junit)) {
            ++file_tests;
          }
         else {
            ++file_methods;
          }
       }
      else if (o1 instanceof AbstractTypeDeclaration) {
         AbstractTypeDeclaration itd = (AbstractTypeDeclaration) o1;
         boolean subtest = testclass;
         if (Modifier.isStatic(itd.getModifiers())) subtest = false;
         processType(itd,subtest,junit);
       }
    }
}


private boolean isTestMethod(MethodDeclaration mtd,boolean testclass,boolean junit)
{
   if (mtd.getName().getIdentifier().startsWith("test")) {
      if (testclass || junit) return true;
    }
   for (Object o4 : mtd.modifiers()) {
      if (o4 instanceof Annotation) {
         Annotation an = (Annotation) o4;
         Name anm = an.getTypeName();
         if (anm instanceof QualifiedName) {
            QualifiedName qn = (QualifiedName) anm;
            anm = qn.getName();
          }
         String ans = anm.getFullyQualifiedName();
         if (ans.equals("Test")) return true;
       }
    }
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Time-out compilation                                                    */
/*                                                                              */
/********************************************************************************/

private CompilationUnit compile(File f)
{
   Compiler c = new Compiler(f);
   c.start();
  
   return c.getResult();
}


private static class Compiler extends Thread {
   
   private File for_file;
   private CompilationUnit file_result;
   private boolean is_done;
   
   
   Compiler(File f) {
      super("Compile " + f.getPath());
      for_file = f;
      file_result = null;
    }
   
   @SuppressWarnings("deprecation")
   synchronized CompilationUnit getResult() {
      long start = System.currentTimeMillis();
      while (!is_done) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
         if (System.currentTimeMillis() - start > 10000) break;
       }
      if (file_result == null) {
         // compiler might go into infinite loop in getToken
         interrupt();
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
         if (isAlive()) stop();
       }
      return file_result;
    }
   
   @Override public void run() {
      String cnts = null;
      file_result = null;
      try {
         cnts = IvyFile.loadFile(for_file);
         if (cnts == null) return;
         if (cnts.contains("\n#define ")) return;
         file_result = JcompAst.parseSourceFile(cnts);
       }
      catch (IOException e) { }
      finally {
        synchronized (this) {
           is_done = true;
           notifyAll();
         }
       }
    }
   
}       // end of inner class Compiler




}       // end of class RoseEvalCounter




/* end of RoseEvalCounter.java */

