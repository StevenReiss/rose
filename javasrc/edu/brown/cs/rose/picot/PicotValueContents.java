/********************************************************************************/
/*                                                                              */
/*              PicotValueContents.java                                         */
/*                                                                              */
/*      Information about value building to date.                               */
/*      This class is immutable after fixupValues is called                     */
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RootValidate.RootTrace;
import edu.brown.cs.rose.root.RootValidate.RootTraceCall;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;
import edu.brown.cs.rose.root.RootValidate.RootTraceVariable;

class PicotValueContents implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


private Map<String,PicotCodeFragment> value_map;
private Map<JcompType,List<PicotCodeFragment>> computed_values;
private PicotValueChecker value_checker;
private Map<String,String> base_value_map;
private PicotCodeFragment init_code;
private Boolean setup_value;
private RootTrace value_result;
private JcompTyper jcomp_typer;
private Map<String,RootTraceValue> variable_values;


private static final PicotCodeFragment bad_fragment;

static {
   bad_fragment = new PicotCodeFragment("<<BAD>>");
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueContents(PicotValueChecker vc,JcompTyper typer)
{
   value_map = new HashMap<>();
   computed_values = new HashMap<>();
   value_checker = vc;
   base_value_map = new HashMap<>();
   init_code = null;
   setup_value = false;
   value_result = null;
   jcomp_typer = typer;
   variable_values = new HashMap<>();
}



PicotValueContents(PicotValueContents vc,PicotCodeFragment addedcode,
      String var,RootTraceValue val)
{
   value_map = new HashMap<>(vc.value_map);
   computed_values = new HashMap<>(vc.computed_values);
   value_checker = vc.value_checker;
   base_value_map = new HashMap<>(vc.base_value_map);
   jcomp_typer = vc.jcomp_typer;
   variable_values = new LinkedHashMap<>(vc.variable_values);
   if (var != null && val != null) {
      variable_values.put(var,val);
    }
   
   if (init_code == null) init_code = addedcode;
   else {
      init_code = init_code.append(addedcode,true);
    }
   
   value_result = null;
   setup_value = null;
   setupContents();
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

RootTrace getTrace()
{
   return value_result;
}



PicotCodeFragment getPreviousValue(String id)
{
   if (id == null) return null;
   
   PicotCodeFragment rslt = value_map.get(id);
   
   if (rslt == bad_fragment) return null;
   if (rslt != null) return rslt;
   
   value_map.put(id,bad_fragment);
   
   return null;
}


void setPreviousValue(String id,PicotCodeFragment pcf)
{
   if (id == null) return;
   if (pcf != null) value_map.put(id,pcf);
}


boolean isValidSetup()                               
{
   return setup_value;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void setupContents()
{
   if (setup_value != null) return;
   
   value_result = value_checker.generateTrace(init_code);
   if (value_result == null || value_result.getException() != null) setup_value = false;
   else if (value_result.getReturnValue() == null) setup_value = false;
   else setup_value = true;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

PicotValueContents fixupValues()
{
   if (setup_value == null || setup_value == false) return null;
   if (value_result == null) return null;
   
   for (Map.Entry<String,RootTraceValue> ent : variable_values.entrySet()) {
      String var = ent.getKey();
      RootTraceValue targetval = ent.getValue();
      RootTraceCall rtc = value_result.getRootContext();
      RootTraceVariable rtvar = rtc.getTraceVariables().get(var);
      if (rtvar == null) continue;
      RootTraceValue sourceval = rtvar.getValueAtTime(value_result,rtc.getEndTime());
      PicotValueContents npvc = checkValue(sourceval,targetval,rtc.getEndTime());
      if (npvc == null) return null;
    }
   
   return this;
}



PicotValueContents checkValue(RootTraceValue sourceval,RootTraceValue targetval,long time)
{
   PicotValueContents rslt = this;
   
   // sourceval is a first approximation of targetval
   // if both are primitives, then they need to be equal: return null if not, this if so
   // if both are objects, they must have the same types
   // if one or both have known ids, then these much match
   // if both are user objects, then check that the fields appearing in targetval 
   //           match those in sourceval.  If they do not, then consider adding code
   //           to change the field (calling a setter) and checking if that works
   //           If it does, then use the resultant ValueContents to proceed
   // if both are objects, associate their ids
   // add the object as a possible value for the type (variable)
   // for all fields, if there is a getter (or field is accessible), 
   //           add the getter expression as a possible value for the type.  
   // the latter might be done recursively
   
   // the basic idea is that this routine should validate all prior variables, set up
   // the id associations between initialization code and target values, define the
   // set of potential object values for each type (for use in later calls), and,
   // for the current variable->value map, add any setter calls that might be useful.
   // the latter can be done by creating a new PicotValueContents with the appropriate
   // setter.  
   //
   // one problem is going to be checking the setter as part of checkValue -- possibly
   // add a separate call that does a checkFieldValue(var,field,rtv) that sees if the
   // value is correct before calling checkValue -- this might fail as the check needs
   // to know the id map.  Alternatively, include var/field/rtv as an initial check
   // for check value -- if the value here fails, don't try other setters, just return
   // null.  This might also just be a set of taboo fields that cause error return rather
   // than trying to find setters.  This allows us to find a set of potential setters.
   
   return rslt;
}


}       // end of class PicotValueContents




/* end of PicotValueContents.java */

