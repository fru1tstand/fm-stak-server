package me.fru1t.stak.server

import dagger.Binds
import dagger.Module
import me.fru1t.stak.server.components.example.Example
import me.fru1t.stak.server.components.example.impl.ExampleImpl
import me.fru1t.stak.server.components.server.Server
import me.fru1t.stak.server.components.server.impl.ServerImpl
import javax.inject.Singleton

/** Provides default bindings for interface to implementations. */
@Module
abstract class ComponentsModule {
  @Binds @Singleton abstract fun bindExample(exampleImpl: ExampleImpl): Example
  @Binds @Singleton abstract fun bindServer(serverImpl: ServerImpl): Server
}
