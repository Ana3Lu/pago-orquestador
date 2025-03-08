package co.edu.unisabana.pago;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/pago")
public class PagoController {

    private final WebClient webClient;

    public PagoController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @CircuitBreaker(name = "notificacionCircuitBreaker", fallbackMethod = "fallbackNotificacion")
    @PostMapping("/notificar")
    public ResponseEntity<String> enviarNotificacion(@RequestParam String proveedor) {
        String url = proveedor.equalsIgnoreCase("aldeamo")
                ? "http://localhost:8081/notificacion"
                : "http://localhost:8082/notificacion";

        String response = webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok("Notificación enviada: " + response);
    }

    public ResponseEntity<String> fallbackNotificacion(String proveedor, String mensaje, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallo el servicio " + proveedor + ". Intentar más tarde.");
    }
}

