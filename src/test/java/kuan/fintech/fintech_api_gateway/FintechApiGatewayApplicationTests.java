package kuan.fintech.fintech_api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import kuan.fintech.fintech_api_gateway.filter.CorrelationIdGatewayFilter;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FintechApiGatewayApplicationTests {

	private static final AtomicReference<String> LAST_CORRELATION_ID = new AtomicReference<>();
	private static final ExecutorService BACKEND_EXECUTOR = Executors.newSingleThreadExecutor();
	private static final HttpServer BACKEND = startBackend();
	private static final String BACKEND_URI = "http://localhost:" + BACKEND.getAddress().getPort();

	@Autowired
	private RouteDefinitionLocator routeDefinitionLocator;

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
		assertThat(port).isPositive();
	}

	@Test
	void exposesGatewayInfo() {
		WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build()
				.get()
				.uri("/gateway/info")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.service").isEqualTo("fintech-api-gateway")
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	void exposesHealth() {
		WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build()
				.get()
				.uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	void configuresExpectedRoutes() {
		StepVerifier.create(routeDefinitionLocator.getRouteDefinitions()
						.map(RouteDefinition::getId)
						.collectList())
				.assertNext(routeIds -> assertThat(routeIds).containsAll(Set.of(
						"auth-service",
						"user-service",
						"kyc-service",
						"wallet-service",
						"ledger-service",
						"payment-service",
						"risk-service",
						"loan-service"
				)))
				.verifyComplete();
	}

	@Test
	void createsCorrelationIdWhenMissing() {
		LAST_CORRELATION_ID.set(null);

		WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build()
				.get()
				.uri("/api/auth/ping")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().exists(CorrelationIdGatewayFilter.CORRELATION_ID_HEADER)
				.expectBody()
				.jsonPath("$.path").isEqualTo("/auth/ping");

		assertThat(LAST_CORRELATION_ID.get()).isNotBlank();
	}

	@Test
	void forwardsExistingCorrelationId() {
		LAST_CORRELATION_ID.set(null);
		String correlationId = "test-corr-001";

		WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build()
				.get()
				.uri("/api/payments/ping")
				.header(CorrelationIdGatewayFilter.CORRELATION_ID_HEADER, correlationId)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(CorrelationIdGatewayFilter.CORRELATION_ID_HEADER, correlationId)
				.expectBody()
				.jsonPath("$.path").isEqualTo("/payments/ping");

		assertThat(LAST_CORRELATION_ID.get()).isEqualTo(correlationId);
	}

	@DynamicPropertySource
	static void configureBackendUris(DynamicPropertyRegistry registry) {
		registry.add("AUTH_SERVICE_URI", () -> BACKEND_URI);
		registry.add("USER_SERVICE_URI", () -> BACKEND_URI);
		registry.add("KYC_SERVICE_URI", () -> BACKEND_URI);
		registry.add("WALLET_SERVICE_URI", () -> BACKEND_URI);
		registry.add("LEDGER_SERVICE_URI", () -> BACKEND_URI);
		registry.add("PAYMENT_SERVICE_URI", () -> BACKEND_URI);
		registry.add("RISK_SERVICE_URI", () -> BACKEND_URI);
		registry.add("LOAN_SERVICE_URI", () -> BACKEND_URI);
	}

	@AfterAll
	static void stopBackend() {
		BACKEND.stop(0);
		BACKEND_EXECUTOR.shutdownNow();
	}

	private static HttpServer startBackend() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
			server.createContext("/", exchange -> {
				LAST_CORRELATION_ID.set(exchange.getRequestHeaders()
						.getFirst(CorrelationIdGatewayFilter.CORRELATION_ID_HEADER));

				byte[] body = ("""
						{"path":"%s"}
						""".formatted(exchange.getRequestURI().getPath())).getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);
				try (var responseBody = exchange.getResponseBody()) {
					responseBody.write(body);
				}
			});
			server.setExecutor(BACKEND_EXECUTOR);
			server.start();
			return server;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to start mock backend", ex);
		}
	}

}
