package app.fiber.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import java.net.InetSocketAddress

/**
 * Connecting to a Cassandra database.
 *
 * @param [host] Host address of the Cassandra instance.
 *
 * @author Tammo0987
 * @since 1.0
 */
class CassandraConnector(private val host: String) {

    /**
     * Creates and build a [session][CqlSession] for executing queries.
     */
    val session: CqlSession = CqlSessionBuilder()
        .addContactPoint(InetSocketAddress(this.host, 9042))
        .withLocalDatacenter("datacenter1")
        .build()

    /**
     * Create and use the right keyspace for this application.
     */
    init {
        val keyspaceName = "fiber_gateway"
        val createKeyspace = SchemaBuilder.createKeyspace(keyspaceName)
            .ifNotExists()
            .withSimpleStrategy(1)

        this.session.execute(createKeyspace.build())
        this.session.execute("USE $keyspaceName")

        Runtime.getRuntime().addShutdownHook(Thread(this::close))
    }

    /**
     * Closes the session at the end of lifetime.
     */
    private fun close() = this.session.close()

}

