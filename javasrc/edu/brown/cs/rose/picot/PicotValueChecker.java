/********************************************************************************/
/*                                                                              */
/*              PicotValueChecker.java                                          */
/*                                                                              */
/*      Check a set of values by running it through SEEDE                       */
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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.dom.CompilationUnit;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.rose.bract.BractFactory;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RootValidate.RootTraceCall;
import edu.brown.cs.rose.root.RootValidate.RootTrace;

class PicotValueChecker implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootValidate base_execution;
private String  setup_contents;
private File    local_file;
private String  package_name;
private String  test_class;

private static AtomicInteger method_ctr = new AtomicInteger(0);

private static final String START_STRING = "/*START*/\n";
private static final String END_STRING = "/*END*/\n";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueChecker(RootControl ctrl,RootValidate base)
{
   base_execution = base;
   setup_contents = null;
   local_file = null;
   package_name = null;
   test_class = null;
   
   setupSession();              
}


void finished()
{
   if (local_file != null) {
      base_execution.finishTestSession(local_file);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getPackageName()                         { return package_name; }

String getTestClassName()                       { return test_class; }



/********************************************************************************/
/*                                                                              */
/*      Run a code sequence to get trace result                                 */
/*                                                                              */
/********************************************************************************/

RootTrace generateTrace(PicotCodeFragment pcf)
{
   if (local_file == null) setupSession();
   
   int start = setup_contents.indexOf(START_STRING);
   start += START_STRING.length();
   int end = setup_contents.indexOf(END_STRING);
   String code = pcf.getCode();
   
   String newcnts = setup_contents.substring(0,start) + code + 
      setup_contents.substring(end);
   
   base_execution.editLocalFile(local_file,start,end,code);
   setup_contents = newcnts;
   
   return base_execution.getTestTrace(local_file);
}


/********************************************************************************/
/*                                                                              */
/*      Create subsession for test                                              */
/*                                                                              */
/********************************************************************************/

private void setupSession()
{
   if (local_file != null) return;
   
   RootTraceCall tc = base_execution.getExecutionTrace().getRootContext();
   String mthd = tc.getMethod();
   int idx1 = mthd.lastIndexOf(".");            // get end of class name
   int idx2 = mthd.lastIndexOf(".",idx1-1);     // get end of package name
   package_name = mthd.substring(0,idx2);
   int id = method_ctr.incrementAndGet();
   test_class = "PicotTestClass_" + id;
   
   local_file = new File("/SEEDE_LOCAL_FILE/SEEDE_" + test_class + ".java");
   
   StringBuffer buf = new StringBuffer();
   buf.append("package " + package_name + ";\n");
   buf.append("public class " + test_class + " {\n");
   buf.append("public " + test_class + "() { }\n");
   buf.append("@org.junit.Test public void test" + id + "()\n");
   buf.append("{ tester" + id + "(); }\n");      
   buf.append("public static boolean tester" + id + "() {\n");
   buf.append(START_STRING);
   buf.append("   // dummy code\n");
   buf.append(END_STRING);
   buf.append("   return true;\n");
   buf.append("}\n");
   buf.append("}\n");
   setup_contents = buf.toString();
   base_execution.addLocalFile(local_file,setup_contents);
   
   int loc = setup_contents.indexOf("public static boolean tester");
   int eloc = setup_contents.indexOf("}\n",loc)+1;
   CompilationUnit cu = JcompAst.parseSourceFile(setup_contents);
   int lin = cu.getLineNumber(loc);
   
   String proj = null;
   
   BractFactory bf = BractFactory.getFactory();
   String mnm = package_name + "." + test_class + ".tester" + id;
   RootLocation baseloc = bf.createLocation(local_file,loc,eloc,lin,proj,mnm);
   base_execution.createTestSession(local_file,baseloc);
}





}       // end of class PicotValueChecker




/* end of PicotValueChecker.java */

