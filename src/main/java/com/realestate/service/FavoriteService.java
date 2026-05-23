package com.realestate.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.realestate.config.SupabaseConfig;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class FavoriteService {

    public static JsonArray getFavorites() throws IOException {
        String url = "/rest/v1/favorites?user_id=eq." + SupabaseConfig.getCurrentUserId()
                + "&select=listing_id,listings(id,title,address,price,price_type,listing_images(url))"
                + "&order=created_at.desc";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static boolean addFavorite(String listingId) throws IOException {
        String body = String.format("{\"user_id\":\"%s\",\"listing_id\":\"%s\"}",
                SupabaseConfig.getCurrentUserId(), listingId);
        Request req = SupabaseConfig.req("/rest/v1/favorites")
                .post(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return r.isSuccessful();
        }
    }

    public static boolean removeFavorite(String listingId) throws IOException {
        Request req = SupabaseConfig.req(
                "/rest/v1/favorites?user_id=eq." + SupabaseConfig.getCurrentUserId()
                        + "&listing_id=eq." + listingId).delete().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return r.isSuccessful();
        }
    }

    public static boolean isFavorite(String listingId) throws IOException {
        String url = "/rest/v1/favorites?user_id=eq." + SupabaseConfig.getCurrentUserId()
                + "&listing_id=eq." + listingId + "&select=listing_id";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray().size() > 0;
        }
    }
}
