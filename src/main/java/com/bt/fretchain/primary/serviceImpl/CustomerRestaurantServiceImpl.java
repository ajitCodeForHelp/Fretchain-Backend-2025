package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.CustomerRestaurantDTO.*;
import com.bt.fretchain.primary.mapper.CustomerRestaurantMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.FoodType;
import com.bt.fretchain.primary.pojo.enums.RestaurantStatus;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.primary.service.CustomerRestaurantService;
import com.bt.fretchain.utils.GeoUtils;
import com.bt.fretchain.utils.PaginationUtils;
import com.bt.fretchain.utils.SearchUtils;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerRestaurantServiceImpl implements CustomerRestaurantService {

    /** Max nearby restaurants whose dishes are considered by the global dish search. */
    private static final int MAX_RESTAURANTS_FOR_DISH_SEARCH = 200;

    private static final int MAX_CUISINE_SEARCH_RESULTS = 10;

    private static final double DEFAULT_RADIUS_KM = 10.0;
    private static final double MAX_RADIUS_KM = 50.0;

    private final RestaurantMasterRepository restaurantRepository;
    private final RestaurantGlobalCategoryRepository cuisineRepository;
    private final RestaurantCategoryRepository categoryRepository;
    private final RestaurantSubCategoryRepository subCategoryRepository;
    private final RestaurantItemRepository itemRepository;
    private final CustomerRestaurantMapper mapper;

    /** Time zone used for "open now". Empty = server default. e.g. Africa/Douala */
    @Value("${marketplace.time-zone:}")
    private String timeZone;

    // =====================================================================
    // 1. Cuisines (home screen chips)
    // =====================================================================

    @Override
    public List<CuisineCard> getCuisines() {
        return cuisineRepository
                .findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameEnAsc()
                .stream()
                .map(mapper::toCuisineCard)
                .toList();
    }

    // =====================================================================
    // 2. Nearby restaurants
    // =====================================================================

    @Override
    public DataTableResponsePacket<RestaurantCard> getNearbyRestaurants(
            NearbyRestaurantRequest request) {

        int pageNumber = request.getPageNumber() == null ? 0 : Math.max(0, request.getPageNumber());
        int pageSize = request.getPageSize() == null ? 20 : Math.max(1, Math.min(50, request.getPageSize()));

        NearbyRestaurantCriteria criteria = new NearbyRestaurantCriteria(
                request.getLatitude(),
                request.getLongitude(),
                radius(request.getRadiusKm()),
                blankToNull(request.getGlobalCategoryUuid()),
                SearchUtils.containsPattern(request.getSearch()),
                Boolean.TRUE.equals(request.getOpenNowOnly()),
                Boolean.TRUE.equals(request.getVegOnly()),
                request.getMinRating(),
                request.getMaxCostForTwo(),
                request.getSortBy(),
                now(),
                pageNumber * pageSize,
                pageSize
        );

        long total = restaurantRepository.countNearbyRestaurants(criteria);
        List<RestaurantCard> cards = total == 0
                ? List.of()
                : toRestaurantCards(restaurantRepository.findNearbyRestaurants(criteria));

        return PaginationUtils.createResponse(
                new PageImpl<>(cards, PageRequest.of(pageNumber, pageSize), total));
    }

    // =====================================================================
    // 3. Restaurant page
    // =====================================================================

    @Override
    public RestaurantDetailResponse getRestaurantDetail(
            String restaurantUuid, Double latitude, Double longitude) {

        RestaurantMaster restaurant = findVisibleRestaurant(restaurantUuid);

        RestaurantDetailResponse response = new RestaurantDetailResponse();
        response.setRestaurant(toRestaurantCard(restaurant, latitude, longitude));
        response.setCategories(buildMenu(restaurant, null));
        return response;
    }

    /** Whole menu with dishes in one call - the usual restaurant page. */
    @Override
    public RestaurantDetailResponse getRestaurantMenu(
            String restaurantUuid, Double latitude, Double longitude) {

        RestaurantMaster restaurant = findVisibleRestaurant(restaurantUuid);

        List<RestaurantItem> dishes = itemRepository.findVisibleMenuItems(restaurant.getRecordId());

        RestaurantDetailResponse response = new RestaurantDetailResponse();
        response.setRestaurant(toRestaurantCard(restaurant, latitude, longitude));
        response.setCategories(buildMenu(restaurant, dishes));
        return response;
    }

    // =====================================================================
    // 4. Dishes of a restaurant (by section / text / veg)
    // =====================================================================

    @Override
    public DataTableResponsePacket<DishCard> getDishes(String restaurantUuid, DishRequest request) {

        RestaurantMaster restaurant = findVisibleRestaurant(restaurantUuid);

        int pageNumber = request.getPageNumber() == null ? 0 : Math.max(0, request.getPageNumber());
        int pageSize = request.getPageSize() == null ? 20 : Math.max(1, Math.min(100, request.getPageSize()));

        Specification<RestaurantItem> spec = (root, query, cb) -> {
            Join<RestaurantItem, RestaurantCategory> category = root.join("category");
            Join<RestaurantItem, RestaurantSubCategory> subCategory = root.join("subCategory", JoinType.LEFT);

            List<Predicate> predicates = visibleDishPredicates(root, category, subCategory, cb);
            predicates.add(cb.equal(root.get("restaurant").get("recordId"), restaurant.getRecordId()));

            if (request.getCategoryUuid() != null && !request.getCategoryUuid().isBlank()) {
                predicates.add(cb.equal(category.get("uuid"), request.getCategoryUuid()));
            }
            if (request.getSubCategoryUuid() != null && !request.getSubCategoryUuid().isBlank()) {
                predicates.add(cb.equal(subCategory.get("uuid"), request.getSubCategoryUuid()));
            }
            if (Boolean.TRUE.equals(request.getVegOnly())) {
                predicates.add(cb.equal(root.get("foodType"), FoodType.VEG));
            }

            String pattern = SearchUtils.containsPattern(request.getSearch());
            if (pattern != null) {
                predicates.add(dishTextMatch(root, category, subCategory, cb, pattern));
            }

            applyDishOrder(root, query, cb, request.getSortBy());
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Unsorted pageable: ordering comes from the specification (coalesce on price etc.)
        Page<RestaurantItem> page = itemRepository.findAll(spec, PageRequest.of(pageNumber, pageSize));

        return PaginationUtils.createResponse(page.map(d -> toDishCard(d, restaurant, null)));
    }

    // =====================================================================
    // 5. Search box: "pizza", "paneer tikka", "Dominos", "biryani"
    // =====================================================================

    @Override
    public SearchResponse search(SearchRequest request) {

        String pattern = SearchUtils.containsPattern(request.getQuery());
        double radius = radius(request.getRadiusKm());
        LocalTime now = now();
        boolean vegOnly = Boolean.TRUE.equals(request.getVegOnly());

        SearchResponse response = new SearchResponse();

        // (a) Cuisines by name -> tap opens nearby list filtered by cuisine
        response.setCuisines(cuisineRepository
                .searchActive(pattern, PageRequest.of(0, MAX_CUISINE_SEARCH_RESULTS))
                .stream()
                .map(mapper::toCuisineCard)
                .toList());

        // (b) Nearby restaurants whose name or cuisine matches
        int limit = request.getRestaurantLimit() == null
                ? 10 : Math.max(1, Math.min(50, request.getRestaurantLimit()));
        response.setRestaurants(toRestaurantCards(restaurantRepository.findNearbyRestaurants(
                new NearbyRestaurantCriteria(
                        request.getLatitude(), request.getLongitude(), radius,
                        null, pattern, false, vegOnly, null, null,
                        RestaurantSortBy.RELEVANCE, now, 0, limit))));

        // (c) Dishes matching the text in any nearby restaurant, nearest open first
        response.setDishes(searchDishesInNearbyRestaurants(request, pattern, radius, now, vegOnly));

        return response;
    }

    private DataTableResponsePacket<DishCard> searchDishesInNearbyRestaurants(
            SearchRequest request, String pattern, double radius, LocalTime now, boolean vegOnly) {

        int pageNumber = request.getPageNumber() == null ? 0 : Math.max(0, request.getPageNumber());
        int pageSize = request.getPageSize() == null ? 20 : Math.max(1, Math.min(50, request.getPageSize()));
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        // Step 1: which restaurants can deliver here (ordered: open first, then nearest)
        List<NearbyRestaurantRow> nearby = restaurantRepository.findNearbyRestaurants(
                new NearbyRestaurantCriteria(
                        request.getLatitude(), request.getLongitude(), radius,
                        null, null, false, false, null, null,
                        RestaurantSortBy.RELEVANCE, now, 0, MAX_RESTAURANTS_FOR_DISH_SEARCH));

        if (nearby.isEmpty()) {
            return PaginationUtils.createResponse(
                    new PageImpl<DishCard>(List.of(), pageRequest, 0));
        }

        List<Long> restaurantIds = nearby.stream().map(NearbyRestaurantRow::restaurantId).toList();
        Map<Long, NearbyRestaurantRow> rowById = nearby.stream()
                .collect(Collectors.toMap(NearbyRestaurantRow::restaurantId, Function.identity()));

        // Step 2: matching dishes inside those restaurants
        Specification<RestaurantItem> spec = (root, query, cb) -> {
            Join<RestaurantItem, RestaurantCategory> category = root.join("category");
            Join<RestaurantItem, RestaurantSubCategory> subCategory = root.join("subCategory", JoinType.LEFT);
            Path<Long> restaurantId = root.get("restaurant").get("recordId");

            List<Predicate> predicates = visibleDishPredicates(root, category, subCategory, cb);
            predicates.add(restaurantId.in(restaurantIds));
            predicates.add(dishTextMatch(root, category, subCategory, cb, pattern));
            if (vegOnly) {
                predicates.add(cb.equal(root.get("foodType"), FoodType.VEG));
            }

            // Order by the restaurant's position in the nearby list, then featured, then name
            CriteriaBuilder.SimpleCase<Long, Integer> rank = cb.selectCase(restaurantId);
            for (int i = 0; i < restaurantIds.size(); i++) {
                rank.when(restaurantIds.get(i), i);
            }
            query.orderBy(
                    cb.asc(rank.otherwise(restaurantIds.size())),
                    cb.desc(root.get("featured")),
                    cb.asc(root.get("nameEn")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RestaurantItem> page = itemRepository.findAll(spec, pageRequest);

        return PaginationUtils.createResponse(page.map(dish -> {
            NearbyRestaurantRow row = rowById.get(dish.getRestaurant().getRecordId());
            return toDishCard(dish, dish.getRestaurant(), row == null ? null : row.distanceKm());
        }));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private RestaurantMaster findVisibleRestaurant(String restaurantUuid) {
        RestaurantMaster restaurant = restaurantRepository.findByUuidAndDeletedFalse(restaurantUuid)
                .orElseThrow(() -> new ResourceNotFoundException("restaurant.notfound"));

        if (!restaurant.isActive() || restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new ResourceNotFoundException("restaurant.notfound");
        }
        return restaurant;
    }

    /** Converts nearby rows to cards keeping the SQL order. */
    private List<RestaurantCard> toRestaurantCards(List<NearbyRestaurantRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, RestaurantMaster> restaurants = restaurantRepository
                .findAllWithGlobalCategoriesByRecordIdIn(
                        rows.stream().map(NearbyRestaurantRow::restaurantId).toList())
                .stream()
                .collect(Collectors.toMap(RestaurantMaster::getRecordId, Function.identity()));

        List<RestaurantCard> cards = new ArrayList<>(rows.size());
        for (NearbyRestaurantRow row : rows) {
            RestaurantMaster restaurant = restaurants.get(row.restaurantId());
            if (restaurant == null) {
                continue;
            }
            RestaurantCard card = mapper.toRestaurantCard(restaurant);
            card.setDistanceKm(row.distanceKm());
            card.setOpenNow(row.openNow());
            card.setEstimatedDeliveryMinutes(GeoUtils.estimateDeliveryMinutes(
                    restaurant.getAvgPreparationMinutes(), row.distanceKm()));
            cards.add(card);
        }
        return cards;
    }

    private RestaurantCard toRestaurantCard(
            RestaurantMaster restaurant, Double latitude, Double longitude) {

        RestaurantCard card = mapper.toRestaurantCard(restaurant);
        card.setOpenNow(isOpenNow(restaurant.getAcceptingOrders(),
                restaurant.getOpeningTime(), restaurant.getClosingTime(), now()));

        if (latitude != null && longitude != null
                && restaurant.getLatitude() != null && restaurant.getLongitude() != null) {

            double distance = GeoUtils.round2(GeoUtils.haversineKm(
                    latitude, longitude,
                    restaurant.getLatitude().doubleValue(), restaurant.getLongitude().doubleValue()));

            card.setDistanceKm(distance);
            card.setEstimatedDeliveryMinutes(GeoUtils.estimateDeliveryMinutes(
                    restaurant.getAvgPreparationMinutes(), distance));
        }
        return card;
    }

    /**
     * Menu section -> sub-section tree with dish counts. Sections without visible
     * dishes are hidden. When {@code dishes} is given, dishes are attached too.
     */
    private List<MenuCategory> buildMenu(RestaurantMaster restaurant, List<RestaurantItem> dishes) {

        // counts[categoryId][subCategoryId or null] = n
        Map<Long, Map<Long, Long>> counts = new HashMap<>();
        for (Object[] row : itemRepository.countVisibleItemsByCategory(restaurant.getRecordId())) {
            counts.computeIfAbsent((Long) row[0], k -> new HashMap<>())
                    .put((Long) row[1], ((Number) row[2]).longValue());
        }

        Map<Long, List<RestaurantSubCategory>> subsByCategory = subCategoryRepository
                .findActiveByRestaurantId(restaurant.getRecordId())
                .stream()
                .collect(Collectors.groupingBy(
                        sc -> sc.getCategory().getRecordId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Long, List<RestaurantItem>> dishesByCategoryOnly = new HashMap<>();
        Map<Long, List<RestaurantItem>> dishesBySubCategory = new HashMap<>();
        if (dishes != null) {
            for (RestaurantItem dish : dishes) {
                if (dish.getSubCategory() == null) {
                    dishesByCategoryOnly.computeIfAbsent(
                            dish.getCategory().getRecordId(), k -> new ArrayList<>()).add(dish);
                } else {
                    dishesBySubCategory.computeIfAbsent(
                            dish.getSubCategory().getRecordId(), k -> new ArrayList<>()).add(dish);
                }
            }
        }

        List<MenuCategory> menu = new ArrayList<>();

        for (RestaurantCategory category :
                categoryRepository.findActiveByRestaurantId(restaurant.getRecordId())) {

            Map<Long, Long> categoryCounts = counts.getOrDefault(category.getRecordId(), Map.of());
            long total = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                continue;
            }

            MenuCategory node = mapper.toMenuCategory(category);
            node.setItemCount(total);
            node.setSubCategories(new ArrayList<>());

            for (RestaurantSubCategory sub :
                    subsByCategory.getOrDefault(category.getRecordId(), List.of())) {
                long subCount = categoryCounts.getOrDefault(sub.getRecordId(), 0L);
                if (subCount == 0) {
                    continue;
                }
                MenuSubCategory subNode = mapper.toMenuSubCategory(sub);
                subNode.setItemCount(subCount);
                if (dishes != null) {
                    subNode.setItems(toDishCards(
                            dishesBySubCategory.getOrDefault(sub.getRecordId(), List.of()), restaurant));
                }
                node.getSubCategories().add(subNode);
            }

            if (dishes != null) {
                node.setItems(toDishCards(
                        dishesByCategoryOnly.getOrDefault(category.getRecordId(), List.of()), restaurant));
            }
            menu.add(node);
        }
        return menu;
    }

    private List<DishCard> toDishCards(List<RestaurantItem> dishes, RestaurantMaster restaurant) {
        return dishes.stream().map(d -> toDishCard(d, restaurant, null)).toList();
    }

    private DishCard toDishCard(RestaurantItem dish, RestaurantMaster restaurant, Double distanceKm) {
        DishCard card = mapper.toDishCard(dish);
        card.setRestaurantUuid(restaurant.getUuid());
        card.setRestaurantName(restaurant.getNameEn());
        card.setDistanceKm(distanceKm);
        return card;
    }

    /** Dish, its section and sub-section are all active, not deleted and available. */
    private List<Predicate> visibleDishPredicates(
            Root<RestaurantItem> root,
            Join<RestaurantItem, RestaurantCategory> category,
            Join<RestaurantItem, RestaurantSubCategory> subCategory,
            CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(root.get("active")));
        predicates.add(cb.isFalse(root.get("deleted")));
        predicates.add(cb.isTrue(root.get("available")));
        predicates.add(cb.isTrue(category.get("active")));
        predicates.add(cb.isFalse(category.get("deleted")));
        predicates.add(cb.or(
                cb.isNull(subCategory.get("recordId")),
                cb.and(cb.isTrue(subCategory.get("active")),
                        cb.isFalse(subCategory.get("deleted")))));
        return predicates;
    }

    /** Text search over dish name, description, tags and its section names. */
    private Predicate dishTextMatch(
            Root<RestaurantItem> root,
            Join<RestaurantItem, RestaurantCategory> category,
            Join<RestaurantItem, RestaurantSubCategory> subCategory,
            CriteriaBuilder cb,
            String pattern) {

        char esc = SearchUtils.LIKE_ESCAPE;
        return cb.or(
                cb.like(cb.lower(root.get("nameEn")), pattern, esc),
                cb.like(cb.lower(root.get("nameFr")), pattern, esc),
                cb.like(cb.lower(root.get("searchTags")), pattern, esc),
                cb.like(cb.lower(category.get("nameEn")), pattern, esc),
                cb.like(cb.lower(category.get("nameFr")), pattern, esc),
                cb.like(cb.lower(subCategory.get("nameEn")), pattern, esc),
                cb.like(cb.lower(subCategory.get("nameFr")), pattern, esc));
    }

    private void applyDishOrder(
            Root<RestaurantItem> root, CriteriaQuery<?> query, CriteriaBuilder cb, DishSortBy sortBy) {

        Expression<Object> effectivePrice =
                cb.coalesce(root.get("discountPrice"), root.get("price"));

        switch (sortBy == null ? DishSortBy.RELEVANCE : sortBy) {
            case PRICE_LOW_TO_HIGH -> query.orderBy(
                    cb.asc(effectivePrice), cb.asc(root.get("nameEn")));
            case PRICE_HIGH_TO_LOW -> query.orderBy(
                    cb.desc(effectivePrice), cb.asc(root.get("nameEn")));
            case NAME -> query.orderBy(cb.asc(root.get("nameEn")));
            case RELEVANCE -> query.orderBy(
                    cb.desc(root.get("featured")),
                    cb.asc(root.get("displayOrder")),
                    cb.asc(root.get("nameEn")));
        }
    }

    /** Same rule as the SQL in RestaurantMasterRepositoryImpl (handles past-midnight closing). */
    private static boolean isOpenNow(
            Boolean acceptingOrders, LocalTime open, LocalTime close, LocalTime now) {

        if (Boolean.FALSE.equals(acceptingOrders)) {
            return false;
        }
        if (open == null || close == null) {
            return true;
        }
        if (!open.isAfter(close)) {
            return !now.isBefore(open) && now.isBefore(close);
        }
        return !now.isBefore(open) || now.isBefore(close);
    }

    private LocalTime now() {
        ZoneId zone = (timeZone == null || timeZone.isBlank())
                ? ZoneId.systemDefault() : ZoneId.of(timeZone);
        return LocalTime.now(zone).withNano(0);
    }

    private static double radius(Double requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_RADIUS_KM;
        }
        return Math.min(requested, MAX_RADIUS_KM);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
