package me.fru1t.stak.server.components.user.impl

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.http.HttpStatusCode
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.DatabaseOperationResult
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.models.UserCreate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class UserControllerImplTest {
  companion object {
    private val TEST_USER_CREATE =
      UserCreate(
          username = "test username", password = "test password", displayName = "test display name")
    private const val TEST_PASSWORD_HASH = "test hash"
  }

  @Mock private lateinit var mockDatabase: Database
  @Mock private lateinit var mockSecurity: Security
  private lateinit var userControllerImpl: UserControllerImpl

  @BeforeEach internal fun setUp() {
    MockitoAnnotations.initMocks(this)
    userControllerImpl = UserControllerImpl(mockDatabase, mockSecurity)
  }

  @Test fun createUser() {
    whenever(mockDatabase.createUser(any())).thenReturn(DatabaseOperationResult(true))
    whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

    val result = userControllerImpl.createUser(TEST_USER_CREATE)

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.OK)
    assertThat(result.value!!.username).isEqualTo(TEST_USER_CREATE.username)
    assertThat(result.value!!.passwordHash).isEqualTo(TEST_PASSWORD_HASH)
    assertThat(result.value!!.displayName).isEqualTo(TEST_USER_CREATE.displayName)
  }

  @Test fun createUser_databaseError() {
    whenever(mockDatabase.createUser(any()))
        .thenReturn(DatabaseOperationResult(error = "some error", isDatabaseError = true))
    whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

    val result = userControllerImpl.createUser(TEST_USER_CREATE)

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.InternalServerError)
    assertThat(result.value).isNull()
  }

  @Test fun createUser_existingUser() {
    whenever(mockDatabase.createUser(any()))
        .thenReturn(DatabaseOperationResult(error = "non-databse error"))
    whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

    val result = userControllerImpl.createUser(TEST_USER_CREATE)

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.Conflict)
    assertThat(result.value).isNull()
  }
}