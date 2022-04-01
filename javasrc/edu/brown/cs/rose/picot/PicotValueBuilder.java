/********************************************************************************/
/*                                                                              */
/*              PicotValueBuilder.java                                          */
/*                                                                              */
/*      Create a value based on SEEDE value                                     */
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootValidate.RootTrace;
import edu.brown.cs.rose.root.RootValidate.RootTraceValue;
import edu.brown.cs.rose.root.RootValidate.RootTraceVariable;

class PicotValueBuilder implements PicotConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum FixupState { NO_PROBLEM, FOUND_FIXES, POTENTIAL_FIXES, NO_FIXES };

private RootTrace               for_trace;
private long                    start_time;
private JcompTyper              jcomp_typer;
private JcompType               collection_type;
private JcompType               map_type;
private PicotValueChecker       value_checker;
private PicotValueContext       cur_context;
private Map<JcompType,PicotClassData> class_data;

private List<RootTraceValue>   work_queue;
private Set<RootTraceValue>    done_values;

private static final double SCORE_PUBLIC = 4;
private static final double SCORE_PACKAGE = 2;
private static final double SCORE_CONSTRUCTOR = 6;
private static final double SCORE_FACTORY = 8;
private static final double SCORE_FIELD = 10;
private static final double SCORE_ANY_FIELD = 5;
private static final double SCORE_DEFAULT = 0;
private static final double SCORE_VALUE = 1;
private static final double SCORE_ANY_VALUE = 2;


private static final AtomicInteger variable_counter = new AtomicInteger(0);
private static final AtomicInteger string_counter = new AtomicInteger(0);



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotValueBuilder(RootControl ctrl,RootValidate rv,long start,JcompTyper typer)
{
   for_trace = rv.getExecutionTrace();
   start_time = start;
   jcomp_typer = typer;
   class_data = new HashMap<>();
   value_checker = new PicotValueChecker(ctrl,rv);
   cur_context = new PicotValueContext(value_checker,jcomp_typer,for_trace,start_time);
   collection_type = typer.findSystemType("java.util.Collection");
   map_type = typer.findSystemType("java.util.Map");
   work_queue = new ArrayList<>();
   done_values = new HashSet<>();
}   



