package agmi.net;

import agmi.Agmi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;

public final class AgmiNetworking {
	private AgmiNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(RequestArtifactPayload.TYPE, RequestArtifactPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ArtifactGenerationStatusPayload.TYPE, ArtifactGenerationStatusPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestArtifactPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof CraftingMenu craftingMenu)) {
				return;
			}

			if (craftingMenu.containerId != payload.containerId()) {
				return;
			}

			Agmi.GENERATION_SERVICE.requestArtifact(craftingMenu, player);
		});
	}
}
