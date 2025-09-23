/********************************************************************************/
/*                                                                              */
/*              RootLineMap.java                                                */
/*                                                                              */
/*      Representing of mapping of lines before and after an edit               */
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
import java.util.HashMap;
import java.util.Map;

public class RootLineMap implements RootConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File            for_file;
private Map<Integer,Integer> known_lines;
private int             max_line;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RootLineMap(File file,int... pos)
{
   for_file = file;
   known_lines = new HashMap<>();
   max_line = -1;
   for (int i = 0; i < pos.length-1; ++i) {
      addMap(pos[i],pos[i+1]);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Add to mapping                                                          */
/*                                                                              */
/********************************************************************************/

void addMap(int orig,int after) 
{
   Integer ivl = known_lines.get(orig);
   if (ivl != null) after = Math.min(ivl,after);
   known_lines.put(orig,after);
   if (orig > max_line) max_line = orig;
}


/********************************************************************************/
/*                                                                              */
/*      Get edited line nubmer                                                  */
/*                                                                              */
/********************************************************************************/

int getEditedLine(File f,int orig)
{
   if (!f.equals(for_file)) return orig;
   Integer v = known_lines.get(orig);
   if (v != null) return v;
   if (orig > max_line && max_line > 0) {
      Integer dv = known_lines.get(max_line);
      return orig + (dv-max_line);
    }
   return orig;
}



}       // end of class RootLineMap




/* end of RootLineMap.java */

