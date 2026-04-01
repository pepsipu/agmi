package agmi.item;

import agmi.codegen.ArtifactCodeExecutor;
import agmi.util.ArtifactStackData;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class GeneratedArtifactItem extends Item {
	public GeneratedArtifactItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand usedHand) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			ArtifactCodeExecutor.triggerUse(serverPlayer, player.getItemInHand(usedHand));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer serverPlayer) {
			ArtifactCodeExecutor.triggerUse(serverPlayer, context.getItemInHand());
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		ArtifactCodeExecutor.triggerHit(attacker, target, stack);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		return ArtifactStackData.tooltip(stack);
	}
}
