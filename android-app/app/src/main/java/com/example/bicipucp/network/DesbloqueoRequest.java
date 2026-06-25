package com.example.bicipucp.network;

public class DesbloqueoRequest {
    private String codigo;
    private String pin;

    public DesbloqueoRequest(String codigo, String pin) {
        this.codigo = codigo;
        this.pin = pin;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
