package com.example.stock.util;

import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求工具类
 */
@Component
public class HttpUtil {

    private final OkHttpClient client;

    public HttpUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException IO异常
     */
    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return "";
    }

    /**
     * 发送带User-Agent的GET请求
     *
     * @param url         请求URL
     * @param userAgent   User-Agent字符串
     * @return 响应内容
     * @throws IOException IO异常
     */
    public String getWithUserAgent(String url, String userAgent) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return "";
    }

    /**
     * 发送POST请求
     *
     * @param url  请求URL
     * @param json JSON请求体
     * @return 响应内容
     * @throws IOException IO异常
     */
    public String postJson(String url, String json) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return "";
    }
}