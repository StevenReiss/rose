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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

private RootTrace               for_trace;
private long                    start_time;
private JcompTyper              jcomp_typer;
private JcompType               collection_type;
private JcompType               map_type;
private PicotValueChecker       value_checker;
private PicotValueContext       cur_context;
private Map<JcompType,PicotClassData> class_data;
private Map<RootTraceValue,PicotCodeFragment> computed_code;


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
   computed_code = new HashMap<>();
   cur_context = new PicotValueContext(this,value_checker,jcomp_typer,for_trace,start_time);
   collection_type = typer.findSystemType("java.util.Collection");
   map_type = typer.findSystemType("java.util.Map");
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
   return cur_context;
}



/********************************************************************************/
/*                                                                              */
/*      Build code to create value                                              */
/*                                                                              */
/********************************************************************************/

PicotCodeFragment buildValue(RootTraceVariable rtv)
{
   RootTraceValue rval = rtv.getValueAtTime(for_trace,start_time);
   return buildValue(rval);
}


PicotCodeFragment buildValue(RootTraceValue rtv)
{
   PicotCodeFragment pcf = computed_code.get(rtv);
   if (pcf != null) return pcf;
   pcf = buildNewValue(rtv);
   if (pcf != null) computed_code.put(rtv,pcf);
   return pcf;
}


