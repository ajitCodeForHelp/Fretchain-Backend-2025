package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.CustomerMarketplaceDTO;
import com.bt.fretchain.primary.pojo.enums.BusinessType;

import java.util.List;

/**
 * Customer browsing for Shops and Restaurants (same flow, different businessType).
 *
 * Flow:
 *   1. getGlobalCategories   -> chips on home screen (Grocery, Pharmacy / Pizza, Biryani)
 *   2. getNearbyShops        -> stores around the customer, filter by chip, sort
 *   3. getShopDetail         -> store header + its category/subcategory tree
 *   4. getShopItems          -> items of a selected category/subcategory (paged)
 *      getShopMenu           -> whole menu in one call (restaurants)
 *   *. search                -> one search box: categories + stores + items
 */
public interface CustomerMarketplaceService {

    List<CustomerMarketplaceDTO.GlobalCategoryCard> getGlobalCategories(
            BusinessType businessType);

    DataTableResponsePacket<CustomerMarketplaceDTO.ShopCard> getNearbyShops(
            BusinessType businessType,
            CustomerMarketplaceDTO.NearbyShopRequest request);

    CustomerMarketplaceDTO.ShopDetailResponse getShopDetail(
            BusinessType businessType, String shopUuid,
            Double latitude, Double longitude);

    CustomerMarketplaceDTO.ShopDetailResponse getShopMenu(
            BusinessType businessType, String shopUuid,
            Double latitude, Double longitude);

    DataTableResponsePacket<CustomerMarketplaceDTO.ItemCard> getShopItems(
            BusinessType businessType, String shopUuid,
            CustomerMarketplaceDTO.ShopItemsRequest request);

    CustomerMarketplaceDTO.SearchResponse search(
            BusinessType businessType,
            CustomerMarketplaceDTO.SearchRequest request);
}
