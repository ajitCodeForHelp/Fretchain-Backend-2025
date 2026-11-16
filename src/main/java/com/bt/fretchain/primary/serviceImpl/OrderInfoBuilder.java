package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.OfferDTO;
import com.bt.fretchain.primary.pojo.*;
import com.bt.fretchain.primary.pojo.enums.FreightOrderType;
import com.bt.fretchain.primary.pojo.enums.PickupType;
import com.bt.fretchain.primary.pojo.enums.StorePaymentMethod;
import com.bt.fretchain.primary.repository.FreightOrderBreakBulkDetailRepository;
import com.bt.fretchain.primary.repository.FreightOrderBulkMaterialDetailRepository;
import com.bt.fretchain.primary.repository.FreightOrderContainerDetailRepository;
import com.bt.fretchain.primary.repository.FreightOrderContainerItemRepository;
import com.bt.fretchain.primary.repository.FreightOrderDocumentRepository;
import com.bt.fretchain.primary.repository.FreightOrderParcelDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns an order into the labelled blocks the driver app shows on the order detail screen. Each order type has
 * its own blocks (container, break bulk, bulk material, parcel, shop, food), so the app needs no per-type code:
 * it draws whatever sections arrive.
 *
 * What a driver gets to see is everything needed to do the job (what the goods are, how to handle them, where the
 * sites are, who to call, how much cash to collect). Prices, margins and the customer's other details stay out.
 */
@Component
@RequiredArgsConstructor
public class OrderInfoBuilder {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final FreightOrderContainerDetailRepository containers;
    private final FreightOrderContainerItemRepository containerItems;
    private final FreightOrderBreakBulkDetailRepository breakBulk;
    private final FreightOrderBulkMaterialDetailRepository bulk;
    private final FreightOrderParcelDetailRepository parcels;
    private final FreightOrderDocumentRepository documents;

    // ================================================================ freight

    /** Fills sections, driver instructions and cash-to-collect of a freight order. */
    void fill(OfferDTO.View v, FreightOrder o) {
        List<OfferDTO.Section> sections = new ArrayList<>();
        v.setInstructions(blank(o.getInstructionForDriver()));

        OfferDTO.Section job = section("job", "Job");
        add(job, "Category", o.getCategory() == null ? null : o.getCategory().getNameEn());
        add(job, "Service", o.getSubCategory() == null ? null : o.getSubCategory().getNameEn());
        add(job, "Vehicle needed", o.getVehicleCategory() == null ? null : o.getVehicleCategory().getNameEn());
        add(job, "Vehicle", o.getVehicle() == null ? null : o.getVehicle().getVehicleNumber());
        add(job, "Pickup", o.getPickupType() == PickupType.NOW || o.getPickupDateTime() == null ? "As soon as possible" : o.getPickupDateTime().format(WHEN));
        if (o.getDistanceKm() != null) add(job, "Distance", o.getDistanceKm() + " km");
        if (o.getEstimatedTimeMinutes() != null) add(job, "Estimated time", o.getEstimatedTimeMinutes() + " min");
        sections.add(job);

        FreightOrderType type = o.getOrderType();
        if (type == FreightOrderType.CONTAINER) containerSection(o, sections);
        else if (type == FreightOrderType.BREAK_BULK) breakBulkSection(o, sections);
        else if (type == FreightOrderType.BULK_MATERIAL) bulkSection(o, v, sections);
        else if (type == FreightOrderType.PARCEL) parcelSection(o, v, sections);

        // files the customer attached (cargo photos, packing list, invoice)
        List<FreightOrderDocument> docs = documents.findByFreightOrder(o);
        if (!docs.isEmpty()) {
            OfferDTO.Section d = section("documents", "Documents and cargo photos");
            for (FreightOrderDocument doc : docs) {
                OfferDTO.Item it = new OfferDTO.Item();
                it.setName(pretty(doc.getDocumentType() == null ? "Document" : doc.getDocumentType().name()));
                it.setUrl(doc.getFileUrl());
                if (doc.getDocumentType() != null && doc.getDocumentType().name().equals("CARGO_IMAGE")) it.setImageUrl(doc.getFileUrl());
                d.getItems().add(it);
            }
            sections.add(d);
        }
        v.setSections(sections);
    }

