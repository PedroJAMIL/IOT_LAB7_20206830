package com.example.pucpvalidadorservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validar")
public class ValidacionController {

    /**
     * Valida que el codigo PUCP tenga exactamente 8 digitos numericos
     * y comience con el prefijo "20".
     *
     * Ejemplos:
     *  - 20230145 -> true
     *  - 19984512 -> false (no empieza con 20)
     *  - 2023014  -> false (solo 7 digitos)
     *  - 2023A145 -> false (contiene letra)
     */
    @GetMapping("/alumno/{codigo}")
    public boolean validarAlumno(@PathVariable String codigo) {
        if (codigo == null || codigo.length() != 8) {
            return false;
        }
        if (!codigo.matches("\\d{8}")) {
            return false;
        }
        return codigo.startsWith("20");
    }

    /**
     * Valida la integridad del PIN numerico de 4 digitos.
     * Retorna true SOLO si el PIN no contiene numeros repetidos de forma consecutiva.
     *
     * Ejemplos:
     *  - 1234 -> true (sin repeticiones consecutivas)
     *  - 1010 -> true (los 1 y 0 estan alternados, no consecutivos)
     *  - 1123 -> false (dos 1 consecutivos)
     *  - 5555 -> false (cuatro 5 consecutivos)
     *  - 1233 -> false (dos 3 consecutivos al final)
     */
    @GetMapping("/candado/{pin}")
    public boolean validarCandado(@PathVariable String pin) {
        if (pin == null || pin.length() != 4) {
            return false;
        }
        if (!pin.matches("\\d{4}")) {
            return false;
        }
        for (int i = 0; i < pin.length() - 1; i++) {
            if (pin.charAt(i) == pin.charAt(i + 1)) {
                return false;
            }
        }
        return true;
    }
}
