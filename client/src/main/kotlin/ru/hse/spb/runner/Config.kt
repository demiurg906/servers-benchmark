package ru.hse.spb.runner

sealed class Config(val x: Int, val label: String) {
    abstract fun toFilename(): String
}

class NConfig(val nRange: IntProgression, val m: Int, val delta: Int, x: Int) : Config(x, "Array size") {
    override fun toString(): String = "$label: from ${nRange.first} to ${nRange.last} with step ${nRange.step}\n" +
    "Number of clients: $m\n" +
    "Delta (ms): $delta\n"

    override fun toFilename(): String = "N_${nRange.first}_${nRange.last}__M_${m}__Delta_${delta.toSeconds()}s"
}

class MConfig(val n: Int, val mRange: IntProgression, val delta: Int, x: Int) : Config(x, "Number of clients") {
    override fun toString(): String = "Array size: $n\n" +
            "$label: from ${mRange.first} to ${mRange.last} with step ${mRange.step}\n" +
            "Delta (ms): $delta\n"

    override fun toFilename(): String = "N_${n}__M_${mRange.first}_${mRange.last}__Delta_${delta.toSeconds()}s"
}

class DeltaConfig(val n: Int, val m: Int, val deltaRange: IntProgression, x: Int) : Config(x, "Delta (ms)") {
    override fun toString(): String = "Array size: $n\n" +
            "Number of clients: $m\n" +
            "$label: from ${deltaRange.first} ms to ${deltaRange.last} ms with step ${deltaRange.step} ms\n"

    override fun toFilename(): String = "N_${n}__M_${m}__Delta_${deltaRange.first.toSeconds()}_${deltaRange.last.toSeconds()}s"
}

private fun Int.toSeconds(): Double = this.toDouble() / 1000