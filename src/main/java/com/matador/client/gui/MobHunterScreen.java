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
	// Paleta de cores moderna (inspirada em Catppuccin Mocha)
	private static final int PANEL_BG = 0xFA1E1E2E;
	private static final int PANEL_HEADER = 0xFA181825;
	private static final int PANEL_BORDER = 0xFF313244;
	private static final int PANEL_SOFT = 0xAA181825;
	
	private static final int CARD_BG = 0xDD313244;
	private static final int CARD_HOVER = 0xFF45475A;
	private static final int CARD_SELECTED = 0xFF89B4FA; // Azul vibrante
	private static final int CARD_BORDER = 0xFF45475A;
	
	private static final int ACCENT = 0xFF89B4FA; // Azul
	private static final int WARNING = 0xFFF9E2AF; // Amarelo
	private static final int TEXT = 0xFFCDD6F4; // Texto principal
	private static final int MUTED = 0xFFA6ADC8; // Texto secundário
	private static final int DANGER = 0xFFF38BA8; // Vermelho
	private static final int GOOD = 0xFFA6E3A1; // Verde pastel

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
	
	// Controle de animação de entrada
	private long initTime;

	public MobHunterScreen(MobHunterController controller) {
		super(Component.literal("Mob Hunter"));
		this.controller = controller;
	}

	@Override
	protected void init() {
		this.initTime = System.currentTimeMillis();
		rebuildLayout();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		// Fundo translúcido do jogo
		context.fill(0, 0, width, height, 0x88000000);
		
		rebuildLayout();
		controller.refreshTarget(Minecraft.getInstance());

		// Calcula a animação de deslizar (slide-up) e opacidade
		float openAnim = Math.min(1f, (System.currentTimeMillis() - initTime) / 350f);
		float easeOutQuart = 1f - (float)Math.pow(1f - openAnim, 4);
		int animOffsetY = (int)((1f - easeOutQuart) * 40); // Sobe 40 pixels suavemente

		// Atualiza o estado de hover com base na posição do mouse ajustada pela animação
		int adjustedMouseY = mouseY - animOffsetY;
		updateHovers(mouseX, adjustedMouseY);

		drawPanel(context, animOffsetY);
		drawHeader(context, animOffsetY);
		drawMobCards(context, animOffsetY);
		drawSettings(context, animOffsetY);
		drawInfo(context, animOffsetY);
		drawActionButtons(context, animOffsetY);
	}

	private void updateHovers(int mouseX, int mouseY) {
		for (MobCard card : mobCards) {
			card.updateHover(card.contains(mouseX, mouseY));
		}
		for (ActionButton btn : actionButtons) {
			btn.updateHover(btn.contains(mouseX, mouseY));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			// Ajusta o clique caso a animação de entrada ainda esteja rodando
			float openAnim = Math.min(1f, (System.currentTimeMillis() - initTime) / 350f);
			float easeOut = 1f - (float)Math.pow(1f - openAnim, 4);
			int animOffsetY = (int)((1f - easeOut) * 40);
			
			double mouseX = click.x();
			double mouseY = click.y() - animOffsetY;

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
		panelWidth = Math.min(620, Math.max(340, width - 24));
		panelHeight = Math.min(400, Math.max(300, height - 24));
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;

		boolean wide = panelWidth >= 560;
		int sidePadding = 20;
		int top = panelY + 68;
		int gridWidth = wide ? panelWidth - 270 : panelWidth - sidePadding * 2;
		int columns = gridWidth >= 300 ? 2 : 1;
		int cardGap = 10;
		int cardWidth = (gridWidth - cardGap * (columns - 1)) / columns;
		int cardHeight = 44;
		List<TargetMobOption> options = TargetMobOption.options();

		// Apenas recria os cartões se a quantidade mudou para não perder as animações de hover
		if (mobCards.size() != options.size()) {
			mobCards.clear();
			for (TargetMobOption option : options) {
				mobCards.add(new MobCard(option, 0, 0, 0, 0));
			}
		}

		for (int index = 0; index < options.size(); index++) {
			int column = index % columns;
			int row = index / columns;
			int x = panelX + sidePadding + column * (cardWidth + cardGap);
			int y = top + row * (cardHeight + cardGap);
			MobCard card = mobCards.get(index);
			card.x = x; card.y = y; card.width = cardWidth; card.height = cardHeight;
		}

		int rows = (int) Math.ceil(options.size() / (double) columns);
		settingsX = panelX + sidePadding;
		settingsY = top + rows * (cardHeight + cardGap) + 16;
		valueX = settingsX + 130;

		if (wide) {
			infoX = panelX + panelWidth - 240;
			infoY = top;
		} else {
			infoX = settingsX;
			infoY = settingsY + 116;
		}

		rebuildActionButtons();
	}

	private void drawPanel(GuiGraphicsExtractor context, int offsetY) {
		int y = panelY + offsetY;
		
		// Sombra sutil / Borda exterior
		drawBorder(context, panelX - 1, y - 1, panelWidth + 2, panelHeight + 2, 0x44000000);
		
		// Fundo Principal
		context.fill(panelX, y, panelX + panelWidth, y + panelHeight, PANEL_BG);
		drawBorder(context, panelX, y, panelWidth, panelHeight, PANEL_BORDER);
		
		// Cabeçalho destacado
		context.fill(panelX + 1, y + 1, panelX + panelWidth - 1, y + 50, PANEL_HEADER);
		context.fill(panelX + 1, y + 50, panelX + panelWidth - 1, y + 51, PANEL_BORDER);
	}

	private void drawHeader(GuiGraphicsExtractor context, int offsetY) {
		int y = panelY + offsetY;
		drawText(context, "Mob Hunter", panelX + 20, y + 15, TEXT, true);

		boolean enabled = controller.getConfig().enabled;
		String status = enabled ? "Sistema Ativo" : "Sistema Inativo";
		int statusColor = enabled ? GOOD : DANGER;
		
		// Bolinha de status
		context.fill(panelX + 20, y + 33, panelX + 25, y + 38, statusColor);
		drawText(context, status, panelX + 32, y + 32, statusColor, false);
	}

	private void drawMobCards(GuiGraphicsExtractor context, int offsetY) {
		drawText(context, "Selecione o Alvo", panelX + 20, panelY + offsetY + 56, MUTED, false);

		for (MobCard card : mobCards) {
			boolean selected = card.option.entityId.equals(controller.getConfig().targetMobId);
			int cardY = card.y + offsetY;
			
			// Cor base interpolada suavemente com a cor de hover
			int baseBg = selected ? 0xFF3B4252 : CARD_BG; // Fundo levemente diferente se selecionado
			int currentBg = lerpColor(baseBg, CARD_HOVER, card.hoverAnim);
			int currentBorder = selected ? ACCENT : lerpColor(CARD_BORDER, WARNING, card.hoverAnim);

			context.fill(card.x, cardY, card.x + card.width, cardY + card.height, currentBg);
			drawBorder(context, card.x, cardY, card.width, card.height, currentBorder);

			// Indicador lateral do selecionado
			if (selected) {
				context.fill(card.x + 1, cardY + 1, card.x + 4, cardY + card.height - 1, ACCENT);
			}

			int iconX = card.x + 14;
			int iconY = cardY + 14;
			drawMobIcon(context, card.option, iconX, iconY);

			int nameColor = selected ? 0xFFFFFFFF : lerpColor(TEXT, 0xFFFFFFFF, card.hoverAnim);
			drawText(context, card.option.displayName, card.x + 40, cardY + 14, nameColor, selected);
		}
	}

	private void drawSettings(GuiGraphicsExtractor context, int offsetY) {
		MobHunterConfig config = controller.getConfig();
		int y = settingsY + offsetY;

		context.fill(settingsX - 4, y, settingsX + Math.min(310, panelWidth - 40), y + 104, PANEL_SOFT);
		drawBorder(context, settingsX - 4, y, Math.min(314, panelWidth - 40), 104, PANEL_BORDER);
		
		drawText(context, "Ajustes de Combate", settingsX + 10, y + 10, TEXT, false);

		drawSettingRow(context, "Raio de Busca", formatOneDecimal(config.searchRadius) + "m", y + 34);
		drawSettingRow(context, "Alcance de Ataque", formatOneDecimal(config.attackDistance) + "m", y + 60);
		drawSettingRow(context, "Atraso (Delay)", config.attackDelayTicks + " tks", y + 86);
	}

	private void drawInfo(GuiGraphicsExtractor context, int offsetY) {
		Minecraft client = Minecraft.getInstance();
		MobHunterConfig config = controller.getConfig();
		TargetMobOption selected = controller.getSelectedOption();
		double distance = controller.getCurrentTargetDistance(client);
		String distanceText = controller.getCurrentTargetName().equals("Nenhum") ? "-" : formatOneDecimal(distance) + "m";
		int y = infoY + offsetY;

		context.fill(infoX - 4, y, infoX + 220, y + 192, PANEL_SOFT);
		drawBorder(context, infoX - 4, y, 224, 192, PANEL_BORDER);

		drawText(context, "Foco Atual", infoX + 10, y + 10, TEXT, false);
		
		// Cartãozinho do alvo atual
		context.fill(infoX + 10, y + 26, infoX + 210, y + 54, 0x44000000);
		drawBorder(context, infoX + 10, y + 26, 200, 28, 0xFF45475A);
		drawMobIcon(context, selected, infoX + 16, y + 32);
		drawText(context, selected.displayName, infoX + 40, y + 36, ACCENT, true);

		drawText(context, "Estatísticas & Módulos", infoX + 10, y + 68, TEXT, false);
		drawText(context, "Alvo:", infoX + 10, y + 86, MUTED, false); 
		drawText(context, controller.getCurrentTargetName(), infoX + 60, y + 86, TEXT, false);
		
		drawText(context, "Distância:", infoX + 10, y + 102, MUTED, false);
		drawText(context, distanceText, infoX + 65, y + 102, TEXT, false);
		
		drawText(context, "À Vista:", infoX + 10, y + 118, MUTED, false);
		drawText(context, controller.getFoundMobCount() + " mobs", infoX + 60, y + 118, TEXT, false);
		
		drawText(context, "Um Clique:", infoX + 10, y + 134, MUTED, false);
		drawText(context, controller.getOneClickStatus(), infoX + 70, y + 134, config.oneClickMode ? WARNING : MUTED, false);
		
		drawText(context, "Log Files:", infoX + 10, y + 150, MUTED, false);
		drawText(context, config.captureMobLog ? "Gravando" : "Pausado", infoX + 70, y + 150, config.captureMobLog ? GOOD : MUTED, false);
		
		drawText(context, "Prioridade:", infoX + 10, y + 166, MUTED, false);
		drawText(context, config.prioritizeRareMobs ? "Raros/Bosses" : "Padrão", infoX + 70, y + 166, config.prioritizeRareMobs ? ACCENT : MUTED, false);
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
		} catch (RuntimeException exception) {
			// Seguro contra componentes indisponíveis
		}
	}

	private void rebuildActionButtons() {
		if(actionButtons.isEmpty()) {
			// Cria os botões apenas uma vez para manter a animação de hover fluida
			actionButtons.add(new ActionButton(Action.TOGGLE, "", 0, 0, 120, 24));
			
			actionButtons.add(new ActionButton(Action.RADIUS_DOWN, "-", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.RADIUS_UP, "+", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DISTANCE_DOWN, "-", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DISTANCE_UP, "+", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DELAY_DOWN, "-", 0, 0, 24, 22));
			actionButtons.add(new ActionButton(Action.DELAY_UP, "+", 0, 0, 24, 22));
			
			actionButtons.add(new ActionButton(Action.ONE_CLICK, "1-Click", 0, 0, 60, 20));
			actionButtons.add(new ActionButton(Action.LOG, "Log", 0, 0, 40, 20));
			actionButtons.add(new ActionButton(Action.PRIORITY, "Raros", 0, 0, 50, 20));
		}
		
		// Atualiza apenas as posições dinâmicas
		actionButtons.get(0).label = controller.getConfig().enabled ? "DESATIVAR MOD" : "ATIVAR MOD";
		actionButtons.get(0).x = panelX + panelWidth - 140;
		actionButtons.get(0).y = panelY + 13;

		int downX = valueX + 50;
		int upX = valueX + 80;
		
		actionButtons.get(1).x = downX; actionButtons.get(1).y = settingsY + 27;
		actionButtons.get(2).x = upX;   actionButtons.get(2).y = settingsY + 27;
		actionButtons.get(3).x = downX; actionButtons.get(3).y = settingsY + 53;
		actionButtons.get(4).x = upX;   actionButtons.get(4).y = settingsY + 53;
		actionButtons.get(5).x = downX; actionButtons.get(5).y = settingsY + 79;
		actionButtons.get(6).x = upX;   actionButtons.get(6).y = settingsY + 79;
		
		actionButtons.get(7).x = infoX + 150; actionButtons.get(7).y = infoY + 130;
		actionButtons.get(8).x = infoX + 170; actionButtons.get(8).y = infoY + 146;
		actionButtons.get(9).x = infoX + 160; actionButtons.get(9).y = infoY + 162;
	}

	private void drawActionButtons(GuiGraphicsExtractor context, int offsetY) {
		for (ActionButton button : actionButtons) {
			int btnY = button.y + offsetY;
			
			int baseBg = 0xFF313244;
			int hoverBg = 0xFF45475A;
			int border = 0xFF585B70;
			int textColor = TEXT;
			
			// Estilo especial para o botão de Ativar/Desativar
			if(button.action == Action.TOGGLE) {
				boolean enabled = controller.getConfig().enabled;
				baseBg = enabled ? 0xFFF38BA8 : 0xFFA6E3A1; // Vermelho se ligado (para desligar), verde se desligado
				hoverBg = enabled ? 0xFFECA4B8 : 0xFFB4EBB0;
				border = baseBg;
				textColor = 0xFF11111B;
			}
			
			int currentBg = lerpColor(baseBg, hoverBg, button.hoverAnim);
			int currentBorder = lerpColor(border, WARNING, button.hoverAnim);

			context.fill(button.x, btnY, button.x + button.width, btnY + button.height, currentBg);
			drawBorder(context, button.x, btnY, button.width, button.height, currentBorder);

			int textWidth = font.width(button.label);
			int textX = button.x + (button.width - textWidth) / 2;
			int textY = btnY + (button.height - 8) / 2; // Centralizado verticalmente
			
			if (button.action == Action.TOGGLE) {
				drawText(context, button.label, textX, textY, textColor, true);
			} else {
				drawText(context, button.label, textX, textY, lerpColor(textColor, 0xFFFFFFFF, button.hoverAnim), false);
			}
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

	private void drawText(GuiGraphicsExtractor context, String text, int x, int y, int color, boolean shadow) {
		if (shadow) {
			context.text(font, text, x + 1, y + 1, 0x88000000); // Sombra mais suave
		}
		context.text(font, text, x, y, color);
	}

	private String formatOneDecimal(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	/**
	 * Interpola gradualmente entre duas cores ARGB.
	 */
	private int lerpColor(int color1, int color2, float delta) {
		int a1 = (color1 >> 24) & 0xff, r1 = (color1 >> 16) & 0xff, g1 = (color1 >> 8) & 0xff, b1 = color1 & 0xff;
		int a2 = (color2 >> 24) & 0xff, r2 = (color2 >> 16) & 0xff, g2 = (color2 >> 8) & 0xff, b2 = color2 & 0xff;
		int a = (int)(a1 + (a2 - a1) * delta);
		int r = (int)(r1 + (r2 - r1) * delta);
		int g = (int)(g1 + (g2 - g1) * delta);
		int b = (int)(b1 + (b2 - b1) * delta);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private enum Action {
		TOGGLE, RADIUS_DOWN, RADIUS_UP, DISTANCE_DOWN, DISTANCE_UP, DELAY_DOWN, DELAY_UP, ONE_CLICK, LOG, PRIORITY
	}

	private static class ActionButton {
		private final Action action;
		private String label;
		private int x, y, width, height;
		private float hoverAnim = 0f;

		private ActionButton(Action action, String label, int x, int y, int width, int height) {
			this.action = action;
			this.label = label;
			this.x = x; this.y = y; this.width = width; this.height = height;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
		
		private void updateHover(boolean isHovered) {
			if (isHovered) hoverAnim = Math.min(1f, hoverAnim + 0.15f);
			else hoverAnim = Math.max(0f, hoverAnim - 0.1f);
		}
	}

	private static class MobCard {
		private final TargetMobOption option;
		private int x, y, width, height;
		private float hoverAnim = 0f;

		private MobCard(TargetMobOption option, int x, int y, int width, int height) {
			this.option = option;
			this.x = x; this.y = y; this.width = width; this.height = height;
		}

		private boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
		
		private void updateHover(boolean isHovered) {
			if (isHovered) hoverAnim = Math.min(1f, hoverAnim + 0.15f);
			else hoverAnim = Math.max(0f, hoverAnim - 0.1f);
		}
	}
}