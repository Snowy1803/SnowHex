package st.infos.elementalcube.snowhex;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import st.infos.elementalcube.snowhex.Token.Level;

public class GIFTokenMaker extends TokenMaker {
	public int gcd(int a, int b) {
	   if (b == 0) return a;
	   return gcd(b, a % b);
	}
	
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
			list.add(createToken(TOKEN_IMAGE_SIZE, i - 4, 4, notice("size", w, h), Level.INFO).withSubtype(GIFToken.SUBTY_GLOBAL_SIZE));
			byte packedFields = array[i++];
			boolean globalColorTableFlag = (packedFields & 0x80) != 0;
			int numGCTEntries = 1 << ((packedFields & 0x7) + 1);
			list.add(createToken(TOKEN_METADATA, i - 1, 1, notice("globalColorTable." + globalColorTableFlag) + "<br/>" + notice("colorRes",
					((packedFields >> 4) & 0x7) + 1) + "<br/>" + notice("sort." + ((packedFields & 0x8) != 0)) + "<br/>" + notice("gctSize",
							numGCTEntries), Level.INFO).withSubtype(GIFToken.SUBTY_LSD_PACKED));
			int bci = array[i++];// Background Color Index
			list.add(createToken(TOKEN_IMAGE_COLOR, i - 1, 1, notice("bci", bci, globalColorTableFlag ? getHexString(Integer.toHexString(getBCI(array,
					i + 2, bci)), 6) : "No GCT"), Level.INFO).withSubtype(GIFToken.SUBTY_LSD_BG));
			int par = array[i++];// Pixel Aspect Ratio
			int gcd = par == 0 ? 0 : gcd(par + 15, 64);
			list.add(createToken(TOKEN_METADATA, i - 1, 1, par == 0 ? notice("par.false") : notice("par", (par + 15) / gcd, 64 / gcd), Level.INFO)
					.withSubtype(GIFToken.SUBTY_PAR));
			if (globalColorTableFlag) {
				for (int j = 0; j < numGCTEntries; j++) {
					list.add(createToken(TOKEN_IMAGE_COLOR, i + j * 3, 3, notice("color", parseColor(array, i + j * 3)), Level.INFO)
							.withSubtype(GIFToken.SUBTY_PALETTE_RGB));
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
		while (true) {
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
							Level.INFO).withSubtype(GIFToken.SUBTY_SUB_POS));
					list.add(createToken(TOKEN_IMAGE_SIZE, i, 4, notice("size", toShort(array[i++], array[i++]), toShort(array[i++], array[i++])),
							Level.INFO).withSubtype(GIFToken.SUBTY_SUB_SIZE));
					list.add(createToken(TOKEN_IMAGE_SIZE, i, 2, notice("charSize", array[i++], array[i++]), Level.INFO)
							.withSubtype(GIFToken.SUBTY_CHAR_SIZE));
					list.add(createToken(TOKEN_IMAGE_COLOR, i, 1, notice("ext.text.fore", array[i++] & 0xFF), Level.INFO)
							.withSubtype(GIFToken.SUBTY_INDEX_PALETTE));
					list.add(createToken(TOKEN_IMAGE_COLOR, i, 1, notice("ext.text.back", array[i++] & 0xFF), Level.INFO)
							.withSubtype(GIFToken.SUBTY_INDEX_PALETTE));
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
									& 0x1) != 0)), Level.INFO).withSubtype(GIFToken.SUBTY_GCE_PACKED));
					list.add(createToken(TOKEN_METADATA, i, 2, notice("ext.gce.delay", toShort(array[i++], array[i++]) / 100F), Level.INFO)
							.withSubtype(GIFToken.SUBTY_GCE_DELAY));
					if ((packedFields & 0x1) != 0) {
						list.add(createToken(TOKEN_IMAGE_COLOR, i, 1, notice("ext.gce.transparentColor", Byte.toUnsignedInt(array[i++])), Level.INFO)
								.withSubtype(GIFToken.SUBTY_INDEX_PALETTE));
					} else {
						list.add(createToken(TOKEN_RESERVED, i, 1));
						i++;
					}
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
							Level.INFO).withSubtype(GIFToken.SUBTY_APP));
					// its supposed to be an auth code but everyone uses it as a version
					list.add(createToken(TOKEN_METADATA, i, 3, notice("ext.app.version", new String(new char[] { (char) array[i++], (char) array[i++],
							(char) array[i++] })), Level.INFO).withSubtype(GIFToken.SUBTY_APP));
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
				list.add(createToken(TOKEN_IMAGE_SIZE, i - 4, 4, notice("pos", x, y), Level.INFO).withSubtype(GIFToken.SUBTY_SUB_POS));
				int w = toShort(array[i++], array[i++]);
				int h = toShort(array[i++], array[i++]);
				list.add(createToken(TOKEN_IMAGE_SIZE, i - 4, 4, notice("size", w, h), Level.INFO).withSubtype(GIFToken.SUBTY_SUB_SIZE));
				int packedFields = array[i++];
				boolean lct = (packedFields & 0x80) != 0;
				int numLCTEntries = 1 << ((packedFields & 0x7) + 1);
				list.add(createToken(TOKEN_METADATA, i - 1, 1, notice("localColorTable." + lct) + "<br/>" + notice("interlace." + ((packedFields
						& 0x40) != 0)) + "<br/>" + notice("sort." + ((packedFields & 0x20) != 0)) + "<br/>" + notice("lctSize", lct ? numLCTEntries : 0),
						Level.INFO).withSubtype(GIFToken.SUBTY_IMG_PACKED));
				if (lct) {
					for (int j = 0; j < numLCTEntries; j++) {
						list.add(createToken(TOKEN_IMAGE_COLOR, i + j * 3, 3, notice("color", parseColor(array, i + j * 3)), Level.INFO)
								.withSubtype(GIFToken.SUBTY_PALETTE_RGB));
					}
					list.add(createToken(TOKEN_IMAGE_PALETTE, i, numLCTEntries * 3));
					i += numLCTEntries * 3;
				}
				list.add(((GIFToken) createToken(TOKEN_METADATA, i++, 1)).withSubtype(GIFToken.SUBTY_LZW));
				i = readSubBlocks(list, array, i, TOKEN_IMAGE_COLOR);
				list.add(createToken(TOKEN_IMAGE_DATA, imgStart, i - imgStart));
				break;
			case 0x3b:
				list.add(createToken(TOKEN_CHUNK_HEADER, i - 1, 1));
				if (i < array.length) {
					list.add(createToken(TOKEN_ERRORED, i, array.length - i, "Trailing data", Level.WARNING));
				}
				return;
			default:
				list.add(createToken(TOKEN_ERRORED, i - 1, 1, notice("block.unknown"), Level.ERROR));
				break;
			}
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
		return new ImageIcon(array).getImage();
	}
	
	@Override
	public Token allocToken() {
		return new GIFToken();
	}
	
	@Override
	public GIFToken createToken(int type, int offset, int length, String desc, Level lvl) {
		return (GIFToken) super.createToken(type, offset, length, desc, lvl);
	}
	
	class GIFToken extends TokenImpl {
		
		private int subtype;
		
		// IMAGE_SIZE
		static final int SUBTY_GLOBAL_SIZE = 1, SUBTY_SUB_POS = 2, SUBTY_SUB_SIZE = 3, SUBTY_CHAR_SIZE = 4;
		// METADATA
		static final int SUBTY_LSD_PACKED = 1, SUBTY_PAR = 2, SUBTY_GCE_PACKED = 3, SUBTY_GCE_DELAY = 4, SUBTY_APP = 5, SUBTY_IMG_PACKED = 6, SUBTY_LZW = 7;
		// IMAGE_COLOR
		static final int SUBTY_LSD_BG = 1, SUBTY_PALETTE_RGB = 2, SUBTY_INDEX_PALETTE = 3;
		
		@Override
		public void init(int type, int offset, int length, String tooltip, Level tooltipLevel) {
			super.init(type, offset, length, tooltip, tooltipLevel);
			subtype = 0;
		}
		
		public GIFToken withSubtype(int subtype) {
			this.subtype = subtype;
			return this;
		}
		
		public int getSubtype() {
			return subtype;
		}
	}
}
