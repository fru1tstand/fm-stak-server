package me.fru1t.stak.server.dagger

import dagger.Binds
import dagger.Module
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.json.JsonDatabase
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.security.impl.SecurityImpl
import me.fru1t.stak.server.components.server.Server
import me.fru1t.stak.server.components.server.impl.ServerImpl
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.session.impl.SessionControllerImpl
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.components.user.impl.UserControllerImpl
import javax.inject.Singleton

/** Provides default bindings for interface to implementations. */
@Module
abstract class ComponentsModule {
  @Binds @Singleton abstract fun bindServer(serverImpl: ServerImpl): Server
  @Binds @Singleton abstract fun bindDatabase(jsonDatabase: JsonDatabase): Database
  @Binds @Singleton abstract fun bindSecurity(securityImpl: SecurityImpl): Security
  @Binds @Singleton abstract fun bindSessionController(
      sessionControllerImpl: SessionControllerImpl): SessionController
  @Binds @Singleton abstract fun bindUserController(
      userControllerImpl: UserControllerImpl): UserController
}
