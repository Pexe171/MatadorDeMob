package com.matador.client.gui;

import com.matador.client.hunter.MobHunterController;
import com.matador.client.hunter.TargetMobOption;
import com.matador.config.MobHunterConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MobHunterScreen extends Screen {
	private static final int PANEL_BG = 0xE6101420;
	private static final int PANEL_BORDER = 0xFF3D4E68;
	private static final int PANEL_SOFT = 0xCC182132;
	private static final int CARD_BG = 0xDD1C2635;
	private static final int CARD_HOVER = 0xEE27364A;
	private static final int CARD_SELECTED = 0xEE1F433D;
	private static final int CARD_BORDER = 0xFF46566E;
	private static final int ACCENT = 0xFF51D6C1;
	private static final int WARNING = 0xFFFFC857;
	private static final int TEXT = 0xFFEAF2FF;
	private static final int MUTED = 0xFF9DAEC5;
	private static final int DANGER = 0xFFFF6B6B;
	private static final int GOOD = 0xFF6EE787;

	private final MobHunterController controller;
	private final List<MobCard> mobCards = new ArrayList<>();
	private final List<ActionButton> actionButtons = new ArrayList<>();

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int settingsX;
	private int settingsY;
	private int valueX;
	private int infoX;
	private int infoY;

	public MobHunterScreen(MobHunterController controller) {
		super(Component.literal("Mob Hunter"));
		this.controller = controller;
	}

	@Override
	protected void init() {
		rebuildLayout();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x99000000);
		rebuildLayout();
		controller.refreshTarget(Minecraft.getInstance());

		drawPanel(context);
		drawHeader(context);
		drawMobCards(context, mouseX, mouseY);
		drawSettings(context);
		drawInfo(context);
		drawActionButtons(context, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			double mouseX = click.x();
			double mouseY = click.y();
			for (MobCard card : mobCards) {
				if (card.contains(mouseX, mouseY)) {
					controller.setTargetMob(card.option, Minecraft.getInstance());
					return true;
				}
			}

			for (ActionButton actionButton : actionButtons) {
				if (actionButton.contains(mouseX, mouseY)) {
					runAction(actionButton.action);
					return true;
				}
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void rebuildLayout() {
		panelWidth = Math.min(620, Math.max(320, width - 24));
		panelHeight = Math.min(382, Math.max(300, height - 24));
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;

		boolean wide = panelWidth >= 560;
		int sidePadding = 18;
		int top = panelY + 62;
		int gridWidth = wide ? panelWidth - 270 : panelWidth - sidePadding * 2;
		int columns = gridWidth >= 300 ? 2 : 1;
		int cardGap = 8;
		int cardWidth = (gridWidth - cardGap * (columns - 1)) / columns;
		int cardHeight = 42;
		List<TargetMobOption> options = TargetMobOption.options();

		mobCards.clear();
		for (int index = 0; index < options.size(); index++) {
			int column = index % columns;
			int row = index / columns;
			int x = panelX + sidePadding + column * (cardWidth + cardGap);
			int y = top + row * (cardHeight + cardGap);
			mobCards.add(new MobCard(options.get(index), x, y, cardWidth, cardHeight));
		}

		int rows = (int) Math.ceil(options.size() / (double) columns);
		settingsX = panelX + sidePadding;
		settingsY = top + rows * (cardHeight + cardGap) + 12;
		valueX = settingsX + 126;

		if (wide) {
			infoX = panelX + panelWidth - 232;
			infoY = top;
		} else {
			infoX = settingsX;
			infoY = settingsY + 112;
		}

		rebuildActionButtons();
	}

	private void drawPanel(GuiGraphicsExtractor context) {
		context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
		drawBorder(context, panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);
		context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 42, 0xB81B2737);
	}

	private void drawHeader(GuiGraphicsExtractor context) {
		drawText(context, "Mob Hunter", panelX + 18, panelY + 18, TEXT);

		boolean enabled = controller.getConfig().enabled;
		String status = enabled ? "Status: ON" : "Status: OFF";
		int statusColor = enabled ? GOOD : DANGER;
		drawText(context, status, panelX + 18, panelY + 34, statusColor);
	}

	private void drawMobCards(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		drawText(context, "Mobs disponiveis", panelX + 18, panelY + 50, MUTED);

		for (MobCard card : mobCards) {
			boolean selected = card.option.entityId.equals(controller.getConfig().targetMobId);
			boolean hovered = card.contains(mouseX, mouseY);
			int bg = selected ? CARD_SELECTED : hovered ? CARD_HOVER : CARD_BG;
			int border = selected ? ACCENT : hovered ? WARNING : CARD_BORDER;

			context.fill(card.x, card.y, card.x + card.width, card.y + card.height, bg);
			drawBorder(context, card.x, card.y, card.width, card.height, border);

			if (selected) {
				context.fill(card.x + 2, card.y + 2, card.x + 5, card.y + card.height - 2, ACCENT);
			}

			int iconX = card.x + 12;
			int iconY = card.y + 13;
			drawMobIcon(context, card.option, iconX, iconY);

			int nameColor = selected ? 0xFFFFFFFF : TEXT;
			drawText(context, card.option.displayName, card.x + 36, card.y + 12, nameColor);
		}
	}

	private void drawSettings(GuiGraphicsExtractor context) {
		MobHunterConfig config = controller.getConfig();

		context.fill(settingsX - 2, settingsY, settingsX + Math.min(310, panelWidth - 36), settingsY + 104, PANEL_SOFT);
		drawBorder(context, settingsX - 2, settingsY, Math.min(312, panelWidth - 36), 104, 0xFF29384C);
		drawText(context, "Configuracoes", settingsX + 8, settingsY + 8, TEXT);

		drawSettingRow(context, "Raio de busca", formatOneDecimal(config.searchRadius), settingsY + 32);
		drawSettingRow(context, "Distancia ataque", formatOneDecimal(config.attackDistance), settingsY + 58);
		drawSettingRow(context, "Delay ataque", config.attackDelayTicks + " ticks", settingsY + 84);
	}

	private void drawInfo(GuiGraphicsExtractor context) {
		Minecraft client = Minecraft.getInstance();
		MobHunterConfig config = controller.getConfig();
		TargetMobOption selected = controller.getSelectedOption();
		double distance = controller.getCurrentTargetDistance(client);
		String distanceText = controller.getCurrentTargetName().equals("Nenhum") ? "-" : formatOneDecimal(distance) + " blocos";

		context.fill(infoX - 2, infoY, infoX + 214, infoY + 184, PANEL_SOFT);
		drawBorder(context, infoX - 2, infoY, 216, 184, 0xFF29384C);

		drawText(context, "Mob selecionado", infoX + 8, infoY + 8, TEXT);
		drawMobIcon(context, selected, infoX + 10, infoY + 27);
		drawText(context, selected.displayName, infoX + 34, infoY + 31, ACCENT);

		drawText(context, "Informacoes", infoX + 8, infoY + 58, TEXT);
		drawText(context, "Alvo atual: " + controller.getCurrentTargetName(), infoX + 8, infoY + 76, MUTED);
		drawText(context, "Distancia: " + distanceText, infoX + 8, infoY + 92, MUTED);
		drawText(context, "Encontrados: " + controller.getFoundMobCount(), infoX + 8, infoY + 108, MUTED);
		drawText(context, "Um clique: " + controller.getOneClickStatus(), infoX + 8, infoY + 124, config.oneClickMode ? WARNING : 0xFF7F8EA5);
		drawText(context, "Log: " + (config.captureMobLog ? "ON" : "OFF"), infoX + 8, infoY + 140, config.captureMobLog ? GOOD : 0xFF7F8EA5);
		drawText(context, "Prioridade: " + (config.prioritizeRareMobs ? "ON" : "OFF"), infoX + 8, infoY + 156, config.prioritizeRareMobs ? ACCENT : 0xFF7F8EA5);
	}

	private void drawSettingRow(GuiGraphicsExtractor context, String label, String value, int y) {
		drawText(context, label, settingsX + 8, y, MUTED);
		drawText(context, value, valueX, y, ACCENT);
	}

	private void drawMobIcon(GuiGraphicsExtractor context, TargetMobOption option, int x, int y) {
		try {
			ItemStack icon = option.icon();
			if (icon != null && !icon.isEmpty()) {
				context.item(icon, x, y);
				context.itemDecorations(font, icon, x, y);
			}
		} catch (RuntimeException exception) {
			// Item components can be unavailable during early client frames on this runtime.
		}
	}

	private void rebuildActionButtons() {
		actionButtons.clear();
		actionButtons.add(new ActionButton(
			Action.TOGGLE,
			controller.getConfig().enabled ? "Desativar" : "Ativar",
			panelX + panelWidth - 128,
			panelY + 17,
			108,
			20
		));

		int downX = valueX + 72;
		int upX = valueX + 98;
		actionButtons.add(new ActionButton(Action.RADIUS_DOWN, "-", downX, settingsY + 25, 22, 20));
		actionButtons.add(new ActionButton(Action.RADIUS_UP, "+", upX, settingsY + 25, 22, 20));
		actionButtons.add(new ActionButton(Action.DISTANCE_DOWN, "-", downX, settingsY + 51, 22, 20));
		actionButtons.add(new ActionButton(Action.DISTANCE_UP, "+", upX, settingsY + 51, 22, 20));
		actionButtons.add(new ActionButton(Action.DELAY_DOWN, "-", downX, settingsY + 77, 22, 20));
		actionButtons.add(new ActionButton(Action.DELAY_UP, "+", upX, settingsY + 77, 22, 20));
		actionButtons.add(new ActionButton(Action.ONE_CLICK, "Um clique", infoX + 108, infoY + 120, 92, 18));
		actionButtons.add(new ActionButton(Action.LOG, "Log", infoX + 108, infoY + 138, 44, 18));
		actionButtons.add(new ActionButton(Action.PRIORITY, "Prior.", infoX + 156, infoY + 138, 44, 18));
	}

	private void drawActionButtons(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		for (ActionButton button : actionButtons) {
			boolean hovered = button.contains(mouseX, mouseY);
			int bg = hovered ? 0xEE314158 : 0xDD243247;
			int border = hovered ? WARNING : 0xFF596A82;
			context.fill(button.x, button.y, button.x + button.width, button.y + button.height, bg);
			drawBorder(context, button.x, button.y, button.width, button.height, border);

			int textWidth = font.width(button.label);
			int textX = button.x + (button.width - textWidth) / 2;
			int textY = button.y + 6;
			drawText(context, button.label, textX, textY, TEXT);
		}
	}

	private void runAction(Action action) {
		MobHunterConfig config = controller.getConfig();

		switch (action) {
			case TOGGLE -> controller.toggleEnabled(Minecraft.getInstance());
			case RADIUS_DOWN -> config.searchRadius -= 1.0D;
			case RADIUS_UP -> config.searchRadius += 1.0D;
			case DISTANCE_DOWN -> config.attackDistance -= 0.5D;
			case DISTANCE_UP -> config.attackDistance += 0.5D;
			case DELAY_DOWN -> config.attackDelayTicks -= 1;
			case DELAY_UP -> config.attackDelayTicks += 1;
			case ONE_CLICK -> controller.toggleOneClickMode(Minecraft.getInstance());
			case LOG -> controller.toggleMobLog();
			case PRIORITY -> controller.toggleRarePriority();
		}

		config.save();
		controller.refreshTarget(Minecraft.getInstance());
	}

	private void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}

	private void drawText(GuiGraphicsExtractor context, String text, int x, int y, int color) {
		context.text(font, text, x + 1, y + 1, 0x99000000);
		context.text(font, text, x, y, color);
	}

	private String formatOneDecimal(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private enum Action {
		TOGGLE,
		RADIUS_DOWN,
		RADIUS_UP,
		DISTANCE_DOWN,
		DISTANCE_UP,
		DELAY_DOWN,
		DELAY_UP,
		ONE_CLICK,
		LOG,
		PRIORITY
	}

	private static class ActionButton {
		private final Action action;
		private final String label;
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		private ActionButton(Action action, String label, int x, int y, int width, int height) {
			this.action = action;
			this.label = label;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	private static class MobCard {
		private final TargetMobOption option;
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		private MobCard(TargetMobOption option, int x, int y, int width, int height) {
			this.option = option;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}
}
