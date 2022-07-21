package st.infos.elementalcube.snowhex;

import java.awt.Color;
import java.util.HashMap;

import st.infos.elementalcube.snowhex.Token.Level;

public class Theme implements TokenTypes {
	private HashMap<Integer, Format> map = new HashMap<>();
	private HashMap<Level, Format> levels = new HashMap<>();
	
	public static final Theme DEFAULT;
	
	static {
		DEFAULT = new Theme();
		DEFAULT.put(TOKEN_NONE, new Format(Color.BLACK, Color.WHITE));// Black on white
		DEFAULT.put(TOKEN_COMMENT, new Format(new Color(0, 127, 0), null));// Dark green
		DEFAULT.put(TOKEN_FILE_HEADER, new Format(new Color(127, 0, 0), null));// Dark red
		DEFAULT.put(TOKEN_CHUNK_HEADER, new Format(new Color(190, 0, 0), null));// Dark- red
		DEFAULT.put(TOKEN_METADATA, new Format(new Color(0, 0, 190), null));// Dark blue
		DEFAULT.put(TOKEN_RESERVED, new Format(new Color(127, 127, 127), null, null, new Color(127, 127, 127)));// Gray
		DEFAULT.put(TOKEN_LENGTH, new Format(new Color(255, 127, 0), null));// Orange
		DEFAULT.put(TOKEN_IMAGE_PALETTE, new Format(null, new Color(190, 255, 190)));// BACK Light green
		DEFAULT.put(TOKEN_IMAGE_COLOR, new Format(new Color(127, 0, 127), null));// Purple
		DEFAULT.put(TOKEN_IMAGE_DATA, new Format(null, new Color(255, 255, 190)));// BACK Light yellow
		DEFAULT.put(TOKEN_IMAGE_SIZE, new Format(new Color(0, 128, 128), null));// Teal
		DEFAULT.put(TOKEN_CHUNK, new Format(null, new Color(190, 190, 255)));// BACK Blue gray
		DEFAULT.put(TOKEN_COMPRESSED_DATA, new Format(new Color(127, 80, 50), null));// Brown
		DEFAULT.put(TOKEN_CHECKSUM, new Format(new Color(80, 100, 80), null));// Dark gray
		DEFAULT.put(TOKEN_KEYWORD, new Format(new Color(255, 64, 255), null));// Magenta
		DEFAULT.put(TOKEN_STRING, new Format(new Color(0x2A00FF), null));// Red
		DEFAULT.put(Level.ERROR, new Format(null, null, new Color(0xee1d25), null));// SQUIGGLE Red
		DEFAULT.put(Level.WARNING, new Format(null, null, new Color(0xff8000), null));// SQUIGGLE Yellow
	}
	
	public Format get(Integer key) {
		return map.get(key);
	}
	
	public Format put(Integer key, Format value) {
		return map.put(key, value);
	}
	
	public Format get(Level key) {
		return levels.get(key);
	}
	
	public Format put(Level key, Format value) {
		return levels.put(key, value);
	}
}
