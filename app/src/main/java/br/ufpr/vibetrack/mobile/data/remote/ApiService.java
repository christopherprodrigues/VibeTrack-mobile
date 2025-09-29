package br.ufpr.vibetrack.mobile.data.remote;

import br.ufpr.vibetrack.mobile.data.model.ExperimentResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    /**
     * Envia os dados de resultado de um experimento para o backend.
     * @param result O objeto contendo todos os dados do experimento.
     * @return Uma chamada que pode ser executada. Usamos Void como resposta
     * quando não esperamos nenhum corpo de dados de volta do servidor,
     * apenas uma confirmação de sucesso (código 2xx).
     */
    @POST("results")
    Call<Void> submitExperimentResult(@Body ExperimentResult result);

}