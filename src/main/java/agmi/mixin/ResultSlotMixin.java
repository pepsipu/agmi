package agmi.mixin;

import agmi.codegen.ArtifactCodeExecutor;
import agmi.util.ArtifactStackData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
	@Shadow
	@Final
	private CraftingContainer craftSlots;

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void agmi$handleGeneratedTake(Player player, ItemStack stack, CallbackInfo ci) {
		if (!(player instanceof ServerPlayer serverPlayer) || !ArtifactStackData.isGenerated(stack)) {
			return;
		}

		ArtifactCodeExecutor.triggerCraft(serverPlayer, stack.copy());

		for (int slot = 0; slot < this.craftSlots.getContainerSize(); slot++) {
			ItemStack ingredient = this.craftSlots.getItem(slot);
			if (!ingredient.isEmpty()) {
				this.craftSlots.removeItem(slot, 1);
			}
		}

		this.craftSlots.setChanged();
		player.containerMenu.slotsChanged(this.craftSlots);
		ci.cancel();
	}
}
