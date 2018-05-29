package me.fru1t.stak.server.components.database.json.models

import me.fru1t.stak.server.models.User

/** Table that stores all users. */
data class UserTable(val users: MutableList<User>)
