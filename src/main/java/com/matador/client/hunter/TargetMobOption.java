package com.matador.client.hunter;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TargetMobOption {
	private static List<TargetMobOption> options;

	public final String entityId;
	public final String displayName;
	public final EntityType<?> entityType;
	public ItemStack icon;
	private final Supplier<Item> iconItemSupplier;

	public TargetMobOption(String entityId, String displayName, EntityType<?> entityType, ItemStack icon) {
		this.entityId = entityId;
		this.displayName = displayName;
		this.entityType = entityType;
		this.icon = icon;
		this.iconItemSupplier = icon::getItem;
	}

	private TargetMobOption(String entityId, String displayName, EntityType<?> entityType, Supplier<Item> iconItemSupplier) {
		this.entityId = entityId;
		this.displayName = displayName;
		this.entityType = entityType;
		this.icon = null;
		this.iconItemSupplier = iconItemSupplier;
	}

	public static List<TargetMobOption> options() {
		if (options == null) {
			options = Collections.unmodifiableList(Arrays.asList(
				new TargetMobOption("minecraft:rabbit", "Coelho", EntityType.RABBIT, () -> Items.RABBIT_SPAWN_EGG),
				new TargetMobOption("minecraft:chicken", "Galinha", EntityType.CHICKEN, () -> Items.CHICKEN_SPAWN_EGG),
				new TargetMobOption("minecraft:cow", "Vaca", EntityType.COW, () -> Items.COW_SPAWN_EGG),
				new TargetMobOption("minecraft:sheep", "Ovelha", EntityType.SHEEP, () -> Items.SHEEP_SPAWN_EGG),
				new TargetMobOption("minecraft:pig", "Porco", EntityType.PIG, () -> Items.PIG_SPAWN_EGG),
				new TargetMobOption("minecraft:camel", "Camelo", EntityType.CAMEL, () -> Items.CAMEL_SPAWN_EGG),
				new TargetMobOption("minecraft:donkey", "Burro", EntityType.DONKEY, () -> Items.DONKEY_SPAWN_EGG),
				new TargetMobOption("minecraft:horse", "Cavalo", EntityType.HORSE, () -> Items.HORSE_SPAWN_EGG),
				new TargetMobOption("minecraft:mule", "Mula", EntityType.MULE, () -> Items.MULE_SPAWN_EGG),
				new TargetMobOption("minecraft:cat", "Gato", EntityType.CAT, () -> Items.CAT_SPAWN_EGG),
				new TargetMobOption("minecraft:parrot", "Papagaio", EntityType.PARROT, () -> Items.PARROT_SPAWN_EGG),
				new TargetMobOption("minecraft:wolf", "Lobo", EntityType.WOLF, () -> Items.WOLF_SPAWN_EGG),
				new TargetMobOption("minecraft:armadillo", "Tatu", EntityType.ARMADILLO, () -> Items.ARMADILLO_SPAWN_EGG),
				new TargetMobOption("minecraft:bat", "Morcego", EntityType.BAT, () -> Items.BAT_SPAWN_EGG),
				new TargetMobOption("minecraft:bee", "Abelha", EntityType.BEE, () -> Items.BEE_SPAWN_EGG),
				new TargetMobOption("minecraft:fox", "Raposa", EntityType.FOX, () -> Items.FOX_SPAWN_EGG),
				new TargetMobOption("minecraft:goat", "Cabra", EntityType.GOAT, () -> Items.GOAT_SPAWN_EGG),
				new TargetMobOption("minecraft:llama", "Lhama", EntityType.LLAMA, () -> Items.LLAMA_SPAWN_EGG),
				new TargetMobOption("minecraft:ocelot", "Jaguatirica", EntityType.OCELOT, () -> Items.OCELOT_SPAWN_EGG),
				new TargetMobOption("minecraft:panda", "Panda", EntityType.PANDA, () -> Items.PANDA_SPAWN_EGG),
				new TargetMobOption("minecraft:polar_bear", "Urso Polar", EntityType.POLAR_BEAR, () -> Items.POLAR_BEAR_SPAWN_EGG),
				new TargetMobOption("minecraft:axolotl", "Axolote", EntityType.AXOLOTL, () -> Items.AXOLOTL_SPAWN_EGG),
				new TargetMobOption("minecraft:cod", "Bacalhau", EntityType.COD, () -> Items.COD_SPAWN_EGG),
				new TargetMobOption("minecraft:dolphin", "Golfinho", EntityType.DOLPHIN, () -> Items.DOLPHIN_SPAWN_EGG),
				new TargetMobOption("minecraft:frog", "Sapo", EntityType.FROG, () -> Items.FROG_SPAWN_EGG),
				new TargetMobOption("minecraft:glow_squid", "Lula Brilhante", EntityType.GLOW_SQUID, () -> Items.GLOW_SQUID_SPAWN_EGG),
				new TargetMobOption("minecraft:nautilus", "Nautilus", EntityType.NAUTILUS, () -> Items.NAUTILUS_SPAWN_EGG),
				new TargetMobOption("minecraft:pufferfish", "Baiacu", EntityType.PUFFERFISH, () -> Items.PUFFERFISH_SPAWN_EGG),
				new TargetMobOption("minecraft:salmon", "Salmão", EntityType.SALMON, () -> Items.SALMON_SPAWN_EGG),
				new TargetMobOption("minecraft:squid", "Lula", EntityType.SQUID, () -> Items.SQUID_SPAWN_EGG),
				new TargetMobOption("minecraft:tadpole", "Girino", EntityType.TADPOLE, () -> Items.TADPOLE_SPAWN_EGG),
				new TargetMobOption("minecraft:tropical_fish", "Peixe Tropical", EntityType.TROPICAL_FISH, () -> Items.TROPICAL_FISH_SPAWN_EGG),
				new TargetMobOption("minecraft:turtle", "Tartaruga", EntityType.TURTLE, () -> Items.TURTLE_SPAWN_EGG),
				new TargetMobOption("minecraft:allay", "Allay", EntityType.ALLAY, () -> Items.ALLAY_SPAWN_EGG),
				new TargetMobOption("minecraft:mooshroom", "Mooshroom", EntityType.MOOSHROOM, () -> Items.MOOSHROOM_SPAWN_EGG),
				new TargetMobOption("minecraft:sniffer", "Farejador", EntityType.SNIFFER, () -> Items.SNIFFER_SPAWN_EGG),
				new TargetMobOption("minecraft:copper_golem", "Golem de Cobre", EntityType.COPPER_GOLEM, () -> Items.COPPER_GOLEM_SPAWN_EGG),
				new TargetMobOption("minecraft:iron_golem", "Golem de Ferro", EntityType.IRON_GOLEM, () -> Items.IRON_GOLEM_SPAWN_EGG),
				new TargetMobOption("minecraft:snow_golem", "Golem de Neve", EntityType.SNOW_GOLEM, () -> Items.SNOW_GOLEM_SPAWN_EGG),
				new TargetMobOption("minecraft:trader_llama", "Lhama do Mercador", EntityType.TRADER_LLAMA, () -> Items.TRADER_LLAMA_SPAWN_EGG),
				new TargetMobOption("minecraft:villager", "Aldeão", EntityType.VILLAGER, () -> Items.VILLAGER_SPAWN_EGG),
				new TargetMobOption("minecraft:wandering_trader", "Mercador Ambulante", EntityType.WANDERING_TRADER, () -> Items.WANDERING_TRADER_SPAWN_EGG),
				new TargetMobOption("minecraft:bogged", "Atolado", EntityType.BOGGED, () -> Items.BOGGED_SPAWN_EGG),
				new TargetMobOption("minecraft:camel_husk", "Casca de Camelo", EntityType.CAMEL_HUSK, () -> Items.CAMEL_HUSK_SPAWN_EGG),
				new TargetMobOption("minecraft:drowned", "Afogado", EntityType.DROWNED, () -> Items.DROWNED_SPAWN_EGG),
				new TargetMobOption("minecraft:husk", "Zumbi-Múmia", EntityType.HUSK, () -> Items.HUSK_SPAWN_EGG),
				new TargetMobOption("minecraft:parched", "Ressecado", EntityType.PARCHED, () -> Items.PARCHED_SPAWN_EGG),
				new TargetMobOption("minecraft:skeleton", "Esqueleto", EntityType.SKELETON, () -> Items.SKELETON_SPAWN_EGG),
				new TargetMobOption("minecraft:skeleton_horse", "Cavalo-Esqueleto", EntityType.SKELETON_HORSE, () -> Items.SKELETON_HORSE_SPAWN_EGG),
				new TargetMobOption("minecraft:stray", "Errante", EntityType.STRAY, () -> Items.STRAY_SPAWN_EGG),
				new TargetMobOption("minecraft:wither", "Wither", EntityType.WITHER, () -> Items.WITHER_SPAWN_EGG),
				new TargetMobOption("minecraft:wither_skeleton", "Esqueleto Wither", EntityType.WITHER_SKELETON, () -> Items.WITHER_SKELETON_SPAWN_EGG),
				new TargetMobOption("minecraft:zombie", "Zumbi", EntityType.ZOMBIE, () -> Items.ZOMBIE_SPAWN_EGG),
				new TargetMobOption("minecraft:zombie_horse", "Cavalo-Zumbi", EntityType.ZOMBIE_HORSE, () -> Items.ZOMBIE_HORSE_SPAWN_EGG),
				new TargetMobOption("minecraft:zombie_nautilus", "Nautilus Zumbi", EntityType.ZOMBIE_NAUTILUS, () -> Items.ZOMBIE_NAUTILUS_SPAWN_EGG),
				new TargetMobOption("minecraft:zombie_villager", "Aldeão Zumbi", EntityType.ZOMBIE_VILLAGER, () -> Items.ZOMBIE_VILLAGER_SPAWN_EGG),
				new TargetMobOption("minecraft:cave_spider", "Aranha das Cavernas", EntityType.CAVE_SPIDER, () -> Items.CAVE_SPIDER_SPAWN_EGG),
				new TargetMobOption("minecraft:spider", "Aranha", EntityType.SPIDER, () -> Items.SPIDER_SPAWN_EGG),
				new TargetMobOption("minecraft:breeze", "Breeze", EntityType.BREEZE, () -> Items.BREEZE_SPAWN_EGG),
				new TargetMobOption("minecraft:creaking", "Rangente", EntityType.CREAKING, () -> Items.CREAKING_SPAWN_EGG),
				new TargetMobOption("minecraft:creeper", "Creeper", EntityType.CREEPER, () -> Items.CREEPER_SPAWN_EGG),
				new TargetMobOption("minecraft:elder_guardian", "Guardião Ancião", EntityType.ELDER_GUARDIAN, () -> Items.ELDER_GUARDIAN_SPAWN_EGG),
				new TargetMobOption("minecraft:guardian", "Guardião", EntityType.GUARDIAN, () -> Items.GUARDIAN_SPAWN_EGG),
				new TargetMobOption("minecraft:phantom", "Phantom", EntityType.PHANTOM, () -> Items.PHANTOM_SPAWN_EGG),
				new TargetMobOption("minecraft:silverfish", "Traça", EntityType.SILVERFISH, () -> Items.SILVERFISH_SPAWN_EGG),
				new TargetMobOption("minecraft:slime", "Slime", EntityType.SLIME, () -> Items.SLIME_SPAWN_EGG),
				new TargetMobOption("minecraft:warden", "Warden", EntityType.WARDEN, () -> Items.WARDEN_SPAWN_EGG),
				new TargetMobOption("minecraft:witch", "Bruxa", EntityType.WITCH, () -> Items.WITCH_SPAWN_EGG),
				new TargetMobOption("minecraft:evoker", "Invocador", EntityType.EVOKER, () -> Items.EVOKER_SPAWN_EGG),
				new TargetMobOption("minecraft:pillager", "Saqueador", EntityType.PILLAGER, () -> Items.PILLAGER_SPAWN_EGG),
				new TargetMobOption("minecraft:ravager", "Devastador", EntityType.RAVAGER, () -> Items.RAVAGER_SPAWN_EGG),
				new TargetMobOption("minecraft:vindicator", "Vingador", EntityType.VINDICATOR, () -> Items.VINDICATOR_SPAWN_EGG),
				new TargetMobOption("minecraft:vex", "Vex", EntityType.VEX, () -> Items.VEX_SPAWN_EGG),
				new TargetMobOption("minecraft:blaze", "Blaze", EntityType.BLAZE, () -> Items.BLAZE_SPAWN_EGG),
				new TargetMobOption("minecraft:ghast", "Ghast", EntityType.GHAST, () -> Items.GHAST_SPAWN_EGG),
				new TargetMobOption("minecraft:happy_ghast", "Ghast Feliz", EntityType.HAPPY_GHAST, () -> Items.HAPPY_GHAST_SPAWN_EGG),
				new TargetMobOption("minecraft:hoglin", "Hoglin", EntityType.HOGLIN, () -> Items.HOGLIN_SPAWN_EGG),
				new TargetMobOption("minecraft:magma_cube", "Cubo de Magma", EntityType.MAGMA_CUBE, () -> Items.MAGMA_CUBE_SPAWN_EGG),
				new TargetMobOption("minecraft:piglin", "Piglin", EntityType.PIGLIN, () -> Items.PIGLIN_SPAWN_EGG),
				new TargetMobOption("minecraft:piglin_brute", "Piglin Bruto", EntityType.PIGLIN_BRUTE, () -> Items.PIGLIN_BRUTE_SPAWN_EGG),
				new TargetMobOption("minecraft:strider", "Lavágante", EntityType.STRIDER, () -> Items.STRIDER_SPAWN_EGG),
				new TargetMobOption("minecraft:zoglin", "Zoglin", EntityType.ZOGLIN, () -> Items.ZOGLIN_SPAWN_EGG),
				new TargetMobOption("minecraft:zombified_piglin", "Piglin Zumbificado", EntityType.ZOMBIFIED_PIGLIN, () -> Items.ZOMBIFIED_PIGLIN_SPAWN_EGG),
				new TargetMobOption("minecraft:ender_dragon", "Dragão do Ender", EntityType.ENDER_DRAGON, () -> Items.ENDER_DRAGON_SPAWN_EGG),
				new TargetMobOption("minecraft:enderman", "Enderman", EntityType.ENDERMAN, () -> Items.ENDERMAN_SPAWN_EGG),
				new TargetMobOption("minecraft:endermite", "Endermite", EntityType.ENDERMITE, () -> Items.ENDERMITE_SPAWN_EGG),
				new TargetMobOption("minecraft:shulker", "Shulker", EntityType.SHULKER, () -> Items.SHULKER_SPAWN_EGG)
			));
		}

		return options;
	}

	public ItemStack icon() {
		if (icon == null || icon.isEmpty()) {
			try {
				icon = new ItemStack(iconItemSupplier.get());
			} catch (RuntimeException exception) {
				return null;
			}
		}

		return icon;
	}

	public static TargetMobOption defaultOption() {
		return options().get(0);
	}

	public static Optional<TargetMobOption> byEntityId(String entityId) {
		return options().stream()
			.filter(option -> option.entityId.equals(entityId))
			.findFirst();
	}
}
