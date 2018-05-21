package me.fru1t.stak.server

import dagger.Component
import javax.inject.Singleton

/** Builds the root of the StakServer dependency graph. */
@Singleton
@Component(modules = [StakServerModule::class, ComponentsModule::class])
interface StakServerComponent {
  fun inject(stakServer: StakServer)
}
