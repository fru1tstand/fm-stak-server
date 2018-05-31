package me.fru1t.stak.server

import com.google.common.base.Ticker
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/** The main Stak Server module that provides bootstrapped server objects. */
@Module
class StakServerModule {
  @Provides
  @Singleton
  fun provideGson(): Gson = Gson()

  @Provides
  @Singleton
  @Named(Constants.NAMED_DATABASE_FOLDER)
  fun provideNamedDatabaseFolder(): String = "database"

  @Provides
  @Singleton
  @Named(Constants.NAMED_SESSION_TIMEOUT_HOURS)
  fun provideNamedSessionTimeoutHours(): Long = 24 * 7

  @Provides
  @Singleton
  fun provideCacheTicker(): Ticker = Ticker.systemTicker()
}
