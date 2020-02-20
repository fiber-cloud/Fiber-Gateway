package app.fiber.database

import app.fiber.cache.PodCacheInvalidator
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import io.mockk.*
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.junit.After
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CassandraUserDatabaseTest : KoinTest {

    private val keyspace = "fiber_gateway"
    private val table = "users"

    private val testUser = User(UUID.randomUUID(), "Name", "Password")

    private val session: CqlSession

    private val cassandraUserDatabase: CassandraUserDatabase

    init {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra()
        this.session = EmbeddedCassandraServerHelper.getSession()

        startKoin {}

        val cacheInvalidator = mockk<PodCacheInvalidator>()

        every { cacheInvalidator.invalidate(testUser.uuid) } just Runs

        loadKoinModules(
            module {
                single { cacheInvalidator }
            }
        )

        this.cassandraUserDatabase = CassandraUserDatabase(this.session)
    }

    @After
    fun tearDown() {
        this.deleteTestUser()
        stopKoin()
    }

    @Test
    fun `test if keyspace was created`() {
        this.session.execute("USE system_schema")

        val statement = QueryBuilder.selectFrom("keyspaces")
            .all()
            .build()

        val result = this.session.execute(statement)

        assertNotNull(result.map { it.getString("keyspace_name") }.firstOrNull { it == this.keyspace })
    }

    @Test
    fun `test if table was created`() {
        this.session.execute("USE system_schema")

        val statement = QueryBuilder.selectFrom("tables")
            .all()
            .build()

        val result = this.session.execute(statement)

        assertNotNull(result.map { it.getString("table_name") }.firstOrNull { it == this.table })
    }

    @Test
    fun `test insert user`() {
        this.cassandraUserDatabase.insertUser(this.testUser)

        val statement = QueryBuilder.selectFrom(this.keyspace, this.table)
            .column("user_id")
            .whereColumn("user_id")
            .isEqualTo(QueryBuilder.bindMarker())
            .build()

        val boundStatement = this.session.prepare(statement).bind(this.testUser.uuid)

        val result = this.session.execute(boundStatement)
        val resultUUID = result.map { it.getUuid("user_id") }.firstOrNull()

        assertNotNull(resultUUID)
        assertEquals(this.testUser.uuid, resultUUID)
    }

    @Test
    fun `test get user by id`() {
        this.insertTestUser()

        val user = this.cassandraUserDatabase.getUserById(this.testUser.uuid)

        assertNotNull(user)
        assertEquals(this.testUser, user)
    }

    @Test
    fun `test get user by name`() {
        this.insertTestUser()

        val user = this.cassandraUserDatabase.getUserByName(this.testUser.name)

        assertNotNull(user)
        assertEquals(this.testUser, user)
    }

    @Test
    fun `test delete user`() {
        this.insertTestUser()
        this.cassandraUserDatabase.deleteUser(this.testUser)
    }

    @Test
    fun `test invalidate cache`() {
        this.cassandraUserDatabase.invalidateCache(this.testUser.uuid)
        val podCacheInvalidator = get<PodCacheInvalidator>()

        verify { podCacheInvalidator.invalidate(testUser.uuid) }
    }

    private fun insertTestUser() {
        val userInsert = QueryBuilder.insertInto(this.table)
            .value("user_id", QueryBuilder.bindMarker())
            .value("name", QueryBuilder.bindMarker())
            .value("password", QueryBuilder.bindMarker())
            .build()

        val statement = this.session.prepare(userInsert)

        val boundStatement = statement.bind()
            .setUuid(0, this.testUser.uuid)
            .setString(1, this.testUser.name)
            .setString(2, this.testUser.password)

        this.session.execute(boundStatement)
    }

    private fun deleteTestUser() {
        val userDelete = QueryBuilder.deleteFrom(this.table)
            .whereColumn("user_id")
            .isEqualTo(QueryBuilder.bindMarker())
            .build()

        val statement = this.session.prepare(userDelete)
        val boundStatement = statement.bind()
            .setUuid(0, this.testUser.uuid)

        this.session.execute(boundStatement)
    }

}