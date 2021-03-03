/********************************************************************************/
/*                                                                              */
/*              BractSearch.java                                                */
/*                                                                              */
/*      Interface to COCKER search engine                                       */
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



package edu.brown.cs.rose.bract;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.leash.LeashIndex;
import edu.brown.cs.ivy.leash.LeashResult;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RoseLog;

public class BractSearch implements BractConstants
{



/********************************************************************************/
/*                                                                              */
/*      Static methods                                                          */
/*                                                                              */
/********************************************************************************/

public synchronized static BractSearch getProjectSearch(RootControl ctrl)
{
   if (local_engine == null) {
      local_engine = new BractSearch(ctrl);
    }
   return local_engine;
}


public static BractSearch getGlobalSearch()
{
   if (global_engine == null) {
      global_engine = new BractSearch();
    }
   return global_engine;
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LeashIndex      cocker_index;
private boolean         is_local;

private static BractSearch local_engine;
private static BractSearch global_engine;

private static final String     GLOBAL_HOST = "cocker.cs.brown.edu";
private static final int        GLOBAL_PORT = 10264;            // SSFIX port



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BractSearch(RootControl ctrl)
{
   is_local = true;
   if (is_local) {
      cocker_index = ctrl.getProjectIndex();
    }
}


private BractSearch()
{
   is_local = false;
   cocker_index = new LeashIndex(GLOBAL_HOST,GLOBAL_PORT);
}


/********************************************************************************/
/*                                                                              */
/*      Search methods                                                          */
/*                                                                              */
/********************************************************************************/

public List<BractSearchResult> getResults(ASTNode stmt)
{
   List<BractSearchResult> rslt = new ArrayList<>();
   if (stmt == null) return rslt;
   
   String filename = JcompAst.getSource(stmt).getFileName();
   File file = new File(filename);
   int off = stmt.getStartPosition();
   CompilationUnit cu = (CompilationUnit) stmt.getRoot();
   int line = cu.getLineNumber(off);
   int col = cu.getColumnNumber(off);
   
   List<LeashResult> base = cocker_index.queryStatements(file,line,col);
   if (base == null || base.isEmpty()) return rslt;
   
   for (LeashResult lr : base) {
      RoseLog.logD("BRACT","Leash result: " + lr.getFilePath() + " " + 
            lr.getLines() + " " + lr.getColumns());
    }
   
   
   return rslt;
}


}       // end of class BractSearch




/* end of BractSearch.java */

