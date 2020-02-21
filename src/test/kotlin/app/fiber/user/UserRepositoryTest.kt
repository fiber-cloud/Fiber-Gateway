package app.fiber.user

import app.fiber.database.User
import app.fiber.database.UserDatabase
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import java.util.*
import kotlin.test.assertNull


class UserRepositoryTest : KoinTest {

    private val uuid = UUID.randomUUID()

    private val testUser = User(this.uuid, "Name", "Password")

    private val userRepository by inject<UserRepository>()

    private val module = module {
        single { mockk<UserDatabase>() }
        single { UserRepository() }
    }

    @Before
    fun setUp() {
        startKoin {}
        loadKoinModules(this.module)
    }

    @After
    fun tearDown() {
        unloadKoinModules(this.module)
        stopKoin()
    }

    @Test
    fun `test add user`() {
        val database = get<UserDatabase>()

        every { database.insertUser(testUser) } just Runs

        this.userRepository.addUser(this.testUser)

        verify { database.insertUser(testUser) }
    }

    @Test
    fun `test get user by name`() {
        val database = get<UserDatabase>()

        every { database.getUserByName("Name") } returns this.testUser
        every { database.getUserByName("Wrong Name") } returns null

        assertEquals(this.testUser, this.userRepository.getUserByName("Name"))
        assertNull(this.userRepository.getUserByName("Wrong Name"))
    }

    @Test
    fun `test get user by id`() {
        val wrongUUID = UUID.randomUUID()
        val database = get<UserDatabase>()

        every { database.getUserById(uuid) } returns this.testUser
        every { database.getUserById(wrongUUID) } returns null

        assertEquals(this.testUser, this.userRepository.getUserById(this.uuid.toString()))
        assertNull(this.userRepository.getUserById(wrongUUID.toString()))
    }

    @Test
    fun `test delete user`() {
        val database = get<UserDatabase>()

        every { database.deleteUser(testUser) } just Runs

        this.userRepository.deleteUser(this.testUser)

        verify { database.deleteUser(testUser) }
    }

    @Test
    fun `test invalidate cache`() {
        val database = get<UserDatabase>()
        every { database.invalidateCache(uuid) } just Runs

        this.userRepository.invalidateCache(uuid)

        verify { database.invalidateCache(uuid) }
    }

}

