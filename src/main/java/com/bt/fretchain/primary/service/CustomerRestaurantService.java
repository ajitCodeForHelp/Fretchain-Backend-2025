package com.bt.fretchain.primary.service;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.primary.dto.CustomerRestaurantDTO;

import java.util.List;

/**
 * Customer browsing of restaurants.
 *
 *   1. getCuisines             -> chips on home screen (Pizza, Biryani, Chinese...)
 *   2. getNearbyRestaurants    -> restaurants around the customer, filter by cuisine, sort
 *   3. getRestaurantDetail     -> restaurant header + menu sections
 *   4. getRestaurantMenu       -> header + full menu with dishes in one call
 *      getDishes               -> dishes of a section, paged (large menus)
 *   *. search                  -> one search box: cuisines + restaurants + dishes
 */
public interface CustomerRestaurantService {

    List<CustomerRestaurantDTO.CuisineCard> getCuisines();

    DataTableResponsePacket<CustomerRestaurantDTO.RestaurantCard> getNearbyRestaurants(
            CustomerRestaurantDTO.NearbyRestaurantRequest request);

    CustomerRestaurantDTO.RestaurantDetailResponse getRestaurantDetail(
            String restaurantUuid, Double latitude, Double longitude);

    CustomerRestaurantDTO.RestaurantDetailResponse getRestaurantMenu(
            String restaurantUuid, Double latitude, Double longitude);

    DataTableResponsePacket<CustomerRestaurantDTO.DishCard> getDishes(
            String restaurantUuid, CustomerRestaurantDTO.DishRequest request);

    CustomerRestaurantDTO.SearchResponse search(CustomerRestaurantDTO.SearchRequest request);
}
