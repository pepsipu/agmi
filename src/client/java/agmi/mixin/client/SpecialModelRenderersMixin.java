package agmi.mixin.client;

import agmi.Agmi;
import agmi.client.GeneratedArtifactItemSpecialRenderer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpecialModelRenderers.class)
public abstract class SpecialModelRenderersMixin {
	@Shadow
	@Final
	private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER;

	private static boolean agmi$registered;

	@Inject(method = "bootstrap", at = @At("TAIL"))
	private static void agmi$registerRenderer(CallbackInfo ci) {
		if (agmi$registered) {
			return;
		}

		ID_MAPPER.put(Agmi.id("generated_artifact"), GeneratedArtifactItemSpecialRenderer.Unbaked.MAP_CODEC);
		agmi$registered = true;
	}
}
