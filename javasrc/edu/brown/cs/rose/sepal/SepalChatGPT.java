/********************************************************************************/
/*                                                                              */
/*              SepalChatGPT.java                                               */
/*                                                                              */
/*      Find patch suggestions using ChatGPT                                    */
/*                                                                              */
/********************************************************************************/

package edu.brown.cs.rose.sepal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
// import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.rose.bract.BractFactory;
import edu.brown.cs.rose.root.RootControl;
import edu.brown.cs.rose.root.RootEdit;
import edu.brown.cs.rose.root.RootLocation;
import edu.brown.cs.rose.root.RootRepairFinderDefault;
import edu.brown.cs.rose.root.RoseLog;

import org.eclipse.text.edits.InsertEdit;
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

private ASTNode         patch_node;
private String          patch_contents;
private String          patch_description;
private int             source_position;
private int             source_length;
private String          source_text;
private boolean         no_change;

private static boolean use_chatgpt;

private static String api_host;
private static String api_key;
private static Properties property_cache;
private static File cache_file;

// make null to allow concurrent calls
private static ReentrantLock chatgpt_lock = new ReentrantLock();
private static ReentrantLock cache_lock = new ReentrantLock();

private static boolean use_chat_gpt = false;


static {
   BoardProperties props = BoardProperties.getProperties("Rose");
   api_host = props.getString("Rose.chatgpt.host",null);
   api_key = props.getString("Rose.chatgpt.key",null);
   if (api_host != null && api_host.contains("XXXXXX")) api_host = null;
   if (api_key != null && api_key.contains("XXXXXX")) api_key = null;
   if (api_host != null && api_key != null) use_chatgpt = true;
   else use_chatgpt = false;
   
   File home = new File(System.getProperty("user.home"));
   cache_file = new File(home,".sepalchatgpt.cache");
   property_cache = new Properties();
   try (FileInputStream rdr = new FileInputStream(cache_file)) {
      property_cache.loadFromXML(rdr);
    }
   catch (Throwable t) { }
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override protected void localSetup()
{
   patch_node = null;
   patch_contents = null;
   patch_description = null;
   source_position = -1;
   source_length = -1;
   source_text = null;
   no_change = false;
}



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
   if (!use_chat_gpt) return;
   
   RoseLog.logD("SEPAL","Start CHATGPT check " + use_chatgpt + " " + getProcessor().haveGoodResult() +
         " " + getLocation().getLineNumber() + " " + getLocation().getFile());
// if (getProcessor().haveGoodResult()) return;
   if (!use_chatgpt) return;

   RootControl ctrl = getProcessor().getController();
   patch_node = getResolvedStatementForLocation(null);
   RoseLog.logD("SEPAL","CHATGPT statement " + patch_node);
   if (patch_node == null) return;
// if (patch_node instanceof Block && patch_node.getParent() instanceof MethodDeclaration) {
//    RoseLog.logD("SEPAL","CHATGPT: Skip method body");
//    return;
//  }

   File bfile = getLocation().getFile();
   String bcnts = ctrl.getSourceContents(bfile);
   
   accessChatGPT(ctrl,bcnts);
  
   if (patch_contents == null) {
      RoseLog.logD("SEPAL","CHATGPT: No patch returned");
      return;
    }
   
   cleanupPatch();
   if (patch_contents == null) {
      RoseLog.logD("SEPAL","CHATGPT: No patch returned");
      return;
    }
   
   checkInsertAfter(bcnts);
   
   source_position = patch_node.getStartPosition();
   source_length = patch_node.getLength();
   source_text = bcnts.substring(source_position,source_position+source_length);
    
   handlePartialStatements();
   if (patch_contents == null) {
      RoseLog.logD("SEPAL","CHATGPT: No patch returned");
      return;
    }
   
   computeDiff();
   
   if (no_change) {
      RoseLog.logD("SEPAL","CHATGPT returned original statement");
      return;
    }
   
   RoseLog.logD("SEPAL","Found CHATGPT patch: " + patch_contents);
   
   getDescription(bcnts);
      
   RoseLog.logD("SETUP PATCH " + source_position + " " + source_length + " `" + patch_contents + "'");
   
   TextEdit te = new ReplaceEdit(source_position,source_length, patch_contents);
   
   RootEdit redit = new RootEdit(bfile, te);
   Element edit = redit.getTextEditXml();
   addRepair(edit, patch_description, getClass().getName() + "@ChatGPT", 0.50);
}



