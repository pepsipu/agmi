package agmi.client;

import agmi.block.GeneratedArtifactBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class GeneratedArtifactBlockRenderer implements BlockEntityRenderer<GeneratedArtifactBlockEntity, GeneratedArtifactBlockRenderState> {
	private static final int FULL_BRIGHT = 0x00F000F0;

	@Override
	public GeneratedArtifactBlockRenderState createRenderState() {
		return new GeneratedArtifactBlockRenderState();
	}

	@Override
	public void extractRenderState(
		GeneratedArtifactBlockEntity blockEntity,
		GeneratedArtifactBlockRenderState renderState,
		float tickProgress,
		net.minecraft.world.phys.Vec3 cameraPosition,
		net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress
	) {
		net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, renderState, breakProgress);
		renderState.textureId = ArtifactTextureCache.textureFor(blockEntity.spec().id + ":block", blockEntity.spec().blockTextureBase64());
	}

	@Override
	public void submit(
		GeneratedArtifactBlockRenderState renderState,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		CameraRenderState cameraRenderState
	) {
		submitNodeCollector.submitCustomGeometry(
			poseStack,
			RenderTypes.entityCutout(renderState.textureId),
			(pose, vertexConsumer) -> renderCube(pose, vertexConsumer, renderState.lightCoords)
		);
	}

	private static void renderCube(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light) {
		face(pose, vertexConsumer, FULL_BRIGHT, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
		face(pose, vertexConsumer, FULL_BRIGHT, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F);
		face(pose, vertexConsumer, FULL_BRIGHT, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, 0.0F);
		face(pose, vertexConsumer, FULL_BRIGHT, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F);
		face(pose, vertexConsumer, FULL_BRIGHT, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
		face(pose, vertexConsumer, FULL_BRIGHT, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F);
	}

	private static void face(
		PoseStack.Pose pose,
		VertexConsumer vertexConsumer,
		int light,
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
		vertex(vertexConsumer, pose, x1, y1, z1, 0.0F, 1.0F, normalX, normalY, normalZ, light);
		vertex(vertexConsumer, pose, x2, y2, z2, 1.0F, 1.0F, normalX, normalY, normalZ, light);
		vertex(vertexConsumer, pose, x3, y3, z3, 1.0F, 0.0F, normalX, normalY, normalZ, light);
		vertex(vertexConsumer, pose, x4, y4, z4, 0.0F, 0.0F, normalX, normalY, normalZ, light);
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
		int light
	) {
		vertexConsumer.addVertex(pose, x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, normalX, normalY, normalZ);
	}
}
