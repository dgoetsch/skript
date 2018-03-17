package dev.yn.playground.user.sql

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.yn.playground.auth.sql.AuthSQLActions
import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.publisher.PublishCommand
import dev.yn.playground.sql.ext.deleteAll
import dev.yn.playground.sql.ext.query
import dev.yn.playground.sql.ext.update
import dev.yn.playground.task.Task
import dev.yn.playground.user.models.*
import org.funktionale.tries.Try
import java.time.Instant
import java.util.*
import dev.yn.playground.publisher.ex.*
import dev.yn.playground.task.andThen
import dev.yn.playground.user.context.GetUserContext
import dev.yn.playground.user.userCreatedAddress
import dev.yn.playground.user.userLoginAddress
import org.funktionale.option.getOrElse

object UserTransactions {
    val objectMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule())
    private val createNewSessionKey: (String) -> UserSession = { UserSession(UUID.randomUUID().toString(), it, Instant.now().plusSeconds(3600)) }
    private val publishUserCreateEvent: (UserProfile) -> PublishCommand.Publish =
            { PublishCommand.Publish(userCreatedAddress, objectMapper.writeValueAsBytes(it))}
    private val publishUserLoginEvent: (UserSession) -> PublishCommand.Publish =
            { PublishCommand.Publish(userLoginAddress, objectMapper.writeValueAsBytes(it))}

    private val authorizeUser: Task<String, String, ApplicationContext<GetUserContext>> = Task.mapTryWithContext { userId, context ->
        context.cache.getUserSession()
                .filter { it.userId == userId }
                .map { Try.Success(userId) }
                .getOrElse { Try.Failure<String>(UserError.AuthorizationFailed) }
    }

    val createUserActionChain: Task<UserProfileAndPassword, UserProfile, ApplicationContext<Unit>> =
            Task.identity<UserProfileAndPassword, ApplicationContext<Unit>>()
                    .update(InsertUserProfileMapping)
                    .update(InsertUserPasswordMapping)
                    .publish(publishUserCreateEvent)

    val loginActionChain: Task<UserNameAndPassword, UserSession, ApplicationContext<Unit>> =
            Task.identity<UserNameAndPassword, ApplicationContext<Unit>>()
                    .query(SelectUserIdForLogin)
                    .query(ValidatePasswordForUserId)
                    .query(EnsureNoSessionExists)
                    .map(createNewSessionKey)
                    .update(InsertSession)
                    .publish(publishUserLoginEvent)

    private val onlyIfRequestedUserMatchesSessionUser =  { session: UserSession, userId: String ->
        if (session.userId == userId) {
            Try.Success(userId)
        } else {
            Try.Failure<String>(UserError.AuthorizationFailed)
        }
    }

    val getUserActionChain: Task<String, UserProfile, ApplicationContext<GetUserContext>> =
            AuthSQLActions.validate<String, GetUserContext>()
                    .andThen(authorizeUser)
                    .query(SelectUserProfileById)

    fun deleteAllUserActionChain() = Task.identity<Unit, ApplicationContext<Unit>>()
            .deleteAll("user_relationship_request")
            .deleteAll("user_password")
            .deleteAll("user_session")
            .deleteAll("user_profile")


}