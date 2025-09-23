/********************************************************************************/
/*                                                                              */
/*              PicotMethodDataBinary.java                                      */
/*                                                                              */
/*      Method data computation for byte code methods                           */
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.ivy.jcode.JcodeConstants;
import edu.brown.cs.ivy.jcode.JcodeInstruction;
import edu.brown.cs.ivy.jcode.JcodeMethod;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;

class PicotMethodDataBinary extends PicotMethodData implements JcodeConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcodeMethod     base_method;
private BinaryContext   current_context;
private Map<JcodeInstruction,BinaryContext> saved_contexts;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

PicotMethodDataBinary(JcompSymbol js,JcompTyper typer)
{
   super(js,typer);
   base_method = typer.getMethodCode(js);
   current_context = null;
   saved_contexts = null;
}




/********************************************************************************/
/*                                                                              */
/*      Actual computation                                                      */
/*                                                                              */
/********************************************************************************/

@Override protected void processLocal()
{
   if (base_method == null) return;
   
   current_context = new BinaryContext();
   saved_contexts = new HashMap<>();
   
   int delta = 0;
   if (!method_symbol.isStatic()) {
      delta = 1;
      current_context.setLocal(0,PicotEffectItem.createThisItem());
    }
   List<JcompType> typs = method_symbol.getDeclaredType(jcomp_typer).getComponents();
   
   for (int i = 0; i < base_method.getNumArguments(); ++i) {
      PicotEffectItem pei = PicotEffectItem.createBinaryParameter(i,typs.get(i));
      current_context.setLocal(i+delta,pei);
    }
   
   for (int i = 0; i < base_method.getNumInstructions(); ++i) {
      JcodeInstruction jins = base_method.getInstruction(i);
      processInstruction(jins);
    }
   
   // might want to set up try-catch mappings
   
   current_context = null;
   saved_contexts = null;
}



/********************************************************************************/
/*                                                                              */
/*      Handle the effects of a single instruction                              */
/*                                                                              */
/********************************************************************************/

