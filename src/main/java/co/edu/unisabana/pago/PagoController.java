package co.edu.unisabana.pago;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/pago")
public class PagoController {

    private final WebClient webClient;
    private static final String LOG_FILE = "registro_notificaciones.txt";

    public PagoController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @CircuitBreaker(name = "notificacionCircuitBreaker", fallbackMethod = "fallbackNotificacion")
    @PostMapping("/notificar")
    public ResponseEntity<String> enviarNotificacion() {
        return enviarNotificacionAProveedor("aldeamo"); // Siempre intenta Aldeamo primero
    }

    private ResponseEntity<String> enviarNotificacionAProveedor(String proveedor) {
        String url = proveedor.equalsIgnoreCase("aldeamo")
                ? "http://localhost:8081/notificacion"
                : "http://localhost:8082/notificacion";

        String response = webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Guardar en archivo que se envió a este proveedor
        registrarEnArchivo(proveedor);

        return ResponseEntity.ok("Notificación enviada a " + proveedor);
    }
    public ResponseEntity<String> fallbackNotificacion(Throwable t) {
        log.warn("Fallo Aldeamo, intentando Twilio...");
        return enviarNotificacionAProveedor("twilio"); // Si Aldeamo falla, intenta Twilio
    }

    private void registrarEnArchivo(String proveedor) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(LocalDateTime.now() + " - Notificación enviada a: " + proveedor + "\n");
        } catch (IOException e) {
            log.error("Error escribiendo en el archivo de registro", e);
        }
    }
}