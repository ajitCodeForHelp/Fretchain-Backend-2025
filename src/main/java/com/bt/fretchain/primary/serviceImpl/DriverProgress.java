package com.bt.fretchain.primary.serviceImpl;

import com.bt.fretchain.primary.dto.OfferDTO;
import com.bt.fretchain.primary.pojo.FreightOrder;
import com.bt.fretchain.primary.pojo.StoreOrder;
import com.bt.fretchain.primary.pojo.enums.OrderStatus;
import com.bt.fretchain.primary.pojo.enums.StoreOrderStatus;
import com.bt.fretchain.primary.pojo.enums.StorePaymentMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * The driver's journey for an accepted order: where it is now and the one button to press next.
 *
 *   shop / food : arrived at store -> picked up (once the store marked it READY) -> arrived at customer -> delivered
 *   freight     : start trip -> arrived at pickup -> loading -> loaded -> in transit -> arrived at drop -> unloading -> unloaded -> delivered
 */
final class DriverProgress {

    private DriverProgress() {
    }

    // ---------------------------------------------------------------- shop / food

    static final String ARRIVED_STORE = "ARRIVED_STORE";
    static final String ARRIVED_CUSTOMER = "ARRIVED_CUSTOMER";
    private static final String[][] STORE_STEPS = {
            {"ACCEPTED", "Order accepted"}, {"AT_STORE", "At the store"}, {"PICKED_UP", "Picked up"},
            {"AT_CUSTOMER", "At the customer"}, {"DELIVERED", "Delivered"}};

    static int storePosition(StoreOrder o) {
        if (o.getStatus() == StoreOrderStatus.DELIVERED) return 4;
        if (o.getStatus() == StoreOrderStatus.OUT_FOR_DELIVERY) return ARRIVED_CUSTOMER.equals(o.getDriverStep()) ? 3 : 2;
        return ARRIVED_STORE.equals(o.getDriverStep()) ? 1 : 0;
    }

    static void fill(OfferDTO.View v, StoreOrder o) {
        v.setOrderStatus(o.getStatus().name());
        int pos = storePosition(o);
        v.setSteps(steps(STORE_STEPS, pos));
        boolean ended = o.getStatus() == StoreOrderStatus.REJECTED || o.getStatus() == StoreOrderStatus.CANCELLED;
        v.setStatusLabel(ended ? (o.getStatus() == StoreOrderStatus.CANCELLED ? "Order cancelled" : "Order rejected") : STORE_STEPS[pos][1]);
        v.setNext(ended ? null : storeNext(o, pos));
    }

    private static OfferDTO.NextStep storeNext(StoreOrder o, int pos) {
        switch (pos) {
            case 0: return next("ARRIVED_STORE", "I've arrived at the store", true, null);
            case 1: {
                boolean ready = o.getStatus() == StoreOrderStatus.READY;
                return next("PICKED_UP", "Picked up the order", ready, ready ? null : "Waiting for the store to get the order ready");
            }
            case 2: return next("ARRIVED_CUSTOMER", "I've arrived at the customer", true, null);
            case 3: return next("DELIVERED", o.getPaymentMethod() == StorePaymentMethod.CASH_ON_DELIVERY ? "Delivered and cash collected" : "Delivered", true, null);
            default: return null;
        }
    }

    // ---------------------------------------------------------------- freight

    private static final OrderStatus[] FREIGHT_FLOW = {
            OrderStatus.ACCEPTED, OrderStatus.PICKUP_STARTED, OrderStatus.PICKUP_ARRIVED, OrderStatus.LOADING_STARTED,
            OrderStatus.LOADING_COMPLETED, OrderStatus.IN_TRANSIT, OrderStatus.DROP_ARRIVED, OrderStatus.UNLOADING_STARTED,
            OrderStatus.UNLOADING_COMPLETED, OrderStatus.DELIVERED};
    private static final String[] FREIGHT_LABEL = {
            "Order accepted", "On the way to pickup", "At the pickup", "Loading", "Loaded", "In transit", "At the drop",
            "Unloading", "Unloaded", "Delivered"};
    private static final String[] FREIGHT_BUTTON = {
            "Start trip to pickup", "I've arrived at the pickup", "Start loading", "Loading finished", "Start delivery trip",
            "I've arrived at the drop", "Start unloading", "Unloading finished", "Complete delivery"};

    static int freightPosition(FreightOrder o) {
        for (int i = 0; i < FREIGHT_FLOW.length; i++) {
            if (FREIGHT_FLOW[i] == o.getOrderStatus()) return i;
        }
        return -1;
    }

    static void fill(OfferDTO.View v, FreightOrder o) {
        v.setOrderStatus(o.getOrderStatus().name());
        int pos = freightPosition(o);
        boolean ended = pos < 0;
        List<OfferDTO.Step> list = new ArrayList<>();
        for (int i = 0; i < FREIGHT_FLOW.length; i++) {
            OfferDTO.Step st = new OfferDTO.Step();
            st.setKey(FREIGHT_FLOW[i].name());
            st.setLabel(FREIGHT_LABEL[i]);
            st.setDone(!ended && i <= pos);
            st.setCurrent(!ended && i == pos);
            list.add(st);
        }
        v.setSteps(list);
        v.setStatusLabel(ended ? "Order cancelled" : FREIGHT_LABEL[pos]);
        v.setNext(ended || pos >= FREIGHT_FLOW.length - 1 ? null : next(FREIGHT_FLOW[pos + 1].name(), FREIGHT_BUTTON[pos], true, null));
    }

    static OrderStatus freightNextStatus(FreightOrder o) {
        int pos = freightPosition(o);
        return pos < 0 || pos >= FREIGHT_FLOW.length - 1 ? null : FREIGHT_FLOW[pos + 1];
    }

    // ---------------------------------------------------------------- helpers

    private static List<OfferDTO.Step> steps(String[][] def, int pos) {
        List<OfferDTO.Step> list = new ArrayList<>();
        for (int i = 0; i < def.length; i++) {
            OfferDTO.Step st = new OfferDTO.Step();
            st.setKey(def[i][0]);
            st.setLabel(def[i][1]);
            st.setDone(i <= pos);
            st.setCurrent(i == pos);
            list.add(st);
        }
        return list;
    }

    private static OfferDTO.NextStep next(String key, String label, boolean enabled, String hint) {
        OfferDTO.NextStep n = new OfferDTO.NextStep();
        n.setKey(key);
        n.setLabel(label);
        n.setEnabled(enabled);
        n.setHint(hint);
        return n;
    }
}
