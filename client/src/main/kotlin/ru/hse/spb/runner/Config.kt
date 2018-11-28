package ru.hse.spb.runner

sealed class Config(val x: Int, val label: String)

class NConfig(val nRange: IntProgression, val m: Int, val delta: Int, x: Int) : Config(x, "Array size") {
    override fun toString(): String = "$label: from ${nRange.first} to ${nRange.last} with step ${nRange.step}\n" +
    "Number of clients: $m\n" +
    "Delta (s): $delta\n"
}

class MConfig(val n: Int, val mRange: IntProgression, val delta: Int, x: Int) : Config(x, "Number of clients") {
    override fun toString(): String = "Array size: $n\n" +
            "$label: from ${mRange.first} to ${mRange.last} with step ${mRange.step}\n" +
            "Delta (s): $delta\n"
}
class DeltaConfig(val n: Int, val m: Int, val deltaRange: IntProgression, x: Int) : Config(x, "Delta (s)") {
    override fun toString(): String = "Array size: $n\n" +
            "Number of clients: $m\n" +
            "$label: from ${deltaRange.first} to ${deltaRange.last} with step ${deltaRange.step}\n"
}