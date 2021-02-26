/********************************************************************************/
/*                                                                              */
/*              BushIndex.java                                                  */
/*                                                                              */
/*      Start and maintain cocker database during bubbles session               */
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



package edu.brown.cs.rose.bush;

import java.io.File;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaFileHandler;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.leash.LeashIndex;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.rose.root.RootConstants;

class BushIndex implements BushConstants, RootConstants 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean         did_start;
private LeashIndex      cocker_index;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BushIndex()
{
   did_start = false;
   cocker_index = null;
}



/********************************************************************************/
/*                                                                              */
/*      Start the database if necessary                                         */
/*                                                                              */
/********************************************************************************/

void start()
{
   File bdir = BoardSetup.getBubblesWorkingDirectory();
   File cdir = new File(bdir,"CockerIndex");
   if (!cdir.exists()) cdir.mkdirs();
   BoardLog.logD("BUSH","Start COCKER with " + cdir);
   
   if (!cdir.exists()) return;
   
   cocker_index = new LeashIndex(ROSE_PROJECT_INDEX_TYPE,cdir);
   if (cocker_index.isActive()) did_start = false;
   else did_start = cocker_index.start();
   
   BoardLog.logD("BUSH","Cocker started " + did_start + " " + cocker_index.isActive());
   if (!cocker_index.isActive()) return;
   
   if (did_start) {
      Runtime.getRuntime().addShutdownHook(new StopOnExit());
      addInitialComponents();
    }
   BudaRoot.addFileHandler(new FileHandler());
}



/********************************************************************************/
/*                                                                              */
/*      Setup initial paths                                                     */
/*                                                                              */
/*************`*******************************************************************/

private void addInitialComponents()
{
   List<File> active = cocker_index.getTopFiles();
   
   BumpClient bc = BumpClient.getBump();
   Element xml = bc.getAllProjects();
   if (xml == null) return;
   for (Element pe : IvyXml.children(xml,"PROJECT")) {
      String pnm = IvyXml.getAttrString(pe,"NAME");
      Element pxml = bc.getProjectData(pnm,false,true,false,false,false);
      if (pxml == null) continue;
      Element cpxml = IvyXml.getChild(pxml,"CLASSPATH");
      for (Element rpe : IvyXml.children(cpxml,"PATH")) {
         String ptyp = IvyXml.getAttrString(rpe,"TYPE");
         if (ptyp.equals("SOURCE")) {
            String path = IvyXml.getTextElement(rpe,"SOURCE");
            File pfile = new File(path);
            pfile = pfile.getAbsoluteFile();
            if (active.contains(pfile)) {
               active.remove(pfile);
             }
            else {
               if (pfile.exists()) {
                  BoardLog.logI("BUSH","Monitor " + pfile);
                  cocker_index.monitor(pfile);
                }
             }
          }
       }
    }
   for (File f : active) {
      BoardLog.logI("BUSH","Unmonitor " + f);
      cocker_index.unmonitor(f);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Handle file updates                                                     */
/*                                                                              */
/********************************************************************************/


private class FileHandler implements BudaFileHandler {
   
   public void handleSaveRequest()                      { }
   public void handleCommitRequest()                    { }
   public void handlePropertyChange()                   { }
   public void handleCheckpointRequest()                { }
   public boolean handleQuitRequest()                   { return true; }
   
   public void handleSaveDone() {
      CockerUpdater upd = new CockerUpdater();
      BoardThreadPool.start(upd);
    }
   
   
}       // end of inner class FileHandler



private class CockerUpdater implements Runnable {
   
   @Override public void run() {
      cocker_index.update();
    }
}


/********************************************************************************/
/*                                                                              */
/*      Hanldle exit                                                            */
/*                                                                              */
/********************************************************************************/

private class StopOnExit extends Thread {
   
   @Override public void run() {
      cocker_index.stop();
    }
   
}       // end of inner class StopOnExit




}       // end of class BushIndex




/* end of BushIndex.java */

