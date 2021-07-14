/********************************************************************************/
/*                                                                              */
/*              SepalSequencer.java                                             */
/*                                                                              */
/*      Find patch suggestions using Sequncer                                   */
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

import org.eclipse.jdt.core.dom.Statement;
import org.w3c.dom.Element;

import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootRepairFinderDefault;
import edu.brown.cs.sequencer.application.ApplicationQuery;
import edu.brown.cs.sequencer.repair.RepairResult;

public class SepalSequencer extends RootRepairFinderDefault
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


private ApplicationQuery        sequencer_connect;

private static final int MAX_LOCAL_CHECK = 5;
private static final int MAX_LOCAL_RESULTS = 5;
private static final double SEARCH_THRESHOLD = 1.0;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SepalSequencer()
{
   sequencer_connect = null;
}


@Override synchronized protected void localSetup()
{
// if (pingServer()) return;
   
// String [] args = new String[] { };
// ApplicationServerRegulation asr = new ApplicationServerRegulation(args);
// asr.startServer();
   
   sequencer_connect = new ApplicationQuery();
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
   
   File bfile = getLocation().getFile();
   int lno = getLocation().getLineNumber();
   String bcnts = ctrl.getSourceContents(bfile);
   List<RepairResult> rslts = sequencer_connect.execute(bfile.getPath(),bcnts,lno);
   if (rslts == null || rslts.isEmpty()) return;
   
   int rct = 0;
   int fnd = 0;
   for (RepairResult rr : rslts) {
      Element edit = rr.getTextEditXML();
      double f = rr.getScore();
      if (f < SEARCH_THRESHOLD) continue;
      if (++rct > MAX_LOCAL_RESULTS) break;
      String logdata = getClass().getName() + "@" + f;
      String desc = "SequenceR suggests";       // edit.getDescription
      addRepair(edit,desc,logdata,f);
      if (++fnd > MAX_LOCAL_CHECK) break;
    }
}






/********************************************************************************/
/*                                                                              */
/*      Check if server is alive                                                */
/*                                                                              */
/********************************************************************************/

// private boolean pingServer()
// {
// String [] args = new String[] { };
// ApplicationServerRegulation asr = new ApplicationServerRegulation(args);
// String ping = "<PING />";
// try {
//    Element rslt = asr.sendMessage(ping);
//    if (rslt != null && IvyXml.isElement(rslt,"RESULT")) return true;
//  }
// catch (IOException e) {
//  }
// catch (MalformedMessageException e) { }
// 
// return false;
// }









}       // end of class SepalSequencer




/* end of SepalSequencer.java */

