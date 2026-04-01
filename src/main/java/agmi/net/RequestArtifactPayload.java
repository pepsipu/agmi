package agmi.net;

import agmi.Agmi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestArtifactPayload(int containerId) implements CustomPacketPayload {
	public static final Type<RequestArtifactPayload> TYPE = new Type<>(Agmi.id("request_artifact"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestArtifactPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		RequestArtifactPayload::containerId,
		RequestArtifactPayload::new
	);

	@Override
	public Type<RequestArtifactPayload> type() {
		return TYPE;
	}
}
