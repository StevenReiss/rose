/********************************************************************************/
/*                                                                              */
/*              BushSuggestPanel.java                                           */
/*                                                                              */
/*     Panel and bubble to show repair suggestions                              */
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.rose.bush.BushConstants.BushRepairAdder;
import edu.brown.cs.rose.root.RootEdit;
import edu.brown.cs.rose.root.RootLocation;

class BushSuggestPanel implements BushConstants, BushRepairAdder
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BushProblem     for_problem;
private BushLocation    for_location;
private Component       from_panel;
private JPanel          content_pane;
private SuggestList     suggestion_list;
private SuggestListModel list_model;
private JLabel          suggestions_pending;

private static boolean do_preview = false;

private static final String PENDING = "Finding suggestions ...";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BushSuggestPanel(Component src,BushProblem prob,BushLocation loc)
{
   from_panel = src;
   for_problem = prob;
   for_location = loc;
   content_pane = null;
}




/********************************************************************************/
/*                                                                              */
/*      Create the actual bubble                                                */
/*                                                                              */
/********************************************************************************/

BudaBubble createBubble()
{
   content_pane = createDisplay();
   
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(from_panel);
   BudaBubble bbl = new PanelBubble(content_pane);
   bba.addBubble(bbl,from_panel,null,
         BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_BELOW);
   bbl.setVisible(true);
   
   return bbl;
}


private JPanel createDisplay()
{
   SwingGridPanel pnl = new SuggestPanel();
   pnl.beginLayout();
   pnl.addBannerLabel("Suggest Repairs for " + for_problem.getDescription());
   RootLocation loc = for_problem.getBugLocation();
   if (loc != null) {
      pnl.addSectionLabel("   At " + loc.getLineNumber() + " in " +
            loc.getMethod());
    }
   if (for_location != null) {
      pnl.addSectionLabel("Fix " + for_location.getLineNumber() + " in " +
            for_location.getMethod());
    }
   pnl.addSeparator();
   
   suggestions_pending = new JLabel(PENDING);
   pnl.addLabellessRawComponent("PENDING",suggestions_pending);
   list_model = new SuggestListModel();
   suggestion_list = new SuggestList(list_model);
   pnl.addLabellessRawComponent("REPAIRS",new JScrollPane(suggestion_list));
   suggestion_list.setVisible(false);
   
   return pnl;
}



/********************************************************************************/
/*                                                                              */
/*      Handle repairs                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void addRepair(BushRepair repair)
{ 
   if (repair == null) return;
   
   int sz = list_model.getSize();
   if (sz == 0) {
      suggestions_pending.setVisible(false);
      suggestion_list.setVisible(true);
    }
   
   list_model.addElement(repair);
   BudaBubble bbl = BudaRoot.findBudaBubble(suggestion_list);
   Dimension d1 = bbl.getPreferredSize();
   bbl.setSize(d1);
   
   BoardLog.logD("BUSH","Suggest size " + list_model.getSize() + " " +
         suggestion_list.getVisibleRowCount() + " " + d1);
   
}


@Override public void doneRepairs()
{ 
   int sz = list_model.getSize();
   if (sz == 0) {
      suggestions_pending.setText("No suggestions found");
    }
}


/********************************************************************************/
/*                                                                              */
/*      Main panel                                                              */
/*                                                                              */
/********************************************************************************/

private class SuggestPanel extends SwingGridPanel {

   private static final long serialVersionUID = 1;
   
   SuggestPanel() {
      setBackground(BoardColors.getColor("Rose.background.color"));
      setOpaque(true);
    }
   
}	// end of inner class SimplePanel


/********************************************************************************/
/*                                                                              */
/*      Bubble for the panel                                                    */
/*                                                                              */
/********************************************************************************/

private class PanelBubble extends BudaBubble {

   private static final long serialVersionUID = 1;
   
   PanelBubble(Component cnts) {
      setContentPane(cnts);
    }
   
   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();
      
      Point p0 = SwingUtilities.convertPoint(this,e.getPoint(),suggestion_list);
      int row = suggestion_list.locationToIndex(p0);
      Rectangle r0 = suggestion_list.getCellBounds(row,row);
     
      if (r0.contains(p0)) {
         BushRepair br = list_model.getElementAt(row);
         if (br != null) {
            if (do_preview) menu.add(new PreviewAction(br));
            menu.add(new RepairAction(br));
            menu.add(new SourceAction(br));
          }
       }
      
      menu.add(getFloatBubbleAction());
      
      menu.show(this,p0.x,p0.y-5);
    }

}	// end of inner class PanelBubble



/********************************************************************************/
/*                                                                              */
/*      Our list widget                                                         */
/*                                                                              */
/********************************************************************************/

private class SuggestList extends JList<BushRepair> {
   
   private SuggestRenderer suggest_renderer;
   private static final long serialVersionUID = 1;
   
   SuggestList(ListModel<BushRepair> mdl) {
      super(mdl);
      suggest_renderer = null;
      setVisibleRowCount(1);
      addMouseListener(new SuggestMouser());
    }
   
   @Override public SuggestRenderer getCellRenderer() {
      if (suggest_renderer == null) {
         suggest_renderer = new SuggestRenderer();
       }
      return suggest_renderer;
    }
   
}       // end of inner class SuggestList
   
   
private class SuggestRenderer implements ListCellRenderer<BushRepair> {
   
   private DefaultListCellRenderer base_renderer;
   
