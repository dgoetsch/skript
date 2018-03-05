package dev.yn.playground.chatrooom.sql

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.sql.SQLTask
import dev.yn.playground.sql.SQLMapping
import dev.yn.playground.sql.ext.dropTableIfExists
import dev.yn.playground.sql.ext.exec

object ChatRoomSchema {

    val createChatRoomTable = """
    CREATE TABLE IF NOT EXISTS chatroom(
        id text PRIMARY KEY,
        name text,
        description text)""".trimIndent()

    val createChatRoomPermissionTable = """
    CREATE TABLE IF NOT EXISTS chatroom_permission(
        chatroom_id text REFERENCES chatroom(id),
        permission_key text,
        allow_public boolean,
        PRIMARY KEY (chatroom_id, permission_key))""".trimIndent()

    val createChatRoomUserPermissionTable = """
    CREATE TABLE IF NOT EXISTS chatroom_user_permission(
        chatroom_id text REFERENCES chatroom(id),
        user_id text REFERENCES user_profile(id),
        permission_key text,
        date_added timestamp,
        PRIMARY KEY (chatroom_id, user_id, permission_key))""".trimIndent()

    val createBannedUserTable = """
    CREATE TABLE IF NOT EXISTS chatroom_user_banned(
        chatroom_id text REFERENCES chatroom(id),
        user_id text REFERENCES user_profile(id),
        date_added timestamp,
        PRIMARY KEY (chatroom_id, user_id))""".trimIndent()

    val initAction = SQLTask
            .exec<Unit, Unit, ApplicationContext<Unit>>(SQLMapping.Companion.exec(createChatRoomTable))
            .exec(SQLMapping.Companion.exec(createChatRoomPermissionTable))
            .exec(SQLMapping.Companion.exec(createChatRoomUserPermissionTable))
            .exec(SQLMapping.Companion.exec(createBannedUserTable))

    val dropAllAction = dropTableIfExists<ApplicationContext<Unit>>("chatroom_user_banned")
            .dropTableIfExists("chatroom_user_permission")
            .dropTableIfExists("chatroom_permission")
            .dropTableIfExists("chatroom")
}