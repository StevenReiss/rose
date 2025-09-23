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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RoseLog;
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
private PicotValueChecker value_checker;
private PicotCodeFragment init_code;
private Boolean setup_value;
private RootTrace value_result;
private RootTrace target_result;
private JcompTyper jcomp_typer;


// mapping of source id to target id
private Map<String,String> base_value_map;

// set of all computed objects with a particular type
private Map<JcompType,Set<PicotCodeFragment>> computed_values;

// set of computed variables and their target (original) value
private Map<String,RootTraceValue> variable_values;


private static final PicotCodeFragment BAD_FRAGMENT;

static {
   BAD_FRAGMENT = new PicotCodeFragment("<<BAD>>");
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueContents(PicotValueChecker vc,JcompTyper typer,RootTrace trace)
{
   value_checker = vc;
   value_map = new HashMap<>();
   computed_values = new HashMap<>();
   base_value_map = new HashMap<>();
   init_code = null;
   setup_value = false;
   value_result = null;
   target_result = trace;
   jcomp_typer = typer;
   variable_values = new HashMap<>();
}



PicotValueContents(PicotValueContents vc,PicotCodeFragment addedcode,
      String var,RootTraceValue val)
{
   value_checker = vc.value_checker;
   value_map = new HashMap<>(vc.value_map);
   computed_values = new HashMap<>(vc.computed_values);
   base_value_map = new HashMap<>(vc.base_value_map);
   jcomp_typer = vc.jcomp_typer;
   variable_values = new LinkedHashMap<>(vc.variable_values);
   target_result = vc.target_result;
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
   
   if (rslt == BAD_FRAGMENT) return null;
   if (rslt != null) return rslt;
   
   value_map.put(id,BAD_FRAGMENT);
   
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
   base_value_map.clear();
   computed_values.clear();
   
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
   if (setup_value == null || !setup_value) return null;
   if (value_result == null) return null;
   
   for (Map.Entry<String,RootTraceValue> ent : variable_values.entrySet()) {
      String var = ent.getKey();
      PicotCodeFragment acc = new PicotCodeFragment(var);
      RootTraceValue targetval = ent.getValue();
      RootTraceCall rtc = value_result.getRootContext();
      RootTraceVariable rtvar = rtc.getTraceVariables().get(var);
      if (rtvar == null) continue;
      RootTraceValue sourceval = rtvar.getValueAtTime(value_result,rtc.getEndTime());
      PicotValueContents npvc = checkValue(acc,sourceval,targetval,rtc.getEndTime());
      if (npvc == null) return null;
    }
   
   return this;
}



/********************************************************************************/
/*                                                                              */
/*      Check desired value (target) with computed value (source)               */
/*                                                                              */
/********************************************************************************/

PicotValueContents checkValue(PicotCodeFragment acc,RootTraceValue sourceval,RootTraceValue targetval,long time)
{
   PicotValueContents rslt = this;
   
   String srctyp = sourceval.getDataType();
   String tgttyp = targetval.getDataType();
   if (!srctyp.equals(tgttyp)) return null;
   
   switch (srctyp) {
      case "*ANY*" :
         return rslt;
      case "int" :
      case "short" :
      case "long" :
      case "byte" :
      case "char" :
      case "float" :
      case "double" :
      case "java.lang.String" :
      case "java.lang.Class" :
      case "java.lang.Integer" :
      case "java.lang.Short":
      case "java.lang.Long" :
      case "java.lang.Byte" :
      case "java.lang.Character" :
      case "java.lang.Float" :
      case "java.lang.Double" :
         if (!sourceval.getValue().equals(targetval.getValue())) return null;
         return rslt;   
      default :
         break;
    }
   
   String srcid = sourceval.getId();
   String tgtid = targetval.getId();
   if (srcid != null && tgtid != null) {
      String mapid = base_value_map.get(srcid);
      if (mapid != null && !tgtid.equals(mapid)) return null;
      if (mapid == null) base_value_map.put(srcid,tgtid);
    }
   else if (srcid != null || tgtid != null) return null;
   
   JcompType jtyp = jcomp_typer.findSystemType(srctyp);
   if (jtyp == null) return null;
   JcompType coltyp = jcomp_typer.findSystemType("java.util.Collection");
   JcompType maptyp = jcomp_typer.findSystemType("java.util.Map");
   if (jtyp.isCompatibleWith(coltyp)) {
      // handle collections
      return rslt;
    }
   else if (jtyp.isCompatibleWith(maptyp)) {
      // handle maps
      return rslt;
    }
   
   long start = target_result.getRootContext().getStartTime();
   
   if (jtyp.isArrayType()) {
      int i1 = sourceval.getArrayLength();
      int i2 = targetval.getArrayLength();
      if (i1 != i2) return null;
      for (int i = 0; i < i1; ++i) {
         RootTraceValue srcelt = sourceval.getIndexValue(value_result,i,time);
         RootTraceValue tgtelt = targetval.getIndexValue(target_result,i,start);
         PicotCodeFragment idxval = acc.append("[" + i + "]",false);
         compare(idxval,jtyp.getBaseType(),srcelt,tgtelt,time);
       }
    }
   else {
      Map<String,JcompType> flds = jtyp.getFields(jcomp_typer);
      for (Map.Entry<String,JcompType> ent : flds.entrySet()) {
         String fnm = ent.getKey();
         String ftl = fnm;
         int idx = ftl.lastIndexOf(".");
         if (idx > 0) ftl = ftl.substring(idx+1);
         PicotCodeFragment fldval = acc.append("." + ftl,false);
         JcompType ftyp = ent.getValue();
         RootTraceValue srcfld = sourceval.getFieldValue(value_result,fnm,time);
         RootTraceValue tgtfld = targetval.getFieldValue(target_result,fnm,start);
         compare(fldval,ftyp,srcfld,tgtfld,time);
       }
    }
  
   addComputedValue(jtyp,acc);
   
   return rslt;
}



private void addComputedValue(JcompType jtyp,PicotCodeFragment pcf)
{
   Set<PicotCodeFragment> typl = computed_values.get(jtyp);
   if (typl == null) {
      typl = new HashSet<>();
      computed_values.put(jtyp,typl);
    }
   typl.add(pcf);
}



private PicotValueContents compare(PicotCodeFragment acc,JcompType typ,
      RootTraceValue src,RootTraceValue tgt,long time)
{
   if (src == null && tgt == null) return this;
      
   if (tgt == null) {
      // target not needed
      addComputedValue(typ,acc);
      return this;
    }  
   
   RoseLog.logD("PICOT","COMPARE " + src + " AND " + tgt);
   
   PicotValueContents pvc = checkValue(acc,src,tgt,time);
   if (pvc == null) return null;
   
   return pvc;
}


}       // end of class PicotValueContents




/* end of PicotValueContents.java */

