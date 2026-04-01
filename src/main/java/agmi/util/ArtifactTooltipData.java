package agmi.util;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record ArtifactTooltipData(String textureKey, String textureBase64) implements TooltipComponent {
}
