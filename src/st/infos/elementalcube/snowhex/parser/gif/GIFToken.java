package st.infos.elementalcube.snowhex.parser.gif;

import st.infos.elementalcube.snowhex.TokenImpl;

class GIFToken extends TokenImpl {
	
	private int subtype;
	private int index;
	
	// IMAGE_SIZE
	static final int SUBTY_GLOBAL_SIZE = 1, SUBTY_SUB_POS = 2, SUBTY_SUB_SIZE = 3, SUBTY_CHAR_SIZE = 4;
	// METADATA
	static final int SUBTY_LSD_PACKED = 1, SUBTY_PAR = 2, SUBTY_GCE_PACKED = 3, SUBTY_GCE_DELAY = 4, SUBTY_APP = 5, SUBTY_IMG_PACKED = 6, SUBTY_LZW = 7;
	// IMAGE_COLOR
	static final int SUBTY_LSD_BG = 1, SUBTY_PALETTE_RGB = 2, SUBTY_INDEX_PALETTE = 3;
	
	@Override
	public void init(int type, int offset, int length, String tooltip, Level tooltipLevel) {
		super.init(type, offset, length, tooltip, tooltipLevel);
		subtype = 0;
	}
	
	public GIFToken withSubtype(int subtype) {
		this.subtype = subtype;
		return this;
	}
	
	public GIFToken withIndex(int index) {
		this.index = index;
		return this;
	}
	
	public int getSubtype() {
		return subtype;
	}
	
	public int getIndex() {
		return index;
	}
}