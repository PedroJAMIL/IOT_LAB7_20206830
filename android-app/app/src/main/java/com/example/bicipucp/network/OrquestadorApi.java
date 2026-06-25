package com.example.bicipucp.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OrquestadorApi {
    @POST("/bici/solicitar-desbloqueo")
    Call<DesbloqueoResponse> solicitarDesbloqueo(@Body DesbloqueoRequest request);
}
