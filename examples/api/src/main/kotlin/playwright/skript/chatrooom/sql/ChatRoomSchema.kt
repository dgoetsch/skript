package playwright.skript.chatrooom.sql

import playwright.skript.Skript
import playwright.skript.common.ApplicationStage
import playwright.skript.ex.dropTableIfExists
import playwright.skript.ex.exec
import playwright.skript.sql.SQLMapping

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

    val initAction = Skript.identity<Unit, ApplicationStage>()
            .exec(SQLMapping.exec(playwright.skript.chatrooom.sql.ChatRoomSchema.createChatRoomTable))
            .exec(SQLMapping.exec(playwright.skript.chatrooom.sql.ChatRoomSchema.createChatRoomPermissionTable))
            .exec(SQLMapping.exec(playwright.skript.chatrooom.sql.ChatRoomSchema.createChatRoomUserPermissionTable))
            .exec(SQLMapping.exec(playwright.skript.chatrooom.sql.ChatRoomSchema.createBannedUserTable))

    val dropAllAction = Skript.identity<Unit, ApplicationStage>()
            .dropTableIfExists("chatroom_user_banned")
            .dropTableIfExists("chatroom_user_permission")
            .dropTableIfExists("chatroom_permission")
            .dropTableIfExists("chatroom")
}