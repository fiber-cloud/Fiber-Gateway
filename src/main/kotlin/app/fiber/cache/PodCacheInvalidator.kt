package app.fiber.cache

import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

/**
 * [PodCacheInvalidator] to invalidate the cache for a specific user id on all running instances.
 *
 * @author Tammo0987
 * @since 1.0
 */
interface PodCacheInvalidator {

    /**
     * Invalidate the cache.
     *
     * @param [userId] [UUID] specific user id.
     */
    fun invalidate(userId: UUID)

}

/**
 * Kubernetes specific implementation of [PodCacheInvalidator].
 *
 * @author Tammo0987
 * @since 1.0
 */
class KubernetesPodCacheInvalidator : PodCacheInvalidator, KoinComponent {

    override fun invalidate(userId: UUID) {
        val kubernetes by inject<KubernetesClient>()
        val ownIp = System.getenv("POD_IP") ?: ""

        val ips = kubernetes.pods()
            .inNamespace("fiber")
            .withLabel("app", "fiber-gateway")
            .list()
            .items
            .map { it.status.podIP }
            .filter { it != ownIp }

        val client by inject<HttpClient>()

        runBlocking {
            ips.map { ip ->
                launch { client.patch("http://$ip/api/cache/remove/$userId") }
            }.joinAll()
        }
    }

}