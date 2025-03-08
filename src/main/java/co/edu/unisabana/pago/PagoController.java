package co.edu.unisabana.pago;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

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

        registrarEnvio(proveedor); // Escribir en el archivo de logs

        return ResponseEntity.ok("Notificación enviada: " + response);
    }

    public ResponseEntity<String> fallbackNotificacion(String proveedor, Throwable t) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallo el servicio " + proveedor + ". Intentar más tarde.");
    }

    private void registrarEnvio(String proveedor) {
        String mensaje = "Notificación enviada a " + proveedor + " en " + LocalDateTime.now() + "\n";
        try {
            Files.write(Paths.get("notificaciones.log"), mensaje.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace(); // O usar un logger en producción
        }
    }
}
