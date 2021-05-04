/********************************************************************************/
/*                                                                              */
/*              ThornChangeData.java                                            */
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.rose.bud.BudStackFrame;

public class ThornChangeData implements ThornConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<BudStackFrame,ThornChangeMap> frame_changes;
private BudStackFrame                     top_frame;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ThornChangeData()
{
   frame_changes = new HashMap<>();
   top_frame = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void setChanges(BudStackFrame f,ThornChangeMap tcm)
{
   frame_changes.put(f,tcm);
}



ThornChangeMap getChanges(BudStackFrame f)
{
   return frame_changes.get(f);
}


void setTopFrame(BudStackFrame f)
{
   top_frame = f;
}


BudStackFrame getTopFrame()
{
   return top_frame;
}


/********************************************************************************/
/*                                                                              */
/*      Get parameters for a frame                                              */
/*                                                                              */
/********************************************************************************/

public List<ThornVariable> getTopParameters()
{
   return getParameters(top_frame);
}



List<ThornVariable> getParameters(BudStackFrame f)
{
   List<ThornVariable> rslt = new ArrayList<>();
   ThornChangeMap changes = getChanges(f);
   String cls = f.getClassName();
   
   for (ThornChangedItem tcd : changes.values()) {
      JcompSymbol js = tcd.getReference();
      VarData vd = null;
      if (js.isFieldSymbol()) {
         if (tcd.isChanged() && tcd.isRelevant()) {
            if (js.getClassType().getName().equals(cls)) {
               vd = new VarData(js.getName(),ThornVariableType.THIS_FIELD,tcd);
             }
            else {
               vd = new VarData(js.getFullName(),ThornVariableType.FIELD,tcd);
             }
          }
       }
      else {
         ASTNode n = js.getDefinitionNode();
         if (n instanceof SingleVariableDeclaration && 
               n.getParent() instanceof MethodDeclaration) {
            // handle parameters
            if (tcd.isChanged()) {
               vd = new VarData(js.getName(),ThornVariableType.PARAMETER,tcd);
             }
          }
         else {
            // handle locals
          }
       }
      if (vd != null) rslt.add(vd);
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Variable Result structure                                               */
/*                                                                              */
/********************************************************************************/

private static class VarData implements ThornVariable {

   private String var_name; 
   private ThornVariableType var_type;
   
   VarData(String nm,ThornVariableType typ,ThornChangedItem tcd) {
      var_name = nm;
      var_type = typ;
    }
   
   @Override public String getName()                    { return var_name; }
   @Override public ThornVariableType getVariableType() { return var_type; }
   
   @Override public String toString() {
      return var_type.toString() + ":" + var_name;
    }
   
}       // end of inner class VarData



}       // end of class ThornChangeData




/* end of ThornChangeData.java */

