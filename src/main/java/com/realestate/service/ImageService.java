package com.realestate.service;

import com.realestate.config.SupabaseConfig;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class ImageService {

    public static String uploadImage(File file, String listingId) throws IOException {
        String ext = file.getName().replaceAll(".*\\.", "");
        String path = SupabaseConfig.getCurrentUserId() + "/" + listingId + "/" + UUID.randomUUID() + "." + ext;
        String mime = Files.probeContentType(file.toPath());

        RequestBody fileBody = RequestBody.create(file, MediaType.get(mime != null ? mime : "image/jpeg"));
        Request req = SupabaseConfig.req("/storage/v1/object/listing-images/" + path)
                .post(fileBody)
                .header("Content-Type", mime != null ? mime : "image/jpeg")
                .build();

        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (!r.isSuccessful()) throw new IOException("Upload failed: " + r.code());
            String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/listing-images/" + path;
            saveImageRecord(listingId, path, publicUrl);
            return publicUrl;
        }
    }

    private static void saveImageRecord(String listingId, String storagePath, String url) throws IOException {
        String body = String.format("{\"listing_id\":\"%s\",\"storage_path\":\"%s\",\"url\":\"%s\"}",
                listingId, storagePath, url);
        Request req = SupabaseConfig.req("/rest/v1/listing_images")
                .post(RequestBody.create(body, SupabaseConfig.JSON))
                .header("Prefer", "return=minimal")
                .build();
        try (Response r = SupabaseConfig.http().newCall(req).execute()) {
            if (!r.isSuccessful()) {
                String errBody = r.body() != null ? r.body().string() : "no body";
                throw new IOException("Ошибка сохранения записи об изображении: " + r.code() + " " + errBody);
            }
            if (r.body() != null) r.body().close();
        }
    }

    public static boolean deleteImage(String imageId, String storagePath) throws IOException {

        Request del1 = SupabaseConfig.req("/storage/v1/object/listing-images/" + storagePath)
                .delete().build();
        try (Response r1 = SupabaseConfig.http().newCall(del1).execute()) {
            if (r1.body() != null) r1.body().close();
        }

        Request del2 = SupabaseConfig.req("/rest/v1/listing_images?id=eq." + imageId)
                .delete().build();
        try (Response r = SupabaseConfig.http().newCall(del2).execute()) {
            return r.isSuccessful();
        }
    }
}
