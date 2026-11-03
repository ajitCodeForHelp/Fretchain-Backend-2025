package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.bean.DataTableResponsePacket;
import com.bt.fretchain.exception.ResourceNotFoundException;
import com.bt.fretchain.primary.dto.CustomerShopDTO.*;
import com.bt.fretchain.primary.mapper.CustomerShopMapper;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.ShopStatus;
import com.bt.fretchain.primary.repository.*;
import com.bt.fretchain.primary.service.CustomerShopService;
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
public class CustomerShopServiceImpl implements CustomerShopService {

    /** Max nearby shops whose items are considered by the global item search. */
    private static final int MAX_SHOPS_FOR_ITEM_SEARCH = 200;

    private static final int MAX_CATEGORY_SEARCH_RESULTS = 10;

    private static final double DEFAULT_RADIUS_KM = 10.0;
    private static final double MAX_RADIUS_KM = 50.0;

    private final ShopMasterRepository shopRepository;
    private final ShopGlobalCategoryRepository globalCategoryRepository;
    private final ShopCategoryRepository categoryRepository;
    private final ShopSubCategoryRepository subCategoryRepository;
    private final ShopItemRepository itemRepository;
    private final CustomerShopMapper mapper;

    /** Time zone used for "open now". Empty = server default. e.g. Africa/Douala */
    @Value("${marketplace.time-zone:}")
    private String timeZone;

    // =====================================================================
    // 1. Global categories (home screen chips)
    // =====================================================================

    @Override
    public List<GlobalCategoryCard> getGlobalCategories() {
        return globalCategoryRepository
                .findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameEnAsc()
                .stream()
                .map(mapper::toGlobalCategoryCard)
                .toList();
    }

    // =====================================================================
    // 2. Nearby shops
    // =====================================================================

    @Override
    public DataTableResponsePacket<ShopCard> getNearbyShops(NearbyShopRequest request) {

        int pageNumber = request.getPageNumber() == null ? 0 : Math.max(0, request.getPageNumber());
        int pageSize = request.getPageSize() == null ? 20 : Math.max(1, Math.min(50, request.getPageSize()));

        NearbyShopCriteria criteria = new NearbyShopCriteria(
                request.getLatitude(),
                request.getLongitude(),
                radius(request.getRadiusKm()),
                blankToNull(request.getGlobalCategoryUuid()),
                SearchUtils.containsPattern(request.getSearch()),
                Boolean.TRUE.equals(request.getOpenNowOnly()),
                request.getMinRating(),
                request.getSortBy(),
                now(),
                pageNumber * pageSize,
                pageSize
        );

        long total = shopRepository.countNearbyShops(criteria);
        List<ShopCard> cards = total == 0
                ? List.of()
                : toShopCards(shopRepository.findNearbyShops(criteria));

        return PaginationUtils.createResponse(
                new PageImpl<>(cards, PageRequest.of(pageNumber, pageSize), total));
    }

    // =====================================================================
    // 3. Shop page (header + category tree)
    // =====================================================================

    @Override
    public ShopDetailResponse getShopDetail(String shopUuid, Double latitude, Double longitude) {

        ShopMaster shop = findVisibleShop(shopUuid);

        ShopDetailResponse response = new ShopDetailResponse();
        response.setShop(toShopCard(shop, latitude, longitude));
        response.setCategories(buildCategoryTree(shop));
        return response;
    }

    // =====================================================================
    // 4. Items of a shop (by category / subcategory / text)
    // =====================================================================

