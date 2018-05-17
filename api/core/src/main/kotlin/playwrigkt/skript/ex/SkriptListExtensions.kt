package playwrigkt.skript.ex

import playwrigkt.skript.Skript

fun <I, O, O2, Troupe> Skript<I, List<O>, Troupe>.mapList(skript: Skript<O, O2, Troupe>): Skript<I, List<O2>, Troupe> =
        this.compose(Skript.SkriptList(skript))