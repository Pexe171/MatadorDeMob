package com.matador.client;

import com.matador.client.gui.MobHunterScreen;
import com.matador.client.hunter.MobHunterController;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class MatadorClient implements ClientModInitializer {
	public static final String MOD_ID = "matador";

	private static boolean openKeyPressed;

	@Override
	public void onInitializeClient() {
	}

	public static void tick(Minecraft client) {
		MobHunterController controller = MobHunterController.getInstance();
		boolean pressed = client.getWindow() != null && InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_J);

		if (pressed && !openKeyPressed) {
			if (client.screen instanceof MobHunterScreen) {
				controller.stopAutomaticMovement(client);
				client.setScreen(null);
			} else {
				controller.stopAutomaticMovement(client);
				client.setScreen(new MobHunterScreen(controller));
			}
		}

		openKeyPressed = pressed;
		if (client.screen instanceof MobHunterScreen) {
			controller.stopAutomaticMovement(client);
			return;
		}

		controller.tick(client);
	}
}
