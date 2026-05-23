package com.realestate.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

class Profile {
    public String id;
    @SerializedName("full_name")
    public String fullName;
    public String phone;
    public String role;
    @SerializedName("is_blocked")
    public boolean isBlocked;
    @SerializedName("avatar_url")
    public String avatarUrl;
    @SerializedName("created_at")
    public OffsetDateTime createdAt;
}

class Listing {
    public String id;
    @SerializedName("owner_id")
    public String ownerId;
    public String title;
    public String description;
    public String address;
    public String city;
    public double price;
    @SerializedName("price_type")
    public String priceType;
    public int rooms;
    @SerializedName("area_sqm")
    public double areaSqm;
    public int floor;
    @SerializedName("total_floors")
    public int totalFloors;
    @SerializedName("has_ac")
    public boolean hasAc;
    @SerializedName("has_parking")
    public boolean hasParking;
    @SerializedName("pets_allowed")
    public boolean petsAllowed;
    public boolean furnished;
    @SerializedName("property_type")
    public String propertyType;
    public String status;
    @SerializedName("created_at")
    public OffsetDateTime createdAt;

    public List<ListingImage> images;
    public Profile owner;
}

class ListingImage {
    public String id;
    @SerializedName("listing_id")
    public String listingId;
    @SerializedName("storage_path")
    public String storagePath;
    public String url;
    @SerializedName("sort_order")
    public int sortOrder;
}

class Booking {
    public String id;
    @SerializedName("listing_id")
    public String listingId;
    @SerializedName("tenant_id")
    public String tenantId;
    @SerializedName("start_date")
    public LocalDate startDate;
    @SerializedName("end_date")
    public LocalDate endDate;
    @SerializedName("total_price")
    public double totalPrice;
    public double prepayment;
    public String status;
    @SerializedName("card_last4")
    public String cardLast4;
    @SerializedName("card_holder")
    public String cardHolder;
    @SerializedName("card_expiry")
    public String cardExpiry;
    public String comment;
    @SerializedName("created_at")
    public OffsetDateTime createdAt;

    public Listing listing;
    public Profile tenant;
}

class Favorite {
    @SerializedName("user_id")
    public String userId;
    @SerializedName("listing_id")
    public String listingId;
    public Listing listing;
}
