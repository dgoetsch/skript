package  playwright.skript.user

import io.kotlintest.Spec
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.StringSpec
import org.slf4j.LoggerFactory
import playwright.skript.Skript
import playwright.skript.common.ApplicationStage
import playwright.skript.common.ApplicationVenue
import playwright.skript.ex.deserialize
import playwright.skript.result.AsyncResult
import playwright.skript.result.Result
import playwright.skript.sql.SQLCommand
import playwright.skript.sql.SQLError
import playwright.skript.sql.SQLStatement
import playwright.skript.user.extensions.schema.dropUserSchema
import playwright.skript.user.extensions.schema.initUserSchema
import playwright.skript.user.extensions.transaction.deleteAllUsers
import playwright.skript.user.models.*
import playwright.skript.user.sql.EnsureNoSessionExists
import playwright.skript.user.sql.UserSQL
import playwright.skript.user.sql.ValidatePasswordForUserId
import java.util.*


abstract class UserServiceSpec : StringSpec() {

    val LOG = LoggerFactory.getLogger(this.javaClass)

    abstract fun provider(): ApplicationVenue
    abstract fun consumerPerformerProvider(): playwright.skript.consumer.alpha.ConsumerStage
    abstract fun closeResources()

    val userService: UserService = UserService(provider())

    fun loginConsumer(): playwright.skript.consumer.alpha.Stream<UserSession> {
        return awaitSucceededFuture(
                userLoginConsumer(consumerPerformerProvider(), provider())
                        .stream(Skript.identity<playwright.skript.consumer.alpha.ConsumedMessage, ApplicationStage>()
                                .map { it.body }
                                .deserialize(UserSession::class.java)))!!
    }

    fun createConsumer(): playwright.skript.consumer.alpha.Stream<UserProfile> {
        return awaitSucceededFuture(
                playwright.skript.user.userCreateConsumer(consumerPerformerProvider(), provider())
                        .stream(Skript.identity<playwright.skript.consumer.alpha.ConsumedMessage, ApplicationStage>()
                                .map { it.body }
                                .deserialize(UserProfile::class.java)))!!
    }
    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        awaitSucceededFuture(provider().provideStage().flatMap{ it.dropUserSchema() })
        awaitSucceededFuture(provider().provideStage().flatMap{ it.initUserSchema() })

        spec()
        awaitSucceededFuture(provider().provideStage().flatMap{ it.deleteAllUsers() })
        awaitSucceededFuture(provider().provideStage().flatMap{ it.dropUserSchema() })
        closeResources()
    }

    init {
        "Login a userName" {
            val userId = UUID.randomUUID().toString()
            val password = "pass1"
            val userName = "sally"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)
            val createStream = createConsumer()
            val loginStream= loginConsumer()

            createStream.isRunning() shouldBe true
            loginStream.isRunning() shouldBe true

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val session = awaitSucceededFuture(userService.loginUser(userAndPassword))
            session?.userId shouldBe userId
            session shouldNotBe null

            awaitStreamItem(createStream, user)
            awaitStreamItem(loginStream, session!!)
            awaitSucceededFuture(createStream.stop())
            awaitSucceededFuture(loginStream.stop())
        }

        "Fail to loginActionChain a user with a bad password" {
            val userId = UUID.randomUUID().toString()
            val password = "pass2"
            val userName = "sally2"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val expectedError = SQLError.OnCommand(
                    SQLCommand.Query(SQLStatement.Parameterized(ValidatePasswordForUserId.selectUserPassword, listOf(userId, "bad"))),
                    UserError.AuthenticationFailed)
            awaitFailedFuture(
                    userService.loginUser(userAndPassword.copy(password = "bad")),
                    expectedError)
        }

        "Fail to loginActionChain a user who is already logged in" {
            val userId = UUID.randomUUID().toString()
            val password = "pass3"
            val userName = "sally3"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            awaitSucceededFuture(userService.loginUser(userAndPassword))?.userId shouldBe userId
            val expectedError = SQLError.OnCommand(
                    SQLCommand.Query(SQLStatement.Parameterized(EnsureNoSessionExists.selectUserSessionExists, listOf(userId))),
                    UserError.SessionAlreadyExists(userId))
            awaitFailedFuture(
                    userService.loginUser(userAndPassword.copy(password = password)),
                    expectedError)
        }

        "Allow a user to get themselves once logged in" {
            val userId = UUID.randomUUID().toString()
            val password = "pass4"
            val userName = "sally4"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val session = awaitSucceededFuture(userService.loginUser(userAndPassword))!!

            awaitSucceededFuture(
                    userService.getUser(userId, session.sessionKey),
                    user)
        }

        "Not Allow a user to select another user once logged in" {
            val userId = UUID.randomUUID().toString()
            val password = "pass5"
            val userName = "sally5"
            val user = UserProfile(userId, userName, false)
            val userAndPassword = UserNameAndPassword(userName, password)

            val userId2 = UUID.randomUUID().toString()
            val password2 = "pass6"
            val userName2 = "sally6"
            val user2 = UserProfile(userId2, userName2, false)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)
            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user2, password2)),
                    user2)

            val session = awaitSucceededFuture(userService.loginUser(userAndPassword))!!

            awaitFailedFuture(
                    userService.getUser(userId2, session.sessionKey),
                    SQLError.OnCommand(
                            SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(session.sessionKey))),
                            UserError.AuthenticationFailed))
        }

        "Not Allow a user with a bogus key to select another user" {
            val userId = UUID.randomUUID().toString()
            val password = "pass7"
            val userName = "sally7"
            val user = UserProfile(userId, userName, false)

            awaitSucceededFuture(
                    userService.createUser(UserProfileAndPassword(user, password)),
                    user)

            val sessionKey = UUID.randomUUID().toString()

            awaitFailedFuture(
                    userService.getUser(userId, sessionKey),
                    SQLError.OnCommand(
                            SQLCommand.Query(SQLStatement.Parameterized(UserSQL.selectSessionByKey, listOf(sessionKey))),
                            UserError.AuthenticationFailed))
        }
    }

    fun <T> awaitStreamItem(stream: playwright.skript.consumer.alpha.Stream<T>, expected: T, maxDuration: Long = 1000L) {
        val start = System.currentTimeMillis()
        while(!stream.hasNext() && System.currentTimeMillis() - start < maxDuration) {
            Thread.sleep(100)
        }
        val result = stream.next()
        when(result) {
            is Result.Success -> result.result shouldBe expected
            is Result.Failure -> throw result.error
        }
    }

    fun <T> awaitSucceededFuture(future: AsyncResult<T>, result: T? = null, maxDuration: Long = 1000L): T? {
        val start = System.currentTimeMillis()
        while(!future.isComplete() && System.currentTimeMillis() - start < maxDuration) {
            Thread.sleep(100)
        }
        if(!future.isComplete()) fail("Timeout")
        if(future.isFailure()) LOG.error("Expected Success", future.error())
        future.isSuccess() shouldBe true
        result?.let { future.result() shouldBe it }
        return future.result()
    }

    fun <T> awaitFailedFuture(future: AsyncResult<T>, cause: Throwable? = null, maxDuration: Long = 1000L): Throwable? {
        val start = System.currentTimeMillis()
        while(!future.isComplete() && System.currentTimeMillis() - start < maxDuration) {
            Thread.sleep(100)
        }
        future.isFailure() shouldBe true
        cause?.let { future.error() shouldBe it}
        return future.error()
    }



}