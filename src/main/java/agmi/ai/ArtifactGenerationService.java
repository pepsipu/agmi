package agmi.ai;

import agmi.Agmi;
import agmi.net.ArtifactGenerationStatusPayload;
import agmi.util.ArtifactStackData;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ArtifactGenerationService {
	private final AgmiConfig config;
	private final ArtifactCache cache;
	private final OpenRouterClient openRouterClient;
	private final ExecutorService executor;
	private final Map<String, CompletableFuture<ArtifactSpec>> pending = new ConcurrentHashMap<>();
	private final Map<String, Long> lastNotifications = new ConcurrentHashMap<>();

	public ArtifactGenerationService(AgmiConfig config) {
		this.config = config;
		this.cache = new ArtifactCache(config.cacheDir());
		this.openRouterClient = new OpenRouterClient(config);
		this.executor = Executors.newFixedThreadPool(2);
	}

	public void requestArtifact(CraftingMenu menu, ServerPlayer player) {
		CraftingContainer craftSlots = this.craftingContainer(menu);
		ResultContainer resultSlots = this.resultContainer(menu);
		if (craftSlots == null || resultSlots == null) {
			this.sendGenerationStatus(player, menu.containerId, false);
			return;
		}

		if (CraftingSignature.isEmpty(craftSlots)) {
			this.sendGenerationStatus(player, menu.containerId, false);
			return;
		}

		String signature = CraftingSignature.rawSignature(craftSlots);
		String signatureHash = CraftingSignature.hash(signature);
		String promptDescription = CraftingSignature.promptDescription(craftSlots);
		Optional<ArtifactSpec> cachedSpec = this.cache.load(signatureHash);

		if (cachedSpec.isPresent()) {
			ArtifactSpec spec = cachedSpec.get();
			boolean codeReady = spec.hasExecutableSource();
			boolean texturesReady = spec.hasRequiredTextures() || !this.config.generateTextures();
			if ((codeReady && texturesReady) || !this.config.hasApiKey()) {
				this.showArtifact(menu, resultSlots, spec);
				this.sendGenerationStatus(player, menu.containerId, false);
				return;
			}
		}

		if (!this.config.hasApiKey()) {
			this.notifyOnceEvery(player, "missing_api_key", 5_000L, "AGMI is disabled until OPENROUTER_API_KEY is available.");
			this.sendGenerationStatus(player, menu.containerId, false);
			return;
		}

		CompletableFuture<ArtifactSpec> existingFuture = this.pending.get(signatureHash);
		if (existingFuture != null) {
			this.sendGenerationStatus(player, menu.containerId, true);
			return;
		}

		this.sendGenerationStatus(player, menu.containerId, true);
		CompletableFuture<ArtifactSpec> future;
		if (cachedSpec.isPresent()) {
			ArtifactSpec cached = cachedSpec.get();
			future = CompletableFuture.supplyAsync(() -> {
				this.openRouterClient.generateMissingTextures(cached);
				return ArtifactSpecSanitizer.sanitize(cached, signatureHash);
			}, this.executor);
		} else {
			future = CompletableFuture.supplyAsync(
				() -> this.openRouterClient.generateArtifact(signatureHash, promptDescription),
				this.executor
			);
		}

		this.pending.put(signatureHash, future);
		future.whenComplete((spec, throwable) -> {
			((ServerLevel) player.level()).getServer().execute(() -> {
				this.pending.remove(signatureHash);
				this.sendGenerationStatus(player, menu.containerId, false);

				if (throwable != null) {
					Agmi.LOGGER.warn("Artifact generation failed for {}", signatureHash, throwable);
					player.sendSystemMessage(Component.literal("AGMI failed to generate that artifact."));
					return;
				}

				this.cache.save(signatureHash, spec);
				if (player.containerMenu == menu && signatureHash.equals(CraftingSignature.hash(CraftingSignature.rawSignature(craftSlots)))) {
					this.showArtifact(menu, resultSlots, spec);
				}
			});
		});
	}

	private void showArtifact(CraftingMenu menu, ResultContainer resultSlots, ArtifactSpec spec) {
		resultSlots.setItem(0, ArtifactStackData.createStack(spec));
		menu.broadcastChanges();
	}

	private void sendGenerationStatus(ServerPlayer player, int containerId, boolean generating) {
		ServerPlayNetworking.send(player, new ArtifactGenerationStatusPayload(containerId, generating));
	}

	private CraftingContainer craftingContainer(CraftingMenu menu) {
		if (menu.getInputGridSlots().isEmpty()) {
			return null;
		}

		Slot firstInputSlot = menu.getInputGridSlots().getFirst();
		return firstInputSlot.container instanceof CraftingContainer craftingContainer ? craftingContainer : null;
	}

	private ResultContainer resultContainer(CraftingMenu menu) {
		return menu.getResultSlot().container instanceof ResultContainer resultContainer ? resultContainer : null;
	}

	private void notifyOnceEvery(ServerPlayer player, String key, long intervalMs, String message) {
		long now = System.currentTimeMillis();
		long previous = this.lastNotifications.getOrDefault(key + ":" + player.getUUID(), 0L);

		if (now - previous < intervalMs) {
			return;
		}

		this.lastNotifications.put(key + ":" + player.getUUID(), now);
		player.sendSystemMessage(Component.literal(message));
	}
}
