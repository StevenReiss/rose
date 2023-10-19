/********************************************************************************/
/*                                                                              */
/*              SepalChatGPT.java                                               */
/*                                                                              */
/*      Find patch suggestions using ChatGPT                                    */
/*                                                                              */
/********************************************************************************/

package edu.brown.cs.rose.sepal;

import java.io.File;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Element;

import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootEdit;
import edu.brown.cs.rose.root.RootRepairFinderDefault;
import edu.brown.cs.rose.root.RoseLog;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;


import chatgpt_repair.ChatgptRepair;

public class SepalChatGPT extends RootRepairFinderDefault
{

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean use_chatgpt = true;

private String api_host = "https://api.aikey.one";
private String api_key = "sk-BTijAxza2faVBDpy452344F136154561Ab9f4fF2037d47E0";



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public double getFinderPriority()
{
   return 0.625;
}



@Override public void process()
{
   RoseLog.logD("SEPAL","Start CHATGPT check " + use_chatgpt + " " + getProcessor().haveGoodResult());
// if (getProcessor().haveGoodResult()) return;
   if (!use_chatgpt) return;

   RootControl ctrl = getProcessor().getController();
   ASTNode stmt = getResolvedStatementForLocation(null);
   RoseLog.logD("SEPAL","CHATGPT statement " + stmt);
   if (stmt == null) return;

   File bfile = getLocation().getFile();
// int lno = getLocation().getLineNumber();
   String bcnts = ctrl.getSourceContents(bfile);

   // get enclosing method
   ASTNode enclosingMethod = stmt.getParent();
   while (enclosingMethod != null && 
         enclosingMethod.getNodeType() != ASTNode.METHOD_DECLARATION && 
         enclosingMethod.getNodeType() != ASTNode.TYPE_DECLARATION) {
      enclosingMethod = enclosingMethod.getParent();
   }
   // TODO: How to get the source code of this method? Is this correct?
   String buggy_method = bcnts.substring(enclosingMethod.getStartPosition(),
      enclosingMethod.getStartPosition() + enclosingMethod.getLength());

   // get patch from ChatGPT (one patch per faulty line)
   String patch = null;
   for (int i = 0; i < 2; ++i) {
      try {
         // 1: getPatch(buggy_method: String, faulty_stmt_start_index: int, faulty_stmt_length: int, api_host: String, api_key: String)
         // 2: getPatch(buggy_method: String, faulty_stmt_line: int, api_host: String, api_key: String)
         patch = ChatgptRepair.getPatch(buggy_method,
               stmt.getStartPosition() - enclosingMethod.getStartPosition(), stmt.getLength(),
               api_host, api_key);
         break;
       }
      catch (com.plexpt.chatgpt.exception.ChatException e) {
         RoseLog.logI("SEPAL","Rate limit: " + e);
         // rate limit
         return;
       }
      catch (Throwable e) {
         RoseLog.logE("SEPAL","Problem with chatgpt",e);
         // this can be retried successfully
       }
    }
   
   if (patch == null) return;
   
   boolean ifpatch = false;
   if (stmt instanceof IfStatement && patch.trim().startsWith("(")) {
      IfStatement ifstmt = (IfStatement) stmt;
      stmt = ifstmt.getExpression();
      patch = "(" + patch + ")";
      ifpatch = true;
    }
   else if (stmt instanceof IfStatement) {
      patch = checkIfStatement(patch);
    }
   
   int spos = stmt.getStartPosition();
   int slen = stmt.getLength();
   
   IDocument doc = ctrl.getSourceDocument(bfile);
   String rep = null;
   try {
      rep = doc.get(spos,slen);
    }
   catch (BadLocationException e) {
      rep = stmt.toString();
    }
   
   if (rep.replace(" ","").equals(patch.replace(" ",""))) {
      return;
    }
   
   int idx2 = patch.indexOf("```");
   if (idx2 >= 0) {
      int idx = patch.indexOf("\n",idx2);
      if (idx > 0) patch = patch.substring(idx+1);
    }
   int idx3 = patch.lastIndexOf("```");
   if (idx3 > 0) {
      int idx4 = patch.lastIndexOf("\n",idx3);
      if (idx4 > 0) patch = patch.substring(0,idx4);
    }
   
   RoseLog.logD("SEPAL","Found CHATGPT patch: " + patch);
   
   String desc = null;
   int len = slen;
   int patchlf = patch.indexOf("\n");
   int stmtlt = rep.indexOf("\n");
   if (ifpatch) {
      desc = "Replace if exprsssion with " + patch;
    }
   if (patchlf < 0) {
      if (stmtlt > 0) {
         len = stmtlt;
       }
      desc = "Replace line with " + patch;
    }
   else {
      desc = "Replace statement with " + patch.replace("\n"," ");
    }
      
   // add repair
   // addRepair(edit: Element/ASTRewrite, description: String, logdata: String, priority_score: double)
   // TODO: How to create an edit xml? Is this correct?
   TextEdit te = new ReplaceEdit(spos,len, patch);
   
   RootEdit redit = new RootEdit(bfile, te);
   Element edit = redit.getTextEditXml();
   addRepair(edit, desc, getClass().getName() + "@ChatGPT", 0.25);
}



private String checkIfStatement(String patch)
{
   int idx1 = patch.indexOf("(");
   int lvl = 0;
   int idx2 = -1;
   boolean needparen = false;
   for (int i = idx1+1; i < patch.length(); ++i) {
      char ch = patch.charAt(i);
      if (Character.isWhitespace(ch)) ;
      else if (ch == '(') ++lvl;
      else if (ch == ')' && lvl > 0) {
         --lvl;
         if (lvl == 0) idx2 = i+1;
       }
      else if (lvl == 0) {
         if (ch == '&' || ch == '|') needparen = true;
         else if (ch == '{') break;
         else if (!needparen) break;
       }
    }
   if (needparen && idx2 > 0) {
      String p0 = patch.substring(0,idx1);
      String p1 = patch.substring(idx1,idx2);
      String p2 = patch.substring(idx2);
      patch = p0 + "(" + p1 + ")" + p2;
    }
   return patch;
}
}       // end of class SepalChatGPT


/* end of SepalChatGPT.java */
