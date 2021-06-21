/********************************************************************************/
/*                                                                              */
/*              ThornChangeMap.java                                             */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;

public class ThornChangeMap extends HashMap<JcompSymbol,ThornChangedItem> implements ThornConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private final static long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ThornChangeMap()
{ }



/********************************************************************************/
/*                                                                              */
/*      Access Methods                                                          */
/*                                                                              */
/********************************************************************************/

public boolean shouldBeUsed(ASTNode n)
{
   RelevanceChecker rc = new RelevanceChecker();
   n.accept(rc);
   
   return rc.isChanged() || rc.isRelevant();
}



private class RelevanceChecker extends ASTVisitor {
   
   private boolean is_changed;
   private boolean is_relevant;
   
   RelevanceChecker() {
      is_changed = false;
      is_relevant = false;
    }
   
   boolean isChanged()                  { return is_changed; }
   boolean isRelevant()                 { return is_relevant; }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js == null) js = JcompAst.getDefinition(n);
      if (js == null) return;
      ThornChangedItem tci = get(js);
      if (tci != null) {
         is_changed |= tci.isChanged();
         is_relevant |= tci.isRelevant();
       }
    }
   
}       // end of inner class RelevanceChecker


}       // end of class ThornChangeMap




/* end of ThornChangeMap.java */

