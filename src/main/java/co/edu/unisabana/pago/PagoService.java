package co.edu.unisabana.pago;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PagoService {

    private final WebClient webClient;
    private static final String LOG_FILE = "registro_notificaciones.txt";
    private final CircuitBreaker circuitBreaker;

    public PagoService(WebClient.Builder webClientBuilder, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("notificacionCircuitBreaker");
    }

    public CompletableFuture<ResponseEntity<String>> enviarNotificacion() {
        return intentarEnvio("Aldeamo")
                .onErrorResume(e -> {
                    log.warn("Circuit Breaker activado. Intentando con Twilio...");
                    return fallbackNotificacion();
                })
                .toFuture()
                .exceptionally(e -> {
                    log.error("Todos los proveedores fallaron. Error: {}", e.getMessage());
                    return ResponseEntity.status(500).body("Todos los proveedores fallaron");
                });
    }

    private Mono<ResponseEntity<String>> intentarEnvio(String proveedor) {
        String url = "http://localhost:" + (proveedor.equalsIgnoreCase("Aldeamo") ? "8081" : "8082") + "/notificacion";

        log.info("Enviando notificación a: {}", proveedor);

        return webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    log.info("Notificación enviada a: {}", proveedor);
                    registrarEnArchivo(proveedor);
                    return ResponseEntity.ok(proveedor);
                })
                .doOnError(e -> log.warn("Error con {}: {}", proveedor, e.getMessage()))
                .onErrorResume(e -> {
                    circuitBreaker.onError(Duration.ofSeconds(1).toMillis(), TimeUnit.MILLISECONDS, e);
                    return Mono.error(new RuntimeException("Fallo en " + proveedor, e));
                });
    }

    private Mono<ResponseEntity<String>> fallbackNotificacion() {
        return intentarEnvio("Twilio")
                .doOnError(e -> log.warn("Twilio también falló: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Todos los proveedores fallaron. No se pudo enviar la notificación.");
                    return Mono.just(ResponseEntity.status(500).body("Todos los proveedores fallaron"));
                });
    }

    private void registrarEnArchivo(String proveedor) {
        try {
            String mensaje = LocalDateTime.now() + " - Notificación enviada a: " + proveedor + "\n";
            Files.write(Paths.get(LOG_FILE), mensaje.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Error en el registro de notificación", e);
        }
    }
}

