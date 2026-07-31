package com.shieldbreaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ShieldBreakerMod implements ModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int axeSlot = -1;
    private int previousSlot = -1;
    private boolean swapped = false;
    private long swapTime = 0;

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
                PlayerEntity target = findTarget();
                if (target != null && target.isBlocking()) {
                    breakShield(target);
                } else {
                    resetSlot();
                }
            } else {
                resetSlot();
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

    private void breakShield(PlayerEntity target) {
        if (mc.interactionManager == null || mc.player == null) return;
        
        PlayerInventory inv = mc.player.getInventory();
        
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }
        
        if (axeSlot == -1) return;
        
        if (!swapped) {
            previousSlot = inv.selectedSlot;
            inv.selectedSlot = axeSlot;
            swapped = true;
            swapTime = System.currentTimeMillis();
        }
        
        if (System.currentTimeMillis() - swapTime > 100) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            resetSlot();
            swapTime = System.currentTimeMillis() + 500;
        }
    }

    private void resetSlot() {
        if (swapped && previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
            swapped = false;
            previousSlot = -1;
            axeSlot = -1;
        }
    }
}
