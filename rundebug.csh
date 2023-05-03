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
   set WHAT = ( quix chart lang math3 math time closure mockito )
endif

java --version
echo $CP


java -Xmx16000m -cp ${CP} edu.brown.cs.rose.roseeval.RoseEvalRunner -RX $WHAT |& tee ~/RoseEval/$i.out


