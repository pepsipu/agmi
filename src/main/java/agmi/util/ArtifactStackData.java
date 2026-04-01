package agmi.util;

import agmi.Agmi;
import agmi.ai.ArtifactKind;
import agmi.ai.ArtifactSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public final class ArtifactStackData {
	private static final Identifier TEXTURED_ITEM_MODEL_ID = Agmi.id("generated/generated_artifact");
	private static final Identifier TEXTURED_BLOCK_ITEM_MODEL_ID = Agmi.id("generated/generated_artifact_block");
	private static final String TAG_ROOT = "agmi";
	private static final String TAG_ID = "id";
	private static final String TAG_NAME = "name";
	private static final String TAG_KIND = "kind";
	private static final String TAG_RARITY = "rarity";
	private static final String TAG_SUMMARY = "summary";
	private static final String TAG_JAVA_SOURCE = "java_source";
	private static final String TAG_JAVA_PREVIEW = "java_preview";
	private static final String TAG_ITEM_TEXTURE_PROMPT = "item_texture_prompt";
	private static final String TAG_BLOCK_TEXTURE_PROMPT = "block_texture_prompt";
	private static final String TAG_ITEM_TEXTURE_BASE64 = "item_texture_png_base64";
	private static final String TAG_BLOCK_TEXTURE_BASE64 = "block_texture_png_base64";
	private static final String TAG_TEXTURE_BASE64 = "texture_png_base64";

	private ArtifactStackData() {
	}

	public static ItemStack createStack(ArtifactSpec spec) {
		ItemStack stack = spec.artifactKind() == ArtifactKind.BLOCK
			? new ItemStack(Agmi.GENERATED_ARTIFACT_BLOCK_ITEM)
			: new ItemStack(Agmi.GENERATED_ARTIFACT_ITEM);

		CompoundTag root = new CompoundTag();
		root.putString(TAG_ID, spec.id);
		root.putString(TAG_NAME, spec.name);
		root.putString(TAG_KIND, spec.kind);
		root.putString(TAG_RARITY, spec.rarity);
		root.putString(TAG_SUMMARY, spec.summary);
		root.putString(TAG_ITEM_TEXTURE_PROMPT, spec.itemTexturePrompt());
		root.putString(TAG_BLOCK_TEXTURE_PROMPT, spec.blockTexturePrompt());
		root.putString(TAG_JAVA_SOURCE, spec.generatedJavaSource);
		root.putString(TAG_ITEM_TEXTURE_BASE64, spec.itemTextureBase64());
		root.putString(TAG_BLOCK_TEXTURE_BASE64, spec.blockTextureBase64());
		root.putString(TAG_TEXTURE_BASE64, spec.itemTextureBase64());

		CompoundTag container = new CompoundTag();
		container.put(TAG_ROOT, root);
		CustomData.set(DataComponents.CUSTOM_DATA, stack, container);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(spec.name));
		stack.set(DataComponents.RARITY, spec.mcRarity());
		List<Component> lore = defaultLore(spec);
		if (!lore.isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(lore));
		}
		if (!spec.itemTextureBase64().isBlank()) {
			stack.set(DataComponents.ITEM_MODEL, texturedModelId(spec));
		}

		if (spec.mcRarity() == net.minecraft.world.item.Rarity.EPIC) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}

		return stack;
	}

	public static ArtifactSpec read(ItemStack stack) {
		CompoundTag root = readRootTag(stack);
		if (root == null) {
			return null;
		}

		return readFromTag(root);
	}

	public static ArtifactSpec readFromTag(CompoundTag root) {
		if (root == null) {
			return null;
		}

		ArtifactSpec spec = new ArtifactSpec();
		spec.id = root.getStringOr(TAG_ID, "");
		spec.name = root.getStringOr(TAG_NAME, "");
		spec.kind = root.getStringOr(TAG_KIND, ArtifactKind.ITEM.serializedName());
		spec.rarity = root.getStringOr(TAG_RARITY, "rare");
		spec.summary = root.getStringOr(TAG_SUMMARY, "");
		spec.itemTexturePrompt = root.getStringOr(TAG_ITEM_TEXTURE_PROMPT, "");
		spec.blockTexturePrompt = root.getStringOr(TAG_BLOCK_TEXTURE_PROMPT, spec.itemTexturePrompt);
		spec.generatedJavaSource = root.getStringOr(TAG_JAVA_SOURCE, root.getStringOr(TAG_JAVA_PREVIEW, ""));
		spec.itemTexturePngBase64 = root.getStringOr(TAG_ITEM_TEXTURE_BASE64, root.getStringOr(TAG_TEXTURE_BASE64, ""));
		spec.blockTexturePngBase64 = root.getStringOr(TAG_BLOCK_TEXTURE_BASE64, spec.itemTexturePngBase64);
		spec.texturePrompt = spec.itemTexturePrompt;
		spec.texturePngBase64 = spec.itemTexturePngBase64;
		return spec;
	}

	public static CompoundTag writeRootTag(ArtifactSpec spec) {
		CompoundTag root = new CompoundTag();
		root.putString(TAG_ID, spec.id);
		root.putString(TAG_NAME, spec.name);
		root.putString(TAG_KIND, spec.kind);
		root.putString(TAG_RARITY, spec.rarity);
		root.putString(TAG_SUMMARY, spec.summary);
		root.putString(TAG_ITEM_TEXTURE_PROMPT, spec.itemTexturePrompt());
		root.putString(TAG_BLOCK_TEXTURE_PROMPT, spec.blockTexturePrompt());
		root.putString(TAG_JAVA_SOURCE, spec.generatedJavaSource);
		root.putString(TAG_ITEM_TEXTURE_BASE64, spec.itemTextureBase64());
		root.putString(TAG_BLOCK_TEXTURE_BASE64, spec.blockTextureBase64());
		root.putString(TAG_TEXTURE_BASE64, spec.itemTextureBase64());
		return root;
	}

	public static CompoundTag readRootTag(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null || customData.isEmpty()) {
			return null;
		}

		CompoundTag container = customData.copyTag();
		if (!container.contains(TAG_ROOT)) {
			return null;
		}

		return container.getCompoundOrEmpty(TAG_ROOT);
	}

	public static boolean isGenerated(ItemStack stack) {
		return readRootTag(stack) != null;
	}

	public static ItemStack emptyAwareCopy(ItemStack stack) {
		return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	public static java.util.Optional<TooltipComponent> tooltip(ItemStack stack) {
		ArtifactSpec spec = read(stack);
		if (spec == null || spec.itemTextureBase64().isBlank()) {
			return java.util.Optional.empty();
		}

		return java.util.Optional.of(new ArtifactTooltipData(spec.id + ":tooltip", spec.itemTextureBase64()));
	}

	public static Identifier texturedModelId(ArtifactSpec spec) {
		return spec.artifactKind() == ArtifactKind.BLOCK ? TEXTURED_BLOCK_ITEM_MODEL_ID : TEXTURED_ITEM_MODEL_ID;
	}

	public static Identifier texturedModelId(ItemStack stack) {
		ArtifactSpec spec = read(stack);
		if (spec == null) {
			return TEXTURED_ITEM_MODEL_ID;
		}

		return texturedModelId(spec);
	}

	private static List<Component> defaultLore(ArtifactSpec spec) {
		List<Component> lore = new ArrayList<>();
		if (!spec.summary.isBlank()) {
			lore.add(Component.literal(spec.summary));
		}
		return lore;
	}
}
