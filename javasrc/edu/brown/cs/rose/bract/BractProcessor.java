/********************************************************************************/
/*                                                                              */
/*              BractProcessor.java                                             */
/*                                                                              */
/*      Semantic quick fix (quickrepair) processor                              */
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

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.rose.root.RootControl;

public class BractProcessor extends Thread implements BractConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl rose_control;
private String reply_id;
private BractProblem for_problem;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BractProcessor(RootControl ctrl,String id,BractProblem prob)
{ 
   rose_control = ctrl;
   reply_id = id;
   for_problem = prob;
}



/********************************************************************************/
/*                                                                              */
/*      Main processing method                                                  */
/*                                                                              */
/********************************************************************************/

@Override public void run()
{
   if (for_problem != null) {
      // process the problem here
    }
   
   CommandArgs args = new CommandArgs("NAME",reply_id);
   rose_control.sendRoseMessage("ENDSUGGEST",args,null,-1);
}



}       // end of class BractProcessor




/* end of BractProcessor.java */

