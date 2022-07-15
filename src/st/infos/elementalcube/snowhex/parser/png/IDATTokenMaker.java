package st.infos.elementalcube.snowhex.parser.png;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;

// Note: don't add this class to the list of token makers, it depends on its parent
public class IDATTokenMaker extends TokenMaker {
	
	private PNGTokenMaker parent;
	private InflatedSubDocument doc;
	
	public IDATTokenMaker(PNGTokenMaker parent, InflatedSubDocument doc) {
		this.parent = parent;
		this.doc = doc;
	}

	@Override
	public List<Token> generateTokens(byte[] array) {
		ByteBuffer buf = ByteBuffer.wrap(array);
		ArrayList<Token> tokens = new ArrayList<>();
		for (int y = 0; y < parent.height; y++) {
			byte filter = buf.get();
			tokens.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, "Filter: " + filter, Level.INFO));
			if (parent.bitDepth == 8 || parent.bitDepth == 16) {
				// yay, byte boundary, can show more info
				int bytesPerPixel = getComponentCount() * (parent.bitDepth / 8);
				for (int x = 0; x < parent.width; x++) {
					tokens.add(createToken(TOKEN_IMAGE_COLOR, buf.position(), bytesPerPixel));
					buf.position(buf.position() + bytesPerPixel);
				}
			} else {
				// skip it
				int bits = parent.width * parent.bitDepth * getComponentCount();
				int bytes = (bits + 7) / 8; // ceil
				tokens.add(createToken(TOKEN_COMPRESSED_DATA, buf.position(), bytes));
				buf.position(buf.position() + bytes);
			}
		}
		return tokens;
	}

	@Override
	public String getName() {
		return "png.idat";
	}

	@Override
	public Object getDump(byte[] array) {
		return parent.getDump(doc.getParent().getBytes());
	}
	
	private int getComponentCount() {
		switch (parent.colorType) {
		case 0:
			return 1; // gray
		case 2:
			return 3; // rgb
		case 3:
			return 1; // palette index
		case 4:
			return 2; // gray + alpha
		case 6:
			return 4; // rgba
			default:
				return 0;
		}
	}
}
