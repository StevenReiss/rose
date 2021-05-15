/********************************************************************************/
/*                                                                              */
/*              RootMetrics.java                                                */
/*                                                                              */
/*      Calls to generate data for learning and analysis of the use of ROSE     */
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



package edu.brown.cs.rose.root;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardUpload;
import edu.brown.cs.ivy.exec.IvyExecQuery;

public class RootMetrics implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean         collect_commands;
private List<String>    command_data;

private static String   user_id;
private static String   run_id;


static {
   BoardProperties bp = BoardProperties.getProperties("Rose"); user_id = bp.getProperty("Rose.user.id");
   if (user_id == null) {
      user_id = getUserId();
      bp.setProperty("Rose.user.id",user_id);
      try {
         bp.save();
       }
      catch (IOException e) { }
    }
   run_id = Integer.toString(new Random().nextInt(1000000));
}


private static RootMetrics      the_metrics = new RootMetrics();



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private RootMetrics()
{
   collect_commands = true;
   command_data = new ArrayList<>();
   
   if (user_id != null && run_id != null) {
      Runtime.getRuntime().addShutdownHook(new DumpData());
    }
}



/********************************************************************************/
/*                                                                              */
/*      Public interface for commands                                           */
/*                                                                              */
/********************************************************************************/

public static void noteCommand(String src,String cmd,Object ... args)
{
   RoseLog.logD("ROOT","COMMAND: " + cmd + "@" + src + ":" + Arrays.toString(args));
   
   if (!the_metrics.collect_commands) return;
   
   StringBuffer buf = new StringBuffer();
   buf.append(src);
   buf.append(",");
   buf.append(cmd);
   for (Object o : args) {
      buf.append(",");
      buf.append(String.valueOf(o));
    }
   buf.append(",");
   buf.append(System.currentTimeMillis());
   
   the_metrics.saveCommand(buf.toString());
}



/********************************************************************************/
/*                                                                              */
/*      Command handling methods                                                */
/*                                                                              */
/********************************************************************************/

private void saveCommand(String cmd)
{
   if (command_data == null) return;
   
   synchronized(command_data) {
      command_data.add(cmd);
    }
}


private void dumpCommands()
{
   if (command_data == null || command_data.size() == 0) return;
   
   List<String> oldmcds;
   synchronized(command_data) {
      oldmcds = command_data;
      command_data = new ArrayList<>();
    }
   
   File f = null;
   try {
      f = File.createTempFile("RoseMetrics_COMMANDS_",".csv");
      PrintWriter pw = new PrintWriter(new FileWriter(f));
      for (String s : oldmcds) pw.println(s);
      pw.close();
      sendFile(f,"COMMANDS",true);
    }
   catch (Throwable e) {
      RoseLog.logE("ROOT","Problem writing command file",e);
    }
   finally {
      if (f != null) f.delete();
    }
}



/********************************************************************************/
/*                                                                              */
/*      File sending methods                                                    */
/*                                                                              */
/********************************************************************************/

private void sendFile(File f,String type,boolean delete) throws IOException
{
   new BoardUpload(f,user_id,run_id);
   
   if (delete) f.delete();
}



/********************************************************************************/
/*                                                                              */
/*      Finalization routines                                                   */
/*                                                                              */
/********************************************************************************/

private class DumpData extends Thread {
   
   @Override public void run() {
      dumpCommands();
    }
   
}       // end of inner class DumpData



/********************************************************************************/
/*                                                                              */
/*      Create a user id                                                        */
/*                                                                              */
/********************************************************************************/

private static String getUserId()
{
   String unm = System.getProperty("user.name");
   String hnm = IvyExecQuery.computeHostName();
   if (hnm == null) hnm = IvyExecQuery.getHostName();
   if (unm == null) unm = "USER";
   if (hnm == null) hnm = "HOST";
   
   byte [] drslt;
   
   try {
      MessageDigest mdi = MessageDigest.getInstance("MD5");
      mdi.update(unm.getBytes());
      mdi.update(hnm.getBytes());
      drslt = mdi.digest();
    }
   catch (NoSuchAlgorithmException e) {
      RoseLog.logE("ROOT","Problem creating user name: " + e);
      return unm + "@" + hnm;
    }
   
   int rslt = 0;
   for (int i = 0; i < drslt.length; ++i) {
      int j = i % 4;
      rslt ^= (drslt[i] << (j*8));
    }
   rslt &= 0x7fffffff;
   
   String pfx = "ROSE";
   return pfx + Integer.toString(rslt);
}



}       // end of class RootMetrics




/* end of RootMetrics.java */

