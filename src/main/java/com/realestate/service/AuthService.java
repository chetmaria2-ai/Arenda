package com.realestate.service;

import com.google.gson.*;
import com.realestate.config.SupabaseConfig;
import okhttp3.*;

import java.io.IOException;

public class AuthService {

    private static final Gson gson = new Gson();

    public record AuthResult(boolean ok, String error,
                             String userId, String accessToken, String refreshToken,
                             String role, boolean isBlocked) {
    }

    public static AuthResult register(String email, String password,
                                      String fullName, String phone) throws IOException {
        JsonObject data = new JsonObject();
        data.addProperty("full_name", fullName);
        data.addProperty("phone", phone);

        JsonObject payload = new JsonObject();
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        payload.add("data", data);

        String body = gson.toJson(payload);

        Request req = SupabaseConfig.req("/auth/v1/signup")
                .post(RequestBody.create(body, SupabaseConfig.JSON))
                .build();

        try (Response resp = SupabaseConfig.http().newCall(req).execute()) {
            JsonObject json = JsonParser.parseString(resp.body().string()).getAsJsonObject();
            if (!resp.isSuccessful() || json.has("error"))
                return fail(json);
            return parseSession(json);
        }
    }

    public static AuthResult login(String email, String password) throws IOException {
        JsonObject loginPayload = new JsonObject();
        loginPayload.addProperty("email", email);
        loginPayload.addProperty("password", password);
        String body = gson.toJson(loginPayload);
        Request req = SupabaseConfig.req("/auth/v1/token?grant_type=password")
                .post(RequestBody.create(body, SupabaseConfig.JSON))
                .build();

        try (Response resp = SupabaseConfig.http().newCall(req).execute()) {
            JsonObject json = JsonParser.parseString(resp.body().string()).getAsJsonObject();
            if (!resp.isSuccessful() || json.has("error"))
                return fail(json);

            AuthResult session = parseSession(json);
            if (session.ok()) {

                return fetchProfile(session);
            }
            return session;
        }
    }

    public static void updateEmail(String newEmail) throws IOException {
        String body = "{\"email\":\"" + newEmail + "\"}";
        Request req = SupabaseConfig.req("/auth/v1/user")
                .put(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (!r.isSuccessful()) {
                String rb = r.body().string();
                JsonObject err = JsonParser.parseString(rb).getAsJsonObject();
                throw new IOException(err.has("msg") ? err.get("msg").getAsString() : rb);
            }
        }
    }

    public static void updatePassword(String newPassword) throws IOException {
        String body = "{\"password\":\"" + newPassword + "\"}";
        Request req = SupabaseConfig.req("/auth/v1/user")
                .put(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (!r.isSuccessful()) {
                String rb = r.body().string();
                JsonObject err = JsonParser.parseString(rb).getAsJsonObject();
                throw new IOException(err.has("msg") ? err.get("msg").getAsString() : rb);
            }
        }
    }

    public static void logout() throws IOException {
        Request req = SupabaseConfig.req("/auth/v1/logout")
                .post(RequestBody.create("{}", SupabaseConfig.JSON))
                .build();
        SupabaseConfig.http().newCall(req).execute().close();
        SupabaseConfig.clearSession();
    }

    private static AuthResult parseSession(JsonObject json) {
        JsonObject user = json.getAsJsonObject("user");
        String userId = user != null ? user.get("id").getAsString() : null;
        String access = json.has("access_token") ? json.get("access_token").getAsString() : null;
        String refresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
        if (access != null) SupabaseConfig.setSession(access, refresh, userId);
        return new AuthResult(true, null, userId, access, refresh, "user", false);
    }

    private static AuthResult fetchProfile(AuthResult session) throws IOException {
        Request req = SupabaseConfig.req("/rest/v1/profiles?id=eq." + session.userId()
                        + "&select=role,is_blocked")
                .get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            JsonArray arr = JsonParser.parseString(r.body().string()).getAsJsonArray();
            if (arr.size() > 0) {
                JsonObject p = arr.get(0).getAsJsonObject();
                String role = p.get("role").getAsString();
                boolean blocked = p.get("is_blocked").getAsBoolean();
                return new AuthResult(true, null, session.userId(),
                        session.accessToken(), session.refreshToken(), role, blocked);
            }
        }
        return session;
    }

    private static AuthResult fail(JsonObject json) {
        String msg = json.has("error_description") ? json.get("error_description").getAsString()
                : json.has("msg") ? json.get("msg").getAsString()
                : "Ошибка авторизации";
        return new AuthResult(false, msg, null, null, null, null, false);
    }
}
