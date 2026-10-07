package com.swampd.superficialtrauma.common.damage;

import java.util.Locale;

/** Optional wound metadata, never a replacement for vanilla health or the wound merge key. */
public enum GunshotRegion {
    HEAD, CHEST;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "gunshot_region.superficialtrauma." + serializedName();
    }
}
