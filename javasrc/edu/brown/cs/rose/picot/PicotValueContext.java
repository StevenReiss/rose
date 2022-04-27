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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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

// current code for trace values
private Map<RootTraceValue,PicotCodeFragment> computed_code;

private Set<PicotValueProblem> found_problems;

private Map<String,LinkedList<PicotAlternative>> alternative_map;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueContext(PicotValueChecker vc,JcompTyper typer,RootTrace trace,long tgttime)
{
   value_checker = vc;
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
   computed_code = new HashMap<>();
   found_problems = null;
   alternative_map = new HashMap<>();
}


PicotValueContext(PicotValueContext vc,PicotCodeFragment addedcode)
{
   value_checker = vc.value_checker;
   computed_values = new HashMap<>(vc.computed_values);
   base_value_map = new HashMap<>(vc.base_value_map);
   jcomp_typer = vc.jcomp_typer;
   variable_values = new LinkedHashMap<>(vc.variable_values);
   target_result = vc.target_result;
   known_variables = new HashSet<>(vc.known_variables);
   alternative_map = new HashMap<>(vc.alternative_map);
   computed_code = new HashMap<>(vc.computed_code);
   
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
   
   setupContents(var);
}

















void noteComputed(RootTraceValue rtv,PicotCodeFragment pcf)
{
   if (pcf == null) computed_code.remove(rtv);
   else computed_code.put(rtv,pcf);
}


PicotCodeFragment getComputedValue(RootTraceValue rtv)
{
   return computed_code.get(rtv);
}


PicotCodeFragment getFinalComputedValue(RootTraceValue rtv)
{
   PicotCodeFragment pcf = computed_code.get(rtv);
   if (pcf == null) return null;
   String code = pcf.getCode();
   if (code.startsWith("v") && !code.contains(".")) {
      if (!known_variables.contains(code)) return null;
    }
   return pcf;
}



/********************************************************************************/
/*                                                                              */
/*      General access methods                                                  */
/*                                                                              */
/********************************************************************************/

boolean isValidSetup()                               
{
   return setup_value;
}

void setVariableKnown(String var)
{
   known_variables.add(var);
}


void setKnown(RootTraceValue rtv)
{
   PicotCodeFragment pcf = computed_code.get(rtv);
   known_variables.add(pcf.getCode());
}


Set<PicotValueAccessor> getValuesForType(JcompType typ)
{
   return computed_values.get(typ);
}


Collection<PicotValueProblem> getProblems()
{
   return found_problems;
}


Collection<PicotValueProblem> getProblems(RootTraceValue rtv)
{
   List<PicotValueProblem> rslt = new ArrayList<>();
   if (found_problems != null) {
      for (PicotValueProblem p : found_problems) {
         RootTraceValue prtv = p.getTargetBaseValue();
         if (prtv == rtv) rslt.add(p);
         else if (prtv == null && p.getTargetValue() == rtv) rslt.add(p);
       }
    }
   
   return rslt;
}


PicotCodeFragment getInitializationCode()   
{
   return init_code;
}


PicotCodeFragment getCode()
{
   return new PicotCodeFragment(value_checker.getCode());
}



String getPackageName()
{
   return value_checker.getPackageName();
}

String getTestClassName()
{
   return value_checker.getTestClassName();
}

