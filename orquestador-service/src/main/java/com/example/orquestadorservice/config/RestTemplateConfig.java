package com.example.orquestadorservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate con balanceo de carga via Eureka.
     * Permite invocar microservicios por su spring.application.name
     * en lugar de por host:puerto, ej:
     *   restTemplate.getForObject("http://pucp-validador-service/validar/alumno/{codigo}", ...)
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