void finished()
{ 
   value_checker.finished();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

JcompTyper getJcompTyper()                      { return jcomp_typer; }




PicotValueContext getInitializationContext()
{
   if (!work_queue.isEmpty()) {
      boolean fg = setupInitializations();
      if (!fg) return null;
      work_queue.clear();
    }
   
   return cur_context;
}




/********************************************************************************/
/*                                                                              */
/*      Add items to evaluate                                                   */
/*                                                                              */
/********************************************************************************/

void computeValue(RootTraceVariable rtv)
{
   if (rtv == null) return;
   RootTraceValue rval = rtv.getValueAtTime(for_trace,start_time);
   computeValue(rval);
}



void computeValue(RootTraceValue rtv) 
{
   if (rtv == null || done_values.contains(rtv)) return;
   done_values.add(rtv);
   
   PicotCodeFragment pcf = buildSimpleValue(rtv);
   if (pcf == null) {
      queueValues(rtv);
    }
}


private void queueValues(RootTraceValue rtv)
{
   String typ = rtv.getDataType();
   JcompType jtyp = jcomp_typer.findType(typ);
   
   if (jtyp.isCompatibleWith(collection_type)) {
      RootTraceValue ftv = rtv.getFieldValue(for_trace,"@toArray",start_time);
      if (ftv != null) {
         int ct = ftv.getArrayLength();
         for (int i = 0; i < ct; ++i) {
            RootTraceValue etv = ftv.getIndexValue(for_trace,i,1000000000);
            computeValue(etv);
          } 
       }
    }
   else if (jtyp.isCompatibleWith(map_type)) {
      RootTraceValue ftv = rtv.getFieldValue(for_trace,"@toArray",100000000);
      if (ftv != null) {
         int ct = ftv.getArrayLength();
         for (int i = 0; i < ct; ++i) {
            RootTraceValue etv = ftv.getIndexValue(for_trace,i,1000000000);
            computeValue(etv);
          } 
       }
    }
   else if (jtyp.isArrayType()) {
      int ct = rtv.getArrayLength();
      for (int i = 0; i < ct; ++i) {
         RootTraceValue ftv = rtv.getIndexValue(for_trace,i,start_time);
         computeValue(ftv);
       }
    }
   else {
      Map<String,JcompType> flds = jtyp.getFields(jcomp_typer);
      for (String fld : flds.keySet()) {
         String fnm = fld;
         int idx = fnm.lastIndexOf(".");
         if (idx > 0) fnm = fld.substring(idx+1);
         if (fnm.equals("this") || fnm.startsWith("this$")) continue;
         
         JcompSymbol fldsym = jtyp.lookupField(jcomp_typer,fnm);
         if (fldsym == null) {
            RoseLog.logE("PICOT","Can't find field " + fld);
            continue;
          }
         
         RootTraceValue ftv = rtv.getFieldValue(for_trace,fld,start_time);
         computeValue(ftv);
       }
    }
   
   work_queue.add(rtv);
}



/********************************************************************************/
/*                                                                              */
/*      Process work queue                                                      */
/*                                                                              */
/********************************************************************************/

private boolean setupInitializations()
{
   Stack<PicotValueContext> done = new Stack<>();
   
   for ( ; ; ) {
      boolean fg = setupCreationInitializations(done);
      if (fg) {
         fg = setupFixupInitializations();
       }
      if (fg) break;
      if (done.isEmpty()) return false;
      cur_context = done.pop();
    }
   
   return true;
}



private boolean setupCreationInitializations(Stack<PicotValueContext> done)
{
   boolean retry = true;
   boolean updated = true;
   
   while (retry) {
      if (!updated) return false; 
      updated = false;
      retry = false;
      for (RootTraceValue rtv : work_queue) {
         String rtypnm = rtv.getDataType();
         JcompType jtyp = jcomp_typer.findType(rtypnm);
         PicotCodeFragment var = cur_context.getComputedValue(rtv);
         PicotAlternativeType typ = cur_context.hasAlternatives(rtv);
         if (var == null) {
            if (typ == PicotAlternativeType.NONE) {
               setupCreateAlternatives(rtv);
               typ = cur_context.hasAlternatives(rtv);
             }
            if (typ == PicotAlternativeType.FAIL) retry = true;
            boolean createok = true;
            for ( ; ; ) {
               PicotAlternative pat = cur_context.getNextAlternative(rtv);
               if (pat == null) break;
               PicotValueContext startctx = pat.getStartContext();
               String variable = "v" + variable_counter.incrementAndGet(); 
               String decl = jtyp.getName() + " " + variable + " = " +
                     pat.getCodeFragment().getCode() + ";\n";
               PicotCodeFragment npcf = new PicotCodeFragment(decl);
               RoseLog.logD("PICOT","TRY: " + npcf.getCode());
               PicotValueContext npvc = new PicotValueContext(startctx,npcf,variable,rtv);
               if (npvc.isValidSetup()) {
                  done.push(cur_context);
                  npvc.noteComputed(rtv,new PicotCodeFragment(variable));
                  for ( ; ; ) {
                     List<PicotCodeFragment> fixes = new ArrayList<>();
                     FixupState state = getFixupAlternatives(npvc,rtv,fixes);
                     if (state == FixupState.NO_PROBLEM) {
                        npvc.setKnown(rtv);
                        break;
                      }
                     else if (state == FixupState.NO_FIXES) {
                        createok = false;
                        break;
                      }
                     if (fixes == null || fixes.isEmpty()) break;
                     boolean chng = false;
                     for (PicotCodeFragment pcf : fixes) {
                        PicotValueContext nnpvc = new PicotValueContext(npvc,pcf,null,null);
                        if (nnpvc.isValidSetup()) {
                           chng = true;
                           npvc = nnpvc;
                           break;    
                         }
                      }
                     if (!chng) break;
                   }
                  if (createok) {
                     cur_context = npvc;
                     updated = true;
                   }
                  break;
                }
             }
          }
       }
    }
   
   return true;
}


private boolean setupFixupInitializations()
{
   boolean retry = true;
   boolean updated = true;
   
   while (retry) {
      retry = false;
      updated = false;
      for (RootTraceValue rtv : work_queue) {
         PicotValueContext npvc = cur_context;
         for ( ; ; ) {
            List<PicotCodeFragment> fixes = new ArrayList<>();
            FixupState state = getFixupAlternatives(npvc,rtv,fixes);
            if (state == FixupState.NO_PROBLEM) {
               npvc.setKnown(rtv);
               break;
             }
            retry = true;
            boolean chng = false;
            for (PicotCodeFragment pcf : fixes) {
               PicotValueContext nnpvc = new PicotValueContext(npvc,pcf,null,null);
               if (nnpvc.isValidSetup()) {
                  chng = true;
                  npvc = nnpvc;
                  break;    
                }
             }
            if (!chng) break;
          }
         if (npvc != cur_context) {
            updated = true;
            cur_context = npvc;
          }
       }
      if (!updated) {
        if (retry) return false;
        else break;
       }
    }
   
   return true; 
}

private void setupCreateAlternatives(RootTraceValue rtv)
{
   String typnm = rtv.getDataType();
   JcompType typ = jcomp_typer.findType(typnm);
   
   Map<String,JcompType> flds = typ.getFields(jcomp_typer);
   PicotClassData pcd = getClassData(typ);
   PicotFieldMap fldval = new PicotFieldMap();
   Collection<JcompSymbol> mthds = pcd.getMethods(); 
   
   // first determine if there is an implied default constructor
   boolean havecnst = false;
   for (JcompSymbol js : mthds) {
      if (js.isConstructorSymbol()) {
         havecnst = true;
       }
    }
   
   // next get the target value for all fields
   for (String fld : flds.keySet()) {
      String fnm = fld;
      int idx = fnm.lastIndexOf(".");
      if (idx > 0) fnm = fld.substring(idx+1);
      if (fnm.equals("this") || fnm.startsWith("this$")) continue;
      JcompSymbol fldsym = typ.lookupField(jcomp_typer,fnm);
      if (fldsym == null) continue;
      RootTraceValue ftv = rtv.getFieldValue(for_trace,fld,start_time);
      if (ftv == null) continue;                // not needed
      PicotCodeFragment rslt = cur_context.getComputedValue(ftv);
      if (rslt != null) {
         fldval.put(fldsym,rslt);
       }
    }
   
   // build maps and collections using default constructor and added code
   if (typ.isCompatibleWith(collection_type) || typ.isCompatibleWith(map_type)) {
      havecnst = false;
      mthds = new ArrayList<>();
    }
   
   List<PicotCodeFragment> rslts = new ArrayList<>();
   if (!havecnst && !typ.isArrayType()) {
      // use default constructor
      String ccode = "new " + typ.getName() + "()";
      rslts.add(new PicotCodeFragment(ccode));
    }
   else if (typ.isArrayType()) {
      // use default array constructor
      String ccode = "new " + typ.getBaseType().getName() + "[" + rtv.getArrayLength() + "]";
      rslts.add(new PicotCodeFragment(ccode));  
    }
   
   // check if we have known values of the target type that we might want to use
   Set<PicotValueAccessor> known = cur_context.getValuesForType(typ);
   if (known != null) {
      for (PicotValueAccessor pva : known) {
         if (pva.getAccessorType() == PicotAccessorType.VARIABLE) continue;
         PicotCodeFragment apcf = pva.getGetterCode(this,null);
         if (apcf != null) {
            rslts.add(apcf);
          }      
       }
    }
   
   for (JcompSymbol js : mthds) {
      if (js.isPrivate()) continue;
      PicotMethodData pmd = null;
      if (js.isConstructorSymbol()) {
         if (typ.needsOuterClass()) continue;
         pmd = pcd.getDataForMethod(js);
       }
      else if (js.isStatic() && js.getType().getBaseType() == typ) {
         pmd = pcd.getDataForMethod(js);
       }
      if (pmd != null) {
         // add possible constructors to rslts
         buildCodeForMethod(js,pmd,null,fldval,rslts);
       }
    }
   
   Collections.sort(rslts);
   
   cur_context.addAlternatives(rtv,PicotAlternativeType.CREATE,rslts);
}



private FixupState getFixupAlternatives(PicotValueContext ctx,RootTraceValue rtv,
        List<PicotCodeFragment> rslt)
{
   Collection<PicotValueProblem> probs = ctx.getProblems(rtv);
   
   if (probs == null || probs.isEmpty()) return FixupState.NO_PROBLEM;
   
   rslt.clear();
   
   for (PicotValueProblem prob : probs) {
      List<PicotCodeFragment> fixes = new ArrayList<>();
      FixupState state = computeFixes(ctx,prob,fixes);
      if (state == FixupState.NO_FIXES) return FixupState.NO_FIXES;
      if (state == FixupState.FOUND_FIXES) rslt.addAll(fixes);
    } 
   
   if (rslt.isEmpty()) return FixupState.POTENTIAL_FIXES;
   
   return FixupState.FOUND_FIXES;
}



/********************************************************************************/
/*                                                                              */
/*      Build code to create value                                              */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment buildSimpleValue(RootTraceVariable rtv)
{
   RootTraceValue rval = rtv.getValueAtTime(for_trace,start_time);
   return buildSimpleValue(rval);
}


private PicotCodeFragment buildSimpleValue(RootTraceValue rtv)
{
   if (rtv.isNull()) return new PicotCodeFragment("null");
   
   PicotCodeFragment rslt = null;
   rslt = cur_context.getComputedValue(rtv);
   if (rslt != null) return rslt;
   
   String typ = rtv.getDataType();
   JcompType jtyp = jcomp_typer.findType(typ);
   if (jtyp == null) return null;
   if (jtyp.isPrimitiveType()) {
      rslt = buildPrimitiveValue(jtyp,rtv.getValue());
    }
   else if (jtyp.isStringType()) {
      rslt = buildStringValue(rtv.getValue());
    }
   else if (jtyp.isEnumType()) {
      String efld = rtv.getEnum();
      if (efld != null) {
         rslt = new PicotCodeFragment(jtyp.getName() + "." + efld);
       }
    }
   else if (jtyp.isArrayType()) {
      rslt = buildSimpleArrayValue(rtv,jtyp);
    }
   else if (jtyp.isBinaryType()) {
      rslt = buildSimpleSystemObjectValue(rtv,jtyp);
    }
   else if (jtyp.getName().equals("java.lang.Class")) {
      rslt = buildClassValue(rtv.getValue());
    }
   else if (jtyp.getName().equals("java.io.File")) {
      rslt = buildFileValue(rtv.getValue());
    }
   
   cur_context.noteComputed(rtv,rslt);
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Primitive types                                                         */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment buildPrimitiveValue(JcompType typ,String val)
{
   String rslt = null;
   
   switch (typ.getName()) {
      case "int" :
         rslt = val;
         break;
      case "short" :
         rslt = "((short) " + val + ")";
         break;
      case "byte" :
         rslt = "((byte) " + val + ")";
         break;
      case "long" :
         if (val.endsWith("L") || val.endsWith("l")) rslt = val;
         else rslt = val + "L";
         break;
      case "char" :
         char cv = (char) Integer.parseInt(val);
         String sv = String.valueOf(cv);
         rslt = "'" + IvyFormat.formatChar(sv) + "'";
         break;
      case "float" :
         if (val.endsWith("F") || val.endsWith("F")) rslt = val;
         else rslt = val + "F";
         break;
      case "double" :
         if (val.contains(".") || val.contains("E") || val.contains("e")) 
            rslt = val;
         else rslt = val + ".0";
         break;
      case "boolean" :
         if (val.equals("0") || val.equalsIgnoreCase("false")) rslt = "false";
         else rslt = "true";
         break;
    }
   
   if (rslt == null) return null;
   
   return new PicotCodeFragment(rslt);
}


private PicotCodeFragment buildStringValue(String val)
{
   String rslt = "\"" + IvyFormat.formatString(val) + "\"";
   
   return new PicotCodeFragment(rslt);
}


private PicotCodeFragment buildClassValue(String val)
{
   String rslt = val + ".class";
   
   return new PicotCodeFragment(rslt);
}


private PicotCodeFragment buildFileValue(String val)
{
   String rslt = "new java.io.File(" + val + ")";
   
   return new PicotCodeFragment(rslt);
}



/********************************************************************************/
/*                                                                              */
/*      Handle arrays                                                           */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment buildSimpleArrayValue(RootTraceValue rtv,JcompType typ)
{
   int sz = rtv.getArrayLength();
   StringBuffer buf = new StringBuffer();
   buf.append("new " + typ.getBaseType() + "[" + sz + "]");
   if (sz > 0) {
      buf.append(" { ");
      for (int i = 0; i < sz; ++i) {
         RootTraceValue etv = rtv.getIndexValue(for_trace,i,start_time);
         PicotCodeFragment efg = buildSimpleValue(etv);
         if (efg == null) return null;
         if (i > 0) buf.append(" , ");
         buf.append(efg.getCode());
       }
      buf.append(" } ");
    }
   
   return new PicotCodeFragment(buf.toString());
}


/********************************************************************************/
/*                                                                              */
/*      Handle objects                                                          */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment buildSimpleSystemObjectValue(RootTraceValue rtv,JcompType typ)
{
   switch (typ.getName()) {
      case "java.lang.Integer" :
      case "java.lang.Long" :
      case "java.lang.Short" :
      case "java.lang.Byte" :
      case "java.lang.Float" :
      case "java.lang.Double" :
      case "java.lang.Character" :
         String fnm = typ.getName() + ".value";
         RootTraceValue rtv1 = rtv.getFieldValue(for_trace,fnm,start_time);
         String val = "0";
         if (rtv1 != null) val = rtv1.getValue();
         String vcode = typ.getName() + ".valueOf(" + val + ")";
         return new PicotCodeFragment(vcode);
      default :
         break;
    }
   
   return null;
}



private void buildCodeForMethod(JcompSymbol js,PicotMethodData pmd,
      PicotCodeFragment thiscode,PicotFieldMap fldvals,
      List<PicotCodeFragment> rslts)
{
   String call = null;
   Set<String> used = new HashSet<>();
   
   double score = 0;
   if (js.isPublic()) score += SCORE_PUBLIC;
   else if (!js.isProtected()) score += SCORE_PACKAGE;
   
   if (js.isConstructorSymbol()) {
      score += SCORE_CONSTRUCTOR;
      call = "new " + js.getClassType().getName();
    }
   else if (js.isStatic()) {
      score += SCORE_FACTORY;
      call = js.getClassType().getName() + "." + js.getName();
    }
   else if (thiscode == null) return;
   else {
      call = thiscode + "." + js.getName();
    }
   call += "(";
   
   int ct = 0;
   addParameter(call,pmd,ct,fldvals,score,rslts,used);
}


private void addParameter(String pfx,PicotMethodData pmd,int pno,
      PicotFieldMap fldvals,double score,List<PicotCodeFragment> rslts,
      Set<String> used)
{
   List<JcompType> ptyps = pmd.getParameterTypes();
   
   if (pno >= ptyps.size()) {
      rslts.add(new PicotCodeFragment(pfx + ")",score));
      return;
    }
   
   if (pno > 0) pfx += ",";
   JcompType ptyp = ptyps.get(pno);
   List<ParameterData> pvals = getParameterValues(pno,ptyp,pmd,fldvals);
   for (ParameterData pdata : pvals) {
      String call = pfx + pdata.getCode();
      String key = pdata.getKey();
      if (key != null) {
         if (used.contains(key)) continue;
         used.add(key);
       }
      addParameter(call,pmd,pno+1,fldvals,score+pdata.getScore(),rslts,used);
      if (key != null) used.remove(key);
    }
}



private List<ParameterData> getParameterValues(int pno,JcompType ptyp,
      PicotMethodData pmd,
      PicotFieldMap fldvals)
{
   List<ParameterData> rslt = new ArrayList<>();
   Set<PicotCodeFragment> used = new HashSet<>();
   
   // first see if there is a relevant field assignment
   for (PicotMethodEffect meff : pmd.getEffects()) {
      switch (meff.getEffectType()) {
         case SET_FIELD :
            PicotEffectItem srcitm = meff.getEffectSource();
            if (srcitm.getParameterNumber() == pno) {
               PicotEffectItem flditm = meff.getEffectTarget();
               JcompSymbol fldsym = flditm.getSymbolValue();
               PicotCodeFragment fldpcf = fldvals.get(fldsym);
               if (fldpcf != null) {
                  rslt.add(new ParameterData(fldpcf.getCode(),fldsym.getName(),SCORE_FIELD));
                  used.add(fldpcf);
                }
             }
            break;
         default :
            break;
       }
    }
   
   // next use any values of the same type from fields
   for (Map.Entry<JcompSymbol,PicotCodeFragment> ent : fldvals.entrySet()) {
      JcompSymbol fldsym = ent.getKey();
      JcompType fldtyp = fldsym.getType();
      if (fldtyp == ptyp) {
         PicotCodeFragment fldpcf = fldvals.get(fldsym);
         if (fldpcf != null && ! used.contains(fldpcf)) {
            rslt.add(new ParameterData(fldpcf.getCode(),fldsym.getName(),SCORE_ANY_FIELD));
            used.add(fldpcf);
          }
       }
    }
   
   // nest use default value based on parameter type
   if (ptyp.isNumericType()) {
      rslt.add(new ParameterData("0",null,SCORE_DEFAULT));
    }
   else if (ptyp.isBooleanType()) {
      rslt.add(new ParameterData("false",null,SCORE_DEFAULT));
      rslt.add(new ParameterData("true",null,SCORE_DEFAULT));
    }
   else if (ptyp.isStringType()) {
      String sval = "\"S_" + string_counter.incrementAndGet() + "\"";
      rslt.add(new ParameterData(sval,null,SCORE_VALUE));
      rslt.add(new ParameterData("null",null,SCORE_DEFAULT)); 
    }
   else {
      Set<PicotValueAccessor> accs = cur_context.getValuesForType(ptyp);
      if (accs != null) {
         for (PicotValueAccessor acc : accs) {
            PicotCodeFragment pcf = acc.getGetterCode(this,null);
            if (pcf != null && !used.contains(pcf)) {
               rslt.add(new ParameterData(pcf.getCode(),null,SCORE_ANY_VALUE));
             }
          }
       }
      rslt.add(new ParameterData("null",null,SCORE_DEFAULT)); 
    }
   
   return rslt;
}




private static class ParameterData {

   private String code_fragment;
   private String key_string;
   private double code_priority;
   
   ParameterData(String frag,String key,double p) {
      code_fragment = frag;
      key_string = key;
      code_priority = p;
    }
   
   String getCode()                     { return code_fragment; }
   String getKey()                      { return key_string; }
   double getScore()                    { return code_priority; }
      
}       // end of ParameterData




/********************************************************************************/
/*                                                                              */
/*      Create an accessor for a field                                          */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment buildFieldGetter(PicotCodeFragment lhs,JcompSymbol fldsym,JcompType ltyp)
{
   if (fldsym == null) return null;
   if (!fldsym.isPrivate() && !fldsym.isProtected()) {
      PicotCodeFragment fldacc = lhs.append(".",fldsym.getName());
      return fldacc;
    }
   
   PicotClassData pcd = getClassData(ltyp);
   Collection<JcompSymbol> mthds = pcd.getMethods();
   for (JcompSymbol js : mthds) {
      if (js.isStatic() || js.isConstructorSymbol() || js.isPrivate() || js.isProtected())
         continue;
      PicotMethodData pmd = pcd.getDataForMethod(js);
      if (pmd != null) {
         for (PicotMethodEffect pme : pmd.getEffects()) {
            if (pme.getEffectType() == PicotEffectType.RETURN) {
               if (pme.getEffectTarget().getSymbolValue() == fldsym) {
                  JcompType ftyp = js.getType();
                  if (ftyp.getComponents().isEmpty()) {
                     PicotCodeFragment fldacc = lhs.append(".",js.getName(),"()");
                     return fldacc;
                   }
                }
             }
          }
       }
      else {
         if (js.getName().equalsIgnoreCase("get"+ fldsym.getName()) &&
                js.getType().getComponents().isEmpty()) {
            PicotCodeFragment fldacc = lhs.append(".",js.getName(),"()");
            return fldacc;
          }
       }
    }
   
   return null;
}



List<PicotCodeFragment> buildFieldSetter(PicotCodeFragment lhs,JcompSymbol fldsym,JcompType ltyp,
      PicotCodeFragment rhs,RootTraceValue base)
{
   List<PicotCodeFragment> rslt = new ArrayList<>();
   
   if (lhs == null || rhs == null) return rslt;
   
   if (!fldsym.isPrivate() && !fldsym.isProtected()) {
      PicotCodeFragment fldacc = lhs.append(".",fldsym.getName()," = ",rhs.getCode(),";\n");
      rslt.add(fldacc);
      return rslt;
    }
   
   PicotClassData pcd = getClassData(ltyp);
   Collection<JcompSymbol> mthds = pcd.getMethods();
// JcompType ftyp = fldsym.getType();
   
   
   // pass 1
   for (JcompSymbol js : mthds) {
      if (js.isStatic() || js.isConstructorSymbol() || js.isPrivate() || js.isProtected())
         continue;
      PicotMethodData pmd = pcd.getDataForMethod(js);
      if (pmd != null) {
         if (js.getType().getComponents().size() == 1) {
            for (PicotMethodEffect pme : pmd.getEffects()) {
               if (pme.getEffectType() == PicotEffectType.SET_FIELD) {
                  if (pme.getEffectTarget().getSymbolValue() == fldsym) {
                     if (pme.getEffectSource().getItemType() == PicotItemType.PARAMETER) {
                        String code = lhs.getCode() + "." + js.getName() + "(" + rhs.getCode() + ")";
                        PicotCodeFragment fldacc = new PicotCodeFragment(code + ";\n");
                        rslt.add(fldacc);
                      }
                   }
                }
             }
          }
         // handle more complex methods
       }
      else {
         if (js.getName().equalsIgnoreCase("set"+ fldsym.getName()) &&
               js.getType().getComponents().size() == 1) {
            PicotCodeFragment fldacc = lhs.append(".",js.getName(),"(",rhs.getCode(),");\n");
            rslt.add(fldacc);
          }
       }
    }
   
   if (rslt.isEmpty() && base != null) {
      JcompType btyp = jcomp_typer.findSystemType(base.getDataType());
      Map<String,JcompType> fldtyp = btyp.getFields(jcomp_typer);
      for (JcompSymbol js : mthds) {
         if (js.isStatic() || js.isConstructorSymbol() || js.isPrivate() || js.isProtected())
            continue;
         PicotMethodData pmd = pcd.getDataForMethod(js);
         if (pmd != null) {
            // handle methods that set field and something else
          }
         else {
            if (js.getName().startsWith("set")) {
               JcompType mtyp = js.getType();
               List<JcompType> atyps = mtyp.getComponents();
               if (atyps != null && fldtyp != null) {
                  // insure that ftyp is in atyps
                  // if atyps has only one element, ensure name after set doesnt correspond to a field
                  // build all possible calls with types
                }
             }
          }
       }
    }
   
   return rslt; 
}



JcompType getTargetType(RootTraceValue base,String field)
{
   if (base == null) return null;
   RootTraceValue fldval = base.getFieldValue(for_trace,field,cur_context.getTargetTime());
   if (fldval == null) return null;
   String typnam = fldval.getDataType();
   if (typnam == null) return null;
   JcompType jt = jcomp_typer.findSystemType(typnam);
   
   return jt;
}


/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/
   
private PicotClassData getClassData(JcompType typ)
{
   PicotClassData pcd = class_data.get(typ);
   if (pcd == null) {
      pcd = new PicotClassData(typ,jcomp_typer);
      PicotClassData opcd = class_data.putIfAbsent(typ,pcd);
      if (opcd != null) pcd = opcd;
    }
   
   return pcd;
}
   


/********************************************************************************/
/*                                                                              */
/*      Fix up fields                                                           */
/*                                                                              */
/********************************************************************************/

private FixupState computeFixes(PicotValueContext ctx,PicotValueProblem p,
        List<PicotCodeFragment> rslts)
{
   PicotValueAccessor pva = p.getAccessor();
   RootTraceValue tgt = p.getTargetValue();
   RootTraceValue base = p.getTargetBaseValue();
   JcompType tgttyp = jcomp_typer.findSystemType(tgt.getDataType());
   rslts.clear();
   
   if (base == null) {
      if (tgt.isNull()) return FixupState.POTENTIAL_FIXES;
      if (tgttyp.isCompatibleWith(collection_type)) {
         PicotCodeFragment pvf = computeCollectionFix(ctx,tgt,tgttyp,pva);
         if (pvf == null) return FixupState.POTENTIAL_FIXES;
         rslts.add(pvf);
       }
      else if (tgttyp.isCompatibleWith(map_type)) {
         PicotCodeFragment pvf = computeMapFix(ctx,tgt,tgttyp,pva);
         if (pvf == null) return FixupState.POTENTIAL_FIXES;
         rslts.add(pvf);
       }
    }
   else {
      PicotCodeFragment rslt = buildSimpleValue(tgt);
      if (rslt == null) return FixupState.POTENTIAL_FIXES;
      
      rslts = pva.getSetterCodes(this,rslt,base);
      
      if (!tgt.isNull() && tgttyp.isCompatibleWith(collection_type)) {
         PicotCodeFragment pcf = pva.getGetterCode(this,tgttyp);
         rslt = ctx.getFinalComputedValue(tgt);
         if (rslt == null) return FixupState.POTENTIAL_FIXES;
         if (pcf != null) {
            int ctr = variable_counter.incrementAndGet();
            String var = "cv" + ctr;
            String var1 = "cvo" + ctr;
            StringBuffer buf = new StringBuffer();
            buf.append(tgttyp + " " + var + " = " + pcf.getCode() + ";\n");
            buf.append(var + ".clear();\n");
            buf.append("for (Object " + var1 + "  : " + rslt + ") {\n");
            buf.append(var + ".add(" + var1 + ");\n");
            buf.append("}\n");
            rslts.add(new PicotCodeFragment(buf.toString()));
          }
       }
      else if (!tgt.isNull() && tgttyp.isCompatibleWith(map_type)) {
         PicotCodeFragment pcf = pva.getGetterCode(this,tgttyp);
         if (pcf != null) {
            int ctr = variable_counter.incrementAndGet();
            String var = "cv" + ctr;
            StringBuffer buf = new StringBuffer();
            buf.append(tgttyp + " " + var + " = " + pcf.getCode() + ";\n");
            buf.append(var + ".clear();\n");
            buf.append(var + ".putAll(" + rslt + ");\n");
            rslts.add(new PicotCodeFragment(buf.toString()));
          }
       }
    }
    
   if (rslts.isEmpty()) return FixupState.NO_FIXES;
   
   return FixupState.FOUND_FIXES;
}



private PicotCodeFragment computeCollectionFix(PicotValueContext ctx,RootTraceValue tgt,
      JcompType tgttyp,PicotValueAccessor pva)
{
   List<PicotCodeFragment> elts = new ArrayList<>();
   RootTraceValue arr = tgt.getFieldValue(for_trace,"@toArray",100000000);
   for (int i = 0; i < arr.getArrayLength(); ++i) {
      RootTraceValue eltv = arr.getIndexValue(for_trace,i,100000000);
      PicotCodeFragment pcf = ctx.getComputedValue(eltv);
      if (pcf == null) return null;
      elts.add(pcf);
    }
   PicotCodeFragment v0 = pva.getGetterCode(this,tgttyp);
   String var = v0.getCode();
   StringBuffer buf = new StringBuffer();
   buf.append(var + ".clear();\n");
   for (PicotCodeFragment eltf : elts) {
      buf.append(var + ".add(" + eltf + ");\n");
    }
   
   return new PicotCodeFragment(buf.toString());
}


private PicotCodeFragment computeMapFix(PicotValueContext ctx,RootTraceValue tgt,
      JcompType tgttyp,PicotValueAccessor pva)
{
   List<PicotCodeFragment> elts = new ArrayList<>();
   RootTraceValue arr = tgt.getFieldValue(for_trace,"@toArray",100000000);
   for (int i = 0; i < arr.getArrayLength(); ++i) {
      RootTraceValue rtv2 = arr.getIndexValue(for_trace,i,100000000);
      RootTraceValue rtvkey = rtv2.getIndexValue(for_trace,0,100000000);
      PicotCodeFragment cf3 = buildSimpleValue(rtvkey);
      if (cf3 == null) return null;
      RootTraceValue rtvval = rtv2.getIndexValue(for_trace,1,100000000);
      PicotCodeFragment cf4 = buildSimpleValue(rtvval);
      if (cf4 == null) return null;
      elts.add(cf3);
      elts.add(cf4);
    }
   
   PicotCodeFragment v0 = pva.getGetterCode(this,tgttyp);
   String var = v0.getCode();
   StringBuffer buf = new StringBuffer();
   buf.append(var + ".clear();\n");
   for (int i = 0; i < elts.size(); i += 2) {
      buf.append(var + ".put(" + elts.get(i).getCode() + "," +
         elts.get(i+1).getCode() + ");\n");
    }
   
   return new PicotCodeFragment(buf.toString());
}
   

}       // end of class PicotValueBuilder




/* end of PicotValueBuilder.java */

