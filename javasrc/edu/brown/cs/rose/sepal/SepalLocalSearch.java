/********************************************************************************/
/*                                                                              */
/*              SepalLocalSearch.java                                           */
/*                                                                              */
/*      Find similar statements or contexts in project as suggestions           */
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



package edu.brown.cs.rose.sepal;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.rose.bract.BractSearch;
import edu.brown.cs.rose.bract.BractConstants.BractSearchResult;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootRepairFinderDefault;
import edu.brown.cs.rose.root.RoseLog;
import sharpfix.global.PatchAsASTRewriteWithScore;
import sharpfix.patchgen.LocalPatchGenerator;

public class SepalLocalSearch extends RootRepairFinderDefault
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BractSearch     search_engine;

private static final int MAX_LOCAL_CHECK = 5;
private static final int MAX_LOCAL_RESULTS = 5;
private static final double SEARCH_THRESHOLD = 1.0;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalLocalSearch()
{
   search_engine = null;
}


@Override synchronized protected void localSetup()
{
   if (search_engine == null) {
      search_engine = BractSearch.getProjectSearch(getProcessor().getController());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public double getFinderPriority()
{
   return 0.45;
}



@Override public void process()
{
   RootControl ctrl = getProcessor().getController();
   Statement stmt = (Statement) getResolvedStatementForLocation(null);
   if (stmt == null) return;
   List<BractSearchResult> rslts = search_engine.getResults(stmt,SEARCH_THRESHOLD);
   if (rslts == null || rslts.isEmpty()) return; 
   
   File bfile = getLocation().getFile();
   int lno = getLocation().getLineNumber();
   String bcnts = ctrl.getSourceContents(bfile);
   ASTNode bnode = stmt;
   
   int rct = 0;
   int fnd = 0;
   for (BractSearchResult sr : rslts) {
      if (sr.getFile().equals(bfile) && sr.getLineNumber() == lno) continue;
      if (!sr.getFile().exists() || !sr.getFile().canRead()) continue;
      if (++rct > MAX_LOCAL_RESULTS) break;
      String ccnts = ctrl.getSourceContents(sr.getFile());
      ASTNode cnode = ctrl.getNewSourceStatement(sr.getFile(),sr.getLineNumber(),sr.getColumnNumber());
      List<PatchAsASTRewriteWithScore> patches;
      
      try {
         patches = LocalPatchGenerator.makePatches(bcnts,bnode,ccnts,cnode);
       }
      catch (Throwable t) {
         RoseLog.logE("SEPAL","Problem with sharpFix",t);
         continue;
       }
      int ct = patches.size();
      for (int i = 0; i < ct; ++i) {
         PatchAsASTRewriteWithScore r = patches.get(i);
         if (!isRelevant(lno,stmt,r)) continue;
         ASTRewrite rw = r.getASTRewrite();
        
         // need to get a description from the rewrite -- replace Search-based repair with that
         double score = r.getScore();
         // might want to manipulate score a bit
         // rw.rewriteAST().getLength() < MAX_CHANGE_LENGTH
         String logdata = getClass().getName() + "@" + i + "@" + r.getType();
         String desc = r.getDescription();
         addRepair(rw,desc,logdata,score);
         if (++fnd > MAX_LOCAL_CHECK) break;
       }
      // add repair for each returned patch
      RoseLog.logD("SEPAL","Find local search results " + ct + " " + fnd);
      if (fnd > MAX_LOCAL_CHECK) break;
    }
}



private boolean isRelevant(int lno,Statement stmt,PatchAsASTRewriteWithScore r)
{
   if (r.getType().equals("METHODREPLACE")) return false;
   if (r.getType().startsWith("REPLACE")) {
      if (r.getLineNumber() != lno) return false;
    }
   else if (r.getType().startsWith("INSERT")) {
      if (Math.abs(r.getLineNumber()-lno) > 1) return false;
    }
   
   if (r.getHeight() >= 3.0) return false;
   
   return true;
}
   







}       // end of class SepalLocalSearch




/* end of SepalLocalSearch.java */

