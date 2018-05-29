package me.fru1t.stak.server

import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/** The main Stak Server module that provides bootstrapped server objects. */
@Module
class StakServerModule {
  @Provides
  @Singleton
  @Named(Constants.NAMED_DATABASE_FOLDER)
  fun provideNamedDatabaseFolder() = "/"
}
