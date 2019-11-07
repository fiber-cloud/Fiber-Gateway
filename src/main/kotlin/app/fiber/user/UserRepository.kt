package app.fiber.user

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.*
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheBuilder
import com.datastax.oss.driver.shaded.guava.common.cache.CacheLoader
import java.time.Duration
import java.util.*

data class User(val uuid: UUID, val name: String, val password: String)

class UserRepository(private val session: CqlSession) {

    private val table = "User"

    private val cache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(20))
        .build(object : CacheLoader<UUID, Optional<User>>() {
            override fun load(key: UUID): Optional<User> {
                return Optional.ofNullable(this@UserRepository.getUserById(key))
            }
        })

    init {
        this.createTable()
    }

    fun checkId(id: String): User? {
        return this.cache.get(UUID.fromString(id)).orElse(null)
    }

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

    fun deleteUser(user: User) {
        val userDelete = deleteFrom(this.table)
            .whereColumn("user_id")
            .isEqualTo(bindMarker())
            .build()

        val statement = this.session.prepare(userDelete)
        val boundStatement = statement.bind()
            .setUuid(0, user.uuid)

        this.session.execute(boundStatement)
        this.invalidateCache(user.uuid)

        //TODO invalidate all caches (dns resolve)
    }

    fun invalidateCache(uuid: UUID) {
        this.cache.invalidate(uuid)
    }

    private fun createTable() {
        val tableQuery = SchemaBuilder.createTable(this.table)
            .ifNotExists()
            .withPartitionKey("user_id", DataTypes.UUID)
            .withColumn("name", DataTypes.TEXT)
            .withColumn("password", DataTypes.TEXT)

        this.session.execute(tableQuery.build())
    }

}