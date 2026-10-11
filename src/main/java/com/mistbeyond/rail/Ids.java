package com.mistbeyond.rail;

import net.minecraft.resources.Identifier;

public class Ids {
    public static final String MOD_ID = "mrail";

    private Ids() {
    }

    public static Identifier thisMod(String id) {
        return Identifier.fromNamespaceAndPath(MOD_ID, id);
    }
}
