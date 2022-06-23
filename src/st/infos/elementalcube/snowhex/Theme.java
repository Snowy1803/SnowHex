package st.infos.elementalcube.snowhex;

import java.awt.Color;
import java.util.HashMap;

public class Theme {
	private HashMap<Integer, Format> map = new HashMap<>();
	
	public static final Theme DEFAULT;
	
	static {
		DEFAULT = new Theme();
		DEFAULT.put(TokenMaker.TOKEN_ERRORED, new Format(new Color(255, 0, 0), null, true));// Red + underline
		DEFAULT.put(TokenMaker.TOKEN_COMMENT, new Format(new Color(0, 127, 0), null, false));// Dark green
		DEFAULT.put(TokenMaker.TOKEN_FILE_HEADER, new Format(new Color(127, 0, 0), null, false));// Dark red
		DEFAULT.put(TokenMaker.TOKEN_CHUNK_HEADER, new Format(new Color(190, 0, 0), null, false));// Dark- red
		DEFAULT.put(TokenMaker.TOKEN_METADATA, new Format(new Color(0, 0, 190), null, false));// Dark blue
		DEFAULT.put(TokenMaker.TOKEN_RESERVED, new Format(new Color(127, 127, 127), null, false));// Gray
		DEFAULT.put(TokenMaker.TOKEN_LENGTH, new Format(new Color(255, 127, 0), null, false));// Orange
		DEFAULT.put(TokenMaker.TOKEN_IMAGE_PALETTE, new Format(null, new Color(190, 255, 190), false));// BACK Light green
		DEFAULT.put(TokenMaker.TOKEN_IMAGE_COLOR, new Format(new Color(127, 0, 127), null, false));// Purple
		DEFAULT.put(TokenMaker.TOKEN_IMAGE_DATA, new Format(null, new Color(255, 255, 190), false));// BACK Light yellow
		DEFAULT.put(TokenMaker.TOKEN_IMAGE_SIZE, new Format(new Color(0, 128, 128), null, false));// Teal
		DEFAULT.put(TokenMaker.TOKEN_CHUNK, new Format(null, new Color(190, 190, 255), false));// BACK Blue gray
	}
	
	public Format get(Integer key) {
		return map.get(key);
	}
	
	public Format put(Integer key, Format value) {
		return map.put(key, value);
	}
}
