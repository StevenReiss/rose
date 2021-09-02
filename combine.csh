#! /bin/csh -f

set WHAT = $*

if ( X$WHAT == X ) then
   set WHAT = ( quix chart lang math3 math time )
endif

set CATS = ( )
set hdr =
set R = ~/RoseEval/Summary.csv
set S = ~/RoseEval/Summary.out
rm $S
touch $S

foreach i ( $WHAT )
   set x = `ls ~/RoseEval/ROSEEVAL_*${i}*.csv | tail -1`
   set CATS = ( $CATS $x )
   if ( X$hdr == X ) then
      cat $x | head -1 >&! $R
      set hdr = x
   endif
   cat ~/RoseEval/$i.out >> $S
end

foreach i ( $CATS )
   sed '1d' $i >> $R
end

cat ~/RoseEval/Summary.csv
