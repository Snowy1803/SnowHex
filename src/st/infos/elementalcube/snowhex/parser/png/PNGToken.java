package st.infos.elementalcube.snowhex.parser.png;

import st.infos.elementalcube.snowhex.TokenImpl;

class PNGToken extends TokenImpl {
	static final int ERROR_CRC = 1;
	
	int subtype;
	int expectedCrc;
	
	@Override
	public void init(int type, int offset, int length, String tooltip, Level tooltipLevel) {
		super.init(type, offset, length, tooltip, tooltipLevel);
		subtype = 0;
	}
	
	PNGToken expectedCrc(int crc) {
		this.subtype = ERROR_CRC;
		this.expectedCrc = crc;
		return this;
	}
}