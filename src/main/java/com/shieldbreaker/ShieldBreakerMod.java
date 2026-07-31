package com.shieldbreaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ShieldBreakerMod implements ModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static long lastBreak = 0;
    private static long nextBreak = 0;
    private static PlayerEntity currentTarget = null;

    @Override
    public void onInitialize() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Shield Breaker",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_INSERT,
            "Shield Breaker Mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                enabled = !enabled;
                String msg = enabled ? "§aACIK" : "§cKAPALI";
                if (mc.player != null) {
                    mc.player.sendMessage(Text.of("§6[ShieldBreaker] §eDurum: " + msg), true);
                }
            }

            if (enabled && mc.player != null && mc.interactionManager != null) {
                long now = System.currentTimeMillis();
                
                if (currentTarget != null && now >= nextBreak) {
                    doBreakShield(currentTarget);
                    currentTarget = null;
                }
                
                if (currentTarget == null && now - lastBreak > 1000) {
                    PlayerEntity target = findTarget();
                    if (target != null && target.isBlocking()) {
                        currentTarget = target;
                        nextBreak = now + 100;
                    }
                }
            }
        });
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        double closest = 4.0;
        PlayerEntity closestPlayer = null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isBlocking()) {
                double dist = mc.player.distanceTo(player);
                if (dist < closest) {
                    closest = dist;
                    closestPlayer = player;
                }
            }
        }
        return closestPlayer;
    }

    private void doBreakShield(PlayerEntity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        
        // Baltayı bul - anlık geçiş
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }
        
        if (axeSlot == -1) return;
        
        // Direkt baltayla vur - slot değiştir
        int old = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = axeSlot;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.getInventory().selectedSlot = old;
        
        lastBreak = System.currentTimeMillis();
        currentTarget = null;
    }
}
