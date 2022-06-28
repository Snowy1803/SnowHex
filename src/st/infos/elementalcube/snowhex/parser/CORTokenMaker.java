package st.infos.elementalcube.snowhex.parser;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.Token.Level;

public class CORTokenMaker extends TokenMaker {

	static final String OPNAMES[] = {
			"live",
		    "ld",
		    "st",
		    "add",
		    "sub",
		    "and",
		    "or",
		    "xor",
		    "zjmp",
		    "ldi",
		    "sti",
		    "fork",
		    "lld",
		    "lldi",
		    "lfork",
		    "aff"
	};
	
	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> tokens = new ArrayList<>();
		tokens.add(createToken(TOKEN_FILE_HEADER, 0, 4, "Magic number", Level.INFO));
		tokens.add(createToken(TOKEN_COMMENT, 4, 128 + 4, "Program name: " + new String(array, 4, 128 + 4), Level.INFO));
		tokens.add(createToken(TOKEN_LENGTH, 4 + 128 + 4, 4, "Program size: " + ByteBuffer.wrap(array, 4 + 128 + 4, 4).getInt() + " bytes", Level.INFO));
		tokens.add(createToken(TOKEN_COMMENT, 4 + 128 + 4 + 4, 2048 + 4, "Program comment: " + new String(array, 4 + 128 + 4 + 4, 2048 + 4), Level.INFO));
		int offset = 2192;
		while (offset < array.length) {
			if (array[offset] <= 0 || array[offset] > OPNAMES.length) {
				tokens.add(createToken(TOKEN_ERRORED, offset, 1, "Invalid opcode", Level.ERROR));
				offset++;
				continue;
			}
			byte opcode = array[offset];
			tokens.add(createToken(TOKEN_CHUNK_HEADER, offset, 1, OPNAMES[opcode - 1], Level.INFO));
			offset++;
			byte coding;
			switch (opcode) {
			default:
				String s = "";
				coding = array[offset];
				for (int i = 0; i < 4; i++) {
					switch (coding >>> (6 - 2 * i) & 0b11) {
					case 0b01:
						s += "Argument #" + i + " is a register<br>";
						break;
					case 0b10:
						s += "Argument #" + i + " is direct<br>";
						break;
					case 0b11:
						s += "Argument #" + i + " is indirect<br>";
						break;
					}
				}
				tokens.add(createToken(TOKEN_LENGTH, offset, 1, s, Level.INFO));
				offset++;
				break;
			case 1:
			case 9:
			case 12:
			case 15:
				coding = (byte) 0b10000000;
			}
			int parstart = offset;
			for (int i = 0; i < 4; i++) {
				switch (coding >>> (6 - 2 * i) & 0b11) {
				case 0b01:
					tokens.add(createToken(TOKEN_IMAGE_COLOR, offset, 1, "Register r" + array[offset], Level.INFO));
					offset += 1;
					break;
				case 0b10:
					switch (opcode) {
					case 9:
					case 10:
					case 11:
					case 12:
					case 14:
					case 15:
						tokens.add(createToken(TOKEN_IMAGE_SIZE, offset, 2, "Direct: " + ByteBuffer.wrap(array, offset, 2).getShort(), Level.INFO));
						offset += 2;
						break;
					default:
						tokens.add(createToken(TOKEN_IMAGE_SIZE, offset, 4, "Direct: " + ByteBuffer.wrap(array, offset, 4).getInt(), Level.INFO));
						offset += 4;
						break;
					}
					break;
				case 0b11:
					tokens.add(createToken(TOKEN_METADATA, offset, 2, "Indirect: " + ByteBuffer.wrap(array, offset, 2).getShort(), Level.INFO));
					offset += 2;
					break;
				}
			}
			tokens.add(createToken(TOKEN_CHUNK, parstart, offset - parstart));
		}
		return tokens;
	}

	@Override
	public String getName() {
		return "cor";
	}

	@Override
	public Object getDump(byte[] array) {
		return null;
	}

}