// CHECKSTYLE:OFF
private void processInstruction(JcodeInstruction jins)
// CHECKSTYLE:ON
{
   PicotEffectItem itm0;
   PicotEffectItem itm1;
   int i0;
   JcompType rty;
   JcodeInstruction nins = null;
   
   BinaryContext ctx = saved_contexts.get(jins);
   if (ctx != null) {
      if (current_context == null) current_context = ctx;
      else current_context.merge(ctx);
    }
   
   switch (jins.getOpcode()) {
      case NEW :
         JcompType newty = jcomp_typer.findType(jins.getTypeReference().getName());
         itm0 = PicotEffectItem.createNewItem(newty);
         current_context.push(itm0);
         break;
      case ACONST_NULL :
         itm0 = PicotEffectItem.createConstantItem(null,jcomp_typer);
         current_context.push(itm0);
         break;
         
      case CHECKCAST :
      case I2B : case I2C : case I2S :
      case NOP :
         break;
         
      case DUP :
         current_context.handleDup(false,0);
         break;
      case DUP_X1 :
	 current_context.handleDup(false,1);
	 break;
      case DUP_X2 :
	 current_context.handleDup(false,2);
	 break;
      case DUP2 :
	 current_context.handleDup(true,0);
	 break;
      case DUP2_X1 :
	 current_context.handleDup(true,1);
	 break;
      case DUP2_X2 :
	 current_context.handleDup(true,2);
	 break;  
      case MONITORENTER :
      case MONITOREXIT :
         current_context.pop();
         break;
      case POP :
         current_context.pop();
         break;
      case POP2 :
         current_context.pop2();
         break;
      case SWAP :
         current_context.swap();
         break;         
         
      case INSTANCEOF :
      case FADD : case FDIV : case FMUL : case FREM : case FSUB :
      case IADD : case IDIV : case IMUL : case IREM : case ISUB :
      case IAND : case IOR : case IXOR :
      case ISHL : case ISHR : case IUSHR :
      case FCMPG : case FCMPL :
      case D2F : case FNEG : case I2F : case L2F :
      case D2I : case F2I : case L2I : case INEG :  
      case LCMP :
      case AALOAD : case AASTORE :
      case ANEWARRAY :
      case MULTIANEWARRAY :
      case NEWARRAY :
      case ARRAYLENGTH :
      case BALOAD : case CALOAD : case DALOAD : case FALOAD :
      case IALOAD : case LALOAD : case SALOAD :
      case BASTORE : case CASTORE : case DASTORE : case FASTORE :
      case IASTORE : case LASTORE : case SASTORE :
         current_context.pop(jins.getStackPop());
         if (jins.getStackPush() > 0) {
            current_context.push(PicotEffectItem.createExpressionItem());
          }
         break;
      case DADD : case DDIV : case DMUL : case DREM : case DSUB :
      case DCMPG : case DCMPL :
      case DNEG : case F2D : case I2D : case L2D :
         rty = jcomp_typer.DOUBLE_TYPE;
         current_context.pop(jins.getStackPop());
         current_context.push(PicotEffectItem.createExpressionItem(rty));
         break;
      case LADD : case LDIV : case LMUL : case LREM : case LSUB :
      case LAND : case LOR : case LXOR :
      case LSHL : case LSHR : case LUSHR :
      case D2L : case F2L : case I2L : case LNEG :
         rty = jcomp_typer.LONG_TYPE;
         current_context.pop(jins.getStackPop());
         current_context.push(PicotEffectItem.createExpressionItem(rty));
         break;
      case BIPUSH :
      case SIPUSH :
         i0 = jins.getIntValue();
         current_context.push(PicotEffectItem.createConstantItem(i0,jcomp_typer));
         break;
      case DCONST_0 :
         current_context.push(PicotEffectItem.createConstantItem(0.0,jcomp_typer));
         break;
      case DCONST_1 :
         current_context.push(PicotEffectItem.createConstantItem(1.0,jcomp_typer));
         break;
      case FCONST_0 :
         current_context.push(PicotEffectItem.createConstantItem(0.0f,jcomp_typer));
         break;
      case FCONST_1 :
         current_context.push(PicotEffectItem.createConstantItem(1.0f,jcomp_typer));
         break;
      case FCONST_2 :
         current_context.push(PicotEffectItem.createConstantItem(2.0f,jcomp_typer));
         break;
      case LCONST_0 :
         current_context.push(PicotEffectItem.createConstantItem(0L,jcomp_typer));
         break;
      case LCONST_1 :
         current_context.push(PicotEffectItem.createConstantItem(1L,jcomp_typer));
         break;        
      case DLOAD : case DLOAD_0 : case DLOAD_1 : case DLOAD_2 : case DLOAD_3 :
      case FLOAD : case FLOAD_0 : case FLOAD_1 : case FLOAD_2 : case FLOAD_3 :
      case ILOAD : case ILOAD_0 : case ILOAD_1 : case ILOAD_2 : case ILOAD_3 :
      case LLOAD : case LLOAD_0 : case LLOAD_1 : case LLOAD_2 : case LLOAD_3 :
      case ALOAD : case ALOAD_0 : case ALOAD_1 : case ALOAD_2 : case ALOAD_3 :
         i0 = jins.getLocalVariable();
         current_context.push(current_context.getLocal(i0));
         break;
      case ICONST_0 : case ICONST_1 : case ICONST_2 : case ICONST_3 : case ICONST_4 :
      case ICONST_5 : case ICONST_M1 : 
         i0 = jins.getIntValue();
         current_context.push(PicotEffectItem.createConstantItem(i0,jcomp_typer));
         break;
      case LDC_W :
      case LDC :
      case LDC2_W :
         Object ov = jins.getObjectValue();
         itm0 = PicotEffectItem.createConstantItem(ov,jcomp_typer);
         current_context.push(itm0);
         break;
      case IINC :
         current_context.setLocal(jins.getLocalVariable(),PicotEffectItem.createExpressionItem());
         break;
         
      case DSTORE : case DSTORE_0 : case DSTORE_1 : case DSTORE_2 : case DSTORE_3 :
      case FSTORE : case FSTORE_0 : case FSTORE_1 : case FSTORE_2 : case FSTORE_3 :
      case ISTORE : case ISTORE_0 : case ISTORE_1 : case ISTORE_2 : case ISTORE_3 :
      case LSTORE : case LSTORE_0 : case LSTORE_1 : case LSTORE_2 : case LSTORE_3 :  
      case ASTORE : case ASTORE_0 : case ASTORE_1 : case ASTORE_2 : case ASTORE_3 :
         i0 = jins.getLocalVariable();
         current_context.setLocal(i0,current_context.pop());
         break;
         
      case GOTO :
      case GOTO_W :
      case IF_ACMPEQ : case IF_ACMPNE :
      case IF_ICMPEQ : case IF_ICMPNE :
      case IF_ICMPLT : case IF_ICMPGE : case IF_ICMPGT : case IF_ICMPLE :  
      case IFEQ : case IFNE : case IFLT : case IFGE : case IFGT : case IFLE :
      case IFNONNULL : case IFNULL :
         current_context.pop(jins.getStackPop());
         nins = jins.getTargetInstruction();
         saveContext(nins);
         break;
      case LOOKUPSWITCH :
      case TABLESWITCH :
         current_context.pop(jins.getStackPop());
         for (JcodeInstruction bins : jins.getTargetInstructions()) {
            saveContext(bins);
          }
         break;
      case JSR :
      case JSR_W :
         nins = jins.getTargetInstruction();
         saveContext(nins);
         current_context.push(PicotEffectItem.createExpressionItem());
         break;
      case RET :
         break;
      case ARETURN :
      case DRETURN : case FRETURN : case IRETURN : case LRETURN :
         itm0 = current_context.pop();
         PicotMethodEffect reff = PicotMethodEffect.createReturn(itm0);
         method_effects.add(reff);
         break;
      case RETURN :
         break;
      case INVOKEINTERFACE :
      case INVOKESPECIAL :
      case INVOKESTATIC :
      case INVOKEVIRTUAL :
      case INVOKEDYNAMIC :
         JcodeMethod jm1 = jins.getMethodReference();
         current_context.pop(jins.getStackPop());
         for (int i = 0; i < jins.getStackPush(); ++i) {
            JcompType jt = jcomp_typer.findSystemType(jm1.getReturnType().getName());
            current_context.push(PicotEffectItem.createExpressionItem(jt));
          }
         break;
      case ATHROW :
         current_context.pop();
         break;
         
      case GETFIELD :
         itm0 = current_context.pop();
         JcompType jt = itm0.getDataType();
         if (jt == null) {
            itm0 = PicotEffectItem.createExpressionItem(jt);
          }
         else {
            JcompSymbol fsym = jt.lookupField(jcomp_typer,jins.getFieldReference().getName());
            itm0 = PicotEffectItem.createFieldItem(itm0,fsym);
          }
         current_context.push(itm0);
         break;
      case GETSTATIC :
         current_context.push(PicotEffectItem.createExpressionItem());
         break;
      case PUTFIELD :
         itm0 = current_context.pop();
         itm1 = current_context.pop();
         jt = itm1.getDataType();
         if (jt != null) { 
            JcompSymbol fsym = jt.lookupField(jcomp_typer,jins.getFieldReference().getName());
            itm1 = PicotEffectItem.createFieldItem(itm1,fsym);
            method_effects.add(PicotMethodEffect.createField(itm1,itm0));
          }
         break;
      case PUTSTATIC :
         itm0 = current_context.pop();
         break;
    }
}


