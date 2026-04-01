package agmi.client;

import agmi.util.ArtifactTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public final class ArtifactClientTooltipComponent implements ClientTooltipComponent {
	private static final int PADDING = 2;
	private static final int PREVIEW_SIZE = 48;

	private final ArtifactTooltipData tooltipData;

	public ArtifactClientTooltipComponent(ArtifactTooltipData tooltipData) {
		this.tooltipData = tooltipData;
	}

	@Override
	public int getHeight(Font font) {
		return PREVIEW_SIZE + PADDING * 2;
	}

	@Override
	public int getWidth(Font font) {
		return PREVIEW_SIZE + PADDING * 2;
	}

	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
		Identifier texture = ArtifactTextureCache.textureFor(this.tooltipData);
		graphics.fill(x, y, x + getWidth(font), y + getHeight(font), 0xCC101318);
		graphics.outline(x, y, getWidth(font), getHeight(font), 0xFF5E6978);
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			texture == null ? TextureManager.INTENTIONAL_MISSING_TEXTURE : texture,
			x + PADDING,
			y + PADDING,
			0.0F,
			0.0F,
			PREVIEW_SIZE,
			PREVIEW_SIZE,
			PREVIEW_SIZE,
			PREVIEW_SIZE
		);
	}
}
