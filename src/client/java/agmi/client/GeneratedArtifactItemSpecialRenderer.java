package agmi.client;

import agmi.Agmi;
import agmi.util.ArtifactStackData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GeneratedArtifactItemSpecialRenderer implements SpecialModelRenderer<GeneratedArtifactItemSpecialRenderer.RenderData> {
	private static final float DEPTH = 1.0F / 16.0F;
	private static final Vector3fc MIN_EXTENT = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final Vector3fc MAX_EXTENT = new Vector3f(1.0F, 1.0F, DEPTH);

	@Override
	public void submit(RenderData renderData, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, boolean glint, int color) {
		Identifier textureId = renderData == null ? TextureManager.INTENTIONAL_MISSING_TEXTURE : renderData.textureId();
		submitNodeCollector.submitCustomGeometry(
			poseStack,
			RenderTypes.itemTranslucent(textureId),
			(pose, vertexConsumer) -> renderItemPlate(pose, vertexConsumer, light, overlay)
		);
	}

	@Override
	public void getExtents(java.util.function.Consumer<Vector3fc> consumer) {
		consumer.accept(MIN_EXTENT);
		consumer.accept(MAX_EXTENT);
	}

	@Override
	public RenderData extractArgument(ItemStack stack) {
		var spec = ArtifactStackData.read(stack);
		if (spec == null) {
			return new RenderData(TextureManager.INTENTIONAL_MISSING_TEXTURE);
		}

		return new RenderData(ArtifactTextureCache.textureFor(spec.id + ":item", spec.itemTextureBase64()));
	}

	private static void renderItemPlate(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay) {
		face(pose, vertexConsumer, light, overlay, 0.0F, 0.0F, DEPTH, 1.0F, 0.0F, DEPTH, 1.0F, 1.0F, DEPTH, 0.0F, 1.0F, DEPTH, 0.0F, 0.0F, 1.0F);
		face(pose, vertexConsumer, light, overlay, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F);
		face(pose, vertexConsumer, light, overlay, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, DEPTH, 0.0F, 1.0F, DEPTH, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, 0.0F);
		face(pose, vertexConsumer, light, overlay, 1.0F, 0.0F, DEPTH, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, DEPTH, 1.0F, 0.0F, 0.0F);
		face(pose, vertexConsumer, light, overlay, 0.0F, 1.0F, DEPTH, 1.0F, 1.0F, DEPTH, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
		face(pose, vertexConsumer, light, overlay, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, DEPTH, 0.0F, 0.0F, DEPTH, 0.0F, -1.0F, 0.0F);
	}

	private static void face(
		PoseStack.Pose pose,
		VertexConsumer vertexConsumer,
		int light,
		int overlay,
		float x1,
		float y1,
		float z1,
		float x2,
		float y2,
		float z2,
		float x3,
		float y3,
		float z3,
		float x4,
		float y4,
		float z4,
		float normalX,
		float normalY,
		float normalZ
	) {
		vertex(vertexConsumer, pose, x1, y1, z1, 0.0F, 1.0F, normalX, normalY, normalZ, light, overlay);
		vertex(vertexConsumer, pose, x2, y2, z2, 1.0F, 1.0F, normalX, normalY, normalZ, light, overlay);
		vertex(vertexConsumer, pose, x3, y3, z3, 1.0F, 0.0F, normalX, normalY, normalZ, light, overlay);
		vertex(vertexConsumer, pose, x4, y4, z4, 0.0F, 0.0F, normalX, normalY, normalZ, light, overlay);
	}

	private static void vertex(
		VertexConsumer vertexConsumer,
		PoseStack.Pose pose,
		float x,
		float y,
		float z,
		float u,
		float v,
		float normalX,
		float normalY,
		float normalZ,
		int light,
		int overlay
	) {
		vertexConsumer.addVertex(pose, x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(overlay)
			.setLight(light)
			.setNormal(pose, normalX, normalY, normalZ);
	}

	public record RenderData(Identifier textureId) {
	}

	public enum Unbaked implements SpecialModelRenderer.Unbaked<RenderData> {
		INSTANCE;

		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

		@Override
		public SpecialModelRenderer<RenderData> bake(SpecialModelRenderer.BakingContext context) {
			return new GeneratedArtifactItemSpecialRenderer();
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}
}
