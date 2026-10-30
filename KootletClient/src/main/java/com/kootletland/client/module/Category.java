package com.kootletland.client.module;

public enum Category {
    HUD("HUD"), PVP("PvP"), VISUAL("Visual"), UTILITY("Utility");

    public final String label;
    Category(String label) { this.label = label; }
}
