package st.infos.elementalcube.snowhex.parser.png;

import st.infos.elementalcube.snowhex.TokenImpl;

class PNGToken extends TokenImpl {
	static final int ERROR_CRC = 1;
	static final int PLTE_INDEX = 2;
	static final int COMPRESSED_IDAT = 3;
	static final int COMPRESSED_ZTXT = 4;
	
	int subtype;
	int expectedCrc;
	int index;
	
	@Override
	public void init(int type, int offset, int length, String tooltip, Level tooltipLevel) {
		super.init(type, offset, length, tooltip, tooltipLevel);
		subtype = 0;
		index = 0;
	}
	
	public PNGToken expectedCrc(int crc) {
		this.subtype = ERROR_CRC;
		this.expectedCrc = crc;
		return this;
	}
	
	public PNGToken withPLTEIndex(int index) {
		this.subtype = PLTE_INDEX;
		this.index = index;
		return this;
	}
	
	public PNGToken withIndex(int index) {
		this.index = index;
		return this;
	}
	
	public PNGToken withSubtype(int type) {
		this.subtype = type;
		return this;
	}
	
	@Override
	public int getIndex() {
		return index;
	}
	
	@Override
	public int getSubtype() {
		return subtype;
	}
}