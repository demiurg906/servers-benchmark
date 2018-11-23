package ru.hse.spb.runner

sealed class Config(val x: Int)

class NConfig(val nRange: IntProgression, val m: Int, val delta: Int, x: Int) : Config(x) {
    override fun toString(): String = "Array size: from ${nRange.first} to ${nRange.last} with step ${nRange.step}\n" +
    "Number of clients: $m\n" +
    "Delta: $delta\n"
}

class MConfig(val n: Int, val mRange: IntProgression, val delta: Int, x: Int) : Config(x) {
    override fun toString(): String = "Array size: $n\n" +
            "Number of clients: from ${mRange.first} to ${mRange.last} with step ${mRange.step}\n" +
            "Delta: $delta\n"
}
class DeltaConfig(val n: Int, val m: Int, val deltaRange: IntProgression, x: Int) : Config(x) {
    override fun toString(): String = "Array size: $n\n" +
            "Number of clients: $m\n" +
            "Delta: from ${deltaRange.first} to ${deltaRange.last} with step ${deltaRange.step}\n"
}