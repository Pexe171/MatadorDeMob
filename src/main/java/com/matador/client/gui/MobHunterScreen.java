package com.matador.client.gui;

import com.matador.client.hunter.MobHunterController;
import com.matador.client.hunter.TargetMobOption;
import com.matador.config.MobHunterConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MobHunterScreen extends Screen {
	private static final int PANEL_BG = 0xFA1E1E2E;
	private static final int PANEL_HEADER = 0xFA181825;
	private static final int PANEL_BORDER = 0xFF313244;
	private static final int PANEL_SOFT = 0xAA181825;
	private static final int CARD_BG = 0xDD313244;
	private static final int CARD_HOVER = 0xFF45475A;
	private static final int CARD_BORDER = 0xFF45475A;
	private static final int ACCENT = 0xFF89B4FA;
	private static final int WARNING = 0xFFF9E2AF;
	private static final int TEXT = 0xFFCDD6F4;
	private static final int MUTED = 0xFFA6ADC8;
	private static final int DANGER = 0xFFF38BA8;
	private static final int GOOD = 0xFFA6E3A1;

	private final MobHunterController controller;
	private final List<MobCard> mobCards = new ArrayList<>();
	private final List<ActionButton> actionButtons = new ArrayList<>();
	private final List<TargetMobOption> filteredOptions = new ArrayList<>();

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int searchX;
	private int searchY;
	private int searchWidth;
	private int listX;
	private int listY;
	private int listWidth;
	private int cardWidth;
	private int cardHeight = 42;
	private int columns;
	private int rows;
	private int pageSize;
	private int settingsX;
	private int settingsY;
	private int valueX;
	private int infoX;
	private int infoY;
	private int page = 0;
	private int keyboardIndex = 0;
	private boolean searchFocused;
	private boolean draggingPanel;
	private double dragStartX;
	private double dragStartY;
	private int dragStartOffsetX;
	private int dragStartOffsetY;
	private String searchQuery = "";
	private long initTime;

	public MobHunterScreen(MobHunterController controller) {
		super(Component.literal("Caçador de Mobs"));
		this.controller = controller;
	}

	@Override
	protected void init() {
		initTime = System.currentTimeMillis();
		rebuildLayout();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x88000000);
		rebuildLayout();
		controller.refreshTarget(Minecraft.getInstance());

		float openAnim = Math.min(1f, (System.currentTimeMillis() - initTime) / 220f);
		float easeOut = 1f - (float) Math.pow(1f - openAnim, 4);
		int animOffsetY = (int) ((1f - easeOut) * 28);
		int adjustedMouseY = mouseY - animOffsetY;

		updateHovers(mouseX, adjustedMouseY);
		drawPanel(context, animOffsetY);
		drawHeader(context, animOffsetY);
		drawSearch(context, animOffsetY);
		drawMobCards(context, animOffsetY);
		drawSettings(context, animOffsetY);
		drawInfo(context, animOffsetY);
		drawActionButtons(context, animOffsetY);
	}

	private void rebuildLayout() {
		panelWidth = Math.min(760, Math.max(360, width - 24));
		panelHeight = Math.min(460, Math.max(320, height - 24));

		MobHunterConfig config = controller.getConfig();
		int centeredX = (width - panelWidth) / 2;
		int centeredY = (height - panelHeight) / 2;
		config.panelOffsetX = clamp(config.panelOffsetX, -centeredX + 8, width - centeredX - panelWidth - 8);
		config.panelOffsetY = clamp(config.panelOffsetY, -centeredY + 8, height - centeredY - panelHeight - 8);
		panelX = centeredX + config.panelOffsetX;
		panelY = centeredY + config.panelOffsetY;

		boolean wide = panelWidth >= 650;
		searchX = panelX + 20;
		searchY = panelY + 58;
		listX = panelX + 20;
		listY = panelY + 94;
		listWidth = wide ? panelWidth - 300 : panelWidth - 40;
		searchWidth = listWidth;
		columns = listWidth >= 520 ? 3 : listWidth >= 340 ? 2 : 1;
		int gap = 8;
		cardWidth = (listWidth - gap * (columns - 1)) / columns;
		int listBottom = wide ? panelY + panelHeight - 56 : panelY + panelHeight - 166;
		rows = Math.max(2, (listBottom - listY) / (cardHeight + gap));
		pageSize = Math.max(1, rows * columns);

		rebuildFilteredOptions();
		page = clamp(page, 0, getMaxPage());
		keyboardIndex = clamp(keyboardIndex, 0, Math.max(0, filteredOptions.size() - 1));

		int neededCards = Math.min(pageSize, Math.max(0, filteredOptions.size() - page * pageSize));
		while (mobCards.size() < neededCards) {
			mobCards.add(new MobCard(null, 0, 0, 0, 0));
		}
		while (mobCards.size() > neededCards) {
			mobCards.remove(mobCards.size() - 1);
		}

		for (int index = 0; index < neededCards; index++) {
			int optionIndex = page * pageSize + index;
			int column = index % columns;
			int row = index / columns;
			MobCard card = mobCards.get(index);
			card.option = filteredOptions.get(optionIndex);
			card.optionIndex = optionIndex;
			card.x = listX + column * (cardWidth + gap);
			card.y = listY + row * (cardHeight + gap);
			card.width = cardWidth;
			card.height = cardHeight;
		}

		if (wide) {
			settingsX = panelX + panelWidth - 260;
			settingsY = listY;
			infoX = settingsX;
			infoY = settingsY + 126;
		} else {
			settingsX = panelX + 20;
			settingsY = panelY + panelHeight - 150;
			infoX = panelX + 20;
			infoY = panelY + panelHeight - 150;
		}
		valueX = settingsX + 128;

		rebuildActionButtons();
	}

	private void rebuildFilteredOptions() {
		filteredOptions.clear();
		String normalizedQuery = normalize(searchQuery);
		for (TargetMobOption option : TargetMobOption.options()) {
			if (normalizedQuery.isBlank() || normalize(option.displayName).contains(normalizedQuery) || normalize(option.entityId).contains(normalizedQuery)) {
				filteredOptions.add(option);
			}
		}
	}

	private void updateHovers(int mouseX, int mouseY) {
		for (MobCard card : mobCards) {
			card.updateHover(card.contains(mouseX, mouseY));
		}
		for (ActionButton button : actionButtons) {
			button.updateHover(button.contains(mouseX, mouseY));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(click, doubled);
		}

		double mouseX = click.x();
		double mouseY = click.y() - getAnimOffsetY();
		searchFocused = contains(mouseX, mouseY, searchX, searchY, searchWidth, 24);

		for (ActionButton actionButton : actionButtons) {
			if (actionButton.contains(mouseX, mouseY)) {
				runAction(actionButton.action);
				return true;
			}
		}

		for (MobCard card : mobCards) {
			if (card.contains(mouseX, mouseY)) {
				keyboardIndex = card.optionIndex;
				controller.setTargetMob(card.option, Minecraft.getInstance());
				return true;
			}
		}

		if (contains(mouseX, mouseY, panelX, panelY, panelWidth, 50)) {
			draggingPanel = true;
			dragStartX = click.x();
			dragStartY = click.y();
			dragStartOffsetX = controller.getConfig().panelOffsetX;
			dragStartOffsetY = controller.getConfig().panelOffsetY;
			return true;
		}

		return contains(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight) || super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (draggingPanel) {
			draggingPanel = false;
			controller.getConfig().save();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
		if (!draggingPanel) {
			return super.mouseDragged(click, dragX, dragY);
		}

		MobHunterConfig config = controller.getConfig();
		int centeredX = (width - panelWidth) / 2;
		int centeredY = (height - panelHeight) / 2;
		config.panelOffsetX = clamp(dragStartOffsetX + (int) Math.round(click.x() - dragStartX), -centeredX + 8, width - centeredX - panelWidth - 8);
		config.panelOffsetY = clamp(dragStartOffsetY + (int) Math.round(click.y() - dragStartY), -centeredY + 8, height - centeredY - panelHeight - 8);
		rebuildLayout();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (contains(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
			if (scrollY < 0) {
				changePage(1);
			} else if (scrollY > 0) {
				changePage(-1);
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (searchFocused) {
			if (key == InputConstants.KEY_BACKSPACE && !searchQuery.isEmpty()) {
				searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
				resetSearchPage();
				return true;
			}
			if (key == InputConstants.KEY_DELETE && !searchQuery.isEmpty()) {
				searchQuery = "";
				resetSearchPage();
				return true;
			}
		}

		if (key == InputConstants.KEY_DOWN) {
			moveSelection(1);
			return true;
		}
		if (key == InputConstants.KEY_UP) {
			moveSelection(-1);
			return true;
		}
		if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_PAGEDOWN) {
			changePage(1);
			return true;
		}
		if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_PAGEUP) {
			changePage(-1);
			return true;
		}
		if (key == InputConstants.KEY_HOME) {
			page = 0;
			keyboardIndex = filteredOptions.isEmpty() ? 0 : page * pageSize;
			return true;
		}
		if (key == InputConstants.KEY_END) {
			page = getMaxPage();
			keyboardIndex = filteredOptions.isEmpty() ? 0 : filteredOptions.size() - 1;
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			if (!filteredOptions.isEmpty()) {
				keyboardIndex = clamp(keyboardIndex, 0, filteredOptions.size() - 1);
				controller.setTargetMob(filteredOptions.get(keyboardIndex), Minecraft.getInstance());
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!searchFocused || !event.isAllowedChatCharacter()) {
			return super.charTyped(event);
		}
		String value = event.codepointAsString();
		if (value != null && !value.isBlank() && searchQuery.length() < 48) {
			searchQuery += value;
			resetSearchPage();
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawPanel(GuiGraphicsExtractor context, int offsetY) {
		int y = panelY + offsetY;
		drawBorder(context, panelX - 1, y - 1, panelWidth + 2, panelHeight + 2, 0x44000000);
		context.fill(panelX, y, panelX + panelWidth, y + panelHeight, PANEL_BG);
		drawBorder(context, panelX, y, panelWidth, panelHeight, PANEL_BORDER);
		context.fill(panelX + 1, y + 1, panelX + panelWidth - 1, y + 50, PANEL_HEADER);
		context.fill(panelX + 1, y + 50, panelX + panelWidth - 1, y + 51, PANEL_BORDER);
	}

	private void drawHeader(GuiGraphicsExtractor context, int offsetY) {
		int y = panelY + offsetY;
		drawText(context, "Caçador de Mobs", panelX + 20, y + 12, TEXT, true);
		drawText(context, "Arraste pelo topo para mover", panelX + 20, y + 31, MUTED, false);

		boolean enabled = controller.getConfig().enabled;
		String status = enabled ? "Sistema ativo" : "Sistema inativo";
		int statusColor = enabled ? GOOD : DANGER;
		context.fill(panelX + panelWidth - 254, y + 31, panelX + panelWidth - 249, y + 36, statusColor);
		drawText(context, status, panelX + panelWidth - 242, y + 30, statusColor, false);
	}

	private void drawSearch(GuiGraphicsExtractor context, int offsetY) {
		int y = searchY + offsetY;
		int border = searchFocused ? ACCENT : PANEL_BORDER;
		context.fill(searchX, y, searchX + searchWidth, y + 24, 0xCC11111B);
		drawBorder(context, searchX, y, searchWidth, 24, border);
		String text = searchQuery.isEmpty() ? "Buscar mob pelo nome..." : searchQuery;
		drawText(context, text, searchX + 8, y + 8, searchQuery.isEmpty() ? MUTED : TEXT, false);
		if (searchFocused && (System.currentTimeMillis() / 450) % 2 == 0) {
			int cursorX = searchX + 8 + font.width(searchQuery);
			context.fill(cursorX + 1, y + 6, cursorX + 2, y + 18, ACCENT);
		}

		String count = filteredOptions.size() + " mobs";
		drawText(context, count, searchX + searchWidth - font.width(count) - 8, y + 8, MUTED, false);
	}

	private void drawMobCards(GuiGraphicsExtractor context, int offsetY) {
		drawText(context, "Escolha o mob alvo", listX, listY + offsetY - 14, MUTED, false);

		if (filteredOptions.isEmpty()) {
			drawText(context, "Nenhum mob encontrado", listX + 8, listY + offsetY + 12, WARNING, false);
			return;
		}

		for (MobCard card : mobCards) {
			boolean selected = card.option.entityId.equals(controller.getConfig().targetMobId);
			boolean keyboardSelected = card.optionIndex == keyboardIndex;
			int cardY = card.y + offsetY;
			int baseBg = selected ? 0xFF3B4252 : CARD_BG;
			int currentBg = lerpColor(baseBg, CARD_HOVER, card.hoverAnim);
			int currentBorder = selected ? ACCENT : keyboardSelected ? WARNING : lerpColor(CARD_BORDER, WARNING, card.hoverAnim);

			context.fill(card.x, cardY, card.x + card.width, cardY + card.height, currentBg);
			drawBorder(context, card.x, cardY, card.width, card.height, currentBorder);
			if (selected) {
				context.fill(card.x + 1, cardY + 1, card.x + 4, cardY + card.height - 1, ACCENT);
			}

			drawMobIcon(context, card.option, card.x + 12, cardY + 12);
			int nameColor = selected ? 0xFFFFFFFF : lerpColor(TEXT, 0xFFFFFFFF, card.hoverAnim);
			drawText(context, fitText(card.option.displayName, card.width - 46), card.x + 38, cardY + 9, nameColor, selected);
			drawText(context, fitText(shortEntityId(card.option.entityId), card.width - 46), card.x + 38, cardY + 24, MUTED, false);
		}
	}

	private void drawSettings(GuiGraphicsExtractor context, int offsetY) {
		MobHunterConfig config = controller.getConfig();
		int y = settingsY + offsetY;
		int width = Math.min(240, panelX + panelWidth - settingsX - 20);
		context.fill(settingsX - 4, y, settingsX - 4 + width, y + 110, PANEL_SOFT);
		drawBorder(context, settingsX - 4, y, width, 110, PANEL_BORDER);

		drawText(context, "Ajustes de combate", settingsX + 10, y + 10, TEXT, false);
		drawSettingRow(context, "Raio de busca", formatOneDecimal(config.searchRadius) + "m", y + 34);
		drawSettingRow(context, "Alcance", formatOneDecimal(config.attackDistance) + "m", y + 60);
		drawSettingRow(context, "Atraso", config.attackDelayTicks + " tks", y + 86);
	}

	private void drawInfo(GuiGraphicsExtractor context, int offsetY) {
		if (panelWidth < 650) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		MobHunterConfig config = controller.getConfig();
		TargetMobOption selected = controller.getSelectedOption();
		double distance = controller.getCurrentTargetDistance(client);
		String distanceText = controller.getCurrentTargetName().equals("Nenhum") ? "-" : formatOneDecimal(distance) + "m";
		int y = infoY + offsetY;

		context.fill(infoX - 4, y, infoX + 236, y + 206, PANEL_SOFT);
		drawBorder(context, infoX - 4, y, 240, 206, PANEL_BORDER);
		drawText(context, "Foco atual", infoX + 10, y + 10, TEXT, false);
		context.fill(infoX + 10, y + 26, infoX + 222, y + 54, 0x44000000);
		drawBorder(context, infoX + 10, y + 26, 212, 28, 0xFF45475A);
		drawMobIcon(context, selected, infoX + 16, y + 32);
		drawText(context, fitText(selected.displayName, 172), infoX + 40, y + 36, ACCENT, true);

		drawText(context, "Alvo:", infoX + 10, y + 72, MUTED, false);
		drawText(context, fitText(controller.getCurrentTargetName(), 158), infoX + 70, y + 72, TEXT, false);
		drawText(context, "Distância:", infoX + 10, y + 88, MUTED, false);
		drawText(context, distanceText, infoX + 70, y + 88, TEXT, false);
		drawText(context, "À vista:", infoX + 10, y + 104, MUTED, false);
		drawText(context, controller.getFoundMobCount() + " mobs", infoX + 70, y + 104, TEXT, false);
		drawText(context, "Um clique:", infoX + 10, y + 124, MUTED, false);
		drawText(context, controller.getOneClickStatus(), infoX + 80, y + 124, config.oneClickMode ? WARNING : MUTED, false);
		drawText(context, "Registro:", infoX + 10, y + 144, MUTED, false);
		drawText(context, config.captureMobLog ? "Gravando" : "Pausado", infoX + 80, y + 144, config.captureMobLog ? GOOD : MUTED, false);
		drawText(context, "Prioridade:", infoX + 10, y + 164, MUTED, false);
		drawText(context, config.prioritizeRareMobs ? "Raros" : "Padrão", infoX + 80, y + 164, config.prioritizeRareMobs ? ACCENT : MUTED, false);
		drawText(context, "Pausa:", infoX + 10, y + 184, MUTED, false);
		drawText(context, controller.getTargetSwitchCooldownStatus(), infoX + 80, y + 184, WARNING, false);
	}

	private void drawSettingRow(GuiGraphicsExtractor context, String label, String value, int y) {
		drawText(context, label, settingsX + 10, y, MUTED, false);
		drawText(context, value, valueX - 10, y, ACCENT, true);
	}

	private void drawMobIcon(GuiGraphicsExtractor context, TargetMobOption option, int x, int y) {
		try {
			ItemStack icon = option.icon();
			if (icon != null && !icon.isEmpty()) {
				context.item(icon, x, y);
				context.itemDecorations(font, icon, x, y);
			}
		} catch (RuntimeException ignored) {
		}
	}

	private void rebuildActionButtons() {
		if (actionButtons.isEmpty()) {
			actionButtons.add(new ActionButton(Action.TOGGLE, "", 0, 0, 128, 24));
			actionButtons.add(new ActionButton(Action.FIRST_PAGE, "<<", 0, 0, 28, 22));
			actionButtons.add(new ActionButton(Action.PREV_PAGE, "<", 0, 0, 28, 22));
			actionButtons.add(new ActionButton(Action.NEXT_PAGE, ">", 0, 0, 28, 22));
			actionButtons.add(new ActionButton(Action.LAST_PAGE, ">>", 0, 0, 28, 22));
			actionButtons.add(new ActionButton(Action.CLEAR_SEARCH, "Limpar", 0, 0, 52, 22));
			actionButtons.add(new ActionButton(Action.RADIUS_DOWN, "-", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.RADIUS_UP, "+", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DISTANCE_DOWN, "-", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DISTANCE_UP, "+", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DELAY_DOWN, "-", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DELAY_UP, "+", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.ONE_CLICK, "1 clique", 0, 0, 66, 20));
			actionButtons.add(new ActionButton(Action.LOG, "Log", 0, 0, 40, 20));
			actionButtons.add(new ActionButton(Action.PRIORITY, "Raros", 0, 0, 50, 20));
		}

		actionButtons.get(0).label = controller.getConfig().enabled ? "DESATIVAR" : "ATIVAR";
		actionButtons.get(0).x = panelX + panelWidth - 148;
		actionButtons.get(0).y = panelY + 13;

		int pagerY = panelY + panelHeight - 34;
		int pagerX = listX;
		actionButtons.get(1).x = pagerX; actionButtons.get(1).y = pagerY;
		actionButtons.get(2).x = pagerX + 34; actionButtons.get(2).y = pagerY;
		actionButtons.get(3).x = pagerX + 68; actionButtons.get(3).y = pagerY;
		actionButtons.get(4).x = pagerX + 102; actionButtons.get(4).y = pagerY;
		actionButtons.get(5).x = searchX + searchWidth - 58; actionButtons.get(5).y = searchY + 1;

		int downX = valueX + 40;
		int upX = valueX + 68;
		actionButtons.get(6).x = downX; actionButtons.get(6).y = settingsY + 27;
		actionButtons.get(7).x = upX; actionButtons.get(7).y = settingsY + 27;
		actionButtons.get(8).x = downX; actionButtons.get(8).y = settingsY + 53;
		actionButtons.get(9).x = upX; actionButtons.get(9).y = settingsY + 53;
		actionButtons.get(10).x = downX; actionButtons.get(10).y = settingsY + 79;
		actionButtons.get(11).x = upX; actionButtons.get(11).y = settingsY + 79;
		actionButtons.get(12).x = infoX + 154; actionButtons.get(12).y = infoY + 120;
		actionButtons.get(13).x = infoX + 170; actionButtons.get(13).y = infoY + 140;
		actionButtons.get(14).x = infoX + 164; actionButtons.get(14).y = infoY + 160;
	}

	private void drawActionButtons(GuiGraphicsExtractor context, int offsetY) {
		for (ActionButton button : actionButtons) {
			if (panelWidth < 650 && (button.action == Action.ONE_CLICK || button.action == Action.LOG || button.action == Action.PRIORITY)) {
				continue;
			}
			int btnY = button.y + offsetY;
			int baseBg = 0xFF313244;
			int hoverBg = 0xFF45475A;
			int border = 0xFF585B70;
			int textColor = TEXT;

			if (button.action == Action.TOGGLE) {
				boolean enabled = controller.getConfig().enabled;
				baseBg = enabled ? 0xFFF38BA8 : 0xFFA6E3A1;
				hoverBg = enabled ? 0xFFECA4B8 : 0xFFB4EBB0;
				border = baseBg;
				textColor = 0xFF11111B;
			}
			if ((button.action == Action.CLEAR_SEARCH && searchQuery.isEmpty()) || isDisabledPager(button.action)) {
				baseBg = 0x66313244;
				hoverBg = baseBg;
				border = 0x66585B70;
				textColor = 0x88CDD6F4;
			}

			int currentBg = lerpColor(baseBg, hoverBg, button.hoverAnim);
			int currentBorder = lerpColor(border, WARNING, button.hoverAnim);
			context.fill(button.x, btnY, button.x + button.width, btnY + button.height, currentBg);
			drawBorder(context, button.x, btnY, button.width, button.height, currentBorder);
			int textWidth = font.width(button.label);
			drawText(context, button.label, button.x + (button.width - textWidth) / 2, btnY + (button.height - 8) / 2, textColor, button.action == Action.TOGGLE);
		}

		String pageText = filteredOptions.isEmpty() ? "Página 0/0" : "Página " + (page + 1) + "/" + (getMaxPage() + 1);
		drawText(context, pageText, listX + 144, panelY + panelHeight - 28 + offsetY, MUTED, false);
	}

	private void runAction(Action action) {
		if (isDisabledPager(action) || action == Action.CLEAR_SEARCH && searchQuery.isEmpty()) {
			return;
		}

		MobHunterConfig config = controller.getConfig();
		switch (action) {
			case TOGGLE -> controller.toggleEnabled(Minecraft.getInstance());
			case FIRST_PAGE -> {
				page = 0;
				keyboardIndex = filteredOptions.isEmpty() ? 0 : 0;
			}
			case PREV_PAGE -> changePage(-1);
			case NEXT_PAGE -> changePage(1);
			case LAST_PAGE -> {
				page = getMaxPage();
				keyboardIndex = filteredOptions.isEmpty() ? 0 : page * pageSize;
			}
			case CLEAR_SEARCH -> {
				searchQuery = "";
				resetSearchPage();
			}
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

	private void resetSearchPage() {
		page = 0;
		keyboardIndex = 0;
		rebuildFilteredOptions();
	}

	private void changePage(int delta) {
		int oldPage = page;
		page = clamp(page + delta, 0, getMaxPage());
		if (oldPage != page) {
			keyboardIndex = filteredOptions.isEmpty() ? 0 : clamp(page * pageSize, 0, filteredOptions.size() - 1);
		}
	}

	private void moveSelection(int delta) {
		if (filteredOptions.isEmpty()) {
			keyboardIndex = 0;
			return;
		}
		keyboardIndex = clamp(keyboardIndex + delta, 0, filteredOptions.size() - 1);
		page = clamp(keyboardIndex / pageSize, 0, getMaxPage());
	}

	private boolean isDisabledPager(Action action) {
		return (action == Action.FIRST_PAGE || action == Action.PREV_PAGE) && page <= 0 ||
			(action == Action.NEXT_PAGE || action == Action.LAST_PAGE) && page >= getMaxPage();
	}

	private int getMaxPage() {
		if (filteredOptions.isEmpty()) {
			return 0;
		}
		return Math.max(0, (filteredOptions.size() - 1) / pageSize);
	}

	private int getAnimOffsetY() {
		float openAnim = Math.min(1f, (System.currentTimeMillis() - initTime) / 220f);
		float easeOut = 1f - (float) Math.pow(1f - openAnim, 4);
		return (int) ((1f - easeOut) * 28);
	}

	private void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}

	private void drawText(GuiGraphicsExtractor context, String text, int x, int y, int color, boolean shadow) {
		if (shadow) {
			context.text(font, text, x + 1, y + 1, 0x88000000);
		}
		context.text(font, text, x, y, color);
	}

	private String fitText(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		int limit = Math.max(0, maxWidth - font.width(suffix));
		String result = text;
		while (!result.isEmpty() && font.width(result) > limit) {
			result = result.substring(0, result.length() - 1);
		}
		return result + suffix;
	}

	private String shortEntityId(String entityId) {
		int index = entityId.indexOf(':');
		return index >= 0 ? entityId.substring(index + 1) : entityId;
	}

	private String formatOneDecimal(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private String normalize(String value) {
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
		return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

	private int lerpColor(int color1, int color2, float delta) {
		int a1 = (color1 >> 24) & 0xff, r1 = (color1 >> 16) & 0xff, g1 = (color1 >> 8) & 0xff, b1 = color1 & 0xff;
		int a2 = (color2 >> 24) & 0xff, r2 = (color2 >> 16) & 0xff, g2 = (color2 >> 8) & 0xff, b2 = color2 & 0xff;
		int a = (int) (a1 + (a2 - a1) * delta);
		int r = (int) (r1 + (r2 - r1) * delta);
		int g = (int) (g1 + (g2 - g1) * delta);
		int b = (int) (b1 + (b2 - b1) * delta);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private int clamp(int value, int min, int max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	private enum Action {
		TOGGLE, FIRST_PAGE, PREV_PAGE, NEXT_PAGE, LAST_PAGE, CLEAR_SEARCH,
		RADIUS_DOWN, RADIUS_UP, DISTANCE_DOWN, DISTANCE_UP, DELAY_DOWN, DELAY_UP,
		ONE_CLICK, LOG, PRIORITY
	}

	private static class ActionButton {
		private final Action action;
		private String label;
		private int x, y, width, height;
		private float hoverAnim;

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

		private void updateHover(boolean hovered) {
			hoverAnim = hovered ? Math.min(1f, hoverAnim + 0.15f) : Math.max(0f, hoverAnim - 0.1f);
		}
	}

	private static class MobCard {
		private TargetMobOption option;
		private int optionIndex;
		private int x, y, width, height;
		private float hoverAnim;

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

		private void updateHover(boolean hovered) {
			hoverAnim = hovered ? Math.min(1f, hoverAnim + 0.15f) : Math.max(0f, hoverAnim - 0.1f);
		}
	}
}
