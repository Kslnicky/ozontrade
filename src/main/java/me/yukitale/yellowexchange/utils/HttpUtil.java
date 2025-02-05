package me.yukitale.yellowexchange.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class HttpUtil {

    private final CloseableHttpClient HTTP_CLIENT = HttpClientBuilder.create().build();

    public HttpGet createGet(String url) {
        return new HttpGet(url);
    }

    @SneakyThrows
    public HttpPost createPost(String url, String data) {
        HttpPost httpPost = new HttpPost(url);

        StringEntity params = new StringEntity(data);
        httpPost.setEntity(params);

        return httpPost;
    }

    public CloseableHttpResponse sendRequest(HttpUriRequest httpUriRequest) throws IOException {
        return HTTP_CLIENT.execute(httpUriRequest);
    }

    public String readAndCloseResponse(CloseableHttpResponse httpResponse) {
        String response = null;
        try {
            response = new String(httpResponse.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            httpResponse.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }
}