/********************************************************************************/
/*                                                                              */
/*      Access ChatGPT                                                          */
/*                                                                              */
/********************************************************************************/

private void accessChatGPT(RootControl ctrl,String bcnts)
{
   // get enclosing method
   ASTNode enclosingmethod = patch_node;
   if (patch_node instanceof MethodDeclaration) {
      MethodDeclaration md = (MethodDeclaration) patch_node;
      patch_node = md.getBody();
    }
   while (enclosingmethod != null && 
         (enclosingmethod.getNodeType() != ASTNode.METHOD_DECLARATION &&
               enclosingmethod.getNodeType() != ASTNode.INITIALIZER)) {
      enclosingmethod = enclosingmethod.getParent();
      // TODO: also allow for fields in a type declaration -- need to change query, etc.
    }
   if (enclosingmethod == null) {
      enclosingmethod = patch_node.getParent();
      while (enclosingmethod != null && 
            enclosingmethod.getNodeType() != ASTNode.TYPE_DECLARATION) {
         enclosingmethod = enclosingmethod.getParent();
       }
    }
   if (enclosingmethod == null) {
      RoseLog.logD("SEPAL","CHATGPT: No enclosing method");
      return;
    }
   // TODO: How to get the source code of this method? Is this correct?
   String buggymethod = bcnts.substring(enclosingmethod.getStartPosition(),
         enclosingmethod.getStartPosition() + enclosingmethod.getLength());
   
   patch_contents = null;
   
   String key = null;
   if (property_cache != null) {
      String k1 = buggymethod + "@" + patch_node.getStartPosition() + "@" +
         patch_node.getLength();
      key = IvyFile.digestString(k1);
      if (key !=  null) {
         if (cache_lock != null) cache_lock.lock();
         try {
            patch_contents = property_cache.getProperty(key);
          }
         catch (Throwable t) {
            RoseLog.logE("SEPAL","Problem accessing Property cache",t);
          }
         finally {
            if (cache_lock != null) cache_lock.unlock();
          }
       }
    }
   
   if (patch_contents != null && patch_contents.equals("?")) patch_contents = null;
   
   if (patch_contents == null) {
      for (int i = 0; i < 2; ++i) {
         RoseLog.logD("TRY CHATGPT " + i + " " + chatgpt_lock.isLocked());
         if (chatgpt_lock != null) chatgpt_lock.lock();
         int spos = patch_node.getStartPosition() - enclosingmethod.getStartPosition();
         try {
            // 1: getPatch(buggy_method: String, faulty_stmt_start_index: int, faulty_stmt_length: int, api_host: String, api_key: String)
            // 2: getPatch(buggy_method: String, faulty_stmt_line: int, api_host: String, api_key: String)
            patch_contents = ChatgptRepair.getPatch(buggymethod,
                  spos,
                  patch_node.getLength(),
                  api_host, api_key);
            break;
          }
         catch (NullPointerException e) {
            RoseLog.logE("SEPAL","Fatal problem with CHATGPT",e);
            patch_contents = "?";
            break;
          }
         catch (Throwable e) {
            RoseLog.logE("SEPAL","Problem with chatgpt " + i + ":",e);
            buggymethod = buggymethod.substring(0,spos+patch_node.getLength());
            // this can be retried successfully
          }
         finally {
            if (chatgpt_lock != null) chatgpt_lock.unlock();
          }
         try {
            Thread.sleep(500);
          }
         catch (InterruptedException e) { }
         
       }
      if (patch_contents != null && key != null) {
         if (cache_lock != null) cache_lock.lock();
         try (FileOutputStream ots = new FileOutputStream(cache_file)) {
            property_cache.setProperty(key,patch_contents);
            property_cache.storeToXML(ots,"SepalChatGPT");
          }
         catch (Throwable t) {
            RoseLog.logE("SEPAL","Problem saving properties",t);
          }
         finally {
            if (cache_lock != null) cache_lock.unlock();
          }
       }
      
      if (patch_contents.equals("?")) {
         patch_contents = null;
       }
    }
}



