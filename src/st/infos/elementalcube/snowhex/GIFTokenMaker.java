package st.infos.elementalcube.snowhex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import st.infos.elementalcube.snowhex.Token.Level;

public class GIFTokenMaker extends TokenMaker {
	
	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			int i = 0;
			byte ver;
			if (array[i++] == 0x47 && array[i++] == 0x49 && array[i++] == 0x46 && array[i++] == 0x38 && ((ver = array[i++]) == 0x39 || ver == 0x37)
					&& array[i++] == 0x61) {
				list.add(createToken(TOKEN_FILE_HEADER, i - 6, 6));
			} else {
				list.add(createToken(TOKEN_ERRORED, 0, 1, notice("header"), Level.ERROR));
				break gen;
			}
			int w = toShort(array[i++], array[i++]);
			int h = toShort(array[i++], array[i++]);
			list.add(createToken(TOKEN_IMAGE_SIZE, i - 4, 4, notice("size", w, h), Level.INFO));
			byte packedFields = array[i++];
			boolean globalColorTableFlag = (packedFields & 0x80) != 0;
			int numGCTEntries = 1 << ((packedFields & 0x7) + 1);
			list.add(createToken(TOKEN_METADATA, i - 1, 1, notice("globalColorTable." + globalColorTableFlag) + "<br/>" + notice("colorRes",
					((packedFields >> 4) & 0x7) + 1) + "<br/>" + notice("sort." + ((packedFields & 0x8) != 0)) + "<br/>" + notice("gctSize",
							numGCTEntries), Level.INFO));
			int bci = array[i++];// Background Color Index
			list.add(createToken(TOKEN_IMAGE_COLOR, i - 1, 1, notice("bci", bci, globalColorTableFlag ? getHexString(Integer.toHexString(getBCI(array,
					i + 2, bci)), 6) : "No GCT"), Level.INFO));
			int par = array[i++];// Pixel Aspect Ratio
			list.add(createToken(TOKEN_METADATA, i - 1, 1, par == 0 ? notice("par.false") : notice("par", (par + 15) / 64), Level.INFO));
			if (globalColorTableFlag) {
				for (int j = 0; j < numGCTEntries; j++) {
					list.add(createToken(TOKEN_IMAGE_COLOR, i + j * 3, 3, notice("color", parseColor(array, i + j * 3)), Level.INFO));
				}
				list.add(createToken(TOKEN_IMAGE_PALETTE, i, numGCTEntries * 3));
				i += numGCTEntries * 3;
			}
			readBlocks(list, array, i);
		} catch (IndexOutOfBoundsException e) {
			list.add(createToken(TOKEN_ERRORED, array.length - 1, 1, notice("ioob"), Level.ERROR));
		}
		return list;
	}
	
	private void readBlocks(ArrayList<Token> list, byte[] array, int i) {
		int intro = array[i++];
		switch (intro) {
		case 0x21:
			// Ext
			byte ext = array[i++];
			switch (ext) {
			case 1:
				int extStart = i - 2;
				list.add(createToken(TOKEN_CHUNK_HEADER, i - 2, 2, notice("ext.header", "Plain Text"), Level.INFO));
				list.add(createToken(TOKEN_LENGTH, i, 1, notice("block.length", array[i++]), Level.INFO));
				list.add(createToken(TOKEN_IMAGE_SIZE, i, 4, notice("pos", toShort(array[i++], array[i++]), toShort(array[i++], array[i++])),
						Level.INFO));
				list.add(createToken(TOKEN_IMAGE_SIZE, i, 4, notice("size", toShort(array[i++], array[i++]), toShort(array[i++], array[i++])),
						Level.INFO));
				list.add(createToken(TOKEN_IMAGE_SIZE, i, 2, notice("charSize", array[i++], array[i++]), Level.INFO));
				list.add(createToken(TOKEN_IMAGE_COLOR, i, 1, notice("ext.text.fore", array[i++] & 0xFF), Level.INFO));
				list.add(createToken(TOKEN_IMAGE_COLOR, i, 1, notice("ext.text.back", array[i++] & 0xFF), Level.INFO));
				i = readSubBlocks(list, array, i, -1);
				list.add(createToken(TOKEN_CHUNK, extStart, i - extStart));
				break;
			case -7:
				list.add(createToken(TOKEN_CHUNK, i - 2, 8));
				list.add(createToken(TOKEN_CHUNK_HEADER, i - 2, 2, notice("ext.header", "Graphic Control"), Level.INFO));
				list.add(createToken(TOKEN_LENGTH, i, 1, notice("block.length", array[i++]), Level.INFO));
				int packedFields = array[i++];
				list.add(createToken(TOKEN_METADATA, i - 1, 1, notice("ext.gce.disposal." + ((packedFields >> 2) & 0x3)) + "<br/>" + notice(
						"ext.gce.userInput." + ((packedFields & 0x2) != 0)) + "<br/>" + notice("ext.gce.transparentColor." + ((packedFields
								& 0x1) != 0)), Level.INFO));
				list.add(createToken(TOKEN_METADATA, i, 2, notice("ext.gce.delay", toShort(array[i++], array[i++]) / 10F), Level.INFO));
				list.add(createToken(TOKEN_METADATA, i, 1, notice("ext.gce.transparentColor", Byte.toUnsignedInt(array[i++])), Level.INFO));
				byte terminator = array[i++];
				if (terminator != 0) {
					list.add(createToken(TOKEN_ERRORED, i - 1, 1, notice("block.noterminator"), Level.ERROR));
				} else list.add(createToken(TOKEN_RESERVED, i - 1, 1));
				break;
			case -2:
				extStart = i - 2;
				list.add(createToken(TOKEN_CHUNK_HEADER, i - 2, 2, notice("ext.header", "Comment"), Level.INFO));
				i = readSubBlocks(list, array, i, TOKEN_COMMENT);
				list.add(createToken(TOKEN_CHUNK, extStart, i - extStart));
				break;
			case -1:
				extStart = i - 2;
				list.add(createToken(TOKEN_CHUNK_HEADER, i - 2, 2, notice("ext.header", "Application"), Level.INFO));
				list.add(createToken(TOKEN_LENGTH, i, 1, notice("block.length", array[i++]), Level.INFO));
				list.add(createToken(TOKEN_METADATA, i, 8, notice("ext.app", new String(new char[] { (char) array[i++], (char) array[i++],
						(char) array[i++], (char) array[i++], (char) array[i++], (char) array[i++], (char) array[i++], (char) array[i++] })),
						Level.INFO));
				list.add(createToken(TOKEN_METADATA, i, 3, notice("ext.app.version", new String(new char[] { (char) array[i++], (char) array[i++],
						(char) array[i++] })), Level.INFO));
				i = readSubBlocks(list, array, i, -1);
				list.add(createToken(TOKEN_CHUNK, extStart, i - extStart));
				break;
			default:
				list.add(createToken(TOKEN_ERRORED, i - 2, 2, notice("block.unknown"), Level.ERROR));
				break;
			}
			break;
		case 0x2C:
			// Img
			int imgStart = i - 1;
			list.add(createToken(TOKEN_CHUNK_HEADER, i - 1, 1, notice("block.img"), Level.INFO));
			int x = toShort(array[i++], array[i++]);
			int y = toShort(array[i++], array[i++]);
			list.add(createToken(TOKEN_IMAGE_SIZE, i - 4, 4, notice("pos", x, y), Level.INFO));
			int w = toShort(array[i++], array[i++]);
			int h = toShort(array[i++], array[i++]);
			list.add(createToken(TOKEN_IMAGE_SIZE, i - 4, 4, notice("size", w, h), Level.INFO));
			int packedFields = array[i++];
			boolean lct = (packedFields & 0x80) != 0;
			int numLCTEntries = 1 << ((packedFields & 0x7) + 1);
			list.add(createToken(TOKEN_METADATA, i - 1, 1, notice("localColorTable." + lct) + "<br/>" + notice("interlace." + ((packedFields
					& 0x40) != 0)) + "<br/>" + notice("sort." + ((packedFields & 0x20) != 0)) + "<br/>" + notice("lctSize", lct ? numLCTEntries : 0),
					Level.INFO));
			if (lct) {
				for (int j = 0; j < numLCTEntries; j++) {
					list.add(createToken(TOKEN_IMAGE_COLOR, i + j * 3, 3, notice("color", parseColor(array, i + j * 3)), Level.INFO));
				}
				list.add(createToken(TOKEN_IMAGE_PALETTE, i, numLCTEntries * 3));
				i += numLCTEntries * 3;
			}
			list.add(createToken(TOKEN_METADATA, i++, 1));
			i = readSubBlocks(list, array, i, TOKEN_IMAGE_COLOR);
			list.add(createToken(TOKEN_IMAGE_DATA, imgStart, i - imgStart));
			break;
		case 0x3b:
			list.add(createToken(TOKEN_CHUNK_HEADER, i - 1, 1));
			return;
		default:
			list.add(createToken(TOKEN_ERRORED, i - 1, 1, notice("block.unknown"), Level.ERROR));
			break;
		}
		if (array.length != i) {// Missing trailer?
			readBlocks(list, array, i);
		}
	}
	
	private int readSubBlocks(ArrayList<Token> list, byte[] array, int i, int token) {
		while (true) {
			int len = Byte.toUnsignedInt(array[i++]);
			if (len == 0) {
				list.add(createToken(TOKEN_RESERVED, i - 1, 1));
				return i;
			}
			list.add(createToken(TOKEN_LENGTH, i - 1, 1));
			if (token >= 0) list.add(createToken(token, i, len));
			i += len;
		}
	}
	
	private static String parseColor(byte[] array, int i) {
		return getHexString(Integer.toHexString(((array[i++] & 0xFF) << 16) | ((array[i++] & 0xFF) << 8) | ((array[i++] & 0xFF) << 0)), 6);
	}
	
	private static int getBCI(byte[] array, int i, int bci) {
		return (Byte.toUnsignedInt(array[i + (bci * 3)]) << 16) | (Byte.toUnsignedInt(array[i + (bci * 3 + 1)]) << 8) | Byte.toUnsignedInt(array[i
				+ (bci * 3 + 2)]);
	}
	
	private static short toShort(byte b, byte c) {
		return toShort(b, c, ByteOrder.LITTLE_ENDIAN);
	}
	
	@Override
	public String getName() {
		return "gif";
	}
	
	@Override
	public Object getDump(byte[] array) {
		try {
			return ImageIO.read(new ByteArrayInputStream(array));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
