package st.infos.elementalcube.snowhex.parser;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;

public class ASN1TokenMaker extends TokenMaker {

	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		ByteBuffer buf = ByteBuffer.wrap(array);
		try {
			while (buf.hasRemaining()) {
				readChunk(list, buf);
			}
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, notice("ioob"), Level.ERROR));
		}
		return list;
	}
	
	// returns true for end of content 
	private boolean readChunk(ArrayList<Token> list, ByteBuffer buf) {
		int chunkstart = buf.position();
		byte id = buf.get();
		int tag = (id >> 6) & 0b11;
		boolean constructed = (id & (1 << 5)) != 0;
		long type = id & 0b11111;
		if (type == 31) { // long form
			type = 0;
			byte next;
			do {
				next = buf.get();
				type = (type << 7) | (next & 0b1111111);
			} while ((next & (1 << 7)) != 0);
		}
		list.add(createToken(TOKEN_CHUNK_HEADER, chunkstart, buf.position() - chunkstart,
				notice("id." + tag, notice("id.constructed." + constructed), type), Level.INFO));
		int lengthstart = buf.position();
		byte lengthb = buf.get();
		int length;
		boolean indefinite;
		if (lengthb < 0) {
			int rest = lengthb & 0b1111111;
			indefinite = rest == 0;
			length = 0;
			byte next;
			for (int i = 0; i < rest; i++) {
				next = buf.get();
				length = (length << 8) | (next & 0xff);
			}
		} else {
			length = lengthb;
			indefinite = false;
		}
		list.add(createToken(TOKEN_LENGTH, lengthstart, buf.position() - lengthstart, notice("length", length), Level.INFO));
		if (!constructed) { // primitive
			list.add(createToken(TOKEN_METADATA, buf.position(), length));
			buf.position(buf.position() + length);
			return id == 0;
		}
		int contentstart = buf.position();
		if (indefinite) {
			while (!readChunk(list, buf));
		} else {
			while (buf.position() < contentstart + length) {
				readChunk(list, buf);
			}
		}
		list.add(createToken(TOKEN_CHUNK, chunkstart, buf.position() - chunkstart));
		return false;
	}

	@Override
	public String getName() {
		return "asn1";
	}

	@Override
	public Object getDump(byte[] array) {
		return null;
	}
}