@SuppressWarnings("unused")
private boolean equalsIgnoreSpaces(String s1,String s2) 
{
   String s1a = s1.replaceAll("\\s+","");
   String s2a = s2.replaceAll("\\s+","");
   return s1a.equals(s2a);
}


/********************************************************************************/
/*                                                                              */
/*      Clean up output from ChatGPT                                            */
/*                                                                              */
/********************************************************************************/

private void cleanupPatch()
{
   int idx2 = patch_contents.indexOf("```");
   if (idx2 >= 0) {
      int idx = patch_contents.indexOf("\n",idx2);
      if (idx > 0) patch_contents = patch_contents.substring(idx+1);
    }
   int idx3 = patch_contents.lastIndexOf("```");
   if (idx3 > 0) {
      int idx4 = patch_contents.lastIndexOf("\n",idx3);
      if (idx4 > 0) patch_contents = patch_contents.substring(0,idx4);
    }
   
   if (patch_node instanceof IfStatement && !patch_contents.trim().startsWith("if")) {
      boolean condonly = false;
      if (patch_contents.contains(";") || patch_contents.contains("}")) condonly = false;
      else if (patch_contents.startsWith("(")) condonly = true;
      else if (patch_contents.contains("&&") || patch_contents.contains("||")) condonly = true;
      if (condonly) {
         IfStatement ifstmt = (IfStatement) patch_node;
         patch_node = ifstmt.getExpression();
         patch_contents = "(" + patch_contents + ")";
       }
    }
   else if (patch_node instanceof IfStatement && patch_contents.trim().startsWith("if ")) {
      patch_contents = checkIfStatement(patch_contents);
    }
   
   if (patch_contents.startsWith("`") && patch_contents.endsWith("`")) {
      int len = patch_contents.length();
      patch_contents = patch_contents.substring(1,len-1);
    }
}



