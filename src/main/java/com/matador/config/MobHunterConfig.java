package com.matador.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MobHunterConfig {
	public static final String DEFAULT_TARGET_MOB_ID = "minecraft:rabbit";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("matador-mob-hunter.json");

	public boolean enabled = false;
	public String targetMobId = DEFAULT_TARGET_MOB_ID;
	public double searchRadius = 16.0D;
	public double attackDistance = 3.0D;
	public int attackDelayTicks = 10;
	public boolean oneClickMode = false;
	public double oneClickSwitchHealth = 100.0D;
	public boolean captureMobLog = true;
	public boolean prioritizeRareMobs = true;
	public int panelOffsetX = 0;
	public int panelOffsetY = 0;

	public static MobHunterConfig load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				MobHunterConfig config = GSON.fromJson(reader, MobHunterConfig.class);
				if (config != null) {
					config.normalize();
					return config;
				}
			} catch (IOException ignored) {
				// Usa a configuração padrão se o arquivo não puder ser lido.
			}
		}

		MobHunterConfig config = new MobHunterConfig();
		config.save();
		return config;
	}

	public void save() {
		normalize();

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
			// Mods do lado do cliente não devem fechar o jogo por falha ao salvar configuração.
		}
	}

	public void normalize() {
		if (targetMobId == null || targetMobId.isBlank()) {
			targetMobId = DEFAULT_TARGET_MOB_ID;
		}

		searchRadius = clamp(searchRadius, 4.0D, 64.0D);
		attackDistance = clamp(attackDistance, 1.0D, 6.0D);
		oneClickSwitchHealth = clamp(oneClickSwitchHealth, 1.0D, 1000.0D);
		attackDelayTicks = Math.max(1, Math.min(attackDelayTicks, 40));
		panelOffsetX = Math.max(-10000, Math.min(panelOffsetX, 10000));
		panelOffsetY = Math.max(-10000, Math.min(panelOffsetY, 10000));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
