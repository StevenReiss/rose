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

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.rose.bush.BushConstants.BushRepairAdder;

class BushSuggestPanel implements BushConstants, BushRepairAdder
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BushProblem     for_problem;
private BumpLocation    for_location;
private Component       from_panel;
private JPanel          content_pane;
private JList<BushRepair> suggestion_list;
private DefaultListModel<BushRepair> list_model;
private JLabel          suggestions_pending;

private static final String PENDING = "Finding suggestions ...";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BushSuggestPanel(Component src,BushProblem prob,BumpLocation loc)
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
   bba.addBubble(bbl,from_panel,null,BudaConstants.PLACEMENT_LOGICAL);
   bbl.setVisible(true);
   
   return bbl;
}


private JPanel createDisplay()
{
   SwingGridPanel pnl = new SuggestPanel();
   pnl.beginLayout();
   pnl.addBannerLabel("Suggest Repairs for " + for_problem.getDescription());
   if (for_location != null) {
      pnl.addSectionLabel("At location " + for_location.toString());
    }
   pnl.addSeparator();
   
   suggestions_pending = new JLabel(PENDING);
   pnl.addLabellessRawComponent("PENBDING",suggestions_pending);
   list_model = new DefaultListModel<>();
   suggestion_list = new JList<>(list_model);
   pnl.addLabellessRawComponent("REPAIRS",suggestion_list);
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

}	// end of inner class PanelBubble

}       // end of class BushSuggestPanel




/* end of BushSuggestPanel.java */

