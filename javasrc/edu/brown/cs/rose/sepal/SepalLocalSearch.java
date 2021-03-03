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

import java.util.List;

import org.eclipse.jdt.core.dom.Statement;

import edu.brown.cs.rose.bract.BractSearch;
import edu.brown.cs.rose.bract.BractConstants.BractSearchResult;
import edu.brown.cs.rose.root.RootRepairFinderDefault;

public class SepalLocalSearch extends RootRepairFinderDefault
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BractSearch     search_engine;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalLocalSearch()
{
   search_engine = null;
}


@Override protected void localSetup()
{
   search_engine = BractSearch.getProjectSearch(getProcessor().getController());
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override protected double getFinderPriority()
{
   return 0.5;
}




@Override public void process()
{
    Statement stmt = (Statement) getResolvedStatementForLocation(null);
    List<BractSearchResult> rslts = search_engine.getResults(stmt);
    if (rslts == null) return;
    // handle results here
}








}       // end of class SepalLocalSearch




/* end of SepalLocalSearch.java */

