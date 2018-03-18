package playwright.skript.common

import playwright.skript.Skript
import playwright.skript.performer.PublishPerformer
import playwright.skript.performer.SQLPerformer
import playwright.skript.performer.SerializePerformer
import playwright.skript.result.AsyncResult
import playwright.skript.stage.PublishStage
import playwright.skript.stage.SQLStage
import playwright.skript.stage.SerializeStage
import playwright.skript.venue.Venue

class ApplicationVenue(
        val publishProvider: Venue<PublishPerformer>,
        val sqlProvider: Venue<SQLPerformer>,
        val serializeProvider: Venue<SerializePerformer>
): Venue<ApplicationStage> {
    override fun provideStage(): AsyncResult<ApplicationStage> {
        return sqlProvider.provideStage().flatMap { sqlPerformer ->
                    publishProvider.provideStage().flatMap { publishPerformer ->
                        serializeProvider.provideStage().map { serializePerformer ->
                            ApplicationStage(publishPerformer, sqlPerformer, serializePerformer)
                        }
                    }
                }
    }

    fun <I, O> runOnContext(skript: Skript<I, O, ApplicationStage>, i: I): AsyncResult<O> {
        return provideStage()
                .flatMap { skript.run(i, it) }
    }
}

class ApplicationStage(
        private val publishPerformer: PublishPerformer,
        private val sqlPerformer: SQLPerformer,
        private val serializePerformer: SerializePerformer):
        PublishStage<PublishPerformer>,
        SQLStage<SQLPerformer>,
        SerializeStage<SerializePerformer> {
    override fun getPublishPerformer(): PublishPerformer = publishPerformer
    override fun getSerializePerformer(): SerializePerformer = serializePerformer
    override fun getSQLPerformer(): SQLPerformer = sqlPerformer
}