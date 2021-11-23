/********************************************************************************/
/*                                                                              */
/*              PicotValueContext.java                                          */
/*                                                                              */
/*      Context holding set of initializations for a test                       */
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootValidate.RootTrace;
import edu.brown.cs.rose.root.RootValidate.RootTraceCall;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;
import edu.brown.cs.rose.root.RootValidate.RootTraceVariable;

class PicotValueContext implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PicotValueBuilder value_builder;
private Map<String,PicotCodeFragment> value_map;
private PicotValueChecker value_checker;
private PicotCodeFragment init_code;
private Boolean setup_value;
private RootTrace value_result;
private RootTrace target_result;
private long target_time;
private JcompTyper jcomp_typer;


// mapping of source id to target id
private Map<String,String> base_value_map;

// set of all computed objects with a particular type
private Map<JcompType,Set<PicotValueAccessor>> computed_values;

// set of computed variables and their target (original) value
private Map<String,RootTraceValue> variable_values;
private Set<String> known_variables;

private Set<PicotValueProblem> found_problems;


private static final PicotCodeFragment bad_fragment;

static {
   bad_fragment = new PicotCodeFragment("<<BAD>>");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueContext(PicotValueBuilder bldr,PicotValueChecker vc,
      JcompTyper typer,RootTrace trace,long tgttime)
{
   value_checker = vc;
   value_builder = bldr;
   value_map = new HashMap<>();
   computed_values = new HashMap<>();
   base_value_map = new HashMap<>();
   init_code = null;
   setup_value = false;
   value_result = null;
   target_result = trace;
   target_time = tgttime;
   jcomp_typer = typer;
   variable_values = new HashMap<>();
   known_variables = new HashSet<>();
   found_problems = null;
}


PicotValueContext(PicotValueContext vc,PicotCodeFragment addedcode)
{
   value_checker = vc.value_checker;
   value_builder = vc.value_builder;
   value_map = new HashMap<>(vc.value_map);
   computed_values = new HashMap<>(vc.computed_values);
   base_value_map = new HashMap<>(vc.base_value_map);
   jcomp_typer = vc.jcomp_typer;
   variable_values = new LinkedHashMap<>(vc.variable_values);
   target_result = vc.target_result;
   known_variables = new HashSet<>(vc.known_variables);
   
   if (vc.init_code == null) init_code = addedcode;
   else {
      init_code = vc.init_code.append(addedcode,true);
    }
   
   value_result = null;
   setup_value = null;
   found_problems = null;
}



PicotValueContext(PicotValueContext vc,PicotCodeFragment addedcode,
      String var,RootTraceValue val)
{
   this(vc,addedcode);
   
   if (var != null && val != null) {
      variable_values.put(var,val);
    }
   
   setupContents();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment getPreviousValue(String id)
{
   if (id == null) return null;
   
   PicotCodeFragment rslt = value_map.get(id);
   
   if (rslt == bad_fragment) return null;
   if (rslt != null) return rslt;
   
   value_map.put(id,bad_fragment);
   
   return null;
}


boolean hasBeenDone(String id)
{
   if (id == null) return false;
   
   return value_map.get(id) != null;
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

void setVariableKnown(String var)
{
   known_variables.add(var);
}


Set<PicotValueAccessor> getValuesForType(JcompType typ)
{
   return computed_values.get(typ);
}


Collection<PicotValueProblem> getProblems()
{
   return found_problems;
}


PicotCodeFragment getInitializationCode()   
{
   return init_code;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/


RootTrace getTrace()                            
{ 
   if (value_result == null) {
      value_result = value_checker.generateTrace(init_code);
    }
   
   return value_result;
}


long getTargetTime()                            { return target_time; }


private void setupContents()
{
   if (setup_value != null) return;
   
   value_result = value_checker.generateTrace(init_code);
   base_value_map.clear();
   computed_values.clear();
   
   if (value_result == null || value_result.getException() != null) setup_value = false;
   else if (value_result.getReturnValue() == null) setup_value = false;
   else {
      boolean fg = fixupValues();
      setup_value = fg;
    }
}






private boolean fixupValues()
{
   if (value_result == null) return false;
   
   RootTraceCall rtc = value_result.getRootContext();
   long srctime = rtc.getEndTime();
   
   for (Map.Entry<String,RootTraceValue> ent : variable_values.entrySet()) {
      String var = ent.getKey();
      RootTraceValue targetval = ent.getValue();
      boolean force = known_variables.contains(var);
      PicotValueAccessor acc = PicotValueAccessor.createVariableAccessor(var,
            targetval.getDataType());
      RootTraceVariable rtvar = rtc.getTraceVariables().get(var);
      if (rtvar == null) continue;
      RootTraceValue sourceval = rtvar.getValueAtTime(value_result,srctime);
      boolean fg = checkValue(acc,sourceval,targetval,null,srctime,target_time,force);
      if (force && !fg) return false;
    }
   
   return true; 
}



/********************************************************************************/
/*                                                                              */
/*      Compare values with target                                              */
/*                                                                              */
/********************************************************************************/

boolean checkValue(PicotValueAccessor acc,RootTraceValue sourceval,
      RootTraceValue targetval,RootTraceValue baseval,
      long stime,long ttime,boolean force)
{
   if (targetval == null || sourceval == null) return true;
   
   boolean fg = checkValueCompute(acc,sourceval,targetval,stime,ttime,force);
   
   if (!fg && !force) {
      PicotValueProblem prob = new PicotValueProblem(acc,sourceval,targetval,baseval);
      if (found_problems == null) found_problems = new LinkedHashSet<>();
      found_problems.add(prob);
    }
   
   return fg;
}



boolean checkValueCompute(PicotValueAccessor acc,RootTraceValue sourceval,
      RootTraceValue targetval,long stime,long ttime,boolean force)
{
   String srctyp = sourceval.getDataType();
   String tgttyp = targetval.getDataType();
   if (!srctyp.equals(tgttyp)) return false;
   
   switch (srctyp) {
      case "*ANY*" :
         return true;
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
         if (!sourceval.getValue().equals(targetval.getValue())) return false;
         return true;  
      case "java.util.Random" :
         return true;
      default :
         break;
    }
   
   boolean rslt = true;
   String srcid = sourceval.getId();
   String tgtid = targetval.getId();
   if (srcid != null && tgtid != null) {
      String mapid = base_value_map.get(srcid);
      if (mapid != null && !tgtid.equals(mapid)) return false;
      if (mapid == null) base_value_map.put(srcid,tgtid);
    }
   else if (srcid != null || tgtid != null) return false;
   
   JcompType jtyp = jcomp_typer.findSystemType(srctyp);
   if (jtyp == null) return false;
   JcompType coltyp = jcomp_typer.findSystemType("java.util.Collection");
   JcompType maptyp = jcomp_typer.findSystemType("java.util.Map");
   if (jtyp.isCompatibleWith(coltyp)) {
      JcompType arrtyp = jcomp_typer.findSystemType("java.lang.Object[]");
      RootTraceValue srccnts = sourceval.getFieldValue(value_result,"@toArray",stime);
      RootTraceValue tgtcnts = targetval.getFieldValue(target_result,"@toArray",ttime);
      if (srccnts != null && tgtcnts != null) {
         boolean nrslt = compare(acc,arrtyp,srccnts,tgtcnts,targetval,stime,ttime,force);
         if (!nrslt) {
            RoseLog.logD("PICOT","Collection Contents don't match ");
          }
       }
      return rslt;
    }
   else if (jtyp.isCompatibleWith(maptyp)) {
      // handle maps
      return true;
    }
   
   if (jtyp.isArrayType()) {
      int i1 = sourceval.getArrayLength();
      int i2 = targetval.getArrayLength();
      if (i1 != i2) return false;
      for (int i = 0; i < i1; ++i) {
         RootTraceValue srcelt = sourceval.getIndexValue(value_result,i,stime);
         RootTraceValue tgtelt = targetval.getIndexValue(target_result,i,ttime);
         PicotValueAccessor idxval = PicotValueAccessor.createArrayAccessor(acc,
               jtyp.getBaseType(),i);
         rslt &= compare(idxval,jtyp.getBaseType(),srcelt,tgtelt,targetval,stime,ttime,force);
       }
    }
   else {
      Map<String,JcompType> flds = jtyp.getFields(jcomp_typer);
      for (Map.Entry<String,JcompType> ent : flds.entrySet()) {
         String fnm = ent.getKey();
         String ftl = fnm;
         int idx = ftl.lastIndexOf(".");
         if (idx > 0) ftl = ftl.substring(idx+1);
         RootTraceValue srcfld = sourceval.getFieldValue(value_result,fnm,stime);
         RootTraceValue tgtfld = targetval.getFieldValue(target_result,fnm,ttime);
         if (tgtfld == null) continue;
         String tgtfldtypname = tgtfld.getDataType();
         JcompType tgtfldtyp = jcomp_typer.findSystemType(tgtfldtypname);
         PicotValueAccessor fldacc = PicotValueAccessor.createFieldAccessor(acc,ftl,tgtfldtyp);
         rslt &= compare(fldacc,tgtfldtyp,srcfld,tgtfld,targetval,stime,ttime,force);
       }
    }
   
   addComputedValue(jtyp,acc);
   
   return rslt;
}



private void addComputedValue(JcompType jtyp,PicotValueAccessor pcf)
{
   Set<PicotValueAccessor> typl = computed_values.get(jtyp);
   if (typl == null) {
      typl = new HashSet<>();
      computed_values.put(jtyp,typl);
    }
   typl.add(pcf);
}



private boolean compare(PicotValueAccessor acc,JcompType typ,
      RootTraceValue src,RootTraceValue tgt,RootTraceValue base,
      long stime,long ttime,boolean force)
{
   if (src == null && tgt == null) return true;
   
   if (tgt == null) {
      // target not needed
      addComputedValue(typ,acc);
      return true;
    }  
   
   RoseLog.logD("PICOT","COMPAREA " + src + " AND " + tgt);
   
   return checkValue(acc,src,tgt,base,stime,ttime,force);
}



}       // end of class PicotValueContext




/* end of PicotValueContext.java */

