package playwrigkt.skript.application

import org.funktionale.tries.Try
import playwrigkt.skript.Skript
import playwrigkt.skript.config.ConfigValue

data class ApplicationResourceLoaderConfig(val name: String,
                                           val dependencyOverrides: Map<String, String>,
                                           val config: ConfigValue = ConfigValue.Empty.Undefined) {
    fun applyOverride(dependency: String): String = dependencyOverrides.get(dependency)?:dependency
}

interface ApplicationResourceLoader<Resource> {
    data class Input(val existingApplicationResources: Map<String, *>, val applicationResourceLoaderConfig: ApplicationResourceLoaderConfig)
    data class StageManagerException(val error: StageManagerError, override val cause: Throwable? = null): Exception(error.toString(), cause)
    sealed class StageManagerError {
        data class NoSuchManager(val name: String): StageManagerError()
    }

    fun name(): String = this::class.java.simpleName.removeSuffix("Loader").decapitalize()
    val dependencies: List<String>
    val loadResource: Skript<Input, Resource, SkriptApplicationLoader>

    fun <OtherResource> loadExistingApplicationResourceSkript(name: String): Skript<Input, OtherResource, SkriptApplicationLoader> =
            Skript.mapTry {
                it.existingApplicationResources.get(it.applicationResourceLoaderConfig.applyOverride(name))
                        ?.let { Try { it as OtherResource } }
                        ?:Try.Failure(ApplicationResourceLoader.StageManagerException(ApplicationResourceLoader.StageManagerError.NoSuchManager(name)))
            }
}