    /** One line saying how much the order says there is, used as the "expected" side of the quantity check. */
    String expectedQuantity(FreightOrder o) {
        FreightOrderType type = o.getOrderType();
        if (type == FreightOrderType.CONTAINER) {
            return containers.findByFreightOrder(o).map(c -> {
                int n = containerItems.findByContainerDetail(c).size();
                return n == 0 ? null : n + (n == 1 ? " container" : " containers");
            }).orElse(null);
        }
        if (type == FreightOrderType.BREAK_BULK) {
            return breakBulk.findByFreightOrder(o).map(b -> b.getNumberOfPieces() == null ? null : b.getNumberOfPieces() + " pieces").orElse(null);
        }
        if (type == FreightOrderType.BULK_MATERIAL) {
            return bulk.findByFreightOrder(o).map(b -> b.getQuantity() == null ? null : trim(b.getQuantity()) + " " + unit(b.getQuantityUnit())).orElse(null);
        }
        if (type == FreightOrderType.PARCEL) {
            return parcels.findByFreightOrder(o).map(p -> p.getNumberOfPackages() == null ? null : p.getNumberOfPackages() + " packages").orElse(null);
        }
        return null;
    }

    private void containerSection(FreightOrder o, List<OfferDTO.Section> out) {
        containers.findByFreightOrder(o).ifPresent(c -> {
            OfferDTO.Section s = section("goods", "Container and cargo");
            add(s, "Container load", c.getLoadType() == null ? null : pretty(c.getLoadType().name()));
            add(s, "Nature of goods", c.getNatureOfGoods());
            if (c.getCargoWeightKg() != null) add(s, "Cargo weight", trim(c.getCargoWeightKg()) + " kg");
            flag(s, "Dangerous goods", c.getDangerousGoods(), true);
            add(s, "Shipping line", c.getShippingLine());
            flag(s, "Return empty container", c.getReturnEmptyContainer(), false);
            flag(s, "Insured", c.getInsurance(), false);
            for (FreightOrderContainerItem it : containerItems.findByContainerDetail(c)) {
                OfferDTO.Item item = new OfferDTO.Item();
                item.setName("Container " + nz(it.getContainerNumber(), "(number not given)"));
                item.setNote(it.getSealNumber() == null || it.getSealNumber().isBlank() ? null : "Seal " + it.getSealNumber());
                s.getItems().add(item);
            }
            out.add(s);
        });
    }

    private void breakBulkSection(FreightOrder o, List<OfferDTO.Section> out) {
        breakBulk.findByFreightOrder(o).ifPresent(b -> {
            OfferDTO.Section s = section("goods", "Cargo");
            String cargo = "OTHER".equalsIgnoreCase(b.getCargoType()) ? nz(b.getOtherCargoType(), "Other") : pretty(b.getCargoType());
            add(s, "Cargo type", cargo);
            add(s, "Details", b.getCargoDetails());
            add(s, "Nature of goods", b.getNatureOfGoods());
            if (b.getNumberOfPieces() != null) add(s, "Number of pieces", String.valueOf(b.getNumberOfPieces()));
            add(s, "Size of each piece", b.getDimensionsPerPiece());
            if (b.getTotalWeightKg() != null) add(s, "Total weight", trim(b.getTotalWeightKg()) + " kg");
            if (b.getTotalVolumeCbm() != null) add(s, "Total volume", trim(b.getTotalVolumeCbm()) + " m3");
            out.add(s);

            OfferDTO.Section care = section("handling", "Handling");
            flag(care, "Fragile", b.getFragile(), true);
            flag(care, "Hazardous material", b.getHazardousMaterial(), true);
            flag(care, "Temperature sensitive", b.getTemperatureSensitive(), true);
            flag(care, "Special handling required", b.getHandlingRequired(), true);
            flag(care, "Safety gear required", b.getSafetyRequired(), true);
            flag(care, "Insured", b.getInsuranceRequired(), false);
            flag(care, "Forklift needed", b.getForkliftNeeded(), false);
            flag(care, "Crane needed", b.getCraneNeeded(), false);
            flag(care, "Trolley needed", b.getTrolleyNeeded(), false);
            flag(care, "Manual labour needed", b.getManualLaborRequired(), false);
            if (!care.getRows().isEmpty()) out.add(care);
        });
    }

    private void bulkSection(FreightOrder o, OfferDTO.View v, List<OfferDTO.Section> out) {
        bulk.findByFreightOrder(o).ifPresent(b -> {
            OfferDTO.Section s = section("goods", "Material");
            add(s, "Material", "OTHER".equalsIgnoreCase(b.getMaterialType()) ? nz(b.getOtherMaterialType(), "Other") : pretty(b.getMaterialType()));
            if (b.getQuantity() != null) add(s, "Quantity", trim(b.getQuantity()) + " " + unit(b.getQuantityUnit()));
            if (b.getEstimatedWeightKg() != null) add(s, "Estimated weight", trim(b.getEstimatedWeightKg()) + " kg");
            if (b.getNumberOfTrucksNeeded() != null) add(s, "Trucks needed", String.valueOf(b.getNumberOfTrucksNeeded()));
            if (b.getEstimatedNumberOfTrips() != null) add(s, "Estimated trips", String.valueOf(b.getEstimatedNumberOfTrips()));
            out.add(s);

            OfferDTO.Section sites = section("sites", "Sites");
            add(sites, "Loading site", b.getLoadingSite());
            add(sites, "Road to the site", pretty(b.getSiteAccessibility()));
            add(sites, "Delivery site", b.getDeliverySite());
            add(sites, "Delivery time window", b.getDeliveryTimeWindow());
            add(sites, "Delivery instructions", b.getDeliveryInstructions());
            if (!sites.getRows().isEmpty()) out.add(sites);
        });
    }

