package playwright.skript.chatrooom.props

import org.funktionale.option.Option
import playwright.skript.auth.AuthSession
import playwright.skript.auth.props.UserSessionProps
import playwright.skript.chatrooom.models.ChatRoom

interface ExistingChatroomProps {
    fun useChatroom(chatroom: ChatRoom)
    fun getChatroom(): Option<ChatRoom>
}

data class ChatroomOperationProps(
        private val sessionKey: String,
        private var session: Option<AuthSession> = Option.None,
        private var chatroom: Option<ChatRoom> = Option.None)
    : UserSessionProps, ExistingChatroomProps {
    override fun useChatroom(chatroom: ChatRoom) {
        this.chatroom = Option.Some(chatroom)
    }

    override fun getChatroom(): Option<ChatRoom> = chatroom

    override fun getUserSessionKey(): String = sessionKey

    override fun setUserSession(userSession: AuthSession) {
        this.session = Option.Some(userSession)
    }

    override fun getUserSession(): Option<AuthSession> = session
}