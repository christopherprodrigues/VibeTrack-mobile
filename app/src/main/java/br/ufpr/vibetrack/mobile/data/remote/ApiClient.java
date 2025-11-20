package br.ufpr.vibetrack.mobile.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 1. Sua URL base.
    //    IMPORTANTE: Adicionei a barra "/" no final. O Retrofit precisa dela.
    private static final String BASE_URL = "http://192.168.7.2:8080/";

    private static Retrofit retrofit = null;
    private static ApiService apiService = null; // Instância do serviço

    // 2. Método privado para construir o Retrofit (só uma vez)
    private static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    // 3. Adiciona o conversor Gson (necessário para enviar o ExperimentResult)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // 4. ESTE É O MÉTODO QUE FALTAVA!
    //    É ele que sua MainActivity está tentando chamar.
    public static ApiService getApiService() {
        if (apiService == null) {
            // 5. O Retrofit "cria" o código da interface para você
            apiService = getClient().create(ApiService.class);
        }
        return apiService;
    }

    // 6. APAGUE a interface "ApiService" que estava aqui dentro.
    //    Ela era redundante e estava errada.
}