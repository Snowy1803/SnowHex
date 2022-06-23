package st.infos.elementalcube.snowhex;

import java.awt.Color;

public class Format {
	public static final Format DEFAULT = new Format(Color.BLACK, Color.WHITE, false);
	private final Color foreground;
	private final Color background;
	private final boolean underlined;
	
	public Format(Color foreground, Color background, boolean underline) {
		this.foreground = foreground;
		this.background = background;
		this.underlined = underline;
	}
	
	public Color getForeground() {
		return foreground;
	}
	
	public Color getBackground() {
		return background;
	}
	
	public boolean isUnderlined() {
		return underlined;
	}
	
	public Format combine(Format f) {
		return new Format(f.foreground == null ? foreground : f.foreground, f.background == null ? background : f.background,
				underlined || f.underlined);
	}
}
