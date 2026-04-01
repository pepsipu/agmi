package agmi;

import agmi.client.ArtifactClientTooltipComponent;
import agmi.client.CraftingGenerationUi;
import agmi.client.GeneratedArtifactBlockRenderer;
import agmi.util.ArtifactTooltipData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class AgmiClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTooltipComponentCallback.EVENT.register(component -> {
			if (component instanceof ArtifactTooltipData tooltipData) {
				return new ArtifactClientTooltipComponent(tooltipData);
			}

			return null;
		});
		CraftingGenerationUi.init();
		BlockEntityRenderers.register(Agmi.GENERATED_ARTIFACT_BLOCK_ENTITY, context -> new GeneratedArtifactBlockRenderer());
	}
}