    @Override
    public DataTableResponsePacket<ItemCard> getShopItems(String shopUuid, ShopItemsRequest request) {

        ShopMaster shop = findVisibleShop(shopUuid);

        int pageNumber = request.getPageNumber() == null ? 0 : Math.max(0, request.getPageNumber());
        int pageSize = request.getPageSize() == null ? 20 : Math.max(1, Math.min(100, request.getPageSize()));

        Specification<ShopItem> spec = (root, query, cb) -> {
            Join<ShopItem, ShopCategory> category = root.join("category");
            Join<ShopItem, ShopSubCategory> subCategory = root.join("subCategory", JoinType.LEFT);

            List<Predicate> predicates = visibleItemPredicates(root, category, subCategory, cb);
            predicates.add(cb.equal(root.get("shop").get("recordId"), shop.getRecordId()));

            if (request.getCategoryUuid() != null && !request.getCategoryUuid().isBlank()) {
                predicates.add(cb.equal(category.get("uuid"), request.getCategoryUuid()));
            }
            if (request.getSubCategoryUuid() != null && !request.getSubCategoryUuid().isBlank()) {
                predicates.add(cb.equal(subCategory.get("uuid"), request.getSubCategoryUuid()));
            }

            String pattern = SearchUtils.containsPattern(request.getSearch());
            if (pattern != null) {
                predicates.add(itemTextMatch(root, category, subCategory, cb, pattern));
            }

            applyItemOrder(root, query, cb, request.getSortBy());
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Unsorted pageable: ordering comes from the specification (coalesce on price etc.)
        Page<ShopItem> page = itemRepository.findAll(spec, PageRequest.of(pageNumber, pageSize));

        return PaginationUtils.createResponse(page.map(item -> toItemCard(item, shop, null)));
    }

    // =====================================================================
    // 5. Search box: "milk", "paracetamol", "Fresh Mart", "pharmacy"
    // =====================================================================

    @Override
    public SearchResponse search(SearchRequest request) {

        String pattern = SearchUtils.containsPattern(request.getQuery());
        double radius = radius(request.getRadiusKm());
        LocalTime now = now();

        SearchResponse response = new SearchResponse();

        // (a) Global categories by name -> tap opens nearby list filtered by category
        response.setCategories(globalCategoryRepository
                .searchActive(pattern, PageRequest.of(0, MAX_CATEGORY_SEARCH_RESULTS))
                .stream()
                .map(mapper::toGlobalCategoryCard)
                .toList());

        // (b) Nearby shops whose name or global category matches
        int shopLimit = request.getShopLimit() == null ? 10 : Math.max(1, Math.min(50, request.getShopLimit()));
        response.setShops(toShopCards(shopRepository.findNearbyShops(new NearbyShopCriteria(
                request.getLatitude(), request.getLongitude(), radius,
                null, pattern, false, null, ShopSortBy.RELEVANCE, now,
                0, shopLimit))));

        // (c) Items matching the text in any nearby shop, nearest open shop first
        response.setItems(searchItemsInNearbyShops(request, pattern, radius, now));

        return response;
    }

    private DataTableResponsePacket<ItemCard> searchItemsInNearbyShops(
            SearchRequest request, String pattern, double radius, LocalTime now) {

        int pageNumber = request.getPageNumber() == null ? 0 : Math.max(0, request.getPageNumber());
        int pageSize = request.getPageSize() == null ? 20 : Math.max(1, Math.min(50, request.getPageSize()));
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        // Step 1: which shops can deliver here (ordered: open first, then nearest)
        List<NearbyShopRow> nearby = shopRepository.findNearbyShops(new NearbyShopCriteria(
                request.getLatitude(), request.getLongitude(), radius,
                null, null, false, null, ShopSortBy.RELEVANCE, now,
                0, MAX_SHOPS_FOR_ITEM_SEARCH));

        if (nearby.isEmpty()) {
            return PaginationUtils.createResponse(
                    new PageImpl<ItemCard>(List.of(), pageRequest, 0));
        }

        List<Long> shopIds = nearby.stream().map(NearbyShopRow::shopId).toList();
        Map<Long, NearbyShopRow> rowByShop = nearby.stream()
                .collect(Collectors.toMap(NearbyShopRow::shopId, Function.identity()));

        // Step 2: matching items inside those shops
        Specification<ShopItem> spec = (root, query, cb) -> {
            Join<ShopItem, ShopCategory> category = root.join("category");
            Join<ShopItem, ShopSubCategory> subCategory = root.join("subCategory", JoinType.LEFT);
            Path<Long> shopId = root.get("shop").get("recordId");

            List<Predicate> predicates = visibleItemPredicates(root, category, subCategory, cb);
            predicates.add(shopId.in(shopIds));
            predicates.add(itemTextMatch(root, category, subCategory, cb, pattern));

            // Order by the shop's position in the nearby list, then featured, then name
            CriteriaBuilder.SimpleCase<Long, Integer> shopRank = cb.selectCase(shopId);
            for (int i = 0; i < shopIds.size(); i++) {
                shopRank.when(shopIds.get(i), i);
            }
            query.orderBy(
                    cb.asc(shopRank.otherwise(shopIds.size())),
                    cb.desc(root.get("featured")),
                    cb.asc(root.get("nameEn")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ShopItem> page = itemRepository.findAll(spec, pageRequest);

        return PaginationUtils.createResponse(page.map(item -> {
            NearbyShopRow row = rowByShop.get(item.getShop().getRecordId());
            return toItemCard(item, item.getShop(), row == null ? null : row.distanceKm());
        }));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private ShopMaster findVisibleShop(String shopUuid) {
        ShopMaster shop = shopRepository.findByUuidAndDeletedFalse(shopUuid)
                .orElseThrow(() -> new ResourceNotFoundException("shop.notfound"));

        if (!shop.isActive() || shop.getStatus() != ShopStatus.APPROVED) {
            throw new ResourceNotFoundException("shop.notfound");
        }
        return shop;
    }

    /** Converts nearby rows to cards keeping the SQL order. */
    private List<ShopCard> toShopCards(List<NearbyShopRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, ShopMaster> shops = shopRepository
                .findAllWithGlobalCategoriesByRecordIdIn(
                        rows.stream().map(NearbyShopRow::shopId).toList())
                .stream()
                .collect(Collectors.toMap(ShopMaster::getRecordId, Function.identity()));

        List<ShopCard> cards = new ArrayList<>(rows.size());
        for (NearbyShopRow row : rows) {
            ShopMaster shop = shops.get(row.shopId());
            if (shop == null) {
                continue;
            }
            ShopCard card = mapper.toShopCard(shop);
            card.setDistanceKm(row.distanceKm());
            card.setOpenNow(row.openNow());
            card.setEstimatedDeliveryMinutes(GeoUtils.estimateDeliveryMinutes(
                    shop.getAvgPreparationMinutes(), row.distanceKm()));
            cards.add(card);
        }
        return cards;
    }

    private ShopCard toShopCard(ShopMaster shop, Double latitude, Double longitude) {
        ShopCard card = mapper.toShopCard(shop);
        card.setOpenNow(isOpenNow(shop.getAcceptingOrders(),
                shop.getOpeningTime(), shop.getClosingTime(), now()));

        if (latitude != null && longitude != null
                && shop.getLatitude() != null && shop.getLongitude() != null) {

            double distance = GeoUtils.round2(GeoUtils.haversineKm(
                    latitude, longitude,
                    shop.getLatitude().doubleValue(), shop.getLongitude().doubleValue()));

            card.setDistanceKm(distance);
            card.setEstimatedDeliveryMinutes(GeoUtils.estimateDeliveryMinutes(
                    shop.getAvgPreparationMinutes(), distance));
        }
        return card;
    }

    /**
     * Category -> subcategory tree with item counts. Categories / subcategories
     * without visible items are hidden.
     */
    private List<ShopCategoryNode> buildCategoryTree(ShopMaster shop) {

        // counts[categoryId][subCategoryId or null] = n
        Map<Long, Map<Long, Long>> counts = new HashMap<>();
        for (Object[] row : itemRepository.countVisibleItemsByCategory(shop.getRecordId())) {
            counts.computeIfAbsent((Long) row[0], k -> new HashMap<>())
                    .put((Long) row[1], ((Number) row[2]).longValue());
        }

        Map<Long, List<ShopSubCategory>> subsByCategory = subCategoryRepository
                .findActiveByShopId(shop.getRecordId())
                .stream()
                .collect(Collectors.groupingBy(
                        sc -> sc.getCategory().getRecordId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ShopCategoryNode> tree = new ArrayList<>();

        for (ShopCategory category : categoryRepository.findActiveByShopId(shop.getRecordId())) {
            Map<Long, Long> categoryCounts = counts.getOrDefault(category.getRecordId(), Map.of());
            long total = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                continue;
            }

            ShopCategoryNode node = mapper.toCategoryNode(category);
            node.setItemCount(total);
            node.setSubCategories(new ArrayList<>());

            for (ShopSubCategory sub : subsByCategory.getOrDefault(category.getRecordId(), List.of())) {
                long subCount = categoryCounts.getOrDefault(sub.getRecordId(), 0L);
                if (subCount == 0) {
                    continue;
                }
                ShopSubCategoryNode subNode = mapper.toSubCategoryNode(sub);
                subNode.setItemCount(subCount);
                node.getSubCategories().add(subNode);
            }
            tree.add(node);
        }
        return tree;
    }

    private ItemCard toItemCard(ShopItem item, ShopMaster shop, Double distanceKm) {
        ItemCard card = mapper.toItemCard(item);
        card.setShopUuid(shop.getUuid());
        card.setShopName(shop.getNameEn());
        card.setDistanceKm(distanceKm);
        card.setInStock(Boolean.TRUE.equals(item.getAvailable())
                && item.getStockQuantity() != null && item.getStockQuantity() > 0);
        return card;
    }

    /** Item, its category and subcategory are all active, not deleted and available. */
    private List<Predicate> visibleItemPredicates(
            Root<ShopItem> root,
            Join<ShopItem, ShopCategory> category,
            Join<ShopItem, ShopSubCategory> subCategory,
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

    /** Text search over item name, brand, tags and its category / subcategory names. */
    private Predicate itemTextMatch(
            Root<ShopItem> root,
            Join<ShopItem, ShopCategory> category,
            Join<ShopItem, ShopSubCategory> subCategory,
            CriteriaBuilder cb,
            String pattern) {

        char esc = SearchUtils.LIKE_ESCAPE;
        return cb.or(
                cb.like(cb.lower(root.get("nameEn")), pattern, esc),
                cb.like(cb.lower(root.get("nameFr")), pattern, esc),
                cb.like(cb.lower(root.get("brand")), pattern, esc),
                cb.like(cb.lower(root.get("searchTags")), pattern, esc),
                cb.like(cb.lower(category.get("nameEn")), pattern, esc),
                cb.like(cb.lower(category.get("nameFr")), pattern, esc),
                cb.like(cb.lower(subCategory.get("nameEn")), pattern, esc),
                cb.like(cb.lower(subCategory.get("nameFr")), pattern, esc));
    }

    private void applyItemOrder(
            Root<ShopItem> root, CriteriaQuery<?> query, CriteriaBuilder cb, ItemSortBy sortBy) {

        Expression<Object> effectivePrice =
                cb.coalesce(root.get("discountPrice"), root.get("price"));

        switch (sortBy == null ? ItemSortBy.RELEVANCE : sortBy) {
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

    /** Same rule as the SQL in ShopMasterRepositoryImpl (handles past-midnight closing). */
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
