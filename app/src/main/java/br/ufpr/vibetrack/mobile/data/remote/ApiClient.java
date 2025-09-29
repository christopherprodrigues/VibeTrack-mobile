package br.ufpr.vibetrack.mobile.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // IMPORTANTE: Substitua pela URL real do seu backend.
    // Use http://10.0.2.2:8080 para se conectar ao localhost do seu computador
    // a partir do emulador Android.
    private static final String BASE_URL = "http://10.0.2.2:8080";

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}