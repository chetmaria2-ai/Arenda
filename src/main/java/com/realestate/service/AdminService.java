package com.realestate.service;

import com.google.gson.*;
import com.realestate.config.SupabaseConfig;
import okhttp3.*;

import java.io.IOException;

public class AdminService {

    public static boolean blockUser(String userId, boolean block) throws IOException {
        String body = String.format("{\"p_admin_id\":\"%s\",\"p_user_id\":\"%s\",\"p_blocked\":%b}",
                SupabaseConfig.getCurrentUserId(), userId, block);
        Request req = SupabaseConfig.req("/rest/v1/rpc/admin_set_block")
                .post(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return r.isSuccessful();
        }
    }

    public static JsonArray getAllUsers() throws IOException {
        Request req = SupabaseConfig.req(
                "/rest/v1/profiles?select=id,full_name,phone,role,is_blocked,created_at"
                        + "&order=created_at.desc").get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static JsonArray getAllListings() throws IOException {
        Request req = SupabaseConfig.req(
                "/rest/v1/listings?select=id,title,status,owner_id,created_at," +
                        "profiles!owner_id(full_name)&order=created_at.desc").get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static JsonArray getAdminLogs() throws IOException {
        String url = "/rest/v1/admin_logs"
                + "?select=id,action,target_type,target_id,details,created_at,"
                + "profiles!admin_id(full_name)"
                + "&order=created_at.desc"
                + "&limit=200";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            String body = r.body().string();
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonArray()) throw new IOException("Ошибка загрузки логов: " + body);
            return parsed.getAsJsonArray();
        }
    }
}
