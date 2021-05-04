/********************************************************************************/
/*                                                                              */
/*              ThornChangedData.java                                           */
/*                                                                              */
/*      Data to handle finding changes.  Note this is immutable                 */
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



package edu.brown.cs.rose.thorn;

import edu.brown.cs.ivy.jcomp.JcompSymbol;

class ThornChangedItem implements ThornConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcompSymbol     ref_value;
private boolean         is_changed;
private boolean         is_relevant;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ThornChangedItem(JcompSymbol js)
{
   ref_value = js;
   is_changed = false;
   is_relevant = false;
}


ThornChangedItem(JcompSymbol js,ThornChangedItem base)
{
   ref_value = js;
   is_relevant = base.is_relevant;
   is_changed = true;
}



private ThornChangedItem(ThornChangedItem base,JcompSymbol js,boolean ch,boolean rl)
{
   ref_value = (js == null ? base.ref_value : js);
   is_changed = base.is_changed | ch;
   is_relevant = base.is_relevant | rl;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

JcompSymbol getReference()                      { return ref_value; }
boolean isChanged()                             { return is_changed; }
boolean isRelevant()                            { return is_relevant; }



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

ThornChangedItem changeReference(JcompSymbol js)
{
   if (js == null || js == ref_value) return this;
   return new ThornChangedItem(this,js,false,false);
}



ThornChangedItem setChanged()
{
   if (is_changed) return this;
   return new ThornChangedItem(this,null,true,false);
}


ThornChangedItem setRelevant()
{
   if (is_relevant) return this;
   return new ThornChangedItem(this,null,false,true);
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   String s = ref_value.toString();
   if (is_changed) s += "#";
   if (is_relevant) s += "@";
   return s;
}




/********************************************************************************/
/*                                                                              */
/*      Equality methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public int hashCode() 
{
   int hc = ref_value.hashCode();
   if (is_changed) hc += 100;
   if (is_relevant) hc += 200;
   return hc;
}




@Override public boolean equals(Object o) {
   if (o instanceof ThornChangedItem) {
      ThornChangedItem vd = (ThornChangedItem) o;
      if (ref_value != vd.ref_value) return false; 
      if (is_changed != vd.is_changed) return false;
      if (is_relevant != vd.is_relevant) return false;
      return true;
    }
   return false;
}

}       // end of class ThornChangedData




/* end of ThornChangedData.java */

