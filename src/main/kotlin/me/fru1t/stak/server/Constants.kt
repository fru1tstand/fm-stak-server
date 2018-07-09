package me.fru1t.stak.server

/** Constants available globally within the stak server. */
object Constants {
  const val LOGIN_USER_PARAM_NAME = "username"
  const val LOGIN_PASS_PARAM_NAME = "password"

  internal const val SESSION_AUTH_NAME = "session-auth"
  internal const val LOGIN_AUTH_NAME = "login"

  /* Dagger @Named constants. */
  internal const val NAMED_DATABASE_FOLDER = "database-folder"
  internal const val NAMED_SESSION_TIMEOUT_HOURS = "session-timeout-hours"
}
