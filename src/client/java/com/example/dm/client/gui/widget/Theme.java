package com.example.dm.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared colors + drawing helpers for DriscolMod's custom "glassy" UI (teal accent).
 * Everything is drawn with plain fills/outlines so no textures are required.
 */
public final class Theme {
	// Accent (teal)
	public static final int ACCENT = 0xFF2FE0CE;
	public static final int ACCENT_DIM = 0xFF16897E;
	public static final int ACCENT_GLOW = 0x552FE0CE;

	// Panels / backgrounds
	public static final int BG_TOP = 0xF00B1017;
	public static final int BG_BOTTOM = 0xF0060A0F;
	public static final int PANEL_TOP = 0xF0161F29;
	public static final int PANEL_BOTTOM = 0xF00C131B;
	public static final int PANEL_BORDER = 0x40FFFFFF;

	// Fields / buttons
	public static final int FIELD_BG = 0x66000000;
	public static final int FIELD_BORDER = 0x33FFFFFF;
	public static final int BTN_BG = 0x1FFFFFFF;
	public static final int BTN_BG_HOVER = 0x402FE0CE;
	public static final int BTN_BG_PRIMARY = 0x282FE0CE;
	public static final int BTN_BG_DISABLED = 0x12FFFFFF;

	// Text
	public static final int TEXT = 0xFFECECEC;
	public static final int TEXT_MUTED = 0xFF8A96A0;
	public static final int TEXT_ACCENT = 0xFF2FE0CE;

	private Theme() {
	}

	/** Full-screen dark gradient background for standalone screens (hub, updates, about). */
	public static void screenBackground(GuiGraphicsExtractor graphics, int width, int height) {
		graphics.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
	}

	/** A glassy card panel with a border and a teal accent bar along the top edge. */
	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fillGradient(x, y, x + w, y + h, PANEL_TOP, PANEL_BOTTOM);
		graphics.outline(x, y, w, h, PANEL_BORDER);
		graphics.fill(x, y, x + w, y + 2, ACCENT);
	}

	/** A background field box (used behind borderless edit boxes). */
	public static void field(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean focused) {
		graphics.fill(x, y, x + w, y + h, FIELD_BG);
		graphics.outline(x, y, w, h, focused ? ACCENT : FIELD_BORDER);
	}

	/** Thin accent separator line. */
	public static void separator(GuiGraphicsExtractor graphics, int x, int y, int w) {
		graphics.fill(x, y, x + w, y + 1, 0x30FFFFFF);
	}
}
