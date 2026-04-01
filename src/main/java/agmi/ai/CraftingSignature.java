package agmi.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CraftingSignature {
	private CraftingSignature() {
	}

	public static boolean isEmpty(CraftingContainer craftingContainer) {
		for (int index = 0; index < craftingContainer.getContainerSize(); index++) {
			if (!craftingContainer.getItem(index).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	public static String rawSignature(CraftingContainer craftingContainer) {
		StringBuilder builder = new StringBuilder();

		for (int index = 0; index < craftingContainer.getContainerSize(); index++) {
			if (index > 0) {
				builder.append('|');
			}

			ItemStack stack = craftingContainer.getItem(index);
			if (stack.isEmpty()) {
				builder.append("empty");
				continue;
			}

			builder.append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
			builder.append('#').append(stack.getCount());

			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null && !customData.isEmpty()) {
				builder.append('@').append(customData.copyTag());
			}
		}

		return builder.toString();
	}

	public static String hash(String signature) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(signature.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (byte current : bytes) {
				builder.append(String.format("%02x", current));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Missing SHA-256", exception);
		}
	}

	public static String promptDescription(CraftingContainer craftingContainer) {
		StringBuilder builder = new StringBuilder();
		int width = craftingContainer.getWidth();
		int height = craftingContainer.getHeight();

		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				int slot = column + row * width;
				ItemStack stack = craftingContainer.getItem(slot);
				builder.append('[').append(row).append(',').append(column).append("] ");

				if (stack.isEmpty()) {
					builder.append("empty");
				} else {
					builder.append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
					builder.append(" x").append(stack.getCount());
					builder.append(" named \"").append(stack.getHoverName().getString()).append('"');
				}

				builder.append('\n');
			}
		}

		return builder.toString().trim();
	}
}
