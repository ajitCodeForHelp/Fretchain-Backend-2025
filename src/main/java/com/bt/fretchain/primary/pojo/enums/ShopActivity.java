package com.bt.fretchain.primary.pojo.enums;

/**
 * Permissions a shop role can be given: one menu (group) x one action.
 *
 *   VIEW   - the menu is visible, records can be opened
 *   ADD    - create records
 *   EDIT   - change records
 *   STATUS - activate / deactivate records
 *
 * Any write action implies VIEW of the same menu. The owner role has everything.
 * To add a menu (e.g. ORDER, REPORT) add its constants here and use them in the
 * controller; the role screen picks them up automatically.
 */
public enum ShopActivity {
    PROFILE_VIEW("PROFILE", Action.VIEW),
    PROFILE_EDIT("PROFILE", Action.EDIT),

    CATEGORY_VIEW("CATEGORY", Action.VIEW),
    CATEGORY_ADD("CATEGORY", Action.ADD),
    CATEGORY_EDIT("CATEGORY", Action.EDIT),
    CATEGORY_STATUS("CATEGORY", Action.STATUS),

    SUBCATEGORY_VIEW("SUBCATEGORY", Action.VIEW),
    SUBCATEGORY_ADD("SUBCATEGORY", Action.ADD),
    SUBCATEGORY_EDIT("SUBCATEGORY", Action.EDIT),
    SUBCATEGORY_STATUS("SUBCATEGORY", Action.STATUS),

    ITEM_VIEW("ITEM", Action.VIEW),
    ITEM_ADD("ITEM", Action.ADD),
    ITEM_EDIT("ITEM", Action.EDIT),
    ITEM_STATUS("ITEM", Action.STATUS),

    ROLE_VIEW("ROLE", Action.VIEW),
    ROLE_ADD("ROLE", Action.ADD),
    ROLE_EDIT("ROLE", Action.EDIT),
    ROLE_STATUS("ROLE", Action.STATUS),

    STAFF_VIEW("STAFF", Action.VIEW),
    STAFF_ADD("STAFF", Action.ADD),
    STAFF_EDIT("STAFF", Action.EDIT),
    STAFF_STATUS("STAFF", Action.STATUS),

    // ---- legacy (roles saved before ADD / EDIT / STATUS existed): grant all write actions
    @Deprecated CATEGORY_MANAGE("CATEGORY", Action.MANAGE),
    @Deprecated SUBCATEGORY_MANAGE("SUBCATEGORY", Action.MANAGE),
    @Deprecated ITEM_MANAGE("ITEM", Action.MANAGE),
    @Deprecated ROLE_MANAGE("ROLE", Action.MANAGE),
    @Deprecated STAFF_MANAGE("STAFF", Action.MANAGE);

    public enum Action { VIEW, ADD, EDIT, STATUS, MANAGE }

    private final String group;
    private final Action action;

    ShopActivity(String group, Action action) {
        this.group = group;
        this.action = action;
    }

    public String getGroup() {
        return group;
    }

    public Action getAction() {
        return action;
    }

    /** Anything but VIEW. */
    public boolean isWrite() {
        return action != Action.VIEW;
    }

    /** Old MANAGE values: still understood, no longer offered. */
    public boolean isLegacy() {
        return action == Action.MANAGE;
    }

    /** The VIEW activity of the same menu. */
    public ShopActivity viewActivity() {
        for (ShopActivity a : values()) {
            if (a.group.equals(group) && a.action == Action.VIEW) {
                return a;
            }
        }
        return this;
    }
}
