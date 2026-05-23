package com.realestate.service;

import com.google.gson.*;
import com.realestate.config.SupabaseConfig;
import okhttp3.*;

import java.io.IOException;

public class CardService {

    public static JsonArray getSavedCards() throws IOException {
        String url = "/rest/v1/saved_cards?user_id=eq." + SupabaseConfig.getCurrentUserId()
                + "&order=is_default.desc,created_at.desc";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            String body = r.body().string();
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonArray()) throw new IOException("Ошибка загрузки карт: " + body);
            return parsed.getAsJsonArray();
        }
    }

    public static void saveCard(String last4, String holder, String expiry, boolean isDefault) throws IOException {
        if (isDefault) clearDefault();
        String body = String.format(
                "{\"user_id\":\"%s\",\"card_last4\":\"%s\",\"card_holder\":\"%s\",\"card_expiry\":\"%s\",\"is_default\":%b}",
                SupabaseConfig.getCurrentUserId(), last4, holder, expiry, isDefault);
        Request req = SupabaseConfig.req("/rest/v1/saved_cards")
                .post(RequestBody.create(body, SupabaseConfig.JSON))
                .header("Prefer", "return=minimal").build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (!r.isSuccessful()) {
                String err = r.body() != null ? r.body().string() : "";
                throw new IOException("Ошибка сохранения карты: " + err);
            }
        }
    }

    public static void deleteCard(String cardId) throws IOException {
        Request req = SupabaseConfig.req("/rest/v1/saved_cards?id=eq." + cardId).delete().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (r.body() != null) r.body().close();
        }
    }

    public static void setDefault(String cardId) throws IOException {
        clearDefault();
        String body = "{\"is_default\":true}";
        Request req = SupabaseConfig.req("/rest/v1/saved_cards?id=eq." + cardId)
                .patch(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (r.body() != null) r.body().close();
        }
    }

    private static void clearDefault() throws IOException {
        String body = "{\"is_default\":false}";
        Request req = SupabaseConfig.req(
                        "/rest/v1/saved_cards?user_id=eq." + SupabaseConfig.getCurrentUserId())
                .patch(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (r.body() != null) r.body().close();
        }
    }
}
