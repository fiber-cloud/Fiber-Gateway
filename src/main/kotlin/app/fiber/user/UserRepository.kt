package app.fiber.user

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.*
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheLoader
import java.time.Duration
import java.util.*

/**
 * Represent a user account.
 *
 * @param [uuid] [UUID] to identify the user as unique.
 * @param [name] Name of the user as an alias for a better distinction.
 * @param [password] Password to authorize if the user of the application has access to this account.
 *
 * @author Tammo0987
 * @since 1.0
 */
data class User(val uuid: UUID, val name: String, val password: String)

/**
 * Repository to save [User] data with a Cassandra implementation.
 *
 * @param [session] [CqlSession] to call queries on Cassandra.
 *
 * @author Tammo0987
 * @since 1.0
 */
class UserRepository(private val session: CqlSession) {

    /**
     * Name of the table for the data model.
     */
    private val table = "users"

    /**
     * Cache for a relief of the Cassandra traffic.
     */
    private val cache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5))
        .build(object : CacheLoader<UUID, User?>() {
            override fun load(key: UUID): User? = this@UserRepository.getUserById(key)
        })

    /**
     * Create the table after initialisation.
     */
    init {
        this.createTable()
    }

    /**
     * Get the [User] instance if existing.
     *
     * @return The found [User] or null if not found.
     */
    fun checkId(id: String): User? {
        return this.cache.get(UUID.fromString(id))
    }

    /**
     * Insert [user] to the database.
     *
     * @param [user] [User] that will be inserted.
     */
    fun insertUser(user: User) {
        val userInsert = insertInto(this.table)
            .value("user_id", bindMarker())
            .value("name", bindMarker())
            .value("password", bindMarker())
            .build()

        val statement = this.session.prepare(userInsert)

        val boundStatement = statement.bind()
            .setUuid(0, user.uuid)
            .setString(1, user.name)
            .setString(2, user.password)

        this.session.execute(boundStatement)
    }

    /**
     * Query [User] in the database by [userId].
     *
     * @param [userId] [UUID] id to identify.
     *
     * @return [User] if found.
     */
    fun getUserById(userId: UUID): User? {
        val userSelect = selectFrom(this.table)
            .all()
            .whereColumn("user_id")
            .isEqualTo(bindMarker())
            .build()

        val statement = this.session.prepare(userSelect)
        val boundStatement = statement.bind()
            .setUuid(0, userId)

        val result = this.session.execute(boundStatement)

        return result.map { row ->
            User(
                row.getUuid("user_id")!!,
                row.getString("name")!!,
                row.getString("password")!!
            )
        }.one()
    }

    /**
     * Query [User] in the database by [name].
     *
     * @param [name] Name to identify.
     *
     * @return [User] if found.
     */
    fun getUserByName(name: String): User? {
        val userSelect = selectFrom(this.table)
            .all()
            .whereColumn("name")
            .isEqualTo(bindMarker())
            .allowFiltering()
            .build()

        val statement = this.session.prepare(userSelect)
        val boundStatement = statement.bind()
            .setString(0, name)

        val result = this.session.execute(boundStatement)

        return result.map { row ->
            User(
                row.getUuid("user_id")!!,
                row.getString("name")!!,
                row.getString("password")!!
            )
        }.one()
    }

    /**
     * Delete [user] from the database.
     *
     * @param [user] [User] to delete.
     */
    fun deleteUser(user: User) {
        val userDelete = deleteFrom(this.table)
            .whereColumn("user_id")
            .isEqualTo(bindMarker())
            .build()

        val statement = this.session.prepare(userDelete)
        val boundStatement = statement.bind()
            .setUuid(0, user.uuid)

        this.session.execute(boundStatement)
        this.cache.invalidate(user.uuid)
    }

    /**
     * Creates the table in the database.
     */
    private fun createTable() {
        val tableQuery = SchemaBuilder.createTable(this.table)
            .ifNotExists()
            .withPartitionKey("user_id", DataTypes.UUID)
            .withColumn("name", DataTypes.TEXT)
            .withColumn("password", DataTypes.TEXT)

        this.session.execute(tableQuery.build())
    }

}