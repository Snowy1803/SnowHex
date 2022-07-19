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
				f.foreground == null ? foreground : f.foreground,
				f.background == null ? background : f.background,
				f.squiggle == null ? squiggle : f.squiggle,
				f.strikethrough == null ? strikethrough : f.strikethrough);
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
