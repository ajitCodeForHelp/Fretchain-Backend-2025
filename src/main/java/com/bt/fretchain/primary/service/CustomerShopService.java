package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.CustomerShopDTO;

import java.util.List;

/**
 * Customer browsing of shops.
 *
 *   1. getGlobalCategories -> chips on home screen (Grocery, Pharmacy...)
 *   2. getNearbyShops      -> shops around the customer, filter by chip, sort
 *   3. getShopDetail       -> shop header + its category/subcategory tree
 *   4. getShopItems        -> items of a selected category/subcategory (paged)
 *   *. search              -> one search box: categories + shops + items
 */
public interface CustomerShopService {

    List<CustomerShopDTO.GlobalCategoryCard> getGlobalCategories();

    DataTableResponsePacket<CustomerShopDTO.ShopCard> getNearbyShops(
            CustomerShopDTO.NearbyShopRequest request);

    CustomerShopDTO.ShopDetailResponse getShopDetail(
            String shopUuid, Double latitude, Double longitude);

    DataTableResponsePacket<CustomerShopDTO.ItemCard> getShopItems(
            String shopUuid, CustomerShopDTO.ShopItemsRequest request);

    CustomerShopDTO.SearchResponse search(CustomerShopDTO.SearchRequest request);
}
