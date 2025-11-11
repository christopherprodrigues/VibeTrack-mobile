package br.ufpr.vibetrack.mobile.data.remote;

import br.ufpr.vibetrack.mobile.data.model.ExperimentResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    /**
     * Envia os dados de resultado de um experimento para o backend.
     */
    // Corrigindo para o endpoint que parece ser o correto
    @POST("/api/dados-biometricos/mobile-data")
    Call<Void> submitExperimentResult(@Body ExperimentResult result);

}