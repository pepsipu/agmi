package agmi.client;

import agmi.Agmi;
import agmi.util.ArtifactTooltipData;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public final class ArtifactTextureCache {
	private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();

	private ArtifactTextureCache() {
	}

	public static Identifier textureFor(ArtifactTooltipData tooltipData) {
		return textureFor(tooltipData.textureKey(), tooltipData.textureBase64());
	}

	public static Identifier textureFor(String specId, String textureBase64) {
		if (textureBase64 == null || textureBase64.isBlank()) {
			return TextureManager.INTENTIONAL_MISSING_TEXTURE;
		}

		String cacheKey = specId + ":" + Integer.toHexString(textureBase64.hashCode());
		return TEXTURES.computeIfAbsent(cacheKey, key -> loadTexture(specId, textureBase64, key));
	}

	private static Identifier loadTexture(String specId, String textureBase64, String cacheKey) {
		try {
			byte[] pngBytes = Base64.getDecoder().decode(textureBase64);
			NativeImage image = NativeImage.read(pngBytes);
			Identifier textureId = Agmi.id("dynamic/" + Integer.toHexString(cacheKey.hashCode()));
			DynamicTexture texture = new DynamicTexture(() -> "agmi/" + cacheKey, image);
			TextureManager textureManager = Minecraft.getInstance().getTextureManager();
			textureManager.register(textureId, texture);
			texture.upload();
			return textureId;
		} catch (IllegalArgumentException | IOException exception) {
			Agmi.LOGGER.warn("Failed to decode generated artifact texture {}", specId, exception);
			return TextureManager.INTENTIONAL_MISSING_TEXTURE;
		}
	}
}
