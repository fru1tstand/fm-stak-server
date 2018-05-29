package me.fru1t.stak.server.components.database.json.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UserTableTest {
  @Test fun default() {
    val result = UserTable(ArrayList())

    assertThat(result.users).isEmpty()
  }
}