PicotCodeFragment buildNewValue(RootTraceValue rtv)
{
   if (rtv.isNull()) return new PicotCodeFragment("null");
   
   PicotCodeFragment rslt = null;
   
   String id = rtv.getId();
   if (cur_context.hasBeenDone(id)) {
      rslt = cur_context.getPreviousValue(id);
      return rslt;
    }
   else {
      // mark as under computation
      cur_context.getPreviousValue(id);
    }
   
   String typ = rtv.getDataType();
   JcompType jtyp = jcomp_typer.findType(typ);
   if (jtyp == null) return null;
   if (jtyp.isPrimitiveType()) {
      rslt = buildPrimitiveValue(jtyp,rtv.getValue());
    }
   else if (jtyp.isStringType()) {
      rslt = buildStringValue(rtv.getValue());
    }
   else if (jtyp.getName().equals("java.lang.Class")) {
      rslt = buildClassValue(rtv.getValue());
    }
   else if (jtyp.getName().equals("java.io.File")) {
      rslt = buildFileValue(rtv.getValue());
    }
   else if (jtyp.isArrayType()) {
       rslt = buildArrayValue(rtv,jtyp);
    }
   else if (jtyp.isFunctionRef()) {
      
    }
   else if (jtyp.isEnumType()) {
      String efld = rtv.getEnum();
      if (efld != null) {
         rslt = new PicotCodeFragment(jtyp.getName() + "." + efld);
       }
    }
   else if (jtyp.isCompatibleWith(collection_type)) {
      rslt = buildCollection(rtv,jtyp);
    }
   else if (jtyp.isCompatibleWith(map_type)) {
      rslt = buildMap(rtv,jtyp);
    }
   else if (jtyp.isCompiledType()) {
      rslt = buildUserObjectValue(rtv,jtyp);
    }
   else {
      rslt = buildSystemObjectValue(rtv,jtyp);
    }
   
   cur_context.setPreviousValue(id,rslt);
   
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

private PicotCodeFragment buildArrayValue(RootTraceValue rtv,JcompType typ)
{
   int sz = rtv.getArrayLength();
   StringBuffer buf = new StringBuffer();
   buf.append("new " + typ.getBaseType() + "[" + sz + "]");
   if (sz > 0) {
      buf.append(" { ");
      for (int i = 0; i < sz; ++i) {
         RootTraceValue etv = rtv.getIndexValue(for_trace,i,start_time);
         PicotCodeFragment efg = buildValue(etv);
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

private PicotCodeFragment buildUserObjectValue(RootTraceValue rtv,JcompType typ)
{
   return buildObjectValue(rtv,typ);
}



private PicotCodeFragment buildObjectValue(RootTraceValue rtv,JcompType typ)
{
   Map<String,JcompType> flds = typ.getFields(jcomp_typer);
   PicotClassData pcd = getClassData(typ);
   PicotFieldMap fldval = new PicotFieldMap();
   Collection<JcompSymbol> mthds = pcd.getMethods(); 
   
   Set<JcompType> paramtypes = new HashSet<>();
   boolean havecnst = false;
   for (JcompSymbol js : mthds) {
      if (js.isConstructorSymbol()) {
         paramtypes.addAll(js.getType().getComponents());
         havecnst = true;
       }
      else if (js.isStatic() && js.getType().getBaseType() == typ) {
         paramtypes.addAll(js.getType().getComponents());
       }
    }
      
   for (String fld : flds.keySet()) {
      String fnm = fld;
      int idx = fnm.lastIndexOf(".");
      if (idx > 0) fnm = fld.substring(idx+1);
      if (fnm.equals("this") || fnm.startsWith("this$")) continue;

      JcompSymbol fldsym = typ.lookupField(jcomp_typer,fnm);
      if (fldsym == null) {
         RoseLog.logE("PICOT","Can't find field " + fld);
         continue;
       }
     
      RootTraceValue ftv = rtv.getFieldValue(for_trace,fld,start_time);
      if (ftv == null) continue;                // not needed
      RoseLog.logD("PICOT","Build value for field " + fnm + " " + ftv.getId() + " " +
           ftv.getDataType());
      if (paramtypes.contains(fldsym.getType())) {
         PicotCodeFragment rslt = buildValue(ftv);
         if (rslt != null) {
            fldval.put(fldsym,rslt);
          }
       }
      else {
         PicotCodeFragment rslt = buildValue(ftv);
         if (rslt != null) {
            fldval.put(fldsym,rslt);
          }
       }
    }
   
   List<PicotCodeFragment> rslts = new ArrayList<>();
   Set<PicotCodeFragment> staticrslts = new HashSet<>();
   
   Set<PicotValueAccessor> known = cur_context.getValuesForType(typ);
   if (known != null) {
      for (PicotValueAccessor pva : known) {
         PicotCodeFragment apcf = pva.getGetterCode(this,null);
         if (apcf != null) {
            rslts.add(apcf);
            staticrslts.add(apcf);
          }      
       }
    }
   
   for (JcompSymbol js : mthds) {
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
   
   if (!havecnst) {
     String ccode = "new " + typ.getName() + "()";
     rslts.add(new PicotCodeFragment(ccode));
    }
   // if type has outer_type, try using it to find factory method
   
   RoseLog.logD("PICOT","FOR " + typ.getName());

   String var = "v" + variable_counter.incrementAndGet();
   PicotValueContext startctx = cur_context;
   for (PicotCodeFragment pcf : rslts) {
      String decl = typ.getName() + " " + var + " = " + pcf.getCode() + ";\n";
      PicotCodeFragment npcf = new PicotCodeFragment(decl);
      RoseLog.logD("PICOT","TRY: " + npcf.getCode());
      PicotValueContext npvc = new PicotValueContext(startctx,npcf,var,rtv);
      if (!npvc.isValidSetup()) continue;
      if (staticrslts.contains(pcf)) {
         Collection<PicotValueProblem> probs = npvc.getProblems();
         if (probs == null || probs.isEmpty()) {
            cur_context = npvc;
            npvc.setVariableKnown(var);
            return new PicotCodeFragment(var);
          }
         continue;
       }
      PicotValueContext nnpvc = fixProblems(npvc);
      if (nnpvc != null) {
         nnpvc.setVariableKnown(var);
         cur_context = nnpvc;
         return new PicotCodeFragment(var);
       }
    }
   
   return null;
}



private PicotCodeFragment buildSystemObjectValue(RootTraceValue rtv,JcompType typ)
{
   // handle Collections by creating empty collection and doing ADD multiple times
   // handle Map by creating empty map and doing PUT multiple times
   // special case Point, etc. when rtv has actual values
   
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
   
   PicotCodeFragment pcf = buildObjectValue(rtv,typ);
   if (pcf != null) return pcf;
   
// Collection<JcompSymbol> mthds = typ.getDefinedMethods(jcomp_typer);
// for (JcompSymbol js : mthds) {
//    if (js.isPublic() && js.isConstructorSymbol()) {
//       if (js.getType().getComponents().size() == 0) {
//          String call = "new " + typ.getName() + "()";
            // add variable and update cur_contents
//          RoseLog.logD("PICOT","SYS TRY : " + call);
//          return new PicotCodeFragment(call);
//        }
//     }
//  }
   
   return null;
}



private void buildCodeForMethod(JcompSymbol js,PicotMethodData pmd,
      PicotCodeFragment thiscode,PicotFieldMap fldvals,
      List<PicotCodeFragment> rslts)
{
   String call = null;
   Set<String> used = new HashSet<>();
   
   if (js.isConstructorSymbol()) {
      call = "new " + js.getClassType().getName();
    }
   else if (js.isStatic()) {
      call = js.getClassType().getName() + "." + js.getName();
    }
   else if (thiscode == null) return;
   else {
      call = thiscode + "." + js.getName();
    }
   call += "(";
   
   int ct = 0;
   addParameter(call,pmd,ct,fldvals,rslts,used);
}


private void addParameter(String pfx,PicotMethodData pmd,int pno,
      PicotFieldMap fldvals,List<PicotCodeFragment> rslts,
      Set<String> used)
{
   List<JcompType> ptyps = pmd.getParameterTypes();
   
   if (pno >= ptyps.size()) {
      rslts.add(new PicotCodeFragment(pfx + ")"));
      return;
    }
   
   if (pno > 0) pfx += ",";
   JcompType ptyp = ptyps.get(pno);
   Map<String,String> pvals = getParameterValues(pno,ptyp,pmd,fldvals);
   for (String s : pvals.keySet()) {
      String call = pfx + s;
      String key = pvals.get(s);
      if (key != null && key.length() == 0) key = null;
      if (key != null) {
         if (used.contains(key)) continue;
         used.add(key);
       }
      addParameter(call,pmd,pno+1,fldvals,rslts,used);
      if (key != null) used.remove(key);
    }
}



private Map<String,String> getParameterValues(int pno,JcompType ptyp,
      PicotMethodData pmd,
      PicotFieldMap fldvals)
{
   Map<String,String> rslt = new LinkedHashMap<>();
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
                  rslt.put(fldpcf.getCode(),fldsym.getName());
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
            rslt.put(fldpcf.getCode(),fldsym.getName());
            used.add(fldpcf);
          }
       }
    }
   
   // nest use default value based on parameter type
   if (ptyp.isNumericType()) {
      rslt.put("0","");
    }
   else if (ptyp.isBooleanType()) {
      rslt.put("false","");
      rslt.put("true","");
    }
   else if (ptyp.isStringType()) {
      String sval = "S_" + string_counter.incrementAndGet();
      rslt.put(sval,"");
      rslt.put("null","");
    }
   else {
      Set<PicotValueAccessor> accs = cur_context.getValuesForType(ptyp);
      if (accs != null) {
         for (PicotValueAccessor acc : accs) {
            PicotCodeFragment pcf = acc.getGetterCode(this,null);
            if (pcf != null && !used.contains(pcf)) rslt.put(pcf.getCode(),"");
          }
       }
      rslt.put("null","");
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Handle Collections                                                      */
/*                                                                              */
/********************************************************************************/

private PicotCodeFragment buildCollection(RootTraceValue rtv,JcompType typ)
{
   List<PicotCodeFragment> elts = new ArrayList<>();
   RootTraceValue rtv1 = rtv.getFieldValue(for_trace,"@toArray",start_time);
   if (rtv1 == null) return null;
   int ct = rtv1.getArrayLength();
   long arraytime = 1000000000;
   for (int i = 0; i < ct; ++i) {
      RootTraceValue rtv2 = rtv1.getIndexValue(for_trace,i,arraytime);
      PicotCodeFragment cf2 = buildValue(rtv2);
      if (cf2 == null) return null;
      elts.add(cf2);
    }
   
   String var = "v" + variable_counter.incrementAndGet();
   String decl = typ.getName() + " " + var + " = new " + typ.getName() + "();\n";
   for (PicotCodeFragment elt : elts) {
      decl += var + ".add(" + elt.getCode() + ");\n";
    }
   PicotCodeFragment bldfrg = new PicotCodeFragment(decl);
   PicotValueContext npvc = new PicotValueContext(cur_context,bldfrg,var,rtv);
   if (npvc.isValidSetup()) {
      cur_context = npvc;
      return new PicotCodeFragment(var);
    }
   
   return null;
}


private PicotCodeFragment buildMap(RootTraceValue rtv,JcompType typ)
{ 
   List<PicotCodeFragment> elts = new ArrayList<>();
   RootTraceValue rtv1 = rtv.getFieldValue(for_trace,"@toArray",start_time);
   if (rtv1 == null) return null;
   int ct = rtv1.getArrayLength();
   long arraytime = 1000000000;
   for (int i = 0; i < ct; ++i) {
      RootTraceValue rtv2 = rtv1.getIndexValue(for_trace,i,arraytime);
      RootTraceValue rtvkey = rtv2.getIndexValue(for_trace,0,arraytime);
      PicotCodeFragment cf3 = buildValue(rtvkey);
      if (cf3 == null) return null;
      RootTraceValue rtvval = rtv2.getIndexValue(for_trace,1,arraytime);
      PicotCodeFragment cf4 = buildValue(rtvval);
      if (cf4 == null) return null;
      elts.add(cf3);
      elts.add(cf4);
    }
   
   String var = "v" + variable_counter.incrementAndGet();
   String decl = typ.getName() + " " + var + " = new " + typ.getName() + "();\n";
   for (int i = 0; i < elts.size(); i += 2) {
      decl += var + ".put(" + elts.get(i).getCode() + "," +
         elts.get(i+1).getCode() + ");\n";
    }
   PicotCodeFragment bldfrg = new PicotCodeFragment(decl);
   PicotValueContext npvc = new PicotValueContext(cur_context,bldfrg,var,rtv);
   if (npvc.isValidSetup()) {
      cur_context = npvc;
      return new PicotCodeFragment(var);
    }
   
   return null;
}

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

private PicotValueContext fixProblems(PicotValueContext ctx)
{
   Collection<PicotValueProblem> probs = ctx.getProblems();
   
   if (probs == null || probs.isEmpty()) return ctx;
   
   PicotValueProblem prob = chooseProblem(probs);
   if (prob == null) return null;
   List<PicotCodeFragment> fixes = computeFixes(prob);
   
   if (fixes == null || fixes.isEmpty()) 
      return null;
   
   for (PicotCodeFragment fix : fixes) {
      PicotValueContext npvc = new PicotValueContext(ctx,fix,null,null);
      if (npvc.isValidSetup()) {
         PicotValueContext nnpvc = fixProblems(npvc);
         if (nnpvc != null) return nnpvc;
       }
    }
   
   return null;
}


private PicotValueProblem chooseProblem(Collection<PicotValueProblem> probs)
{
   if (probs == null) return null;
   
   for (PicotValueProblem p : probs) {
      return p;
    }
   
   return null;
}
   




private List<PicotCodeFragment> computeFixes(PicotValueProblem p)
{
   PicotValueAccessor pva = p.getAccessor();
   RootTraceValue tgt = p.getTargetValue();
   RootTraceValue base = p.getTargetBaseValue();
   
   PicotCodeFragment rslt = buildValue(tgt);
   if (rslt == null) return null;
   
   List<PicotCodeFragment> rslts = pva.getSetterCodes(this,rslt,base);
   
   JcompType tgttyp = jcomp_typer.findSystemType(tgt.getDataType());
   if (tgttyp.isCompatibleWith(collection_type)) {
      PicotCodeFragment pcf = pva.getGetterCode(this,tgttyp);
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
   else if (tgttyp.isCompatibleWith(map_type)) {
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
   
    
   if (rslts.isEmpty()) return null;
   
   return rslts;
}



}       // end of class PicotValueBuilder




/* end of PicotValueBuilder.java */

