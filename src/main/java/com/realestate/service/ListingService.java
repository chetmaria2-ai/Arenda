package com.realestate.service;

import com.google.gson.*;
import com.realestate.config.SupabaseConfig;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ListingService {

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").create();

    public static JsonArray getListings(Map<String, String> filters) throws IOException {
        StringBuilder url = new StringBuilder(
                "/rest/v1/listings?status=eq.active&select=*,listing_images(*),profiles!owner_id(full_name,phone)");

        if (filters != null) {
            if (filters.containsKey("city"))
                url.append("&city=ilike.*")
                        .append(URLEncoder.encode(filters.get("city"), StandardCharsets.UTF_8))
                        .append("*");
            if (filters.containsKey("price_min"))
                url.append("&price=gte.").append(filters.get("price_min"));
            if (filters.containsKey("price_max"))
                url.append("&price=lte.").append(filters.get("price_max"));
            if (filters.containsKey("rooms"))
                url.append("&rooms=eq.").append(filters.get("rooms"));
            if (filters.containsKey("property_type"))
                url.append("&property_type=eq.").append(filters.get("property_type"));
            if (filters.containsKey("pets_allowed"))
                url.append("&pets_allowed=eq.").append(filters.get("pets_allowed"));
            if (filters.containsKey("has_ac"))
                url.append("&has_ac=eq.").append(filters.get("has_ac"));
        }
        url.append("&order=created_at.desc");

        Request req = SupabaseConfig.req(url.toString()).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static JsonArray getListingsGuest() throws IOException {
        return getListingsGuest(null);
    }

    public static JsonArray getListingsGuest(Map<String, String> filters) throws IOException {
        StringBuilder url = new StringBuilder(
                "/rest/v1/listings?status=eq.active" +
                        "&select=id,title,address,city,price,price_type,rooms,area_sqm," +
                        "has_ac,pets_allowed,property_type,listing_images(url,sort_order)");

        if (filters != null) {
            if (filters.containsKey("city"))
                url.append("&city=ilike.*")
                        .append(URLEncoder.encode(filters.get("city"), StandardCharsets.UTF_8))
                        .append("*");
            if (filters.containsKey("price_min"))
                url.append("&price=gte.").append(filters.get("price_min"));
            if (filters.containsKey("price_max"))
                url.append("&price=lte.").append(filters.get("price_max"));
            if (filters.containsKey("rooms"))
                url.append("&rooms=eq.").append(filters.get("rooms"));
            if (filters.containsKey("property_type"))
                url.append("&property_type=eq.").append(filters.get("property_type"));
            if (filters.containsKey("pets_allowed"))
                url.append("&pets_allowed=eq.").append(filters.get("pets_allowed"));
            if (filters.containsKey("has_ac"))
                url.append("&has_ac=eq.").append(filters.get("has_ac"));
        }

        url.append("&order=created_at.desc");
        Request req = SupabaseConfig.req(url.toString()).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static JsonObject getListing(String id, boolean includeContacts) throws IOException {
        String select = includeContacts
                ? "*,listing_images(*),profiles!owner_id(id,full_name,phone,avatar_url)"
                : "*,listing_images(url,sort_order)";
        String url = "/rest/v1/listings?id=eq." + id + "&select=" + select;
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            JsonArray arr = JsonParser.parseString(r.body().string()).getAsJsonArray();
            return arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
        }
    }

    public static JsonArray getMyListings() throws IOException {
        String url = "/rest/v1/listings?owner_id=eq." + SupabaseConfig.getCurrentUserId()
                + "&select=*,listing_images(*)" +
                "&order=created_at.desc";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static JsonObject createListing(Map<String, Object> data) throws IOException {
        data.put("owner_id", SupabaseConfig.getCurrentUserId());
        String body = gson.toJson(data);
        Request req = SupabaseConfig.restReq("/rest/v1/listings")
                .post(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            String responseBody = r.body().string();
            JsonElement parsed = JsonParser.parseString(responseBody);
            if (!r.isSuccessful()) {

                String errMsg = responseBody;
                if (parsed.isJsonObject()) {
                    JsonObject err = parsed.getAsJsonObject();
                    if (err.has("message")) errMsg = err.get("message").getAsString();
                }
                throw new IOException(errMsg);
            }
            if (parsed.isJsonArray()) {
                JsonArray arr = parsed.getAsJsonArray();
                return arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
            }
            return parsed.getAsJsonObject();
        }
    }

    public static boolean updateListing(String id, Map<String, Object> data) throws IOException {
        String body = gson.toJson(data);
        Request req = SupabaseConfig.restReq("/rest/v1/listings?id=eq." + id)
                .patch(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (!r.isSuccessful()) {
                String rb = r.body().string();
                JsonElement parsed = JsonParser.parseString(rb);
                String errMsg = rb;
                if (parsed.isJsonObject() && parsed.getAsJsonObject().has("message"))
                    errMsg = parsed.getAsJsonObject().get("message").getAsString();
                throw new IOException(errMsg);
            }
            return true;
        }
    }

    public static boolean deleteListing(String id) throws IOException {
        return updateListing(id, new HashMap<>() {{
            put("status", "deleted");
        }});
    }

    public static boolean adminDeleteListing(String id, String reason) throws IOException {
        String body = String.format("{\"p_admin_id\":\"%s\",\"p_listing_id\":\"%s\",\"p_reason\":\"%s\"}",
                SupabaseConfig.getCurrentUserId(), id, reason);
        Request req = SupabaseConfig.req("/rest/v1/rpc/admin_delete_listing")
                .post(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return r.isSuccessful();
        }
    }
}
