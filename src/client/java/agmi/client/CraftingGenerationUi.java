package agmi.client;

import agmi.net.ArtifactGenerationStatusPayload;
import agmi.net.RequestArtifactPayload;
import agmi.mixin.client.AbstractContainerScreenAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

public final class CraftingGenerationUi {
	private static final Map<Integer, LoadingState> LOADING = new ConcurrentHashMap<>();
	private static final int SLOT_SIZE = 18;
	private static final int PANEL_TOP_MARGIN = 4;
	private static final int PANEL_SIDE_MARGIN = 8;
	private static final int PANEL_GRID_GAP = 6;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_BAR_INSET = 4;
	private static final int BUTTON_BAR_HEIGHT = 3;
	private static final int INDICATOR_MIN_WIDTH = 18;

	private CraftingGenerationUi() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(ArtifactGenerationStatusPayload.TYPE, (payload, context) -> {
			if (payload.generating()) {
				LOADING.put(payload.containerId(), new LoadingState(System.currentTimeMillis()));
			} else {
				LOADING.remove(payload.containerId());
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof CraftingScreen craftingScreen)) {
				return;
			}

			Button button = Button.builder(
				Component.literal("Generate"),
				ignored -> {
					int containerId = craftingScreen.getMenu().containerId;
					LOADING.put(containerId, new LoadingState(System.currentTimeMillis()));
					ClientPlayNetworking.send(new RequestArtifactPayload(containerId));
				}
			)
				.bounds(0, 0, Button.DEFAULT_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.literal("Generate an AGMI artifact from the current crafting grid.")))
				.build();
			Screens.getWidgets(screen).add(button);

			ScreenEvents.afterTick(screen).register(ignored -> {
				Layout layout = layout(craftingScreen);
				button.setRectangle(layout.buttonWidth(), layout.buttonHeight(), layout.buttonX(), layout.buttonY());
				syncButton(craftingScreen, button);
			});
			ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, tickProgress) -> renderLoadingBar(craftingScreen, button, graphics));
		});
	}

	private static void syncButton(CraftingScreen screen, Button button) {
		boolean generating = LOADING.containsKey(screen.getMenu().containerId);
		button.active = !generating && hasIngredients(screen);
		button.setMessage(Component.literal(generating ? "Generating..." : "Generate"));
	}

	private static boolean hasIngredients(CraftingScreen screen) {
		for (Slot slot : screen.getMenu().getInputGridSlots()) {
			if (slot.hasItem()) {
				return true;
			}
		}

		return false;
	}

	private static void renderLoadingBar(CraftingScreen screen, Button button, GuiGraphicsExtractor graphics) {
		LoadingState state = LOADING.get(screen.getMenu().containerId);
		if (state == null || !button.visible) {
			return;
		}

		int barX = button.getX() + BUTTON_BAR_INSET;
		int barY = button.getBottom() - BUTTON_BAR_INSET - BUTTON_BAR_HEIGHT;
		int barWidth = Math.max(1, button.getWidth() - (BUTTON_BAR_INSET * 2));
		int indicatorWidth = Math.max(INDICATOR_MIN_WIDTH, barWidth / 3);
		int travel = Math.max(1, barWidth - indicatorWidth);
		int frame = (int) (((System.currentTimeMillis() - state.startedAtMillis()) / 20L) % Math.max(1, travel * 2));
		int offset = frame > travel ? travel * 2 - frame : frame;

		graphics.fill(barX, barY, barX + barWidth, barY + BUTTON_BAR_HEIGHT, 0xAA101318);
		graphics.fill(barX + offset, barY, barX + offset + indicatorWidth, barY + BUTTON_BAR_HEIGHT, 0xFFDD6C2F);
	}

	private static Layout layout(CraftingScreen screen) {
		Slot resultSlot = screen.getMenu().getResultSlot();
		int craftGridRight = 0;
		int inventoryTop = Integer.MAX_VALUE;
		for (Slot slot : screen.getMenu().slots) {
			int slotRight = slot.x + SLOT_SIZE;
			if (screen.getMenu().getInputGridSlots().contains(slot)) {
				craftGridRight = Math.max(craftGridRight, slotRight);
				continue;
			}

			if (slot != resultSlot) {
				inventoryTop = Math.min(inventoryTop, slot.y);
			}
		}

		int panelLeft = leftPos(screen) + craftGridRight + PANEL_GRID_GAP;
		int panelRight = leftPos(screen) + imageWidth(screen) - PANEL_SIDE_MARGIN;
		int panelTop = topPos(screen) + resultSlot.y + SLOT_SIZE + PANEL_TOP_MARGIN;
		int panelBottom = topPos(screen) + inventoryTop - PANEL_TOP_MARGIN;
		int buttonWidth = Math.max(48, panelRight - panelLeft);
		int availableHeight = Math.max(BUTTON_HEIGHT, panelBottom - panelTop);
		int buttonY = panelTop + Math.max(0, (availableHeight - BUTTON_HEIGHT) / 2);

		return new Layout(panelLeft, buttonY, buttonWidth, BUTTON_HEIGHT);
	}

	private static int leftPos(CraftingScreen screen) {
		return ((AbstractContainerScreenAccessor) screen).agmi$getLeftPos();
	}

	private static int topPos(CraftingScreen screen) {
		return ((AbstractContainerScreenAccessor) screen).agmi$getTopPos();
	}

	private static int imageWidth(CraftingScreen screen) {
		return ((AbstractContainerScreenAccessor) screen).agmi$getImageWidth();
	}

	private record LoadingState(long startedAtMillis) {
	}

	private record Layout(int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
	}
}
