package dev.yn.playground.user

import dev.yn.playground.sql.UnpreparedSQLActionChain

object UserSchema {

    val createUserProfileTable = """CREATE TABLE IF NOT EXISTS user_profile (
    id text PRIMARY KEY,
    user_name text UNIQUE,
    allow_public_message boolean
);"""

    val createUserPasswordTable = """CREATE TABLE IF NOT EXISTS user_password (
    user_id text REFERENCES user_profile(id) PRIMARY KEY,
    pswhash text
);
"""
    val userSessionUserIdUniqueConstraintName = "user_session_user_id_unique"

    val createUserSessionTable = """CREATE TABLE IF NOT EXISTS user_session(
    session_key text PRIMARY KEY,
    user_id text REFERENCES user_profile(id) CONSTRAINT ${userSessionUserIdUniqueConstraintName} UNIQUE,
    expiration timestamp
);"""

    val userTrustDeviceUserIdUniqueConstraintName = "user_trusted_device_user_id_unique"
    val createUserSessionAccess = """CREATE TABLE IF NOT EXISTS user_trusted_device (
    device_key text PRIMARY KEY,
    user_id text REFERENCES user_profile(id) CONSTRAINT ${userTrustDeviceUserIdUniqueConstraintName} UNIQUE,
    device_name text,
    expiration timestamp
);"""

    val createUserRequestTable = """
CREATE TABLE IF NOT EXISTS user_relationship_request (
    user_id_1 text REFERENCES user_profile(id),
    user_id_2 text REFERENCES user_profile(id),
    initiated timestamp,
    initiator text,
    CONSTRAINT user_relationship_request_pk PRIMARY KEY (user_id_1, user_id_2)
);"""

    fun <P> init(): UnpreparedSQLActionChain<Unit, Unit, P> =
            UnpreparedSQLActionChain.exec<Unit, P>("CREATE EXTENSION IF NOT EXISTS pgcrypto")
                    .exec(createUserProfileTable)
                    .exec(createUserPasswordTable)
                    .exec(createUserSessionTable)
                    .exec(createUserRequestTable)

    fun <P> drop(): UnpreparedSQLActionChain<Unit, Unit, P> =
            UnpreparedSQLActionChain.dropTableIfExists<Unit, P>("user_relationship_request")
                    .dropTableIfExists("user_password")
                    .dropTableIfExists("user_session")
                    .dropTableIfExists("user_profile")
}