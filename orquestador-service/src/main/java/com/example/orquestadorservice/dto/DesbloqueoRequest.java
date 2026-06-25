package com.example.orquestadorservice.dto;

public class DesbloqueoRequest {
    private String codigo;
    private String pin;

    public DesbloqueoRequest() {}

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
