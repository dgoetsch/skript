package playwrigkt.skript

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.funktionale.either.Either
import playwrigkt.skript.ex.andThen
import playwrigkt.skript.ex.join

class SkriptSpec : StringSpec() {
    init {
        "a skript can have no stage" {
            val skript = Skript.map<Int, String, Unit> { it.toString() }
            skript.run(10, Unit).result() shouldBe "10"
        }

        "a skript can be chained" {
            val skript = Skript
                    .map<Int, String, Unit> { it.toString() }
                    .map { it.toLong() * 2 }
            skript.run(10, Unit).result() shouldBe 20L
        }

        "a skript stage can provide properties" {
            data class Config(val appName: String)
            data class ConfigTroupe(val config: Config)

            val skript =
                    Skript.map<Int, String, ConfigTroupe>{ it.toString() }
                            .mapWithTroupe { i, c -> "Application ${c.config.appName} received $i" }
            skript.run(10, ConfigTroupe(Config("Example"))).result() shouldBe "Application Example received 10"
        }

        "A skript can branch based on the result of a skript" {
            val double = Skript.map<Int, Int, Unit> { it * 2 }
            val half = Skript.map<Int, Int, Unit> { it / 2 }
            val rightIfGreaterThanTen = Skript.map<Int, Either<Int, Int>, Unit> {
                if(it > 10) {
                    Either.right(it)
                } else {
                    Either.left(it)
                }
            }

            val skript = Skript.branch(rightIfGreaterThanTen)
                    .left(double)
                    .right(half)

            skript.run(5, Unit).result() shouldBe 10
            skript.run(16, Unit).result() shouldBe 8
        }

        "A skript can transform and branch based on the result of a skript" {
            val double: Skript<Double, Double, Unit> = Skript.map { it * 2 }
            val stringLength = Skript.map<String, Int, Unit> { it.length }
            val toLong: Skript<Number, Long, Unit> = Skript.map { it.toLong() }

            val rightIfGreaterThanTen = Skript.map<Int, Either<Double, String>, Unit> {
                if(it > 10) {
                    Either.right(it.toString())
                } else {
                    Either.left(it.toDouble())
                }
            }

            val skript = Skript.branch(rightIfGreaterThanTen)
                    .left(double.andThen(toLong))
                    .right(stringLength.andThen(toLong))

            skript.run(5, Unit).result() shouldBe 10L
            skript.run(16, Unit).result() shouldBe 2L
        }

        "Two skripts can be run concurrently" {
            val sum: Skript<List<Int>, Int, Unit> = Skript.map { it.sum() }
            val length: Skript<List<*>, Int, Unit> = Skript.map { it.size }

            val average = Skript.both(sum, length).join { sum, length -> sum.toDouble() / length }

            val input = listOf(1, 3, 5, 6, 3)
            average.run(input, Unit).result() shouldBe input.average()
        }

        "A skript can split and join" {
            val initialSkript = Skript.identity<String, Unit>()
            val mapping: Skript<String, Int, Unit> = Skript.map { it.length }

            val skript = initialSkript
                    .split(mapping)
                    .join { str, length -> "$str is $length characters long" }

            skript.run("abcde", Unit).result() shouldBe "abcde is 5 characters long"
        }
    }
}