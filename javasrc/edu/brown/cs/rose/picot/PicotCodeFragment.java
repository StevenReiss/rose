/********************************************************************************/
/*                                                                              */
/*              PicotCodeFragment.java                                          */
/*                                                                              */
/*      Fragment of code for test case generation                               */
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


class PicotCodeFragment implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          code_string;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment(String code)
{
   code_string = code;
}




/********************************************************************************/
/*                                                                              */
/*        Access methods                                                        */
/*                                                                              */
/********************************************************************************/

String getCode()
{
   return code_string;
}



/********************************************************************************/
/*                                                                              */
/*      Construction methods                                                    */
/*                                                                              */
/********************************************************************************/

static PicotCodeFragment append(PicotCodeFragment ... frags)
{
   PicotCodeFragment pcf = null;
   
   for (PicotCodeFragment f : frags) {
      if (f != null) {
         if (pcf == null) pcf = f;
         else pcf = pcf.append(f,true);
       }
    }
   
   return pcf;
}



PicotCodeFragment append(PicotCodeFragment pcf,boolean line)
{
   return append(pcf.code_string,line);
}


PicotCodeFragment append(String addcode,boolean line)
{
   String code = code_string;
   if (line && !code.endsWith("\n")) code += "\n";
   code += addcode;
   return new PicotCodeFragment(code);
}

PicotCodeFragment append(String code1,String ... code2)
{
   String code = code_string;
   if (code1 != null) code += code1;
   for (int i = 0; i < code2.length; ++i) {
      if (code2[i] != null) code += code2[i];
    }
   
   return new PicotCodeFragment(code);
}



/********************************************************************************/
/*                                                                              */
/*      Comparison methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public boolean equals(Object o)
{
   if (o instanceof PicotCodeFragment) {
      PicotCodeFragment pcf = (PicotCodeFragment) o;
      return getCode().equals(pcf.getCode());
    }
   
   return false;
}



@Override public int hashCode()
{
   return getCode().hashCode();
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return code_string;
}



}       // end of class PicotCodeFragment




/* end of PicotCodeFragment.java */

