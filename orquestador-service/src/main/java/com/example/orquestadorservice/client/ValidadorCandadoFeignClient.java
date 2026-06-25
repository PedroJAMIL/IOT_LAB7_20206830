package com.example.orquestadorservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "pucp-validador-service")
public interface ValidadorCandadoFeignClient {

    @GetMapping("/validar/candado/{pin}")
    boolean validarCandado(@PathVariable("pin") String pin);
}
