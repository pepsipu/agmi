package agmi.gametest;

import agmi.Agmi;
import agmi.ai.ArtifactKind;
import agmi.ai.ArtifactSpec;
import agmi.ai.CraftingSignature;
import agmi.block.GeneratedArtifactBlockEntity;
import agmi.util.ArtifactStackData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class AgmiClientGameTest implements FabricClientGameTest {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final BlockPos TEST_BLOCK_POS = new BlockPos(0, 90, 5);

	@Override
	public void runTest(ClientGameTestContext context) {
		resetRuntimeState(context);

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientLevel().waitForChunksRender();
			runItemSpritePhase(context, singleplayer);
			runPlacedBlockPhase(context, singleplayer);
		}
	}

	private static void runItemSpritePhase(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		openCraftingMenu(singleplayer);
		context.waitForScreen(CraftingScreen.class);
		seedCachedArtifact(singleplayer, Items.DIRT, buildSpec("Artifact Test Stripe", ArtifactKind.ITEM, createItemTextureBytes()));
		context.waitFor(client -> inputSlot(client, 0).hasItem());
		context.clickScreenButton("Generate");

		ArtifactSpec spec = waitForGeneratedResult(context, ArtifactKind.ITEM);
		moveResultToHotbar(singleplayer, 0);

		context.runOnClient(client -> client.setScreen(null));
		context.waitTicks(20);
		context.waitFor(client -> {
			LocalPlayer player = client.player;
			if (player == null) {
				return false;
			}

			ItemStack hotbarStack = player.getInventory().getItem(0);
			ArtifactSpec hotbarSpec = ArtifactStackData.read(hotbarStack);
			return hotbarSpec != null
				&& spec.id.equals(hotbarSpec.id)
				&& ArtifactStackData.texturedModelId(hotbarSpec).equals(hotbarStack.get(DataComponents.ITEM_MODEL));
		});

		int guiScaledWidth = context.computeOnClient(client -> client.getWindow().getGuiScaledWidth());
		int guiScaledHeight = context.computeOnClient(client -> client.getWindow().getGuiScaledHeight());
		Path screenshot = context.takeScreenshot("agmi-generated-hotbar-item");
		assertHotbarIcon(screenshot, guiScaledWidth, guiScaledHeight, 0);
	}

	private static void runPlacedBlockPhase(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		openCraftingMenu(singleplayer);
		context.waitForScreen(CraftingScreen.class);
		seedCachedArtifact(singleplayer, Items.COBBLESTONE, buildSpec("Artifact Test Prism", ArtifactKind.BLOCK, createBlockTextureBytes()));
		context.waitFor(client -> inputSlot(client, 0).hasItem());
		context.clickScreenButton("Generate");

		ArtifactSpec spec = waitForGeneratedResult(context, ArtifactKind.BLOCK);
		placeGeneratedBlock(singleplayer, spec);

		context.runOnClient(client -> client.setScreen(null));
		context.waitTicks(40);
		singleplayer.getClientLevel().waitForChunksRender();
		context.waitFor(client -> {
			if (client.level == null) {
				return false;
			}

			return client.level.getBlockEntity(TEST_BLOCK_POS) instanceof GeneratedArtifactBlockEntity blockEntity
				&& spec.id.equals(blockEntity.spec().id)
				&& !blockEntity.spec().texturePngBase64.isBlank();
		});

		Path screenshot = context.takeScreenshot("agmi-generated-placed-block");
		assertCenteredBlockTexture(screenshot);
	}

	private static ArtifactSpec waitForGeneratedResult(ClientGameTestContext context, ArtifactKind expectedKind) {
		context.waitFor(client -> {
			Slot slot = resultSlot(client);
			if (!slot.hasItem()) {
				return false;
			}

			ArtifactSpec spec = ArtifactStackData.read(slot.getItem());
			return spec != null
				&& expectedKind.serializedName().equals(spec.kind)
				&& ArtifactStackData.texturedModelId(spec).equals(slot.getItem().get(DataComponents.ITEM_MODEL));
		});

		ItemStack result = context.computeOnClient(client -> resultSlot(client).getItem().copy());
		ArtifactSpec spec = ArtifactStackData.read(result);
		require(spec != null, "AGMI did not produce a generated artifact");
		require(expectedKind.serializedName().equals(spec.kind), "Unexpected artifact kind: " + spec.kind);
		return spec;
	}

	private static void resetRuntimeState(ClientGameTestContext context) {
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve(Agmi.MOD_ID);

		context.runOnClient(client -> {
			Path packRoot = client.getResourcePackDirectory().resolve("agmi_dynamic");
			deleteRecursively(packRoot);
			client.options.resourcePacks.removeIf(pack -> pack.equals("agmi_dynamic") || pack.equals("file/agmi_dynamic"));
			client.options.incompatibleResourcePacks.removeIf(pack -> pack.equals("agmi_dynamic") || pack.equals("file/agmi_dynamic"));
			client.options.save();
		});

		deleteRecursively(configDir.resolve("cache"));

		try {
			Files.createDirectories(configDir);
			Files.writeString(
				configDir.resolve("agmi.properties"),
				"""
				openrouter.api_key=
				openrouter.text_model=openai/gpt-5.4-nano
				openrouter.image_model=google/gemini-2.5-flash-image
				openrouter.site_url=
				openrouter.site_name=AGMI
				crafting.override_vanilla=true
				textures.generate=true
				requests.timeout_seconds=60
				"""
			);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to reset AGMI config", exception);
		}
	}

	private static void openCraftingMenu(TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayers().getFirst();
			serverPlayer.openMenu(new SimpleMenuProvider(
				(containerId, inventory, player) -> new CraftingMenu(containerId, inventory),
				Component.literal("AGMI Test Crafting")
			));
		});
	}

	private static void seedCachedArtifact(TestSingleplayerContext singleplayer, net.minecraft.world.item.Item ingredient, ArtifactSpec spec) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayers().getFirst();
			CraftingMenu menu = (CraftingMenu) serverPlayer.containerMenu;
			Slot inputSlot = menu.getInputGridSlots().getFirst();
			CraftingContainer craftingContainer = (CraftingContainer) inputSlot.container;
			for (int slot = 0; slot < craftingContainer.getContainerSize(); slot++) {
				craftingContainer.setItem(slot, ItemStack.EMPTY);
			}

			craftingContainer.setItem(0, new ItemStack(ingredient));
			String signatureHash = CraftingSignature.hash(CraftingSignature.rawSignature(craftingContainer));
			spec.id = ArtifactSpec.generatedIdForHash(signatureHash);
			spec.generatedJavaSource = noOpProgramSource(spec.generatedClassName());
			writeCacheEntry(signatureHash, spec);
			menu.slotsChanged(craftingContainer);
		});
	}

	private static void moveResultToHotbar(TestSingleplayerContext singleplayer, int hotbarSlot) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer serverPlayer = server.getPlayerList().getPlayers().getFirst();
			CraftingMenu menu = (CraftingMenu) serverPlayer.containerMenu;
			ItemStack result = menu.getResultSlot().getItem().copy();
			serverPlayer.getInventory().setItem(hotbarSlot, result);
			menu.broadcastChanges();
		});
	}

	private static void placeGeneratedBlock(TestSingleplayerContext singleplayer, ArtifactSpec spec) {
		singleplayer.getServer().runOnServer(server -> {
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill -3 89 0 3 89 8 stone");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "fill -3 90 0 3 94 8 air");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @p 0.5 90 1.5 facing 0.5 90 5.5");

			var level = server.overworld();
			level.setBlock(TEST_BLOCK_POS, Agmi.GENERATED_ARTIFACT_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(TEST_BLOCK_POS) instanceof GeneratedArtifactBlockEntity blockEntity) {
				blockEntity.apply(spec);
			}
		});
	}

	private static ArtifactSpec buildSpec(String name, ArtifactKind kind, byte[] textureBytes) {
		ArtifactSpec spec = new ArtifactSpec();
		spec.name = name;
		spec.kind = kind.serializedName();
		spec.rarity = "rare";
		spec.summary = "A cache-seeded artifact used for AGMI client gametests.";
		spec.itemTexturePrompt = "Unused in gametest";
		spec.blockTexturePrompt = kind == ArtifactKind.BLOCK ? "Unused in gametest" : "";
		spec.itemTexturePngBase64 = Base64.getEncoder().encodeToString(textureBytes);
		spec.blockTexturePngBase64 = kind == ArtifactKind.BLOCK ? Base64.getEncoder().encodeToString(textureBytes) : "";
		return spec;
	}

	private static String noOpProgramSource(String className) {
		int lastDot = className.lastIndexOf('.');
		String packageName = className.substring(0, lastDot);
		String simpleName = className.substring(lastDot + 1);
		return """
			package %s;

			public final class %s implements agmi.codegen.GeneratedArtifactProgram {
				public %s() {
				}
			}
			""".formatted(packageName, simpleName, simpleName);
	}

	private static void writeCacheEntry(String signatureHash, ArtifactSpec spec) {
		Path cachePath = FabricLoader.getInstance().getConfigDir()
			.resolve(Agmi.MOD_ID)
			.resolve("cache")
			.resolve("recipes")
			.resolve(signatureHash + ".json");

		try {
			Files.createDirectories(cachePath.getParent());
			Files.writeString(cachePath, GSON.toJson(spec));
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to seed AGMI cache", exception);
		}
	}

	private static Slot resultSlot(net.minecraft.client.Minecraft client) {
		LocalPlayer player = client.player;
		require(player != null, "Missing local player");
		return player.containerMenu.getSlot(0);
	}

	private static Slot inputSlot(net.minecraft.client.Minecraft client, int index) {
		LocalPlayer player = client.player;
		require(player != null, "Missing local player");
		require(player.containerMenu instanceof CraftingMenu, "Crafting menu was not open");
		CraftingMenu craftingMenu = (CraftingMenu) player.containerMenu;
		return craftingMenu.getInputGridSlots().get(index);
	}

	private static byte[] createItemTextureBytes() {
		return createTextureBytes(0xFFD83B3B, 0xFFFFFFFF, PatternKind.X);
	}

	private static byte[] createBlockTextureBytes() {
		return createTextureBytes(0xFF1D9BF0, 0xFFF4D35E, PatternKind.CHECKER);
	}

	private static byte[] createTextureBytes(int primaryColor, int accentColor, PatternKind patternKind) {
		try {
			BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					boolean accent = switch (patternKind) {
						case X -> (x == y) || (x == 15 - y);
						case CHECKER -> ((x / 4) + (y / 4)) % 2 == 0;
					};
					image.setRGB(x, y, accent ? accentColor : primaryColor);
				}
			}

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, "png", output);
			return output.toByteArray();
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to build gametest texture", exception);
		}
	}

	private static void assertHotbarIcon(Path screenshotPath, int guiScaledWidth, int guiScaledHeight, int slotIndex) {
		try {
			BufferedImage screenshot = ImageIO.read(screenshotPath.toFile());
			require(screenshot != null, "Failed to load screenshot " + screenshotPath);

			double scaleX = screenshot.getWidth() / (double) guiScaledWidth;
			double scaleY = screenshot.getHeight() / (double) guiScaledHeight;
			int hotbarLeft = (int) Math.round(((guiScaledWidth - 182) / 2.0D) * scaleX);
			int hotbarTop = (int) Math.round((guiScaledHeight - 22.0D) * scaleY);
			int slotLeft = (int) Math.round(hotbarLeft + (3 + (slotIndex * 20)) * scaleX);
			int slotTop = (int) Math.round(hotbarTop + (3 * scaleY));
			int slotWidth = Math.max(1, (int) Math.round(16.0D * scaleX));
			int slotHeight = Math.max(1, (int) Math.round(16.0D * scaleY));

			int redPixels = 0;
			int whitePixels = 0;
			for (int y = 0; y < slotHeight; y++) {
				for (int x = 0; x < slotWidth; x++) {
					int argb = screenshot.getRGB(slotLeft + x, slotTop + y);
					int alpha = (argb >>> 24) & 0xFF;
					int red = (argb >>> 16) & 0xFF;
					int green = (argb >>> 8) & 0xFF;
					int blue = argb & 0xFF;
					if (alpha > 200 && red > 180 && green < 110 && blue < 110) {
						redPixels++;
					}
					if (alpha > 200 && red > 220 && green > 220 && blue > 220) {
						whitePixels++;
					}
				}
			}

			int area = slotWidth * slotHeight;
			require(redPixels >= Math.max(24, area / 3), "Generated hotbar icon did not render the custom sprite");
			require(whitePixels >= Math.max(8, area / 16), "Generated hotbar icon lost its highlight detail");
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to verify generated hotbar icon", exception);
		}
	}

	private static void assertCenteredBlockTexture(Path screenshotPath) {
		try {
			BufferedImage screenshot = ImageIO.read(screenshotPath.toFile());
			require(screenshot != null, "Failed to load screenshot " + screenshotPath);

			int centerX = screenshot.getWidth() / 2;
			int centerY = (int) Math.round(screenshot.getHeight() * 0.6D);
			int radiusX = Math.max(35, screenshot.getWidth() / 18);
			int radiusY = Math.max(40, screenshot.getHeight() / 11);
			int cyanPixels = 0;
			int yellowPixels = 0;

			for (int y = Math.max(0, centerY - radiusY); y < Math.min(screenshot.getHeight(), centerY + radiusY); y++) {
				for (int x = Math.max(0, centerX - radiusX); x < Math.min(screenshot.getWidth(), centerX + radiusX); x++) {
					int argb = screenshot.getRGB(x, y);
					int alpha = (argb >>> 24) & 0xFF;
					int red = (argb >>> 16) & 0xFF;
					int green = (argb >>> 8) & 0xFF;
					int blue = argb & 0xFF;
					if (alpha > 200 && red < 80 && green > 100 && blue > 160) {
						cyanPixels++;
					}
					if (alpha > 200 && red > 150 && green > 130 && blue < 120) {
						yellowPixels++;
					}
				}
			}

			require(cyanPixels >= 800, "Placed generated block did not render its custom base texture");
			require(yellowPixels >= 800, "Placed generated block did not render its custom accent texture");
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to verify placed generated block texture", exception);
		}
	}

	private static void deleteRecursively(Path root) {
		if (root == null || !Files.exists(root)) {
			return;
		}

		try (var paths = Files.walk(root)) {
			paths.sorted((left, right) -> Integer.compare(right.getNameCount(), left.getNameCount())).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (Exception exception) {
					throw new IllegalStateException("Failed to delete " + path, exception);
				}
			});
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to clean " + root, exception);
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private enum PatternKind {
		X,
		CHECKER
	}
}
