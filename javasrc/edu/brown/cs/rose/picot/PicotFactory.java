/********************************************************************************/
/*                                                                              */
/*              PicotFactory.java                                               */
/*                                                                              */
/*      Program Interface for Creation Of Testcases -- main entries             */
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

import org.w3c.dom.Element;

import edu.brown.cs.rose.root.RootControl;

public class PicotFactory implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RootControl root_control;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public PicotFactory(RootControl rc)
{
   root_control = rc;
}


/********************************************************************************/
/*                                                                              */
/*      Generate test cases for current stopping point                          */
/*                                                                              */
/********************************************************************************/

public void createTestCase(String rid,Element xml)
{
   PicotTestCreator ptc = new PicotTestCreator(root_control,rid,xml);
   ptc.start();
}


/********************************************************************************/
/*                                                                              */
/*      Insert created test case into class                                     */
/*                                                                              */
/********************************************************************************/

public void insertTestCase(String rid,String cls,Element testcase)
{
   PicotTestInserter pic = new PicotTestInserter(root_control,rid,cls,testcase);
   pic.start();
}



}       // end of class PicotFactory






/* end of PicotFactory.java */

