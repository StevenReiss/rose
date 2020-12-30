/********************************************************************************/
/*                                                                              */
/*              SaverFactory.java                                               */
/*                                                                              */
/*      Seede Access for Verification of Edit-Based Repairs access class        */
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



package edu.brown.cs.rose.saver;

import java.util.Map;


import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.thorn.ThornConstants.ThornVariable;

public class SaverFactory implements SaverConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static SaverFactory the_factory = new SaverFactory();



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public static SaverFactory getFactory()
{
   return the_factory;
}



private SaverFactory()
{
   
}



/********************************************************************************/
/*                                                                              */
/*      Create a validate structure to handle validation data                   */
/*                                                                              */
/********************************************************************************/

public RootValidate createValidate(RootControl ctrl,RootProblem prob,String fid,RootLocation atloc)
{
   BudLaunch bl = new BudLaunch(ctrl,prob);
   
   if (fid == null) {
      SaverStartLocator ssl = new SaverStartLocator(prob,bl,atloc);
      fid = ssl.getStartingFrame();
    }
   if (fid == null) return null;
   
   SaverChangedItems itms = new SaverChangedItems(bl,fid);
   Map<ThornVariable,BudValue> cngs = itms.getChangedItems();
   if (cngs != null) {
      // get values here
    }
   
   return null;
}



}       // end of class SaverFactory




/* end of SaverFactory.java */

