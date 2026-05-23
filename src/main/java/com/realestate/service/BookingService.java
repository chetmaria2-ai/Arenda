package com.realestate.service;

import com.google.gson.*;
import com.realestate.config.SupabaseConfig;
import okhttp3.*;

import java.io.IOException;

public class BookingService {

    private static final Gson gson = new Gson();

    public static JsonObject createBooking(
            String listingId,
            String startDate,
            String endDate,
            double prepayment,
            String cardLast4,
            String cardHolder,
            String cardExpiry,
            String comment) throws IOException {

        JsonObject b = new JsonObject();
        b.addProperty("listing_id", listingId);
        b.addProperty("tenant_id", SupabaseConfig.getCurrentUserId());
        b.addProperty("start_date", startDate);
        if (endDate != null && !endDate.isBlank()) b.addProperty("end_date", endDate);
        b.addProperty("prepayment", prepayment);
        if (cardLast4 != null) b.addProperty("card_last4", cardLast4);
        if (cardHolder != null) b.addProperty("card_holder", cardHolder);
        if (cardExpiry != null) b.addProperty("card_expiry", cardExpiry);
        if (comment != null) b.addProperty("comment", comment);

        Request req = SupabaseConfig.restReq("/rest/v1/bookings")
                .post(RequestBody.create(gson.toJson(b), SupabaseConfig.JSON)).build();

        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            String body = r.body().string();
            if (!r.isSuccessful()) {
                JsonObject err = JsonParser.parseString(body).getAsJsonObject();
                throw new IOException(err.has("message") ? err.get("message").getAsString() : body);
            }
            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
            return arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
        }
    }

    public static JsonArray getMyBookings() throws IOException {
        String url = "/rest/v1/bookings?tenant_id=eq." + SupabaseConfig.getCurrentUserId()
                + "&select=*,listings(title,address,price,price_type,listing_images(url))"
                + "&order=created_at.desc";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static JsonArray getBookingsForOwner() throws IOException {
        String ids = getOwnerListingIds();
        if (ids.isEmpty()) return new JsonArray();
        String url = "/rest/v1/bookings?listing_id=in.("
                + ids + ")"
                + "&select=*,profiles!tenant_id(full_name,phone)"
                + "&order=created_at.desc";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return JsonParser.parseString(r.body().string()).getAsJsonArray();
        }
    }

    public static boolean cancelBooking(String bookingId) throws IOException {
        String body = "{\"status\":\"cancelled\"}";
        Request req = SupabaseConfig.req("/rest/v1/bookings?id=eq." + bookingId
                        + "&tenant_id=eq." + SupabaseConfig.getCurrentUserId())
                .patch(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return r.isSuccessful();
        }
    }

    public static boolean confirmBooking(String bookingId) throws IOException {
        String body = String.format("{\"p_booking_id\":\"%s\",\"p_actor_id\":\"%s\"}",
                bookingId, SupabaseConfig.getCurrentUserId());
        Request req = SupabaseConfig.req("/rest/v1/rpc/confirm_booking")
                .post(RequestBody.create(body, SupabaseConfig.JSON)).build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            return r.isSuccessful();
        }
    }

    private static String getOwnerListingIds() throws IOException {
        String url = "/rest/v1/listings?owner_id=eq." + SupabaseConfig.getCurrentUserId()
                + "&select=id";
        Request req = SupabaseConfig.req(url).get().build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            JsonArray arr = JsonParser.parseString(r.body().string()).getAsJsonArray();
            StringBuilder ids = new StringBuilder();
            for (JsonElement e : arr) {
                if (ids.length() > 0) ids.append(",");
                ids.append(e.getAsJsonObject().get("id").getAsString());
            }
            return ids.toString();
        }
    }
}
