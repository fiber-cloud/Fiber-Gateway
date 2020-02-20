package app.fiber.database

import app.fiber.cache.PodCacheInvalidator
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheLoader
import org.koin.core.KoinComponent
import org.koin.core.get
import java.time.Duration
import java.util.*

/**
 * Implementation of [UserDatabase] for Cassandra.
 *
 * @property [session] Session to connect to Cassandra.
 *
 * @author Tammo0987
 * @since 1.0
 */
class CassandraUserDatabase(private val session: CqlSession?) : UserDatabase, KoinComponent {

    /**
     * Name of the database table.
     */
    private val table = "users"

    /**
     * Cache for the [User] to increase the performance.
     */
    private val cache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5))
        .build(object : CacheLoader<UUID, User?>() {
            override fun load(key: UUID): User? = loadUserById(key)
        })

    init {
        this.session?.let {
            this.createKeyspace()
            this.createTable()
            Runtime.getRuntime().addShutdownHook(Thread(this.session::close))
        }
    }

    /**
     * Insert [User] in the database.
     *
     * @param [user] [User] the user which will be inserted.
     */
    override fun insertUser(user: User) {
        this.invalidateCache(user.uuid)

        val userInsert = QueryBuilder.insertInto(this.table)
            .value("user_id", QueryBuilder.bindMarker())
            .value("name", QueryBuilder.bindMarker())
            .value("password", QueryBuilder.bindMarker())
            .build()

        val statement = this.session!!.prepare(userInsert)

        val boundStatement = statement.bind()
            .setUuid(0, user.uuid)
            .setString(1, user.name)
            .setString(2, user.password)

        this.session.execute(boundStatement)
    }

    /**
     * Get specific user by id [userId].
     *
     * @param [userId] [UUID] id of the user.
     *
     * @return [User] user with the specific id.
     */
    override fun getUserById(userId: UUID): User? = this.cache.get(userId)

    /**
     * Get specific user by name [name].
     *
     * @param [name] id of the user.
     *
     * @return [User] user with the specific name.
     */
    override fun getUserByName(name: String): User? {
        val userSelect = QueryBuilder.selectFrom(this.table)
            .all()
            .whereColumn("name")
            .isEqualTo(QueryBuilder.bindMarker())
            .allowFiltering()
            .build()

        val statement = this.session!!.prepare(userSelect)
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
     * Delete a specific user.
     *
     * @param [user] [User] user to delete.
     */
    override fun deleteUser(user: User) {
        val userDelete = QueryBuilder.deleteFrom(this.table)
            .whereColumn("user_id")
            .isEqualTo(QueryBuilder.bindMarker())
            .build()

        val statement = this.session!!.prepare(userDelete)
        val boundStatement = statement.bind()
            .setUuid(0, user.uuid)

        this.session.execute(boundStatement)
        this.invalidateCache(user.uuid)
    }

    /**
     * Invalidate cache for [userId].
     *
     * @param [userId] [UUID] user id to invalidate.
     */
    override fun invalidateCache(userId: UUID) {
        this.cache.invalidate(userId)

        val podCacheInvalidator = get<PodCacheInvalidator>()
        podCacheInvalidator.invalidate(userId)
    }

    /**
     * Load user by id from the database.
     *
     * @param [userId] [UUID] id from the user to load.
     */
    private fun loadUserById(userId: UUID): User? {
        val userSelect = QueryBuilder.selectFrom(this.table)
            .all()
            .whereColumn("user_id")
            .isEqualTo(QueryBuilder.bindMarker())
            .build()

        val statement = this.session!!.prepare(userSelect)
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
     * Create the keyspace in Cassandra.
     */
    private fun createKeyspace() {
        val keyspaceName = "fiber_gateway"
        val createKeyspace = SchemaBuilder.createKeyspace(keyspaceName)
            .ifNotExists()
            .withSimpleStrategy(1)

        this.session!!.execute(createKeyspace.build())
        this.session.execute("USE $keyspaceName")
    }

    /**
     * Create the table in Cassandra.
     */
    private fun createTable() {
        val tableQuery = SchemaBuilder.createTable(this.table)
            .ifNotExists()
            .withPartitionKey("user_id", DataTypes.UUID)
            .withColumn("name", DataTypes.TEXT)
            .withColumn("password", DataTypes.TEXT)

        this.session!!.execute(tableQuery.build())
    }

}