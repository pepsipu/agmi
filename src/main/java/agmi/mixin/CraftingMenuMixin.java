package agmi.mixin;

import agmi.codegen.ArtifactCodeExecutor;
import agmi.util.ArtifactStackData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin extends AbstractCraftingMenu {
	protected CraftingMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
		super(menuType, containerId, width, height);
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void agmi$handleGeneratedQuickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index != 0) {
			return;
		}

		ItemStack stack = this.resultSlots.getItem(0);
		if (!ArtifactStackData.isGenerated(stack)) {
			return;
		}

		ItemStack output = stack.copy();
		if (!this.moveItemStackTo(stack, 10, 46, true)) {
			cir.setReturnValue(ItemStack.EMPTY);
			return;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			ArtifactCodeExecutor.triggerCraft(serverPlayer, output);
		}

		for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
			ItemStack ingredient = this.craftSlots.getItem(slot);
			if (!ingredient.isEmpty()) {
				this.craftSlots.removeItem(slot, 1);
			}
		}

		this.craftSlots.setChanged();
		this.resultSlots.setItem(0, ItemStack.EMPTY);
		this.slotsChanged(this.craftSlots);
		cir.setReturnValue(output);
	}
}
