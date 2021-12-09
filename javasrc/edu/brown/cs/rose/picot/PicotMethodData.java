/********************************************************************************/
/*                                                                              */
/*              PicotMethodData.java                                            */
/*                                                                              */
/*      Collection of data about a method                                       */
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

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;

abstract class PicotMethodData implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected List<PicotMethodEffect> method_effects;
protected List<JcompSymbol>       method_parameters; 
protected JcompSymbol             method_symbol;
protected JcompTyper              jcomp_typer;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotMethodData(JcompSymbol js,JcompTyper typer)
{
   method_symbol = js;
   jcomp_typer = typer;
   method_effects = null;
   method_parameters = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

List<PicotMethodEffect> getEffects()
{
   if (method_effects == null) {
      process();
    }
   
   return method_effects;
}


List<JcompSymbol> getParameters()
{
   if (method_effects == null) {
      process();
    }
   
   return method_parameters;
}



List<JcompType> getParameterTypes()
{
   if (method_symbol != null) {
      return method_symbol.getType().getComponents();
    }
   List<JcompType> typs = new ArrayList<>();
   for (JcompSymbol js : method_parameters) {
      typs.add(js.getType());
    }
   return typs;
}




/********************************************************************************/
/*                                                                              */
/*      Process a method to find its effects                                    */
/*                                                                              */
/********************************************************************************/

private void process()
{
   method_effects = new ArrayList<>();
   method_parameters = new ArrayList<>();
   
   processLocal();
}


protected abstract void processLocal();



}       // end of class PicotMethodData




/* end of PicotMethodData.java */

