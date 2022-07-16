package st.infos.elementalcube.snowhex.parser.png;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.ByteSelection;
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
		int bytesPerPixel = getComponentCount() * (parent.bitDepth / 8);
		byte[] currRow = new byte[bytesPerPixel * parent.width];
		byte[] prevRow = new byte[bytesPerPixel * parent.width];
		ArrayList<Token> tokens = new ArrayList<>();
		for (int y = 0; y < parent.height; y++) {
			byte filter = buf.get();
			tokens.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, "Filter: " + filter, Level.INFO));
			if (parent.bitDepth == 8 || parent.bitDepth == 16) { // byte boundary, can show more info
				int rowStart = buf.position();
				buf.get(currRow);
				filterRow(currRow, y == 0 ? null : prevRow, filter, bytesPerPixel * parent.width, bytesPerPixel);
				for (int x = 0; x < parent.width; x++) {
					// We show # then hex data even for a palette index...
					tokens.add(createToken(TOKEN_IMAGE_COLOR, rowStart + x * bytesPerPixel, bytesPerPixel,
							"Color: #" + new ByteSelection(ArrayUtils.subarray(currRow, x * bytesPerPixel, (x + 1) * bytesPerPixel)), Level.INFO));
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

	private void filterRow(byte rowByteBuffer[], byte[] prevRow, int rowFilter, int rowByteWidth, int bytesPerSample) {
		int x = 0;
		switch (rowFilter) {
		case 0:
			break;
		case 1:
			for (x = bytesPerSample; x < rowByteWidth; x++)
				rowByteBuffer[x] += rowByteBuffer[x - bytesPerSample];
			break;
		case 2:
			if (prevRow != null)
				for (; x < rowByteWidth; x++)
					rowByteBuffer[x] += prevRow[x];
			break;
		case 3:
			if (prevRow != null) {
				for (; x < bytesPerSample; x++)
					rowByteBuffer[x] += (0xff & prevRow[x]) >> 1;
				for (; x < rowByteWidth; x++)
					rowByteBuffer[x] += ((prevRow[x] & 0xFF) + (rowByteBuffer[x - bytesPerSample] & 0xFF)) >> 1;
			} else
				for (x = bytesPerSample; x < rowByteWidth; x++)
					rowByteBuffer[x] += (rowByteBuffer[x - bytesPerSample] & 0xFF) >> 1;
			break;
		case 4:
			if (prevRow != null) {
				for (; x < bytesPerSample; x++)
					rowByteBuffer[x] += prevRow[x];
				for (; x < rowByteWidth; x++) {
					int a, b, c, p, pa, pb, pc;
					a = rowByteBuffer[x - bytesPerSample] & 0xFF;
					b = prevRow[x] & 0xFF;
					c = prevRow[x - bytesPerSample] & 0xFF;
					p = a + b - c;
					pa = p > a ? p - a : a - p;
					pb = p > b ? p - b : b - p;
					pc = p > c ? p - c : c - p;
					rowByteBuffer[x] += (pa <= pb) && (pa <= pc) ? a : pb <= pc ? b : c;
				}
			} else
				for (x = bytesPerSample; x < rowByteWidth; x++)
					rowByteBuffer[x] += rowByteBuffer[x - bytesPerSample];
			break;
		default:
			break;
		}
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
