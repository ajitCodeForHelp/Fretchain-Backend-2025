package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.exception.BadRequestException;
import com.bt.fretchain.primary.dto.DispatchDTO;
import com.bt.fretchain.primary.pojo.Driver;
import com.bt.fretchain.primary.pojo.PlatformSetting;
import com.bt.fretchain.primary.pojo.SystemUser;
import com.bt.fretchain.primary.pojo.Vehicle;
import com.bt.fretchain.primary.pojo.VehicleCategory;
import com.bt.fretchain.primary.pojo.enums.AssignmentMode;
import com.bt.fretchain.primary.pojo.enums.DeliveryService;
import com.bt.fretchain.primary.repository.PlatformSettingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Chooses drivers for shop and food orders.
 *
 * Freight orders are matched by the vehicle type the customer picks. Shop / food orders have no
 * vehicle choice, so a driver is eligible only when ALL of these hold:
 *   1. driver: active, verified by the platform, available and online, with a fresh location
 *   2. the driver has a vehicle that is active and verified
 *   3. that vehicle's category declares the service (SHOP_DELIVERY / FOOD_DELIVERY)
 * Eligible drivers inside the search radius are ranked by distance to the store. MANUAL mode shows
 * the ranked list to staff; AUTO mode takes the first one ({@link #pick}). The same list backs both,
 * so switching mode never changes who is eligible.
 */
@Service
@RequiredArgsConstructor
public class DispatchService {

    /** Drivers whose last location is older than this are not offered (unknown location is allowed). */
    private static final long FRESH_LOCATION_MINUTES = 15;

    private final PlatformSettingRepository settingRepository;

    @PersistenceContext
    private EntityManager em;

    // ---------------------------------------------------------------- settings

    @Transactional(readOnly = true)
    public DispatchDTO.Settings settings() {
        PlatformSetting s = settingRepository.findTopByOrderByRecordIdAsc().orElse(new PlatformSetting());
        DispatchDTO.Settings d = new DispatchDTO.Settings();
        d.setMode(s.getStoreAssignmentMode() == null ? AssignmentMode.MANUAL : s.getStoreAssignmentMode());
        d.setRadiusKm(s.getStoreAssignmentRadiusKm() == null ? 5.0 : s.getStoreAssignmentRadiusKm());
        d.setFreightMode(s.getFreightAssignmentMode() == null ? AssignmentMode.MANUAL : s.getFreightAssignmentMode());
        d.setFreightRadiusKm(s.getFreightAssignmentRadiusKm() == null ? 30.0 : s.getFreightAssignmentRadiusKm());
        d.setStoreOfferSeconds(s.getStoreOfferSeconds() == null ? 30 : s.getStoreOfferSeconds());
        d.setFreightOfferSeconds(s.getFreightOfferSeconds() == null ? 120 : s.getFreightOfferSeconds());
        d.setFreightWaveSize(s.getFreightWaveSize() == null ? 3 : s.getFreightWaveSize());
        return d;
    }

    @Transactional
    public DispatchDTO.Settings updateSettings(DispatchDTO.Settings req, SystemUser actor) {
        PlatformSetting s = settingRepository.findTopByOrderByRecordIdAsc().orElse(new PlatformSetting());
        s.setStoreAssignmentMode(req.getMode());
        s.setStoreAssignmentRadiusKm(req.getRadiusKm());
        if (req.getFreightMode() != null) s.setFreightAssignmentMode(req.getFreightMode());
        if (req.getFreightRadiusKm() != null) s.setFreightAssignmentRadiusKm(req.getFreightRadiusKm());
        if (req.getStoreOfferSeconds() != null) s.setStoreOfferSeconds(req.getStoreOfferSeconds());
        if (req.getFreightOfferSeconds() != null) s.setFreightOfferSeconds(req.getFreightOfferSeconds());
        if (req.getFreightWaveSize() != null) s.setFreightWaveSize(req.getFreightWaveSize());
        s.setModifiedBy(actor.getRecordId());
        settingRepository.save(s);
        return settings();
    }

    // ---------------------------------------------------------------- candidates

    /** Eligible drivers around a point, nearest first. radiusKm null = the configured radius. */
    @Transactional(readOnly = true)
    public List<DispatchDTO.Candidate> candidates(DeliveryService service, double lat, double lng, Double radiusKm, int limit) {
        return candidates(service, lat, lng, radiusKm, limit, null);
    }

    /** vendorUuid (login uuid of a fleet owner) limits the list to that fleet owner's drivers; null = everyone. */
    @Transactional(readOnly = true)
    public List<DispatchDTO.Candidate> candidates(DeliveryService service, double lat, double lng, Double radiusKm, int limit, String vendorUuid) {
        if (service == DeliveryService.FREIGHT) {
            // freight is matched by the customer's vehicle type (existing flow), not by this finder
            throw new BadRequestException("dispatch.service.invalid");
        }
        double radius = radiusKm != null ? radiusKm : settings().getRadiusKm();
        LocalDateTime fresh = LocalDateTime.now().minusMinutes(FRESH_LOCATION_MINUTES);

        List<DispatchDTO.Candidate> result = new ArrayList<>();
        for (Driver d : readyDrivers()) {
            Vehicle v = d.getVehicle();
            if (!v.getVehicleCategory().supports(service) || d.getCurrentLat() == null || d.getCurrentLng() == null) {
                continue;
            }
            if (vendorUuid != null && (d.getVendor() == null || !vendorUuid.equals(d.getVendor().getUuid()))) {
                continue;
            }
            if (d.getLastLocationUpdatedAt() != null && d.getLastLocationUpdatedAt().isBefore(fresh)) {
                continue;
            }
            double km = distanceKm(lat, lng, d.getCurrentLat(), d.getCurrentLng());
            if (km <= radius) {
                result.add(toCandidate(d, km));
            }
        }
        result.sort(Comparator.comparing(DispatchDTO.Candidate::getDistanceKm));
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    /**
     * Freight: ready drivers whose vehicle category is the one the customer picked, nearest to the pickup first.
     * pickup coordinates missing = no distance filter (drivers are listed without a distance).
     * excludeDriverIds: drivers who already rejected / let the offer expire.
     */
    @Transactional(readOnly = true)
    public List<DispatchDTO.Candidate> freightCandidates(com.bt.fretchain.primary.pojo.FreightOrder order, Double radiusKm,
                                                         int limit, String vendorUuid, Set<Long> excludeDriverIds) {
        if (order.getVehicleCategory() == null) {
            return List.of();
        }
        Double pLat = order.getPickupAddress() == null ? null : order.getPickupAddress().getLatitude();
        Double pLng = order.getPickupAddress() == null ? null : order.getPickupAddress().getLongitude();
        double radius = radiusKm != null ? radiusKm : settings().getFreightRadiusKm();
        LocalDateTime fresh = LocalDateTime.now().minusMinutes(FRESH_LOCATION_MINUTES);
        Long categoryId = order.getVehicleCategory().getRecordId();

        List<DispatchDTO.Candidate> result = new ArrayList<>();
        for (Driver d : readyDrivers()) {
            Vehicle v = d.getVehicle();
            if (!categoryId.equals(v.getVehicleCategory().getRecordId())) continue;
            if (excludeDriverIds != null && excludeDriverIds.contains(d.getRecordId())) continue;
            if (vendorUuid != null && (d.getVendor() == null || !vendorUuid.equals(d.getVendor().getUuid()))) continue;
            Double km = null;
            if (pLat != null && pLng != null) {
                if (d.getCurrentLat() == null || d.getCurrentLng() == null) continue;
                if (d.getLastLocationUpdatedAt() != null && d.getLastLocationUpdatedAt().isBefore(fresh)) continue;
                km = distanceKm(pLat, pLng, d.getCurrentLat(), d.getCurrentLng());
                if (km > radius) continue;
            }
            result.add(toCandidate(d, km == null ? null : km));
        }
        result.sort(Comparator.comparing(DispatchDTO.Candidate::getDistanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    /** Same as {@link #candidates} but without the radius / service rules, for checking one chosen driver. */
    public String freightBlockingReason(Driver d, com.bt.fretchain.primary.pojo.FreightOrder order) {
        if (!d.isActive()) return "INACTIVE";
        if (!Boolean.TRUE.equals(d.getVerified())) return "NOT_VERIFIED";
        Vehicle v = d.getVehicle();
        if (v == null || v.isDeleted()) return "NO_VEHICLE";
        if (!v.isActive()) return "VEHICLE_INACTIVE";
        if (!Boolean.TRUE.equals(v.getVerified())) return "VEHICLE_NOT_VERIFIED";
        if (order.getVehicleCategory() == null || v.getVehicleCategory() == null
                || !order.getVehicleCategory().getRecordId().equals(v.getVehicleCategory().getRecordId())) return "CATEGORY_MISMATCH";
        if (!Boolean.TRUE.equals(d.getAvailable())) return "NOT_AVAILABLE";
        if (!Boolean.TRUE.equals(d.getOnlineStatus())) return "OFFLINE";
        return null;
    }

    /** What AUTO mode would assign: the nearest eligible driver, if any. */
    @Transactional(readOnly = true)
    public Optional<DispatchDTO.Candidate> pick(DeliveryService service, double lat, double lng) {
        return candidates(service, lat, lng, null, 1).stream().findFirst();
    }

    // ---------------------------------------------------------------- readiness

    /** How many drivers can serve each service and what blocks the others (setup checks for admins). */
    @Transactional(readOnly = true)
    public DispatchDTO.Overview overview() {
        DispatchDTO.Overview o = new DispatchDTO.Overview();
        o.setSettings(settings());
        List<Driver> all = loadDrivers("");
        List<DispatchDTO.Readiness> list = new ArrayList<>();
        for (DeliveryService service : new DeliveryService[]{DeliveryService.SHOP_DELIVERY, DeliveryService.FOOD_DELIVERY}) {
            DispatchDTO.Readiness r = new DispatchDTO.Readiness();
            r.setService(service);
            r.setTotalDrivers(all.size());
            Map<String, Long> blocked = new LinkedHashMap<>();
            long ready = 0;
            for (Driver d : all) {
                String reason = blockingReason(d, service);
                if (reason == null) {
                    ready++;
                } else {
                    blocked.merge(reason, 1L, Long::sum);
                }
            }
            r.setReady(ready);
            r.setBlockedBy(blocked);
            list.add(r);
        }
        o.setReadiness(list);

        Map<String, List<DeliveryService>> cats = new LinkedHashMap<>();
        em.createQuery("select c from VehicleCategory c where c.deleted = false and c.active = true order by c.nameEn", VehicleCategory.class)
                .getResultList()
                .forEach(c -> cats.put(c.getNameEn(), c.getServices().isEmpty() ? List.of(DeliveryService.FREIGHT) : List.copyOf(c.getServices())));
        o.setCategories(cats);
        return o;
    }

    /** First failing check for a driver and service, or null when the driver is ready. */
    public String blockingReason(Driver d, DeliveryService service) {
        if (!d.isActive()) return "INACTIVE";
        if (!Boolean.TRUE.equals(d.getVerified())) return "NOT_VERIFIED";
        Vehicle v = d.getVehicle();
        if (v == null || v.isDeleted()) return "NO_VEHICLE";
        if (!v.isActive()) return "VEHICLE_INACTIVE";
        if (!Boolean.TRUE.equals(v.getVerified())) return "VEHICLE_NOT_VERIFIED";
        if (!v.getVehicleCategory().supports(service)) return "CATEGORY_NOT_ENABLED";
        if (!Boolean.TRUE.equals(d.getAvailable())) return "NOT_AVAILABLE";
        if (!Boolean.TRUE.equals(d.getOnlineStatus())) return "OFFLINE";
        return null;
    }

    // ---------------------------------------------------------------- helpers

    /** Drivers passing every check except the service and the location (those depend on the order). */
    private List<Driver> readyDrivers() {
        return loadDrivers(" and d.active = true and d.verified = true and d.available = true and d.onlineStatus = true"
                + " and v.deleted = false and v.active = true and v.verified = true");
    }

    private List<Driver> loadDrivers(String extra) {
        String jpql = "select d from Driver d left join fetch d.vehicle v left join fetch v.vehicleCategory left join fetch d.vendor "
                + "where d.deleted = false" + extra;
        if (!extra.isEmpty()) {
            jpql = jpql.replace("left join fetch d.vehicle v", "join fetch d.vehicle v");
        }
        return em.createQuery(jpql, Driver.class).getResultList();
    }

    private DispatchDTO.Candidate toCandidate(Driver d, Double km) {
        DispatchDTO.Candidate c = new DispatchDTO.Candidate();
        c.setDriverUuid(d.getUuid());
        c.setFullName(d.getFullName());
        c.setMobile(d.getMobile());
        c.setCountryCode(d.getCountryCode());
        c.setProfileImageUrl(d.getProfileImageUrl());
        c.setVendorName(d.getVendor() == null ? null : d.getVendor().getFullName());
        c.setVehicleNumber(d.getVehicle().getVehicleNumber());
        c.setVehicleCategory(d.getVehicle().getVehicleCategory().getNameEn());
        c.setDistanceKm(km == null ? null : Math.round(km * 100.0) / 100.0);
        c.setRating(d.getOverallRating());
        c.setLastLocationUpdatedAt(d.getLastLocationUpdatedAt());
        return c;
    }

    static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
