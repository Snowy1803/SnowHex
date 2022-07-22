package st.infos.elementalcube.snowhex;

import java.awt.Color;

public class Format {
	private final Color foreground;
	private final Color background;
	private final Color squiggle;
	private final Color strikethrough;

	public Format(Color foreground, Color background, Color squiggle, Color strikethrough) {
		this.foreground = foreground;
		this.background = background;
		this.squiggle = squiggle;
		this.strikethrough = strikethrough;
	}
	
	public Format(Color foreground, Color background) {
		this(foreground, background, null, null);
	}
	
	public Color getForeground() {
		return foreground;
	}
	
	public Color getBackground() {
		return background;
	}
	
	public boolean isSquiggly() {
		return squiggle != null;
	}
	
	public Color getSquiggleColor() {
		return squiggle;
	}
	
	public boolean isStrikedThrough() {
		return strikethrough != null;
	}
	
	public Color getStrikeThroughColor() {
		return strikethrough;
	}
	
	public Format combine(Format f) {
		if (f == null)
			return this;
		return new Format(
				blend(foreground, f.foreground),
				blend(background, f.background),
				blend(squiggle, f.squiggle),
				blend(strikethrough, f.strikethrough));
	}
	
	private static Color blend(Color bottom, Color top) {
		if (top == null)
			return bottom;
		if (bottom == null)
			return top;
		float falpha = top.getAlpha() / 255f;
		float factor = falpha / 255f;
		float ifactor = (1 - falpha) / 255f;
		return new Color(
				top.getRed() * factor + bottom.getRed() * ifactor,
				top.getGreen() * factor + bottom.getGreen() * ifactor,
				top.getBlue() * factor + bottom.getBlue() * ifactor,
				falpha + bottom.getAlpha() * ifactor);
	}
	
	public Format faded() {
		return new Format(
				transparentColor(foreground),
				transparentColor(background.darker()),
				transparentColor(squiggle),
				transparentColor(strikethrough));
	}
	
	private static Color transparentColor(Color c) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), 127);
	}
}
