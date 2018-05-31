package me.fru1t.stak.server

/** Constants available globally within the stak server. */
object Constants {
  const val SESSION_AUTH_NAME = "session-auth"
  const val LOGIN_AUTH_NAME = "login"

  /* Dagger @Named constants. */
  const val NAMED_DATABASE_FOLDER = "database-folder"
  const val NAMED_SESSION_TIMEOUT_HOURS = "session-timeout-hours"
}
