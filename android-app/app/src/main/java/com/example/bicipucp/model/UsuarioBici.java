package com.example.bicipucp.model;

public class UsuarioBici {
    private String uid;
    private String nombre;
    private String correo;
    private String codigo;
    private String iotAuthToken;
    private int desbloqueoExpiraEn;
    private String timestampAprobacion;
    private String fotoUrl;

    public UsuarioBici() {} // requerido por Firestore

    public UsuarioBici(String uid, String nombre, String correo, String codigo,
                       String iotAuthToken, int desbloqueoExpiraEn, String timestampAprobacion) {
        this.uid = uid;
        this.nombre = nombre;
        this.correo = correo;
        this.codigo = codigo;
        this.iotAuthToken = iotAuthToken;
        this.desbloqueoExpiraEn = desbloqueoExpiraEn;
        this.timestampAprobacion = timestampAprobacion;
        this.fotoUrl = null;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getIotAuthToken() { return iotAuthToken; }
    public void setIotAuthToken(String iotAuthToken) { this.iotAuthToken = iotAuthToken; }
    public int getDesbloqueoExpiraEn() { return desbloqueoExpiraEn; }
    public void setDesbloqueoExpiraEn(int desbloqueoExpiraEn) { this.desbloqueoExpiraEn = desbloqueoExpiraEn; }
    public String getTimestampAprobacion() { return timestampAprobacion; }
    public void setTimestampAprobacion(String timestampAprobacion) { this.timestampAprobacion = timestampAprobacion; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
