/********************************************************************************/
/*                                                                              */
/*              RoseEvalSolution.java                                           */
/*                                                                              */
/*      Representation of a evaluation solution to check                        */
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



package edu.brown.cs.rose.roseeval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class RoseEvalSolution implements RoseEvalConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<Integer>    line_set;
private String          patch_file;
private List<String>    match_strings;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RoseEvalSolution(Element xml) 
{
   patch_file = IvyXml.getAttrString(xml,"FILE");
   match_strings = new ArrayList<>();
   
   for (Element melt : IvyXml.children(xml,"MATCH")) {
      String m = IvyXml.getText(melt);
      if (m != null) match_strings.add(m);
    }
   if (match_strings.isEmpty()) {
      String m = IvyXml.getTextElement(xml,"MATCH");
      if (m != null) match_strings.add(m);
    }
   
   String lns = IvyXml.getAttrString(xml,"LINE");
   if (lns == null) lns = IvyXml.getAttrString(xml,"LINES");
   line_set = parseLines(lns);
}


RoseEvalSolution(String expect)
{
   String [] elts = expect.split("@");
   String find = elts[1];
   line_set = parseLines(elts[0]);
   patch_file = null;
   if (elts.length > 2) patch_file = elts[2];
   match_strings = new ArrayList<>();
   match_strings.add(find);
}




private static Set<Integer> parseLines(String cnts)
{
   if (cnts == null) return null;
   
   Set<Integer> rslt = new HashSet<>();
   StringTokenizer tok = new StringTokenizer(cnts,",-",true);
   int last = -1;
   int from = -1;
   while (tok.hasMoreTokens()) {
      String s = tok.nextToken();
      if (Character.isDigit(s.charAt(0))) {
         int v = Integer.parseInt(s);
         if (from > 0) {
            for (int i = from+1; i <= v; ++i) {
               rslt.add(i);
             }
          }
         else {
            rslt.add(v);
            last = v;
          }
         from = -1;
       }
      else if (s.equals("-")) from = last;
      else from = -1;
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Matching routines                                                       */
/*                                                                              */
/********************************************************************************/

boolean match(RoseEvalSuggestion sug)
{
   if (!line_set.isEmpty() && !line_set.contains(sug.getLine())) return false;
   
   if (patch_file != null && !sug.getFile().contains(patch_file)) return false;
   
   if (match_strings != null && !match_strings.isEmpty()) {
      for (String s : match_strings) {
         int idx = sug.getDescription().indexOf(s);
         if (idx >= 0) return true;
       }
      return false;
    }
   
   return true;
}


}       // end of class RoseEvalSolution




/* end of RoseEvalSolution.java */