private void handlePartialStatements()
{
   if ((patch_node instanceof IfStatement || 
         patch_node instanceof WhileStatement ||
         patch_node instanceof ForStatement) 
         && patch_contents.endsWith("{")) {
      int idx4 = source_text.indexOf("{");
      if (idx4 > 0) {
         source_text = source_text.substring(0,idx4+1);
         source_length = source_text.length();
       }
    }
   else if (patch_node instanceof Block && !patch_contents.trim().startsWith("{")) {
      patch_contents = "{ " + patch_contents + " }";
//    patch_contents = null;
    }
   else if (patch_node instanceof Statement) {
      String tp = patch_contents.trim();
      if (!tp.endsWith(";") && !tp.endsWith("}")) {
         patch_contents += ";";
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Diff patch with original code                                           */
/*                                                                              */
/********************************************************************************/

private void computeDiff()
{
   int repmatch = 0;
   int patmatch = 0;
   int rlin = 0;
   int rpos = 0;
   int ppos = 0;
   while (rpos < source_text.length() && ppos < patch_contents.length()) {
      char repch = source_text.charAt(rpos);
      char patch = patch_contents.charAt(ppos);
      if (repch == patch) {
         if (repch == '\n') ++rlin;
         ++rpos;
         ++ppos;
         repmatch = rpos;
         patmatch = ppos;
       }
      else if (Character.isWhitespace(repch)) {
         if (repch == '\n') ++ rlin;
         ++rpos;
       }
      else if (Character.isWhitespace(patch)) {
         ++ppos;
       }
      else break;
    }
   // Source is now used form 0 to repmatch
   // Patch is then used from patmatch
   
   int erepmatch = source_text.length();
   int epatmatch = patch_contents.length();
   int repos = source_text.length()-1;
   int pepos = patch_contents.length()-1;
   while (repos >= repmatch && pepos >= patmatch) {
      char repch = source_text.charAt(repos);
      char patch = patch_contents.charAt(pepos);
      if (repch == patch) {
         erepmatch = repos;
         epatmatch = pepos;
         --repos;
         --pepos;
       }
      else if (Character.isWhitespace(repch)) {
         --repos;
       }
      else if (Character.isWhitespace(patch)) {
         --pepos;
       }
      else break;
    }
   
   // Source is now used from 0 to repmatch and erepmatch to end
   // Patch is now used from patmatch to epatmatch
   
   RoseLog.logD("SEPAL","DIFFR ORIG:  " + source_length + " " + 
         IvyFormat.formatString(source_text));
   RoseLog.logD("SEPAL","DIFFR PATCH: " + patch_contents.length() + " " + 
         IvyFormat.formatString(patch_contents));
   RoseLog.logD("SEPAL","DIFFR START: " + repmatch + " " + patmatch + " " + rlin);
   RoseLog.logD("SEPAL","DIFFR END: " + erepmatch + " " + epatmatch);
   
   if (patmatch >= epatmatch) no_change = true;
   else {
      patch_contents = patch_contents.substring(patmatch,epatmatch);
      source_position = source_position + repmatch;
      // include any spaces in rpos
      source_length = erepmatch - repmatch;
      RootLocation oloc = getLocation();
      BractFactory bf = BractFactory.getFactory();
      RootLocation loc = bf.createLocation(oloc.getFile(),
            source_position,source_position+source_length,
            oloc.getLineNumber() + rlin,
            oloc.getProject(),oloc.getMethod(),oloc.getPriority());
      setLocation(loc);
      ASTNode pn = getResolvedAstNodeForLocation(loc);
      int epos = source_position+source_length;
      while (pn != null && pn.getStartPosition() + pn.getLength() < epos) {
         pn = pn.getParent();
       }
      if (pn == null) {
         patch_node = getResolvedAstNodeForLocation(loc);
       }
      else patch_node = pn;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Get Description for patch                                               */
/*                                                                              */
/********************************************************************************/

private void getDescription(String bcnts)
{
   patch_description = null;
   
   // get relevant node and node type
   String what = null;
   ASTNode usenode = patch_node;
   ASTNode prevnode = usenode;
   
   while (usenode != null && (usenode instanceof Expression || usenode instanceof Type)) {
      prevnode = usenode;
      switch (usenode.getNodeType()) {
         case ASTNode.METHOD_INVOCATION :
         case ASTNode.CONSTRUCTOR_INVOCATION :
         case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
            what = "Call";
            break;
         case ASTNode.CLASS_INSTANCE_CREATION :
            what = "New";
            break;
         case ASTNode.LAMBDA_EXPRESSION :
            what = "Lambda";
            break;
         case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
            what = "Declaration";
            break;
         default :
            break;
       }
      if (what != null) break;
      usenode = usenode.getParent();
    }
   
   // now get original text and patched text
// int pstart = patch_node.getStartPosition();
// int plen = patch_node.getLength();
   int pstart1 = prevnode.getStartPosition();
   int plen1 = prevnode.getLength();
   String origtxt = bcnts.substring(pstart1,pstart1+plen1);
   String txt0 = origtxt.substring(0,source_position-pstart1);
   String txt1 = patch_contents;
   String txt2 = origtxt.substring(source_position-pstart1+source_length);
// if (source_length > 0) {
//    txt2 = origtxt.substring(pstart - pstart1 + source_length,plen1);
//  }
// else {
//    txt2 = "";
//  }
   String newtxt = txt0 + txt1 + txt2;
   
   origtxt = IvyFormat.formatString(compress(origtxt));
   newtxt = IvyFormat.formatString(compress(newtxt));
   
   patch_description = "G: Use " + newtxt + " instead of " + trunc(origtxt,24);
}


private String trunc(String txt,int len)
{
   if (txt.length() < len) return txt;
   return txt.substring(0,len-2) + "..";
}


private String compress(String text)
{
   StringBuffer buf = new StringBuffer();
   char havespace = 0;
   for (int i = 0; i < text.length(); ++i) {
      char ch = text.charAt(i);
      if (Character.isWhitespace(ch)) {
         if (havespace == 0) {
            havespace = ch;
          }
       }
      else {
         if (havespace != 0) buf.append(havespace);
         havespace = 0;
         buf.append(ch);
       }
    }
   
   return buf.toString();
}




private String checkIfStatement(String patch)
{
   int idx1 = patch.indexOf("(");
   int lvl = 0;
   int idx2 = -1;
   boolean needparen = false;
   for (int i = idx1; i < patch.length(); ++i) {
      char ch = patch.charAt(i);
      if (Character.isWhitespace(ch)) ;
      else if (ch == '(') ++lvl;
      else if (ch == ')' && lvl > 0) {
         --lvl;
         if (lvl == 0) idx2 = i+1;
       }
      else if (ch == ')') break;
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
   else if (idx2 == patch.length()) {
      // if (...) 
      // Need to extract condition and map patch node to expression
      IfStatement ifstmt = (IfStatement) patch_node;
      patch_node = ifstmt.getExpression();
      patch = patch.substring(idx1+1,idx2-1);
      
      // handle start of if here
    }
   return patch;
}




/********************************************************************************/
/*                                                                              */
/*      Treat patch as code to insert after given statement                     */
/*                                                                              */
/********************************************************************************/

private void checkInsertAfter(String bcnts)
{
   ASTNode stmt = patch_node;
   String patch = patch_contents;
   if (!(stmt instanceof Statement)) return;
   if (stmt instanceof ReturnStatement) return;
   if (!(stmt.getParent() instanceof Block)) return;
   ASTNode patchast = JcompAst.parseStatement(patch,true);
   if (patchast == null) return;
   if (patchast.getClass() == stmt.getClass()) {
      if (patchast instanceof ExpressionStatement && stmt instanceof ExpressionStatement) {
         ExpressionStatement esp = (ExpressionStatement) patchast;
         ExpressionStatement ssp = (ExpressionStatement) stmt;
         if (esp.getExpression() instanceof Assignment &&
               ssp.getExpression() instanceof Assignment) {
            Assignment pass = (Assignment) esp.getExpression();
            Assignment sass = (Assignment) ssp.getExpression();
            if (pass.getLeftHandSide().toString().equals(sass.getLeftHandSide().toString()))
               return;
          }
         else return;
       }
      else return;
    }
   
   String tp = patch.trim();
   if (!tp.endsWith(";") && !tp.endsWith("}")) {
      String xp = patchast.toString();
      if (xp.trim().endsWith(";")) patch += ";";
    }
   
   // if the statements and patch are close, then skip
   int epos = stmt.getStartPosition() + stmt.getLength();
   int xepos;
   int lfct = 0;
   for (xepos = epos; xepos < bcnts.length(); ++xepos) {
      char ch = bcnts.charAt(xepos);
      if (!Character.isWhitespace(ch)) break;
      if (ch == '\n' && lfct++ > 0) break;
    }
   String white = bcnts.substring(epos,xepos);
   if (white.isEmpty()) white = "\n   ";
   String insert = patch+white;
   
   String insdesc = IvyFormat.formatString(compress(patch));
   String after = IvyFormat.formatString(compress(stmt.toString()));
   String desc = "Insert " + insdesc + " after " + trunc(after,24);
   
   File bfile = getLocation().getFile();
   RootLocation oloc = getLocation();
   BractFactory bf = BractFactory.getFactory();
   CompilationUnit cu = (CompilationUnit) stmt.getRoot();
   int lno = cu.getLineNumber(xepos);
   RootLocation nloc = bf.createLocation(bfile,xepos,xepos,lno,
         oloc.getProject(),oloc.getMethod(),oloc.getPriority());
   TextEdit te = new InsertEdit(xepos,insert);
   RootEdit redit = new RootEdit(bfile,te);
   Element edit = redit.getTextEditXml();
   setLocation (nloc);
   double prob = 0.50;
   // reduce priority for multiline patches
   addRepair(edit,desc,getClass().getName() + "@ChatGPT",prob);
   setLocation(oloc);
}


}       // end of class SepalChatGPT


/* end of SepalChatGPT.java */
