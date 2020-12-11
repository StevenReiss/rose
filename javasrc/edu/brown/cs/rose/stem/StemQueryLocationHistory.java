/********************************************************************************/
/*                                                                              */
/*              StemQueryLocationHistory.java                                   */
/*                                                                              */
/*      Handle location history queries                                         */
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

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.rose.root.RootProblem;
import edu.brown.cs.rose.root.RoseException;

class StemQueryLocationHistory extends StemQueryHistory
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StemQueryLocationHistory(StemMain ctrl,RootProblem prob)
{
   super(ctrl,prob);
}

/********************************************************************************/
/*                                                                              */
/*      Process location query                                                  */
/*                                                                              */
/********************************************************************************/

@Override void process(StemMain stem,IvyXmlWriter xw) throws RoseException
{
   String locxml = getExecLocation();
   if (locxml == null) throw new RoseException("Location undefined");
   
   Element hrslt = getLocationData(stem,locxml);
   outputGraph(hrslt,xw); // method body goes here
}







/********************************************************************************/
/*                                                                              */
/*      Set up and execute query                                                */
/*                                                                              */
/********************************************************************************/

private Element getLocationData(StemMain stem,String locxml)
{
   stem.waitForAnalysis();
   
   CommandArgs args = new CommandArgs("FILE",for_file.getAbsolutePath(),
         "QTYPE","LOCATION",
         "LINE",line_number,
         "METHOD",method_name);
   
   String qxml = locxml;
   String sxml = getXmlForStack();
   if (sxml != null) {
      if (qxml == null) qxml = sxml;
      else qxml += sxml;
    }
   Element rslt = stem.sendFaitMessage("FLOWQUERY",args,qxml);
   
   return rslt;
}




}       // end of class StemQueryLocationHistory




/* end of StemQueryLocationHistory.java */

