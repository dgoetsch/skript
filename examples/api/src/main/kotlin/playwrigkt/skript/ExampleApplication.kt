package playwrigkt.skript

import playwrigkt.skript.application.ApplicationRegistry
import playwrigkt.skript.application.ExampleApplicationLoader
import playwrigkt.skript.application.SkriptApplicationLoader
import playwrigkt.skript.application.loadApplication
import playwrigkt.skript.ex.lift
import playwrigkt.skript.http.Http
import playwrigkt.skript.http.server.HttpServer
import playwrigkt.skript.produktion.Produktion
import playwrigkt.skript.queue.QueueMessage
import playwrigkt.skript.result.AsyncResult
import playwrigkt.skript.stagemanager.ApplicationStageManager
import playwrigkt.skript.stagemanager.SyncJacksonSerializeStageManager
import playwrigkt.skript.troupe.ApplicationTroupe
import playwrigkt.skript.troupe.SyncFileTroupe
import playwrigkt.skript.user.http.createUserHttpEndpointSkript
import playwrigkt.skript.user.http.getUserHttpEndpointSkript
import playwrigkt.skript.user.http.loginUserHttpEndpointSkript
import playwrigkt.skript.venue.HttpServerVenue
import playwrigkt.skript.venue.QueueVenue

fun createApplication(configFile: String): AsyncResult<ExampleApplication> {
    val loader = SkriptApplicationLoader(SyncFileTroupe, SyncJacksonSerializeStageManager().hireTroupe(), ApplicationRegistry())

    return loadApplication
            .run(configFile, loader)
            .map { it.applicationResources.get(ExampleApplicationLoader.name()) }
            .map { it as ExampleApplication }
}

data class ExampleApplication(val stageManager: ApplicationStageManager,
                              val httpServerVenue: HttpServerVenue,
                              val queueVenue: QueueVenue) {
    companion object {
        val userCreatedAddress = "user.updated"
        val userLoginAddress = "user.login"
    }

    fun loadHttpProduktions(): AsyncResult<List<Produktion>> =
        listOf(createUserHttpProduktion(),
                loginuserHttpProduktion(),
                getUserHttpProduktion())
                .lift()

    fun queueConsumerProduktion(queue: String, skript: Skript<QueueMessage, Unit, ApplicationTroupe>): AsyncResult<out Produktion> =
        queueVenue.produktion(skript, stageManager, queue)

    fun teardown(): AsyncResult<List<Unit>> =
        listOf(stageManager.tearDown(), httpServerVenue.teardown(), queueVenue.teardown())
                .lift()

    private fun createUserHttpProduktion() =
            httpServerVenue.produktion(
                    createUserHttpEndpointSkript,
                    stageManager,
                    HttpServer.Endpoint(
                            "/users",
                            emptyMap(),
                            Http.Method.Post))

    private fun loginuserHttpProduktion() =
            httpServerVenue.produktion(
                    loginUserHttpEndpointSkript,
                    stageManager,
                    HttpServer.Endpoint(
                            "/login",
                            emptyMap(),
                            Http.Method.Post))

    private fun getUserHttpProduktion() =
            httpServerVenue.produktion(
                    getUserHttpEndpointSkript,
                    stageManager,
                    HttpServer.Endpoint(
                            "/users/{userId}",
                            mapOf("Authorization" to emptyList()),
                            Http.Method.Get))
}


