package com.matador.client.hunter;

import com.matador.config.MobHunterConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MobHunterController {
    private static final Path MOB_LOG_PATH = FabricLoader.getInstance().getConfigDir().resolve("matador-mob-hunter-seen-mobs.log");
    private static MobHunterController instance;
    private final MobHunterConfig config;
    private final Set<String> loggedMobKeys = new HashSet<>();
    private final Set<UUID> completedOneClickTargets = new HashSet<>();
    private final Random random = new Random();

    private LivingEntity currentTarget;
    private int foundMobCount;
    private int attackCooldownTicks;
    private boolean controllingMovement;
    private int oneClickTargetId = -1;
    private int ticksSinceLastTargetChange = 0; // Previne ataques instantâneos ao trocar de alvo

    // Variáveis para suavização da mira (Smooth Aiming)
    private float currentYaw;
    private float currentPitch;
    private float targetYaw;
    private float targetPitch;

    // Offsets fixos gerados ao trocar de alvo para evitar Jittering (tremedeira detectável pelo anti-cheat)
    private double targetOffsetX;
    private double targetOffsetY;
    private double targetOffsetZ;

    private MobHunterController() {
        this.config = MobHunterConfig.load();
        ensureValidTargetMob();
    }

    public static MobHunterController getInstance() {
        if (instance == null) {
            instance = new MobHunterController();
        }
        return instance;
    }

    public MobHunterConfig getConfig() {
        return config;
    }

    public void tick(Minecraft client) {
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }

        ticksSinceLastTargetChange++;

        if (client.level == null || client.player == null) {
            stopMoving(client);
            clearTarget();
            foundMobCount = 0;
            completedOneClickTargets.clear();
            return;
        }

        if (config.oneClickMode && oneClickTargetId != -1) {
            if (isValidWaitingTarget(client)) {
                stopMoving(client);
                return;
            }

            oneClickTargetId = -1;
            clearTarget();
        }

        refreshTarget(client);

        if (!config.enabled || currentTarget == null || client.gameMode == null) {
            stopMoving(client);
            return;
        }

        double distance = getCurrentTargetDistance(client);

        lookAtTargetSmoothly(client);

        if (distance > config.attackDistance) {
            walkToTarget(client);
            return;
        }

        stopMoving(client);

        // Só ataca se tiver terminado o cooldown e se a mira estiver repousada após a troca de alvo (mínimo 3 ticks)
        if (attackCooldownTicks <= 0 && ticksSinceLastTargetChange > 3) {
            // Aplica uma ligeira aleatoriedade à distância de ataque
            if (distance <= config.attackDistance - (random.nextDouble() * 0.3)) {
                 
                 // SÓ ATACA SE A MIRA ESTIVER NO ALVO. Tolerância de 15 graus. 
                 // (Evita kicks do Vulcan por atacar entidades fora da retícula / KillAura flag)
                 if (isAimingAtTarget(targetYaw, targetPitch, currentYaw, currentPitch, 15.0F)) {
                     client.gameMode.attack(client.player, currentTarget);
                     client.player.swing(InteractionHand.MAIN_HAND);
                     
                     // Aleatorizar ligeiramente o atraso do ataque
                     int delayVariation = random.nextInt(5) - 2; // Variação de -2 a +2 ticks
                     attackCooldownTicks = Math.max(1, config.attackDelayTicks + delayVariation);

                     LivingEntity hitTarget = currentTarget;
                     logMob(hitTarget, config.oneClickMode ? "HIT_ONCE_WAIT" : "ATTACK", distance);
                     if (config.oneClickMode) {
                         oneClickTargetId = hitTarget.getId();
                     }
                 }
            }
        }
    }

    public void toggleEnabled(Minecraft client) {
        config.enabled = !config.enabled;
        if (!config.enabled) {
            stopMoving(client);
            clearTarget();
            oneClickTargetId = -1;
            completedOneClickTargets.clear();
        }
        config.save();
    }

    public void stopAutomaticMovement(Minecraft client) {
        stopMoving(client);
    }

    public void setTargetMob(TargetMobOption option, Minecraft client) {
        config.targetMobId = option.entityId;
        config.save();
        clearTarget();
        oneClickTargetId = -1;
        completedOneClickTargets.clear();
        refreshTarget(client);
    }

    public void clearTarget() {
        currentTarget = null;
        ticksSinceLastTargetChange = 0;
    }

    public void refreshTarget(Minecraft client) {
        if (client.level == null || client.player == null) {
            currentTarget = null;
            foundMobCount = 0;
            return;
        }

        TargetMobOption option = getSelectedOption();
        Player player = client.player;
        AABB searchBox = player.getBoundingBox().inflate(config.searchRadius);
        LivingEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        int bestPriority = Integer.MIN_VALUE;
        int count = 0;
        boolean currentTargetStillValid = false;

        for (Entity entity : client.level.getEntities(player, searchBox, candidate -> candidate.getType() == option.entityType)) {
            if (!(entity instanceof LivingEntity livingEntity) || !isValidTarget(livingEntity, player)) {
                continue;
            }

            count++;
            if (currentTarget != null && livingEntity.getUUID().equals(currentTarget.getUUID())) {
                currentTargetStillValid = true;
            }

            double distanceSquared = livingEntity.distanceToSqr(player);
            logSeenMob(livingEntity, Math.sqrt(distanceSquared));

            int priority = getMobPriority(livingEntity);

            if (priority > bestPriority || priority == bestPriority && distanceSquared < nearestDistanceSquared) {
                bestPriority = priority;
                nearestDistanceSquared = distanceSquared;
                nearest = livingEntity;
            }
        }

        foundMobCount = count;

        if (currentTargetStillValid) {
            return;
        }
        
        // Atualizar o alvo apenas se ele mudou para evitar reiniciar o smooth aim
        if (currentTarget != nearest) {
            currentTarget = nearest;
            ticksSinceLastTargetChange = 0; // Reset ao trocar de alvo
            
            if (client.player != null) {
                currentYaw = client.player.getYRot();
                currentPitch = client.player.getXRot();
            }
            
            // Define os offsets de forma randômica APENAS ao trocar de alvo (evita tremedeira da mira)
            if (nearest != null) {
                targetOffsetX = (random.nextDouble() - 0.5) * 0.4;
                targetOffsetY = random.nextDouble() * 0.3; // Um pouco mais focado perto do meio
                targetOffsetZ = (random.nextDouble() - 0.5) * 0.4;
            }
        }
    }

    public TargetMobOption getSelectedOption() {
        return TargetMobOption.byEntityId(config.targetMobId)
                .orElseGet(() -> {
                    config.targetMobId = TargetMobOption.defaultOption().entityId;
                    config.save();
                    return TargetMobOption.defaultOption();
                });
    }

    public String getCurrentTargetName() {
        if (currentTarget == null) {
            return "Nenhum";
        }
        Component name = currentTarget.getDisplayName();
        return name == null ? "Nenhum" : name.getString();
    }

    public double getCurrentTargetDistance(Minecraft client) {
        if (currentTarget == null || client.player == null) {
            return 0.0D;
        }
        return Math.sqrt(currentTarget.distanceToSqr(client.player));
    }

    public int getFoundMobCount() {
        return foundMobCount;
    }

    public String getOneClickStatus() {
        if (!config.oneClickMode) {
            return "OFF";
        }
        return oneClickTargetId == -1 ? "ON" : "Aguardando";
    }

    public void toggleOneClickMode(Minecraft client) {
        config.oneClickMode = !config.oneClickMode;
        oneClickTargetId = -1;
        completedOneClickTargets.clear();
        stopMoving(client);
        config.save();
        refreshTarget(client);
    }

    public void toggleMobLog() {
        config.captureMobLog = !config.captureMobLog;
        config.save();
    }

    public void toggleRarePriority() {
        config.prioritizeRareMobs = !config.prioritizeRareMobs;
        config.save();
    }

    private void ensureValidTargetMob() {
        if (TargetMobOption.byEntityId(config.targetMobId).isEmpty()) {
            config.targetMobId = TargetMobOption.defaultOption().entityId;
            config.save();
        }
    }

    private void walkToTarget(Minecraft client) {
        if (client.options == null || client.player == null) {
            return;
        }
        client.options.keyUp.setDown(true);
        client.options.keySprint.setDown(true);
        controllingMovement = true;
    }

    private void stopMoving(Minecraft client) {
        if (!controllingMovement || client.options == null) {
            return;
        }
        client.options.keyUp.setDown(false);
        client.options.keySprint.setDown(false);
        controllingMovement = false;
    }

    private void lookAtTargetSmoothly(Minecraft client) {
        if (client.player == null || currentTarget == null) {
            return;
        }

        double dx = (currentTarget.getX() + targetOffsetX) - client.player.getX();
        double dz = (currentTarget.getZ() + targetOffsetZ) - client.player.getZ();
        
        // Apontar de forma ligeiramente aleatória para a zona do corpo/cabeça mantendo o ruído fixo
        double dy = (currentTarget.getY() + (currentTarget.getBbHeight() * 0.5) + targetOffsetY) - client.player.getEyeY();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        
        targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));

        // Limita o pitch para não virar a cabeça além do limite humano possível (<-90 ou >90 causará kick)
        targetPitch = Math.max(-90.0F, Math.min(90.0F, targetPitch));

        // Velocidade de interpolação
        float aimSpeed = 0.20f + (random.nextFloat() * 0.1f); 

        currentYaw = lerpAngle(currentYaw, targetYaw, aimSpeed);
        currentPitch = lerp(currentPitch, targetPitch, aimSpeed);

        client.player.setYRot(currentYaw);
        client.player.setXRot(currentPitch);
    }
    
    private float lerp(float start, float end, float delta) {
        return start + delta * (end - start);
    }

    private float lerpAngle(float start, float end, float delta) {
        float difference = end - start;
        while (difference < -180.0F) difference += 360.0F;
        while (difference >= 180.0F) difference -= 360.0F;
        return start + delta * difference;
    }

    // Checa se a mira já está perto o suficiente do alvo (evita ataque de killaura)
    private boolean isAimingAtTarget(float tYaw, float tPitch, float cYaw, float cPitch, float tolerance) {
        float yawDiff = Math.abs(tYaw - cYaw) % 360.0F;
        if (yawDiff > 180.0F) {
            yawDiff = 360.0F - yawDiff;
        }
        float pitchDiff = Math.abs(tPitch - cPitch);
        
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }

    private boolean isValidWaitingTarget(Minecraft client) {
        if (currentTarget == null || client.player == null) {
            return false;
        }
        if (currentTarget.getId() != oneClickTargetId || !currentTarget.isAlive() || currentTarget.isRemoved()) {
            return false;
        }

        double distanceSquared = currentTarget.distanceToSqr(client.player);
        return distanceSquared <= config.searchRadius * config.searchRadius;
    }

    private void logSeenMob(LivingEntity entity, double distance) {
        if (!config.captureMobLog) {
            return;
        }
        String key = entity.getType() + "|" + getEntityName(entity) + "|" + getMobPriority(entity);
        if (loggedMobKeys.add(key)) {
            logMob(entity, "SEEN", distance);
        }
    }

    private void logMob(LivingEntity entity, String event, double distance) {
        if (!config.captureMobLog) {
            return;
        }
        try {
            Files.createDirectories(MOB_LOG_PATH.getParent());
            String line = String.format(
                    Locale.ROOT,
                    "[%s] %s id=%d uuid=%s type=%s name=\"%s\" priority=%d displayHealth=%.1f entityHealth=%.1f/%.1f distance=%.1f%n",
                    LocalDateTime.now(), event, entity.getId(), entity.getUUID(), entity.getType(), getEntityName(entity),
                    getMobPriority(entity), getDisplayHealth(entity), entity.getHealth(), entity.getMaxHealth(), distance
            );
            Files.writeString(MOB_LOG_PATH, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging is diagnostic only and must not crash the client.
        }
    }

    private int getMobPriority(LivingEntity entity) {
        if (!config.prioritizeRareMobs) {
            return 0;
        }
        String name = getEntityName(entity).toLowerCase(Locale.ROOT);
        int rarityPriority = 0;
        if (name.contains("legendary") || name.contains("lendario")) {
            rarityPriority = 5000;
        } else if (name.contains("mythic") || name.contains("mitico")) {
            rarityPriority = 4500;
        } else if (name.contains("epic") || name.contains("epico")) {
            rarityPriority = 3500;
        } else if (name.contains("rare") || name.contains("raro")) {
            rarityPriority = 2500;
        } else if (name.contains("uncommon") || name.contains("incomum")) {
            rarityPriority = 1000;
        } else if (name.contains("common") || name.contains("comum")) {
            rarityPriority = 100;
        }
        return rarityPriority + Math.min(999, (int) (getDisplayHealth(entity) / 10.0D));
    }

    private double getDisplayHealth(LivingEntity entity) {
        return parseDisplayHealth(getEntityName(entity));
    }

    private double parseDisplayHealth(String name) {
        int marker = Math.max(name.lastIndexOf('\u2764'), Math.max(name.lastIndexOf("HP"), name.lastIndexOf("hp")));
        if (marker < 0) {
            return -1.0D;
        }
        String value = name.substring(marker + 1).trim();
        if (value.isEmpty()) {
            return -1.0D;
        }
        String normalizedValue = value.toUpperCase(Locale.ROOT);
        boolean thousands = normalizedValue.contains("K");
        boolean millions = normalizedValue.contains("M");
        StringBuilder number = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isDigit(character) || character == ',' || character == '.') {
                number.append(character == ',' ? '.' : character);
            } else if (number.length() > 0) {
                break;
            }
        }
        if (number.isEmpty()) {
            return -1.0D;
        }
        try {
            double parsed = Double.parseDouble(number.toString());
            if (millions) {
                return parsed * 1_000_000.0D;
            }
            return thousands ? parsed * 1000.0D : parsed;
        } catch (NumberFormatException exception) {
            return -1.0D;
        }
    }

    private String getEntityName(LivingEntity entity) {
        Component name = entity.getDisplayName();
        if (name == null) {
            return entity.getType().toString();
        }
        return name.getString();
    }

    private boolean isValidTarget(LivingEntity entity, Player player) {
        return entity.isAlive() && !entity.isRemoved() &&
                (!config.oneClickMode || entity.getId() == oneClickTargetId || oneClickTargetId == -1) &&
                !completedOneClickTargets.contains(entity.getUUID()) &&
                entity.distanceToSqr(player) <= config.searchRadius * config.searchRadius;
    }
}