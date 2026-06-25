package com.example.bicipucp.network;

import com.google.gson.annotations.SerializedName;

public class DesbloqueoResponse {
    private String status;

    @SerializedName("iot_auth_token")
    private String iotAuthToken;

    @SerializedName("desbloqueo_expira_en")
    private int desbloqueoExpiraEn;

    @SerializedName("timestamp_aprobacion")
    private String timestampAprobacion;

    public String getStatus() { return status; }
    public String getIotAuthToken() { return iotAuthToken; }
    public int getDesbloqueoExpiraEn() { return desbloqueoExpiraEn; }
    public String getTimestampAprobacion() { return timestampAprobacion; }
}
