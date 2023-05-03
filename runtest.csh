#! /bin/csh -f

set CP = /pro/rose/java:/pro/ivy/java:/pro/fait/java:/pro/bubbles/java

foreach i (/pro/rose/lib/*.jar)
   set CP = ${CP}:$i
end

foreach i (asm json junit jsoup mysql postgresql)
   set CP = ${CP}:/pro/ivy/lib/${i}.jar
end

foreach i (/pro/ivy/lib/eclipsejar/*.jar)
   set CP = ${CP}:$i
end

foreach i (poppy cocker)
   set CP = ${CP}:/pro/bubbles/lib/${i}.jar
end

set CP = ${CP}:/pro/rose/resources

set WHAT = $*

if ( X$WHAT == X ) then
   set WHAT = ( test01 )
endif

java --version
echo $CP


foreach i ( $WHAT )
   echo Work on $i
   java -Xmx16000m -cp ${CP} edu.brown.cs.rose.roseeval.RoseEvalRunner -RUN $i |& tee ~/RoseEval/$i.out
end


