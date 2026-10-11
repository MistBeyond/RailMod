package com.mistbeyond.rail.api.rail.section;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SignalAspect implements StringRepresentable {
    RED,
    GREEN;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
