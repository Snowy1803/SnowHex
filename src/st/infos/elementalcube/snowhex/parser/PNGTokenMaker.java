package st.infos.elementalcube.snowhex.parser;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import javax.swing.ImageIcon;
import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.Token.Level;

public class PNGTokenMaker extends TokenMaker {
	private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
	
	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			if (!Arrays.equals(array, 0, 8, SIGNATURE, 0, 8)) {
				list.add(createToken(TOKEN_ERRORED, 0, 8, notice("header"), Level.ERROR));
				break gen;
			}
			list.add(createToken(TOKEN_FILE_HEADER, 0, 8));
			readChunks(list, array);
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			list.add(createToken(TOKEN_ERRORED, array.length - 1, 1, notice("ioob"), Level.ERROR));
		}
		return list;
	}
	
	private void readChunks(ArrayList<Token> list, byte[] array) {
		ByteBuffer buf = ByteBuffer.wrap(array);
		buf.position(8);
		buf.order(ByteOrder.BIG_ENDIAN);
		CRC32 crc = new CRC32();
		while (true) {
			int length = buf.getInt();
			list.add(createToken(TOKEN_LENGTH, buf.position() - 4, 4));
			crc.reset();
			crc.update(array, buf.position(), length + 4);
			int type = buf.getInt(); // let's use ints instead of byte[4]
			list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4));
			buf.position(buf.position() + length);
			int checksum = buf.getInt();
			if ((int) crc.getValue() != checksum) {
				list.add(createToken(TOKEN_ERRORED, buf.position() - 4, 4, notice("invalidCrc"), Level.ERROR));
			} else {
				list.add(createToken(TOKEN_RESERVED, buf.position() - 4, 4));
			}
			if (type == 0x49_45_4e_44) { // IEND
				return;
			}
		}
	}
	
	static String parseColor(byte[] array, int i) {
		return getHexString(Integer.toHexString(((array[i++] & 0xFF) << 16) | ((array[i++] & 0xFF) << 8) | ((array[i++] & 0xFF) << 0)), 6);
	}
	
	@Override
	public String getName() {
		return "png";
	}
	
	@Override
	public Object getDump(byte[] array) {
		return new ImageIcon(array).getImage();
	}
}
