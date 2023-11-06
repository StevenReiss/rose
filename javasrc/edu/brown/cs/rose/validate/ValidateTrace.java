/********************************************************************************/
/*                                                                              */
/*              ValidateTrace.java                                              */
/*                                                                              */
/*      Representation of an execution trace                                    */
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



package edu.brown.cs.rose.validate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.bud.BudLaunch;
import edu.brown.cs.rose.bud.BudLocalVariable;
import edu.brown.cs.rose.bud.BudStack;
import edu.brown.cs.rose.bud.BudStackFrame;
import edu.brown.cs.rose.bud.BudType;
import edu.brown.cs.rose.bud.BudValue;
import edu.brown.cs.rose.root.RootTestCase;
import edu.brown.cs.rose.root.RootValidate;
import edu.brown.cs.rose.root.RoseException;
import edu.brown.cs.rose.root.RoseLog;
import edu.brown.cs.rose.root.RootValidate.RootTraceVariable;

public class ValidateTrace implements ValidateConstants, RootValidate.RootTrace
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         seede_result;
private long            problem_time;
private ValidateCall    problem_context;
private Map<Integer,Element> id_map;
private String          thread_id;
private Map<Element,ValidateCall> call_map;
private String          session_id;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ValidateTrace(Element rslt,String tid)
{
   seede_result = IvyXml.getChild(rslt,"CONTENTS");
   session_id = IvyXml.getAttrString(rslt,"ID");
   problem_time = -1;
   problem_context = null;
   thread_id = tid;
   call_map = new HashMap<>();
   setupIdMap();
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public long getProblemTime()   
{
   return problem_time;
}


@Override public ValidateCall getProblemContext()
{
   return problem_context;
}


@Override public ValidateCall getRootContext()
{
   Element runner = getRunner();
  
   return getCallForContext(IvyXml.getChild(runner,"CONTEXT"));
}


ValidateCall getCallForContext(Element ctx)
{
   if (ctx == null) return null;
   
   synchronized (call_map) {
      ValidateCall vc = call_map.get(ctx);
      if (vc == null) {
         vc = new ValidateCall(this,ctx);
         call_map.put(ctx,vc);
       }
      return vc;
    }
}


String getThread()
{
   return thread_id;
}



@Override public ValidateValue getException()
{
   ValidateCall prob = getProblemContext();
   if (prob != null) {
      ValidateVariable thr = prob.getVariables().get("*THROWS*");
      if (thr != null) {
         List<ValidateValue> vals = thr.getValues(this);
         return vals.get(0);
       }
    }
   
   Element runner = getRunner();
   Element ret = IvyXml.getChild(runner,"RETURN");
   String reason = IvyXml.getAttrString(ret,"REASON");
   if (reason == null) return null;
   if (reason.equals("EXCEPTION")) {
      return new ValidateValue(IvyXml.getChild(ret,"VALUE"));
    }
   
   return null;
}



boolean isReturn()
{
   Element runner = getRunner();
   Element ret = IvyXml.getChild(runner,"RETURN");
   String reason = IvyXml.getAttrString(ret,"REASON");
   return reason.equals("RETURN");
}


boolean isCompilerError()
{
   Element runner = getRunner();
   Element ret = IvyXml.getChild(runner,"RETURN");
   String reason = IvyXml.getAttrString(ret,"REASON");
   return reason == null || 
         reason.equals("COMPILER_ERROR") || 
         reason.equals("ERROR");
}



@Override public ValidateValue getReturnValue()
{
   Element runner = getRunner();
   Element ret = IvyXml.getChild(runner,"RETURN");
   String reason = IvyXml.getAttrString(ret,"REASON");
   if (reason == null) return null;
   if (reason.equals("RETURN")) {
      Element rval = IvyXml.getChild(ret,"VALUE");
      if (rval == null) return null;
      return new ValidateValue(rval);
    }
   
   return null;
}


long getExceptionTime()
{
   ValidateCall prob = getProblemContext();
   if (prob != null) {
      ValidateVariable thr = prob.getVariables().get("*THROWS*");
      if (thr != null) {
         long when = prob.getEndTime();
         List<ValidateValue> vals = thr.getValues(this);
         for (ValidateValue vv : vals) {
            if (vv.getStartTime() > 0 && vv.getStartTime() < when) when = vv.getStartTime();
          }
         return when;
       }
    }
  
   Element runner = getRunner();
   Element ret = IvyXml.getChild(runner,"RETURN");
   String reason = IvyXml.getAttrString(ret,"REASON");
   if (reason != null && reason.equals("EXCEPTION")) {
      ValidateCall vc = getRootContext();
      return vc.getEndTime();
    }
   
   return -1;
}

long getExecutionTime()
{
   return IvyXml.getAttrLong(seede_result,"TICKS");
}


private Element getRunner()
{
   if (thread_id !=  null) {
      for (Element runner : IvyXml.children(seede_result,"RUNNER")) {
         String tid = IvyXml.getAttrString(runner,"THREAD");
         if (!tid.equals(thread_id)) continue;
         return runner;
       }
    }
   
   Element runner = IvyXml.getChild(seede_result,"RUNNER");
   return runner;
}



@Override public Map<String,RootTraceVariable> getGlobalVariables()
{
   Map<String,RootTraceVariable> rslt = new LinkedHashMap<>();
   Element glbls = IvyXml.getChild(seede_result,"GLOBALS");
   for (Element e : IvyXml.children(glbls,"VARIABLE")) {
      String nm = IvyXml.getAttrString(e,"NAME");
      rslt.put(nm,new ValidateVariable(e));
    }
   
   return rslt;
}


@Override public String getSessionId()
{
   return session_id; 
}

 
/********************************************************************************/
/*                                                                              */
/*      Find the point in the execution corresponding to the launch             */
/*                                                                              */
/********************************************************************************/

void setupForLaunch(BudLaunch launch)
{
   if (problem_time >= 0 || seede_result == null) return;
   thread_id = launch.getThread();
   
   Stack<String> stack = new Stack<>();
   for (Element runner : IvyXml.children(seede_result,"RUNNER")) {
      String tid = IvyXml.getAttrString(runner,"THREAD");
      if (!tid.equals(thread_id)) continue;
      findProblemTime(IvyXml.getChild(runner,"CONTEXT"),launch,stack);
    }
}



void findProblemTime(Element ctx,BudLaunch launch,Stack<String> stack)
{
   String mthd = IvyXml.getAttrString(ctx,"METHOD");
   stack.push(normalizeName(mthd));
   
   if (checkStack(launch,stack)) {
      findContextTime(ctx,launch);
    }
   else {
      for (Element subctx : IvyXml.children(ctx,"CONTEXT")) {
         findProblemTime(subctx,launch,stack);
       }
    }
   
   stack.pop();
}



private boolean checkStack(BudLaunch launch,Stack<String> stack)
{
   BudStack stk = launch.getStack();
   String base = stack.get(0);
   List<BudStackFrame> frms = stk.getFrames();
   for (int i = frms.size()-1; i >= 0; --i) {
      BudStackFrame frame = frms.get(i);
      String sgn = frame.getFormatSignature();
      String id = frame.getClassName() + "." + frame.getMethodName() + sgn;
      id = normalizeName(id);
      if (id.equals(base)) {
         return checkStack(launch,stack,i);
       }
    }
   return false;
}



private String normalizeName(String mthd)
{
   StringBuffer buf = new StringBuffer();
   int lvl = 0;
   for (int i = 0; i < mthd.length(); ++i) {
      char c = mthd.charAt(i);
      if (c == '<') {
         ++lvl;
         continue;
       }
      else if (c == '>') {
         --lvl;
         continue;
       }
      else if (lvl > 0) continue;
      else if (c == '$') c = '.';
      buf.append(c);
    }
   return buf.toString();
}


private boolean checkStack(BudLaunch launch,Stack<String> stack,int start)
{ 
   BudStack stk = launch.getStack(); 
   BudStackFrame topframe = stk.getTopFrame();
   List<BudStackFrame> frms = stk.getFrames();
   for (int i = start; i >= 0; --i) {
      BudStackFrame frm = frms.get(i);
      String id = frm.getClassName() + "." + frm.getMethodName() + 
            frm.getFormatSignature();
      id = normalizeName(id);
      if (start-i >= stack.size()) return false;
      if (!id.equals(stack.get(start-i))) return false;
      if (frm == topframe) return true;
    }
   return false;
}



private void findContextTime(Element ctx,BudLaunch launch)
{
   Element linevar = null;
   for (Element var : IvyXml.children(ctx,"VARIABLE")) {
      String varname = IvyXml.getAttrString(var,"NAME");
      if (varname.equals("*LINE*")) {
         linevar = var;
         break;
       }
    }
   if (linevar == null) return;
   
   BudStackFrame frame = launch.getStack().getTopFrame();
   String lno = Integer.toString(frame.getLineNumber());
   int lnoi = Integer.parseInt(lno);
   long linetime = -1;
   for (Element val : IvyXml.children(linevar,"VALUE")) {
      long time = IvyXml.getAttrLong(val,"TIME");
      if (time == 0) time = IvyXml.getAttrLong(ctx,"START"); 
      if (linetime > 0) {
         findContextTime(ctx,launch,lnoi,linetime,time-1);
         linetime = -1;
       }
      if (lno.equals(IvyXml.getText(val))) {
         linetime = time;
       }
    }
   if (linetime > 0) {
      findContextTime(ctx,launch,lnoi,linetime,IvyXml.getAttrLong(ctx,"END"));
    }
}



private void findContextTime(Element ctx,BudLaunch launch,int line,long from,long to)
{
   // check local variables in the context vs those of the launch
   BudStackFrame frame = launch.getStack().getTopFrame();
   for (String var : frame.getLocals()) {
      BudLocalVariable local = frame.getLocal(var);
      Element varelt = findVariableInContext(ctx,var,line);
      if (varelt != null) {
         long prev = -1;
         Element prevval = null;
         boolean found = false;
         int foundct = 0;
         for (Element valelt : IvyXml.children(varelt,"VALUE")) {
            long time = IvyXml.getAttrLong(valelt,"TIME");
            if (prev > 0) {
               if (time >= from && prev <= to) {
                  Boolean fg = compareVariable(local,prevval,launch,from,to);
                  if (fg != null) {
                     ++foundct;
                     found |= fg;
                   }
                }
             }
            prev = time;
            prevval = dereference(valelt);
          }
         if (prev > 0) {
            long time = IvyXml.getAttrLong(ctx,"END");
            if (time >= from && prev <= to) {
               Boolean fg = compareVariable(local,prevval,launch,from,to);
               if (fg != null) {
                  ++foundct;
                  found |= fg;
                }
             }
          }
         else if (prev == -1) {
            Boolean fg = compareVariable(local,prevval,launch,from,to);
            if (fg != null) {
               ++foundct;
               found |= fg;
             }
          }
         if (foundct > 0 && !found)
            return;
       }
    }
   
   if (problem_time > 0 && problem_context !=  null) {
      // see if this context is better than saved context
    }
   
   problem_time = from;
   problem_context = getCallForContext(ctx);
}


private Element findVariableInContext(Element ctx,String nm,int lno)
{
   Element best = null;
   int bestln = -1;
   for (Element varelt : IvyXml.children(ctx,"VARIABLE")) {
      String varnam = IvyXml.getAttrString(varelt,"NAME");
      if (varnam.equals(nm)) {
         int vln = IvyXml.getAttrInt(varelt,"LINE");
         if (vln > lno) continue;
         if (best == null || vln > bestln) {
            bestln = vln;
            best = varelt;
          }
       }
    }
   
   return best;
}



/********************************************************************************/
/*                                                                              */
/*      Handle checking execution against a test case                           */
/*                                                                              */
/********************************************************************************/

double checkTest(RootTestCase rtc)
{
   if (rtc == null) return 1.0;
   
   ValidateValue exv = getException();
   double value = 1.0;
   
   RoseLog.logD("VALIDATE","Check test case " + rtc.getThrows() + " " + isReturn() + " " + exv);
   
   if (rtc.getThrows()) {
      if (exv == null) return 0.1;
      String etyp = rtc.getThrowType();
      if (etyp != null) {
         // check if etyp and exv.getDataType() are consistent
         // and scale value accordingly
       }
    }
   else {
      if (!isReturn()) return 0.0;
      ValidateValue rtv = getReturnValue();
      String eval = rtc.getReturnValue();
      if (eval != null && rtv != null) {
         // check return value here and scale value accordingly
       }
    }
   
   // check other values at this point
   
   return value;
}




/********************************************************************************/
/*                                                                              */
/*      Compare variables in execution with those in launch                     */
/*                                                                              */
/********************************************************************************/

private Boolean compareVariable(BudLocalVariable local,Element valelt,BudLaunch launch,long from,long to)
{
   switch (local.getKind()) {
      case "PRIMITIVE" :
         String typ = IvyXml.getAttrString(valelt,"TYPE");
         String valtxt = IvyXml.getText(valelt);
         String lclval = local.getValue();
         switch (typ) {
            case "boolean" :
               if (valtxt.equals("0") && lclval.equals("false")) return true;
               if (valtxt.equals("1") && lclval.equals("true")) return true;
               return false;
            case "double" :
            case "float" :
               if (lclval.equals(valtxt)) return true;
               try {
                  double v1 = Double.valueOf(lclval);
                  double v2 = Double.valueOf(valtxt);
                  if (Math.abs(v1-v2) < 0.0000001) return true;
                }
               catch (NumberFormatException e) { }
               return false;
            case "char" :
               int c1 = 0;
               String s1 = lclval;
               if (s1 != null && s1.length() > 0) c1 = s1.charAt(0); 
               int c2 = Integer.parseInt(valtxt);
               return c1 == c2;
            default :
               return lclval.equals(valtxt);
          }
      case "STRING" :
         valtxt = IvyXml.getText(valelt);
         String ltxt = local.getValue();
         if (ltxt == null && valtxt == null) return true;
         if (ltxt == null || valtxt == null)
            return false;
         boolean fg = ltxt.equals(valtxt);
         if (fg) return true;
         // UTF-16 strings are not correct when reported from bedrock
         if (ltxt.getBytes().length != ltxt.length()) return null;
         return false;
      case "ARRAY" :
         if (local.getType().equals("null")) {
            if (IvyXml.getAttrBool(valelt,"NULL")) return true;
            return false;
          }   
         return compareArray(local,valelt,launch,from,to);
      case "OBJECT" :
         if (local.getType().equals("null") || local.getType().equals("*ANY*")) {
            if (IvyXml.getAttrBool(valelt,"NULL")) return true;
            return false;
          }
         else if (local.getType().equals("java.lang.Class")) {
            return true;
          }
         return compareObject(local,valelt,launch,from,to);
      case "CLASS" :
         System.err.println("CHECK HERE compare CLASS");
         break;
      default :
         break;
    }
   
   return null;
}



private Boolean compareObject(BudLocalVariable local,Element valelt0,BudLaunch launch,long from,long to)
{
   Element valelt = dereference(valelt0);
   if (local.getType().equals("null")) {
      if (IvyXml.getAttrBool(valelt,"NULL")) return true;
      return false;
    }
   
   String ltyp = local.getType();
   String vtype = IvyXml.getAttrString(valelt,"TYPE");
   if (!ltyp.equals(vtype)) {
      int idx = ltyp.indexOf("<");
      if (idx > 0) {
         ltyp = ltyp.substring(0,idx);
         if (!ltyp.equals(vtype)) return false;
       }
    }
   
   BudValue localval = launch.evaluate(local.getName());
   if (localval == null) return null;
   
   int ct = 0;
   for (Element fldelt : IvyXml.children(valelt,"FIELD")) {
      String nm = IvyXml.getAttrString(fldelt,"NAME");
      if (nm.startsWith("@")) continue;
      try {
         BudValue fldval = localval.getFieldValue(nm);
         if (fldval == null) continue;
         Boolean fg = checkValueAtTime(fldval,fldelt,launch,from,to);
         if (fg == null) continue;
         if (!fg) return false;
         ++ct;  // if matched
       }
      catch (RoseException e) { }
    }
   
   if (ct > 0) return true;
   
   return null;
}



private Boolean compareArray(BudLocalVariable local,Element valelt0,BudLaunch launch,long from,long to)
{
   Element valelt = dereference(valelt0);
   if (local.getType().equals("null")) {
      if (IvyXml.getAttrBool(valelt,"NULL")) return true;
      return false;
    }
   
   String s1 = normalizeName(local.getType());
   String s2 = normalizeName(IvyXml.getAttrString(valelt,"TYPE"));
   if (!s1.equals(s2)) return false;

// BudValue localval = launch.evaluate(local.getName());
// int ctxsz = IvyXml.getAttrInt(valelt,"SIZE");
   
   // check number of elements
   // loop for each element
   
   return null;
}



private Boolean checkValueAtTime(BudValue actval,Element valctx,BudLaunch launch,long from,long to)
{
   long prev = -1;
   Element prevval = null;
   int foundct = 0;
   boolean found = false;
   for (Element valelt : IvyXml.children(valctx,"VALUE")) {
      long time = IvyXml.getAttrLong(valelt,"TIME");
      if (prev >= 0) {
         if (time >= from && prev <= to) {
            Boolean fg = compareValueAtTime(actval,prevval,launch,from,to);
            if (fg != null) {
               ++foundct;
               found |= fg;
             }
          }
       }
      prev = time;
      prevval = valelt;
    }
   if (prev > 0 && prev <= to) {
      if (prev <= to) {
         Boolean fg = compareValueAtTime(actval,prevval,launch,from,to);
         if (fg != null) {
            ++foundct;
            found |= fg;
          }
       }
    }
   else if (prev == -1) {
      Boolean fg = compareValueAtTime(actval,prevval,launch,from,to);
      if (fg != null) {
         ++foundct;
         found |= fg;
       }   
    }
   
   if (foundct > 0 && !found) return false;
   if (found) return true;
   
   return null;
}



private Boolean compareValueAtTime(BudValue actval,Element valctx,BudLaunch launch,long from,long to)
{
   String ctxval = IvyXml.getText(valctx);
   String ctxtyp = IvyXml.getAttrString(valctx,"TYPE");
   BudType typ = actval.getDataType();
  
   if (actval.isNull()) {
      return IvyXml.getAttrBool(valctx,"NULL");
    }
   if (ctxval == null) ctxval = "";
   
   // handle primitive types
   switch (typ.getName()) {
      case "boolean" :
         if (actval.getBoolean()) return ctxval.equals("1");
         else return ctxval.equals("0");
      case "int" :
      case "long" :
      case "short" :
      case "byte" :
      case "char" :
         try {
            long l = Long.parseLong(ctxval);
            return l == actval.getInt();
          }
         catch (NumberFormatException e) { }
         return null;
      case "double" :
      case "float" :
         return null;
      case "java.lang.String" :
         if (ctxtyp.equals("java.lang.String")) {
            return actval.getString().equals(ctxval);
          }
         break;
    }

   String s1 = typ.getName(); 
   int idx1 = s1.indexOf("<");
   if (idx1 > 0) s1 = s1.substring(0,idx1);
   String s2 = ctxtyp;
   int idx2 = s2.indexOf("<");
   if (idx2 > 0) s2 = s2.substring(0,idx2);
   s1 = s1.replace("$",".");
   
   if (!s1.equals(s2)) {
      if ((s1.endsWith("Set") || s1.endsWith("SetN")) &&
            (s2.endsWith("Set") || s2.endsWith("SetN"))) {
         return null; 
       }
      return false;
    }
   
   // handle objects and arrays when nested -- ignore for now
   
   return null;
} 



/********************************************************************************/
/*                                                                              */
/*      Setup mapping for ID matching                                           */
/*                                                                              */
/********************************************************************************/

private void setupIdMap()
{
   id_map = new HashMap<>();
   for (Element valelt : IvyXml.elementsByTag(seede_result,"VALUE")) {
      int id = IvyXml.getAttrInt(valelt,"ID");
      if (id < 0) continue;
      if (IvyXml.getAttrBool(valelt,"REF")) continue;
      Element use = id_map.get(id);
      if (use != null) continue;
      id_map.put(id,valelt);
    }
}


Element dereference(Element val)
{
   if (IvyXml.getAttrBool(val,"REF")) {
      int id = IvyXml.getAttrInt(val,"ID");
      return id_map.get(id);
    }
   
   return val;
}


/********************************************************************************/
/*                                                                              */
/*      Location methods                                                        */
/*                                                                              */
/********************************************************************************/

void getExecutedLocations(Set<String> rslt)
{
   ValidateCall vc = getRootContext();
   if (vc != null) vc.getExecutedLocations(rslt);
}

/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return IvyXml.convertXmlToString(seede_result);
}


}       // end of class ValidateTrace




/* end of ValidateTrace.java */

