package st.infos.elementalcube.snowhex;

import java.util.ArrayList;
import java.util.List;

public class Ant3TokenMaker extends TokenMaker {

	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> tokens = new ArrayList<>();
		int offset = 0;
		
		while (offset < array.length) {
			int head = array[offset++] & 0xff;
			boolean compressed = (head & 128) != 0;
			int size = head & 127;
			tokens.add(createToken(TOKEN_CHUNK_HEADER, offset - 1, 1));
			if (compressed) {
				if (size == 0) {
					tokens.add(createToken(TOKEN_IMAGE_PALETTE, offset - 1, 3));
					tokens.add(createToken(TOKEN_LENGTH, offset, 1));
					tokens.add(createToken(TOKEN_METADATA, offset + 1, 1));
					offset += 2;
				} else {
					tokens.add(createToken(TOKEN_IMAGE_DATA, offset - 1, size + 1));
					offset += size;
				}
			} else {
				tokens.add(createToken(TOKEN_CHUNK, offset - 1, size + 1));
				offset += size;
			}
		}
		return tokens;
	}

	@Override
	public String getName() {
		return "ant3";
	}

	@Override
	public Object getDump(byte[] array) {
		return null;
	}

}
