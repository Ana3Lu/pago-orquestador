package co.edu.unisabana.pago;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/pago")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/notificar")
    public CompletableFuture<ResponseEntity<String>> enviarNotificacion() {
        return pagoService.enviarNotificacion();
    }
}