   SuggestRenderer() {
      base_renderer = new DefaultListCellRenderer();
    }
   
   @Override public Component getListCellRendererComponent(JList<? extends BushRepair> l,
         BushRepair r,int idx,boolean sel,boolean foc) {
      String desc = r.getDescription();
      RootLocation loc = r.getLocation();
      String cnts = "<html>" + desc + "<p>";
      cnts += "&nbsp;&nbsp;&nbsp;At " + loc.getLineNumber() + ":" + loc.getMethod();
      cnts += " (" + IvyFormat.formatNumber(r.getValidatedPriority()) + ")";
      Component c = base_renderer.getListCellRendererComponent(l,cnts,idx,sel,foc);
      
      return c;
    }
   
}       // end of inner class SuggestRenderer




private class SuggestMouser extends MouseAdapter {
   
   @Override public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
         int index = suggestion_list.locationToIndex(e.getPoint());
         BushRepair br = list_model.getElementAt(index);
         if (br != null) {
            SourceAction act = new SourceAction(br);
            act.actionPerformed(null);
          }
       }
    }
   
}       // end of inner class SuggestMouser



/********************************************************************************/
/*                                                                              */
/*      Sorted list model                                                       */
/*                                                                              */
/********************************************************************************/

private class SuggestListModel extends DefaultListModel<BushRepair> {
   
   private static final long serialVersionUID = 1;
   
   @Override public synchronized void addElement(BushRepair r) {
      int min = 0;
      int max = getSize()-1;
      while (min <= max) {
         int mid = (min+max)/2;
         BushRepair r1 = elementAt(mid);
         if (r1.getValidatedPriority() >= r.getValidatedPriority()) {
            min = mid+1;
          }
         else {
            max = mid-1;
          }
       }
      
      BoardLog.logD("BUSH","Add repair " + min + " " + getSize() + " " + r.getDescription());
      
      add(min,r);
      
      int sz = Math.min(getSize(),10);
      
      BoardLog.logD("BUSH","Set row count to " + sz);
      
      suggestion_list.setVisibleRowCount(sz+1);
      
    }
   
}       // end of inner class SuggestListModel



/********************************************************************************/
/*                                                                              */
/*      Suggestion actions                                                      */
/*                                                                              */
/********************************************************************************/

private class PreviewAction extends AbstractAction {
   
   private BushRepair for_repair;
   private static final long serialVersionUID = 1;
   
   PreviewAction(BushRepair r) {
      super("Preview repair " + r.getDescription());
      for_repair = r;
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      // metrics("PREVIEW",...
      BoardLog.logD("BUSH","PREVIEW REPAIR " + for_repair.getDescription());
   
    }
   
}       // end of inner class PreviewAction


private class RepairAction extends AbstractAction {

   private BushRepair for_repair;
   private static final long serialVersionUID = 1; 
   
   RepairAction(BushRepair r) {
      super("Make repair " + r.getDescription());
      for_repair = r;
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      // BushFactory.metrics("REPAIR",...
      RootEdit redit = for_repair.getEdit();
      BoardLog.logD("BUSH","MAKE REPAIR " + for_repair.getDescription() + " " + redit.getFile() + " " + redit.getTextEdit());
      boolean fg = BaleFactory.getFactory().applyEdits(redit.getFile(),redit.getTextEdit());
      if (!fg) {
         BoardLog.logD("BUSH","EDIT WITH FILE FAILED");
         BaleFactory.getFactory().applyEdits(redit.getTextEdit());
       }
    }
   
}       // end of inner class RepairAction



private class SourceAction extends AbstractAction implements Runnable {
   
   private BushRepair for_repair;
   private BudaBubble source_bubble;
   private static final long serialVersionUID = 1;
   
   SourceAction(BushRepair r) {
      super ("Show source for " + r.getDescription());
      for_repair = r;
      source_bubble = null;
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      if (for_repair == null) return;
      BoardMetrics.noteCommand("BUSH","GotoSuggestSource");
      // BushFactory.metrics("SOURCE",...
      BushFactory.metrics("GotoSuggestSource",for_repair.getDescription());
      BushLocation loc = (BushLocation) for_repair.getLocation();
      if (loc == null) loc = for_location;
      if (loc == null) return;
      BoardLog.logD("BUSH","Go to source for " + loc + " " + loc.getBumpLocation());
      String proj = loc.getProject();
      BumpLocation bloc = loc.getBumpLocation();
      String fct = loc.getMethod();
      if (bloc != null) fct = bloc.getKey();
      int idx = fct.indexOf("(");
      if (idx > 0) {
         String f1 = fct.substring(0,idx);
         String args = fct.substring(idx+1);
         int idx1 = args.lastIndexOf(")");
         if (idx1 > 0) args = args.substring(0,idx1);
         String a1 = IvyFormat.formatTypeNames(args,",");
         fct = f1 + "(" + a1 + ")";
       }
      BoardLog.logD("BUSH","Source request " + fct);
      source_bubble = BaleFactory.getFactory().createMethodBubble(proj,fct);
      if (source_bubble != null) {
         SwingUtilities.invokeLater(this);
       }
    }
   
   @Override public void run() {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(content_pane);
      BudaBubble bbl = BudaRoot.findBudaBubble(content_pane);
      bba.addBubble(source_bubble,bbl,null,
            BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_GROUPED|BudaConstants.PLACEMENT_MOVETO);
   }
   
}       // end of inner class SourceAction






}       // end of class BushSuggestPanel




/* end of BushSuggestPanel.java */

