package st.infos.elementalcube.snowhex.parser;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;

public class SERTokenMaker extends TokenMaker {
	
	@Override
	public List<Token> generateTokens(byte[] array) {// Is GIF actually
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			int i = 0;
			if (array[i++] == -84 && array[i++] == -19) {
				list.add(createToken(TOKEN_FILE_HEADER, i - 2, 2));
			} else {
				list.add(createToken(TOKEN_FILE_HEADER, 0, 1, notice("header"), Level.ERROR));
				break gen;
			}
			list.add(createToken(TOKEN_IMAGE_SIZE, i, 2, notice("version", toShort(array[i++], array[i++])), Level.INFO));
			readBlocks(list, array, i);
		} catch (IndexOutOfBoundsException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, notice("ioob"), Level.ERROR));
		}
		return list;
	}
	
	private void readBlocks(ArrayList<Token> list, byte[] array, int i) {
		int intro = array[i++];
		switch (intro) {
		case 0x73:// OBJECT | TC_CLASSDESC
			i++;// 0x72
			i = readFieldName(list, array, i);
			break;
		case 0x49:// int | I
			i = readFieldName(list, array, i);
			break;
		default:
			list.add(createToken(TOKEN_CHUNK_HEADER, i - 1, 1, notice("block.unknown"), Level.ERROR));
			break;
		}
		if (array.length != i) {// Missing trailer?
			readBlocks(list, array, i);
		}
	}
	
	@SuppressWarnings("unused")
	private int readSubBlocks(ArrayList<Token> list, byte[] array, int i, int token) {
		int len = array[i++];
		if (len == 0) {
			list.add(createToken(TOKEN_RESERVED, i - 1, 1));
			return i;
		}
		list.add(createToken(TOKEN_LENGTH, i - 1, 1));
		if (token >= 0) list.add(createToken(token, i, len));
		i += len;
		return readSubBlocks(list, array, i, token);
	}
	
	private int readFieldName(ArrayList<Token> list, byte[] array, int i) {
		int lenght = toShort(array[i++], array[i++]);
		System.out.println(lenght);
		list.add(createToken(TOKEN_LENGTH, i - 2, 2));
		String s = new String(Arrays.copyOfRange(array, i, i + lenght));
		list.add(createToken(TOKEN_IMAGE_DATA, i - lenght, lenght, notice("fieldname", s), Level.INFO));
		return i + lenght;
	}
	
	private static short toShort(byte b, byte c) {
		return toShort(b, c, ByteOrder.BIG_ENDIAN);
	}
	
	@Override
	public String getName() {
		return "ser";
	}
	
	@Override
	public Object getDump(byte[] array) {
		return null;
	}
}