private void saveContext(JcodeInstruction jins)
{
   if (jins == null) return;
   BinaryContext octx = saved_contexts.get(jins);
   if (octx == null) {
      saved_contexts.put(jins,new BinaryContext(current_context));
    }
   else { 
      octx.merge(current_context);
    }
}

/********************************************************************************/
/*                                                                              */
/*      Context for evaluation                                                  */
/*                                                                              */
/********************************************************************************/

private static class BinaryContext {

   private Map<Integer,PicotEffectItem> local_vars;
   private Deque<PicotEffectItem> current_stack;
   
   
   BinaryContext() {
      local_vars = new HashMap<>();
      current_stack = new ArrayDeque<>();
    }
   
   BinaryContext(BinaryContext ctx) {
      local_vars = new HashMap<>(ctx.local_vars);
      current_stack = new ArrayDeque<>(ctx.current_stack);
    }
   
   void setLocal(int i,PicotEffectItem itm) {
      local_vars.put(i,itm);
    }
   
   PicotEffectItem getLocal(int i) {
      PicotEffectItem pei = local_vars.get(i);
      if (pei == null) {
         pei = PicotEffectItem.createExpressionItem();
         local_vars.put(i,pei);
       }
      return pei;
    }
   
   void push(PicotEffectItem itm) {
      current_stack.push(itm);
    }
   
