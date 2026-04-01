package agmi.ai;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

public final class ArtifactSpecSanitizer {
	private static final int MAX_SUMMARY_LENGTH = 180;
	private static final int MAX_TEXTURE_INPUT_BYTES = 8_000_000;
	private static final int MAX_JAVA_SOURCE_LENGTH = 48_000;
	private static final int OUTPUT_TEXTURE_SIZE = 16;

	private ArtifactSpecSanitizer() {
	}

	public static ArtifactSpec sanitize(ArtifactSpec raw, String signatureHash) {
		ArtifactSpec spec = raw == null ? new ArtifactSpec() : raw;
		ArtifactSpec sanitized = new ArtifactSpec();
		sanitized.id = ArtifactSpec.generatedIdForHash(signatureHash);
		sanitized.kind = ArtifactKind.fromString(spec.kind).serializedName();
		sanitized.rarity = spec.mcRarity().getSerializedName();
		sanitized.name = sanitizeName(spec.name, sanitized.id);
		sanitized.summary = clamp(spec.summary, MAX_SUMMARY_LENGTH);
		sanitized.itemTexturePrompt = clamp(spec.itemTexturePrompt(), 400);
		sanitized.blockTexturePrompt = clamp(spec.blockTexturePrompt(), 400);
		sanitized.texturePrompt = sanitized.itemTexturePrompt;
		sanitized.generatedJavaSource = clamp(spec.generatedJavaSource, MAX_JAVA_SOURCE_LENGTH);
		sanitized.itemTexturePngBase64 = sanitizeTexture(spec.itemTextureBase64());
		sanitized.blockTexturePngBase64 = sanitizeTexture(spec.blockTextureBase64());
		sanitized.texturePngBase64 = sanitized.itemTexturePngBase64;
		return sanitized;
	}

	private static String sanitizeName(String value, String fallbackId) {
		String trimmed = value == null ? "" : value.trim();
		if (!trimmed.isEmpty()) {
			return clamp(trimmed, 48);
		}

		return "Artifact " + fallbackId.substring(Math.max(0, fallbackId.length() - 6)).toUpperCase();
	}

	private static String sanitizeTexture(String value) {
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty()) {
			return "";
		}

		String base64 = trimmed;
		int commaIndex = trimmed.indexOf(',');
		if (trimmed.startsWith("data:") && commaIndex >= 0) {
			base64 = trimmed.substring(commaIndex + 1);
		}

		try {
			byte[] decoded = Base64.getDecoder().decode(base64);
			if (decoded.length > MAX_TEXTURE_INPUT_BYTES) {
				return "";
			}

			BufferedImage source = ImageIO.read(new ByteArrayInputStream(decoded));
			if (source == null) {
				return "";
			}

			BufferedImage scaled = new BufferedImage(OUTPUT_TEXTURE_SIZE, OUTPUT_TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = scaled.createGraphics();
			try {
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				graphics.drawImage(source, 0, 0, OUTPUT_TEXTURE_SIZE, OUTPUT_TEXTURE_SIZE, null);
			} finally {
				graphics.dispose();
			}

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(scaled, "png", output);
			return Base64.getEncoder().encodeToString(output.toByteArray());
		} catch (Exception ignored) {
			return "";
		}
	}

	private static String clamp(String value, int maxLength) {
		if (value == null) {
			return "";
		}

		String trimmed = value.trim();
		if (trimmed.length() <= maxLength) {
			return trimmed;
		}

		return trimmed.substring(0, maxLength);
	}
}
