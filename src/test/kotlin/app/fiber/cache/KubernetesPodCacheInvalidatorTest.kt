package app.fiber.cache

import io.fabric8.kubernetes.api.model.PodListBuilder
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.NamespacedKubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer
import io.fabric8.kubernetes.client.utils.HttpClientUtils
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.*


class KubernetesPodCacheInvalidatorTest : KoinTest {

    private val uuid = UUID.randomUUID()

    private lateinit var server: KubernetesMockServer
    private lateinit var client: NamespacedKubernetesClient

    @Before
    fun setUp() {
        this.startKubernetes()

        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.toString()) {
                        "http://test-1.com/api/cache/remove/$uuid" -> {
                            respondOk()
                        }
                        "http://test-2.com/api/cache/remove/$uuid" -> {
                            respondOk()
                        }
                        else -> error("Unhandled url ${request.url}")
                    }
                }
            }
        }

        startKoin {}
        loadKoinModules(module {
            single<PodCacheInvalidator> {
                KubernetesPodCacheInvalidator()
            }
            single<KubernetesClient> { client }
            single { httpClient }
        })
    }

    @After
    fun tearDown() {
        this.server.destroy()
        this.client.close()

        stopKoin()
    }

    @Test
    fun `test if cache will be invalidated`() {
        val podList = PodListBuilder()
            .addPod("test-1.com")
            .addPod("test-2.com")
            .build()

        this.server.expect()
            .withPath("/api/v1/namespaces/fiber/pods?labelSelector=app%3Dfiber-gateway")
            .andReturn(200, podList)
            .once()

        val podCacheInvalidator by inject<PodCacheInvalidator>()

        podCacheInvalidator.invalidate(this.uuid)
    }

    private fun startKubernetes() {
        this.server = KubernetesMockServer(false)
        this.server.init()

        val config = ConfigBuilder()
            .withMasterUrl(this.server.url("/").toString())
            .withCaCertFile("")
            .withClientCertFile("")
            .withNamespace("fiber")
            .build()
        this.client = DefaultKubernetesClient(HttpClientUtils.createHttpClientForMockServer(config), config)
    }

    private fun PodListBuilder.addPod(podIp: String): PodListBuilder {
        return this.addNewItem()
            .withNewMetadata()
            .withNamespace("fiber")
            .addToLabels("app", "fiber-gateway")
            .endMetadata()
            .withNewStatus()
            .withNewPodIP(podIp)
            .endStatus()
            .and()
    }

}