String getTestMethodName()
{
   return value_checker.getTestMethodName();
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


private void setupContents(String var)
{
   if (setup_value != null) return;
   
   value_result = value_checker.generateTrace(init_code);
   base_value_map.clear();
   computed_values.clear();
   
   if (value_result == null || value_result.getException() != null) setup_value = false;
   else if (value_result.getReturnValue() == null) setup_value = false;
   else {
      boolean fg = fixupValues(var);
      setup_value = fg;
    }
}






private boolean fixupValues(String var0)
{
   if (value_result == null) return false;
   
   RootTraceCall rtc = value_result.getRootContext();
   long srctime = rtc.getEndTime();
   srctime = 1000000000;
   
   for (Map.Entry<String,RootTraceValue> ent : variable_values.entrySet()) {
      String var = ent.getKey();
//    if (var.equals(var0)) continue;
      RootTraceValue targetval = ent.getValue();
      String tid = targetval.getId();
      if (tid == null) continue;
      RootTraceVariable rtvar = rtc.getTraceVariables().get(var);
      if (rtvar == null) continue;
      RootTraceValue sourceval = rtvar.getValueAtTime(value_result,srctime);
      String sid = sourceval.getId();
      if (sid == null) continue;
      base_value_map.put(sid,tid);
    }
   
   for (Map.Entry<String,RootTraceValue> ent : variable_values.entrySet()) {
      String var = ent.getKey();
      RootTraceValue targetval = ent.getValue();
      boolean force = known_variables.contains(var);
      PicotValueAccessor acc = PicotValueAccessor.createVariableAccessor(var,
            targetval.getDataType());
      RootTraceVariable rtvar = rtc.getTraceVariables().get(var);
      if (rtvar == null) continue;
      RootTraceValue sourceval = rtvar.getValueAtTime(value_result,srctime);
      boolean fg = checkValue(acc,sourceval,targetval,null,srctime,target_time,force,0);
      if (force && !fg) return false;
    }
   
   return true; 
}



/********************************************************************************/
/*                                                                              */
/*      Search alternative management                                           */
/*                                                                              */
/********************************************************************************/

PicotAlternativeType hasAlternatives(RootTraceValue rtv)
{
   return hasAlternatives(rtv.getId());
}


PicotAlternativeType hasAlternatives(String id)
{
   if (id == null) return PicotAlternativeType.FAIL;
   LinkedList<PicotAlternative> alts = alternative_map.get(id);
   if (alts == null) return PicotAlternativeType.NONE;
   if (alts.isEmpty()) return PicotAlternativeType.FAIL;
   // linked list has next to try last
   PicotAlternative pat = alts.getLast();
   return pat.getAlternativeType();
}


void addAlternatives(RootTraceValue rtv,PicotAlternativeType typ,
      List<PicotCodeFragment> codes)
{
   addAlternatives(rtv.getId(),typ,codes);
}


void addAlternatives(String id,PicotAlternativeType typ,List<PicotCodeFragment> codes)
{
   if (id == null) return;
   LinkedList<PicotAlternative> alts = alternative_map.get(id);
   if (alts == null) {
      alts = new LinkedList<>();
      alternative_map.put(id,alts);
    }
   if (codes == null || codes.isEmpty()) return;       
   
   for (PicotCodeFragment c : codes) {
      alts.add(new PicotAlternative(c,this,typ));
    }
}


PicotAlternative getNextAlternative(RootTraceValue rtv)
{
   return getNextAlternative(rtv.getId());
}


PicotAlternative getNextAlternative(String id)
{
   if (id == null) return null;
   LinkedList<PicotAlternative> alts = alternative_map.get(id);
   if (alts == null || alts.size() == 0) return null;
   PicotAlternative alt = alts.removeFirst();
   return alt;
}



/********************************************************************************/
/*                                                                              */
/*      Compare values with target                                              */
/*                                                                              */
/********************************************************************************/

boolean checkValue(PicotValueAccessor acc,RootTraceValue sourceval,
      RootTraceValue targetval,RootTraceValue baseval,
      long stime,long ttime,boolean force,int lvl)
{
   if (targetval == null || sourceval == null) return true;
   
   boolean fg = checkValueCompute(acc,sourceval,targetval,stime,ttime,force,lvl);
   
   if (!fg && !force) {
      PicotValueProblem prob = new PicotValueProblem(acc,sourceval,targetval,baseval);
      if (found_problems == null) found_problems = new LinkedHashSet<>();
      found_problems.add(prob);
    }
   
   return fg;
}



boolean checkValueCompute(PicotValueAccessor acc,RootTraceValue sourceval,
      RootTraceValue targetval,long stime,long ttime,boolean force,int lvl)
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
      else if (lvl > 0) return true;
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
         PicotValueAccessor aacc = PicotValueAccessor.createFieldAccessor(acc,"@toArray",arrtyp);
         boolean nrslt = checkValueCompute(aacc,srccnts,tgtcnts,stime,100000000,force,lvl);
         if (!nrslt) {
            RoseLog.logD("PICOT","Collection Contents don't match ");
            rslt = false;
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
         rslt &= compare(idxval,jtyp.getBaseType(),srcelt,tgtelt,targetval,stime,ttime,force,lvl+1);
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
         rslt &= compare(fldacc,tgtfldtyp,srcfld,tgtfld,targetval,stime,ttime,force,lvl+1);
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
      long stime,long ttime,boolean force,int lvl)
{
   if (src == null && tgt == null) return true;
   
   if (tgt == null) {
      // target not needed
      addComputedValue(typ,acc);
      return true;
    }  
   
   RoseLog.logD("PICOT","COMPAREA " + src + " AND " + tgt);
   
   return checkValue(acc,src,tgt,base,stime,ttime,force,lvl+1);
}



}       // end of class PicotValueContext




/* end of PicotValueContext.java */

