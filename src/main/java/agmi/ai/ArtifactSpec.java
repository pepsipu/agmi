package agmi.ai;

import java.util.Locale;
import net.minecraft.world.item.Rarity;

public final class ArtifactSpec {
	public String id = "";
	public String name = "";
	public String kind = ArtifactKind.ITEM.serializedName();
	public String rarity = "rare";
	public String summary = "";
	public String itemTexturePrompt = "";
	public String blockTexturePrompt = "";
	public String texturePrompt = "";
	public String generatedJavaSource = "";
	public String itemTexturePngBase64 = "";
	public String blockTexturePngBase64 = "";
	public String texturePngBase64 = "";

	public ArtifactKind artifactKind() {
		return ArtifactKind.fromString(this.kind);
	}

	public String itemTexturePrompt() {
		if (!this.itemTexturePrompt.isBlank()) {
			return this.itemTexturePrompt;
		}

		return this.texturePrompt;
	}

	public String blockTexturePrompt() {
		if (!this.blockTexturePrompt.isBlank()) {
			return this.blockTexturePrompt;
		}

		if (!this.texturePrompt.isBlank()) {
			return this.texturePrompt;
		}

		return this.itemTexturePrompt;
	}

	public String itemTextureBase64() {
		if (!this.itemTexturePngBase64.isBlank()) {
			return this.itemTexturePngBase64;
		}

		return this.texturePngBase64;
	}

	public String blockTextureBase64() {
		if (!this.blockTexturePngBase64.isBlank()) {
			return this.blockTexturePngBase64;
		}

		if (!this.texturePngBase64.isBlank()) {
			return this.texturePngBase64;
		}

		return this.itemTexturePngBase64;
	}

	public boolean hasRequiredTextures() {
		if (this.artifactKind() == ArtifactKind.BLOCK) {
			return !this.itemTextureBase64().isBlank() && !this.blockTextureBase64().isBlank();
		}

		return !this.itemTextureBase64().isBlank();
	}

	public boolean hasExecutableSource() {
		return this.generatedJavaSource != null && !this.generatedJavaSource.isBlank();
	}

	public String generatedClassName() {
		String suffix = (this.id == null ? "" : this.id).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
		if (suffix.isBlank()) {
			suffix = "missing";
		}

		return "agmi.generated.Artifact_" + suffix;
	}

	public static String generatedIdForHash(String signatureHash) {
		return "artifact_" + signatureHash.substring(0, Math.min(12, signatureHash.length()));
	}

	public static String generatedClassNameForHash(String signatureHash) {
		return "agmi.generated.Artifact_" + generatedIdForHash(signatureHash).replaceAll("[^A-Za-z0-9_]", "_");
	}

	public Rarity mcRarity() {
		String normalized = this.rarity == null ? "" : this.rarity.trim().toLowerCase();
		return switch (normalized) {
			case "common" -> Rarity.COMMON;
			case "uncommon" -> Rarity.UNCOMMON;
			case "epic" -> Rarity.EPIC;
			default -> Rarity.RARE;
		};
	}
}
