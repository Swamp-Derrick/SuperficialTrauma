package com.swampd.superficialtrauma.common.audio;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Classify the sound, not whether a screen happens to be open when it is played. */
public final class WorldSoundPolicy {
    private WorldSoundPolicy() {}

    public static boolean isExternal(ResourceLocation id, SoundSource category, boolean relative) {
        // Our current sounds are exclusively medical actions, QTE or the player's internal symptoms.
        if (id.getNamespace().equals("superficialtrauma") || id.getNamespace().equals("voicechat")) return false;
        String path = id.getPath();
        if (path.startsWith("ui.") || path.startsWith("ui/") || path.startsWith("gui.") || path.startsWith("gui/")
                || path.startsWith("menu.") || path.startsWith("menu/")) return false;
        // Positional MASTER sounds are still external. Do not exempt PLAYERS: gunshots/footsteps use it.
        return category != SoundSource.MUSIC && category != SoundSource.VOICE
                && !(category == SoundSource.MASTER && relative);
    }
}
