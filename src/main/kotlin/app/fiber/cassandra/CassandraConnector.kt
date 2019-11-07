package app.fiber.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import java.net.InetSocketAddress

class CassandraConnector(private val ip: String) {

    val session: CqlSession = CqlSessionBuilder()
        .addContactPoint(InetSocketAddress(this.ip, 9042))
        .withLocalDatacenter("datacenter1")
        .build()

    init {
        val keyspaceName = "fiber_gateway"
        val createKeyspace = SchemaBuilder.createKeyspace(keyspaceName)
            .ifNotExists()
            .withSimpleStrategy(1)

        this.session.execute(createKeyspace.build())
        this.session.execute("USE $keyspaceName")

        Runtime.getRuntime().addShutdownHook(Thread(this::close))
    }

    private fun close() = this.session.close()

}

