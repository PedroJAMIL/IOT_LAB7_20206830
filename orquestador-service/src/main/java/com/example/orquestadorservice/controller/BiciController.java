package com.example.orquestadorservice.controller;

import com.example.orquestadorservice.client.ValidadorCandadoFeignClient;
import com.example.orquestadorservice.dto.DesbloqueoRequest;
import com.example.orquestadorservice.dto.DesbloqueoResponse;
import com.example.orquestadorservice.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/bici")
public class BiciController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ValidadorCandadoFeignClient validadorCandadoFeignClient;

    /**
     * Endpoint principal del orquestador.
     *
     * Recibe el JSON { "codigo": "...", "pin": "..." } desde la app Android.
     * Orquesta DOS llamadas al pucp-validador-service:
     *   1. Validacion del codigo PUCP via RestTemplate (load-balanced por Eureka).
     *   2. Validacion del PIN del candado via Feign Client (declarativo).
     *
     * Si ambas validaciones son true -> 200 OK con token IoT y timestamp.
     * Si alguna falla            -> 400 Bad Request con mensaje explicativo.
     */
    @PostMapping("/solicitar-desbloqueo")
    public ResponseEntity<?> solicitarDesbloqueo(@RequestBody DesbloqueoRequest request) {
        String codigo = request.getCodigo();
        String pin = request.getPin();

        System.out.println("=== [Orquestador] Solicitud recibida: codigo=" + codigo + ", pin=" + pin + " ===");

        // Defensa basica de campos nulos
        if (codigo == null || codigo.isBlank() || pin == null || pin.isBlank()) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse("Los campos codigo y pin son obligatorios")
            );
        }

        // VALIDACION 1: codigo PUCP via RestTemplate (load-balanced)
        Boolean codigoValido;
        try {
            String url = "http://pucp-validador-service/validar/alumno/" + codigo;
            codigoValido = restTemplate.getForObject(url, Boolean.class);
            System.out.println("[Orquestador] RestTemplate -> codigoValido=" + codigoValido);
        } catch (Exception e) {
            System.err.println("[Orquestador] Error al validar codigo: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                new ErrorResponse("No se pudo contactar al servicio de validacion de alumno")
            );
        }

        if (codigoValido == null || !codigoValido) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse("El codigo de alumno no existe en la base de datos")
            );
        }

        // VALIDACION 2: PIN del candado via Feign
        boolean pinValido;
        try {
            pinValido = validadorCandadoFeignClient.validarCandado(pin);
            System.out.println("[Orquestador] FeignClient -> pinValido=" + pinValido);
        } catch (Exception e) {
            System.err.println("[Orquestador] Error al validar PIN: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                new ErrorResponse("No se pudo contactar al servicio de validacion del candado")
            );
        }

        if (!pinValido) {
            return ResponseEntity.badRequest().body(
                new ErrorResponse("El PIN del candado IoT contiene digitos repetidos consecutivos")
            );
        }

        // EXITO: armar respuesta con UUID + timestamp ISO
        String tokenUuid = UUID.randomUUID().toString().substring(0, 8);
        String iotAuthToken = "PUCP-BIKE-" + tokenUuid;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        DesbloqueoResponse response = new DesbloqueoResponse(
            "APROBADO",
            iotAuthToken,
            120,
            timestamp
        );

        System.out.println("[Orquestador] APROBADO -> token=" + iotAuthToken + ", timestamp=" + timestamp);

        return ResponseEntity.ok(response);
    }
}
