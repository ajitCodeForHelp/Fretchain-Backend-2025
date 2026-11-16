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

    /** One stop on the freight journey: the status it sets, how it reads, and the button that moves the driver into it. */
    record Stage(OrderStatus status, String label, String button) {
    }

    /**
     * The journey for an order with `drops` drop points. The in-transit / arrived / unloading / unloaded cycle repeats
     * for every drop; the order is delivered after the last one.
     */
    static List<Stage> freightPlan(int drops) {
        int n = Math.max(drops, 1);
        List<Stage> plan = new ArrayList<>();
        plan.add(new Stage(OrderStatus.ACCEPTED, "Order accepted", null));
        plan.add(new Stage(OrderStatus.PICKUP_STARTED, "On the way to pickup", "Start trip to pickup"));
        plan.add(new Stage(OrderStatus.PICKUP_ARRIVED, "At the pickup", "I've arrived at the pickup"));
        plan.add(new Stage(OrderStatus.LOADING_STARTED, "Loading", "Start loading"));
        plan.add(new Stage(OrderStatus.LOADING_COMPLETED, "Loaded", "Loading finished"));
        for (int k = 0; k < n; k++) {
            String of = n > 1 ? " (drop " + (k + 1) + " of " + n + ")" : "";
            String no = n > 1 ? " " + (k + 1) : "";
            plan.add(new Stage(OrderStatus.IN_TRANSIT, "In transit" + of, k == 0 ? "Start delivery trip" : "Go to drop " + (k + 1)));
            plan.add(new Stage(OrderStatus.DROP_ARRIVED, "At the drop" + of, "I've arrived at the drop" + no));
            plan.add(new Stage(OrderStatus.UNLOADING_STARTED, "Unloading" + of, "Start unloading"));
            plan.add(new Stage(OrderStatus.UNLOADING_COMPLETED, "Unloaded" + of, "Unloading finished"));
        }
        plan.add(new Stage(OrderStatus.DELIVERED, "Delivered", n > 1 ? "Complete delivery" : "Complete delivery"));
        return plan;
    }

    /** Index in the plan, or -1 when the order is cancelled / not started. */
    static int freightPosition(FreightOrder o, int drops) {
        List<Stage> plan = freightPlan(drops);
        OrderStatus st = o.getOrderStatus();
        if (st == OrderStatus.DELIVERED) return plan.size() - 1;
        for (int i = 0; i < 5; i++) {
            if (plan.get(i).status() == st) return i;
        }
        int k = Math.min(o.getCurrentDropIndex() == null ? 0 : o.getCurrentDropIndex(), Math.max(drops, 1) - 1);
        for (int j = 0; j < 4; j++) {
            if (plan.get(5 + 4 * k + j).status() == st) return 5 + 4 * k + j;
        }
        return -1;
    }

    static void fill(OfferDTO.View v, FreightOrder o, int drops) {
        v.setOrderStatus(o.getOrderStatus().name());
        List<Stage> plan = freightPlan(drops);
        int pos = freightPosition(o, drops);
        boolean ended = pos < 0;
        List<OfferDTO.Step> list = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            OfferDTO.Step st = new OfferDTO.Step();
            st.setKey(i + ":" + plan.get(i).status().name());
            st.setLabel(plan.get(i).label());
            st.setDone(!ended && i <= pos);
            st.setCurrent(!ended && i == pos);
            list.add(st);
        }
        v.setSteps(list);
        v.setCurrentDrop(o.getCurrentDropIndex() == null ? 0 : o.getCurrentDropIndex());
        v.setStatusLabel(ended ? "Order cancelled" : plan.get(pos).label());
        v.setNext(ended || pos >= plan.size() - 1 ? null : next(plan.get(pos + 1).status().name(), plan.get(pos + 1).button(), true, null));
    }

    /** The status the order moves to when the driver presses the button, or null at the end. */
    static OrderStatus freightNextStatus(FreightOrder o, int drops) {
        List<Stage> plan = freightPlan(drops);
        int pos = freightPosition(o, drops);
        return pos < 0 || pos >= plan.size() - 1 ? null : plan.get(pos + 1).status();
    }

    /** The drop (0-based) the order will be on after the next press. */
    static int freightNextDrop(FreightOrder o, int drops) {
        List<Stage> plan = freightPlan(drops);
        int pos = freightPosition(o, drops);
        int next = pos + 1;
        if (pos < 0 || next >= plan.size() - 1 || next < 5) return o.getCurrentDropIndex() == null ? 0 : o.getCurrentDropIndex();
        return (next - 5) / 4;
    }

    /** A proof the driver must have saved before the next button works. */
    record ProofNeed(com.bt.fretchain.primary.pojo.enums.ProofStage stage, int dropSequence) {
    }

    /**
     * Pressing "Loading finished" needs the pickup record (goods check, photos, sender's signature); pressing
     * "Unloading finished" needs that drop's record (condition, photos, receiver's signature).
     */
    static ProofNeed freightProofNeed(FreightOrder o, int drops) {
        OrderStatus next = freightNextStatus(o, drops);
        if (next == OrderStatus.LOADING_COMPLETED) return new ProofNeed(com.bt.fretchain.primary.pojo.enums.ProofStage.PICKUP, 0);
        if (next == OrderStatus.UNLOADING_COMPLETED) return new ProofNeed(com.bt.fretchain.primary.pojo.enums.ProofStage.DROP, freightNextDrop(o, drops) + 1);
        return null;
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
