package com.shieldbreaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ShieldBreakerMod implements ModInitializer {

    public static boolean enabled = false;
    public static boolean showOverlay = true;
    private static KeyBinding toggleKey;
    private static KeyBinding overlayKey;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final double FOV = 90.0;
    private static final double RANGE = 2.8;

    @Override
    public void onInitialize() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Shield Breaker",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_INSERT,
            "Shield Breaker"
        ));
        
        overlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Overlay Ac/Kapat",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_HOME,
            "Shield Breaker"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                enabled = !enabled;
                String msg = enabled ? "§aACIK" : "§cKAPALI";
                if (mc.player != null) {
                    mc.player.sendMessage(Text.of("§6[ShieldBreaker] §eDurum: " + msg), true);
                }
            }
            
            if (overlayKey.wasPressed()) {
                showOverlay = !showOverlay;
            }

            if (enabled && mc.player != null && mc.interactionManager != null) {
                PlayerEntity target = findTargetInFov();
                if (target != null && target.isBlocking()) {
                    breakShield(target);
                }
            }
        });
        
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!showOverlay || mc.player == null) return;
            
            int centerX = drawContext.getScaledWindowWidth() / 2;
            int centerY = drawContext.getScaledWindowHeight() / 2;
            int radius = (int)((FOV / 90.0) * 150);
            
            // FOV dairesi
            for (int i = 0; i < 360; i++) {
                double rad = Math.toRadians(i);
                int x = centerX + (int)(Math.cos(rad) * radius);
                int y = centerY + (int)(Math.sin(rad) * radius);
                if (i % 2 == 0) {
                    drawContext.fill(x, y, x + 1, y + 1, 0x3000FF00);
                }
            }
            
            // Artı işareti
            int color = enabled ? 0xFFFF0000 : 0xFFFFFFFF;
            drawContext.fill(centerX - 1, centerY - 8, centerX + 1, centerY + 8, color);
            drawContext.fill(centerX - 8, centerY - 1, centerX + 8, centerY + 1, color);
            
            // Yazılar
            TextRenderer renderer = mc.textRenderer;
            drawContext.drawText(renderer, Text.of(enabled ? "§aSHIELD BREAKER: ACIK" : "§cSHIELD BREAKER: KAPALI"), 10, 10, 0xFFFFFFFF, true);
            drawContext.drawText(renderer, Text.of("§7FOV: " + (int)FOV + "° | Range: " + RANGE), 10, 25, 0xFFFFFFFF, true);
            drawContext.drawText(renderer, Text.of("§7[HOME] Overlay | [INSERT] Ac/Kapat"), 10, 55, 0xFFAAAAAA, true);
        });
    }

    private boolean isInFov(PlayerEntity target) {
        if (mc.player == null) return false;
        
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = target.getEyePos();
        Vec3d toTarget = targetPos.subtract(playerPos).normalize();
        Vec3d lookVec = mc.player.getRotationVector();
        
        double dot = toTarget.dotProduct(lookVec);
        double angle = Math.acos(Math.max(-1, Math.min(1, dot))) * (180.0 / Math.PI);
        
        return angle <= FOV / 2.0;
    }

    private PlayerEntity findTargetInFov() {
        if (mc.world == null || mc.player == null) return null;
        
        PlayerEntity best = null;
        double bestDist = RANGE;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isBlocking()) continue;
            
            double dist = mc.player.distanceTo(player);
            if (dist > RANGE) continue;
            if (!isInFov(player)) continue;
            
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private void breakShield(PlayerEntity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }
        
        if (axeSlot == -1) return;
        
        int old = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = axeSlot;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.getInventory().selectedSlot = old;
    }
}
