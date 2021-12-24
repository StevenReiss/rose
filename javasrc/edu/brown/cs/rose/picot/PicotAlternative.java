/********************************************************************************/
/*                                                                              */
/*              PicotAlternative.java                                           */
/*                                                                              */
/*      Hold an alternative for creation                                        */
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



class PicotAlternative implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment code_fragment;
private PicotValueContext start_context;
private PicotAlternativeType alternative_type;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotAlternative(PicotCodeFragment pcf,PicotValueContext ctx,PicotAlternativeType t) 
{
   code_fragment = pcf;
   start_context = ctx;
   alternative_type = t;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment getCodeFragment()                  { return code_fragment; }

PicotValueContext getStartContext()                  { return start_context; }

PicotAlternativeType getAlternativeType()            { return alternative_type; }



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   String rslt = alternative_type + ": " + code_fragment.getCode();
   return rslt;
}




}       // end of class PicotAlternative




/* end of PicotAlternative.java */

