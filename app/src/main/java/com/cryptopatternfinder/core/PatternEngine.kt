package com.cryptopatternfinder.core
import kotlin.math.sqrt

object PatternEngine {
    private fun window(r:List<Observation>,s:String,d:java.time.DayOfWeek,m:Int,t:Int)=
        r.filter { it.symbol==s && it.weekday==d && kotlin.math.abs(it.minuteOfDay-m)<=t }
         .sortedBy { it.observedAt }

    private fun cosine(a:List<Double>,b:List<Double>):Double {
        val n=minOf(a.size,b.size)
        if(n==0) return 0.0
        val x=a.takeLast(n); val y=b.takeLast(n)
        val dot=x.zip(y).sumOf { it.first*it.second }
        val nx=sqrt(x.sumOf { it*it }); val ny=sqrt(y.sumOf { it*it })
        return if(nx==0.0 || ny==0.0) 0.0 else dot/(nx*ny)
    }

    fun similarSymbols(
        r:List<Observation>, target:String, d:java.time.DayOfWeek, m:Int,
        tolerance:Int=15, minimum:Int=3
    ):List<SimilarityResult> {
        val a=window(r,target,d,m,tolerance).map { it.changePercent }
        if(a.size<minimum) return emptyList()
        return r.map { it.symbol }.distinct().filter { it!=target }.mapNotNull { s ->
            val rows=window(r,s,d,m,tolerance)
            if(rows.size<minimum) return@mapNotNull null
            val b=rows.map { it.changePercent }
            val n=minOf(a.size,b.size)
            val x=a.takeLast(n); val y=b.takeLast(n)
            val dir=x.zip(y).count { (p,q) -> (p>0)==(q>0) }.toDouble()/n
            val score=.65*((cosine(x,y)+1.0)/2.0)+.35*dir
            SimilarityResult(s,score*100,dir*100,n)
        }.sortedByDescending { it.scorePercent }
    }

    fun recurringPatterns(r:List<Observation>, minimumOccurrences:Int=3):List<RecurringPattern> =
        r.groupBy { Triple(it.symbol,it.weekday,it.minuteOfDay/30) }
         .mapNotNull { (k,v) ->
             if(v.size<minimumOccurrences) return@mapNotNull null
             val counts=Direction.values().associateWith { d -> v.count { it.direction==d } }
             val dom=counts.maxBy { it.value }
             RecurringPattern(k.first,k.second,k.third*30,v.size,dom.key,dom.value*100.0/v.size)
         }
         .sortedWith(compareByDescending<RecurringPattern>{it.consistencyPercent}
             .thenByDescending{it.occurrences})
}
