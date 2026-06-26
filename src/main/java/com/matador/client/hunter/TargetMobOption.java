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
				new TargetMobOption(
					"minecraft:rabbit",
					"Coelho",
					EntityType.RABBIT,
					() -> Items.RABBIT_SPAWN_EGG
				),
				new TargetMobOption(
					"minecraft:chicken",
					"Galinha",
					EntityType.CHICKEN,
					() -> Items.CHICKEN_SPAWN_EGG
				),
				new TargetMobOption(
					"minecraft:cow",
					"Vaca",
					EntityType.COW,
					() -> Items.COW_SPAWN_EGG
				),
				new TargetMobOption(
					"minecraft:sheep",
					"Ovelha",
					EntityType.SHEEP,
					() -> Items.SHEEP_SPAWN_EGG
				),
				new TargetMobOption(
					"minecraft:pig",
					"Porco",
					EntityType.PIG,
					() -> Items.PIG_SPAWN_EGG
				)
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