   PicotEffectItem pop() {
      return current_stack.pop();
    }
   
   void pop(int n) {
      for (int i = 0; i < n; ++i) current_stack.pop();
    }
   
   void pop2() {
      PicotEffectItem pei = current_stack.pop();
      if (!isCategory2(pei)) current_stack.pop();
    }
   
   void swap() {
      PicotEffectItem pei1 = current_stack.pop();
      PicotEffectItem pei2 = current_stack.pop();
      current_stack.push(pei1);
      current_stack.push(pei2);
    }
   
   void merge(BinaryContext ctx) {
      for (Map.Entry<Integer,PicotEffectItem> ent : ctx.local_vars.entrySet()) {
         PicotEffectItem lcl1 = local_vars.get(ent.getKey());
         if (lcl1 == null) local_vars.put(ent.getKey(),ent.getValue());
         else if (isDifferent(lcl1,ent.getValue())) {
            local_vars.put(ent.getKey(),
                  PicotEffectItem.createExpressionItem(lcl1.getDataType()));
          }
       }
      ArrayList<PicotEffectItem> stk = new ArrayList<>(current_stack);
      ArrayList<PicotEffectItem> mstk = new ArrayList<>(ctx.current_stack);
      boolean chng = false;
      for (int i = 0; i < current_stack.size(); ++i) {
         PicotEffectItem stk1 = stk.get(i);
         PicotEffectItem stk2 = mstk.get(i);
         if (isDifferent(stk1,stk2) && stk1.getItemType() != PicotItemType.EXPRESSION) {
            chng = true;
            stk.set(i,PicotEffectItem.createExpressionItem(stk1.getDataType()));
          }
       }
      if (chng) {
         current_stack = new ArrayDeque<>();
         for (PicotEffectItem pei : stk) {
            current_stack.push(pei);
          }
       }
    }
   
   void handleDup(boolean dbl,int lvl) {
      PicotEffectItem v1 = current_stack.pop();
      if (dbl && isCategory2(v1)) dbl = false;
      if (lvl == 2) {
         PicotEffectItem itm1 = current_stack.peek();
         if (dbl) {
            PicotEffectItem itm2 = current_stack.pop();
            itm1 = current_stack.peek();
            current_stack.push(itm2);
          }
         if (isCategory2(itm1)) lvl = 1;
       }
      if (lvl == 0 && !dbl) {                   // dup
         current_stack.push(v1);
         current_stack.push(v1);
       }
      else if (lvl == 0 && dbl) {		// dup2
         PicotEffectItem v2 = current_stack.pop();
         current_stack.push(v2);
         current_stack.push(v1);
         current_stack.push(v2);
         current_stack.push(v1);
       }
      else if (lvl == 1 && !dbl) { 	// dup_x1
         PicotEffectItem v2 = current_stack.pop();
         current_stack.push(v1);
         current_stack.push(v2);
         current_stack.push(v1);
       }
      else if (lvl == 1 && dbl) {		// dup2_x1
         PicotEffectItem v2 = current_stack.pop();
         PicotEffectItem v3 = current_stack.pop();
         current_stack.push(v2);
         current_stack.push(v1);
         current_stack.push(v3);
         current_stack.push(v2);
         current_stack.push(v1);
       }
      else if (lvl == 2 && !dbl) { 	 // dup_x2
         PicotEffectItem v2 = current_stack.pop();
         PicotEffectItem v3 = current_stack.pop();
         current_stack.push(v1);
         current_stack.push(v3);
         current_stack.push(v2);
         current_stack.push(v1);
       }
      else if (lvl == 2 && dbl) {		// dup2_x2
         PicotEffectItem v2 = current_stack.pop();
         PicotEffectItem v3 = current_stack.pop();
         PicotEffectItem v4 = current_stack.pop();
         current_stack.push(v2);
         current_stack.push(v1);
         current_stack.push(v4);
         current_stack.push(v3);
         current_stack.push(v2);
         current_stack.push(v1);
       }
    }
   
   private boolean isCategory2(PicotEffectItem pei) {
      JcompType jt = pei.getDataType();
      return jt != null && jt.isCategory2();
    }
   
   private boolean isDifferent(PicotEffectItem pei1,PicotEffectItem pei2) {
      return false;
    }
   
}       // end of inner class BinaryContext



}       // end of class PicotMethodDataBinary




/* end of PicotMethodDataBinary.java */

