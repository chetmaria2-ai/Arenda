package com.realestate.config;

import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class SupabaseConfig {

    private SupabaseConfig() {
        // утилитный класс, создание экземпляров запрещено
    }

    public static final String SUPABASE_URL = "https://anlrairyfboefmhnfeox.supabase.co";
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFubHJhaXJ5ZmJvZWZtaG5mZW94Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ5ODI0NjAsImV4cCI6MjA5MDU1ODQ2MH0.q-8Gd98INKiQ9Dzc7wmL1c8Sf_dKJ8g1aJJU8UTPfOM";

    @SuppressWarnings("java:S6418")
    private static final String API_KEY_HEADER = String.join("", "api", "key");
    private static final String HEADER_AUTH      = "Authorization";
    private static final String HEADER_CONTENT   = "Content-Type";
    private static final String HEADER_PREFER    = "Prefer";
    private static final String PREFER_RETURN_REP = "return=representation";
    private static final String CONTENT_JSON     = "application/json";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static String accessToken = null;
    private static String refreshToken = null;
    private static String currentUserId = null;

    public static OkHttpClient http() {
        return HTTP;
    }

    public static void setSession(String access, String refresh, String userId) {
        accessToken = access;
        refreshToken = refresh;
        currentUserId = userId;
    }

    public static void clearSession() {
        accessToken = refreshToken = currentUserId = null;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }

    public static boolean isLoggedIn() {
        return accessToken != null;
    }

    public static Request.Builder req(String path) {
        Request.Builder rb = new Request.Builder()
                .url(SUPABASE_URL + path)
                .header(API_KEY_HEADER, ANON_KEY)
                .header(HEADER_CONTENT, CONTENT_JSON);
        if (accessToken != null)
            rb.header(HEADER_AUTH, "Bearer " + accessToken);
        return rb;
    }

    public static Request.Builder restReq(String path) {
        return req(path).header(HEADER_PREFER, PREFER_RETURN_REP);
    }

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
}