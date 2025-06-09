package dpa.interop.auto_ontology.lmstudio_connect;

import com.google.gson.Gson;
import dpa.interop.auto_ontology.lmstudio_connect.dto.ChatRequest;
import dpa.interop.auto_ontology.lmstudio_connect.dto.ChatResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ChatService {

    private static final String API_URL = "http://127.0.0.1:1234/api/v0/chat/completions";
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    public ChatResponse sendChatRequest(ChatRequest request) throws IOException {
        String jsonBody = gson.toJson(request);

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request httpRequest = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // Десериализуем ответ
                return gson.fromJson(response.body().charStream(), ChatResponse.class);
            } else {
                throw new IOException("Failed to get response: " + response.code() + ", " + response.message());
            }
        }
    }
}
