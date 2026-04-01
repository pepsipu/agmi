package agmi.item;

import agmi.util.ArtifactStackData;
import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class GeneratedArtifactBlockItem extends BlockItem {
	public GeneratedArtifactBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		return ArtifactStackData.tooltip(stack);
	}
}