    private void parcelSection(FreightOrder o, OfferDTO.View v, List<OfferDTO.Section> out) {
        parcels.findByFreightOrder(o).ifPresent(p -> {
            OfferDTO.Section s = section("goods", "Parcel");
            add(s, "Package type", pretty(p.getPackageType()));
            if (p.getNumberOfPackages() != null) add(s, "Number of packages", String.valueOf(p.getNumberOfPackages()));
            if (p.getWeightKg() != null) add(s, "Weight", trim(p.getWeightKg()) + " kg");
            add(s, "Packaging", pretty(p.getPackagingType()));
            flag(s, "Fragile", p.getFragile(), true);
            flag(s, "Express delivery", p.getExpressDelivery(), true);
            flag(s, "Same day", p.getSameDay(), true);
            if (p.getScheduledDeliveryTime() != null) add(s, "Deliver by", p.getScheduledDeliveryTime().format(WHEN));
            if (p.getPackageValue() != null) add(s, "Declared value", money(p.getPackageValue()));
            out.add(s);
            if (p.getCodAmount() != null && p.getCodAmount().signum() > 0) {
                v.setCashToCollect(p.getCodAmount());
                OfferDTO.Section pay = section("payment", "Cash on delivery");
                pay.getRows().add(new OfferDTO.Row("Collect from the receiver", money(p.getCodAmount()), true));
                out.add(pay);
            }
        });
    }

    // ================================================================ shop / food

    void fill(OfferDTO.View v, StoreOrder o) {
        List<OfferDTO.Section> sections = new ArrayList<>();
        v.setInstructions(blank(o.getInstructions()));

        OfferDTO.Section items = section("items", o.getStoreType() == com.bt.fretchain.primary.pojo.enums.StoreType.SHOP ? "Items to pick up" : "Food to pick up");
        if (o.getItems() != null) {
            for (StoreOrderItem i : o.getItems()) {
                OfferDTO.Item it = new OfferDTO.Item();
                it.setName(i.getItemName());
                it.setQuantity(i.getQuantity());
                it.setImageUrl(i.getImageUrl());
                items.getItems().add(it);
            }
        }
        sections.add(items);

        OfferDTO.Section pay = section("payment", "Payment");
        boolean cod = o.getPaymentMethod() == StorePaymentMethod.CASH_ON_DELIVERY;
        boolean paid = Boolean.TRUE.equals(o.getPaymentPaid());
        add(pay, "Method", cod ? "Cash on delivery" : pretty(o.getPaymentMethod() == null ? null : o.getPaymentMethod().name()));
        if (cod && !paid) {
            v.setCashToCollect(o.getTotalAmount());
            pay.getRows().add(new OfferDTO.Row("Collect from the customer", money(o.getTotalAmount()), true));
        } else {
            add(pay, "Status", paid ? "Already paid" : "Pay later");
        }
        v.setPaymentPaid(paid);
        sections.add(pay);
        v.setSections(sections);
    }

    // ================================================================ helpers

    private static OfferDTO.Section section(String key, String title) {
        OfferDTO.Section s = new OfferDTO.Section();
        s.setKey(key);
        s.setTitle(title);
        return s;
    }

    private static void add(OfferDTO.Section s, String label, String value) {
        if (value != null && !value.isBlank()) s.getRows().add(new OfferDTO.Row(label, value.trim(), false));
    }

    /** A yes/no field: only listed when true. */
    private static void flag(OfferDTO.Section s, String label, Boolean on, boolean highlight) {
        if (Boolean.TRUE.equals(on)) s.getRows().add(new OfferDTO.Row(label, "Yes", highlight));
    }

    private static String pretty(String code) {
        if (code == null || code.isBlank()) return null;
        String t = code.trim().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String unit(String u) {
        if (u == null) return "";
        return switch (u.toUpperCase()) {
            case "TONS" -> "tons";
            case "CUBIC_METERS" -> "m3";
            default -> pretty(u);
        };
    }

    private static String trim(Double d) {
        return d == null ? null : (d == Math.rint(d) ? String.valueOf(d.longValue()) : String.valueOf(d));
    }

    private static String money(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString() + " XAF";
    }

    private static String nz(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
