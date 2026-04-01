package agmi.net;

import agmi.Agmi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ArtifactGenerationStatusPayload(int containerId, boolean generating) implements CustomPacketPayload {
	public static final Type<ArtifactGenerationStatusPayload> TYPE = new Type<>(Agmi.id("artifact_generation_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactGenerationStatusPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		ArtifactGenerationStatusPayload::containerId,
		ByteBufCodecs.BOOL,
		ArtifactGenerationStatusPayload::generating,
		ArtifactGenerationStatusPayload::new
	);

	@Override
	public Type<ArtifactGenerationStatusPayload> type() {
		return TYPE;
	}
}
