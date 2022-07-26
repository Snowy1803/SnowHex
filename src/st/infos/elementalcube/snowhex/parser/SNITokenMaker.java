package st.infos.elementalcube.snowhex.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;

public class SNITokenMaker extends TokenMaker {
	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			int i = 0;
			if (array[i++] == 0x53 && array[i++] == 0x4d) {
				list.add(createToken(TOKEN_FILE_HEADER, i - 2, 2));
			} else {
				list.add(createToken(TOKEN_FILE_HEADER, 0, 1, invalidSignatureNotice(), Level.ERROR));
				break gen;
			}
			SNIMetadata meta = new SNIMetadata(array[i++]);
			list.add(createToken(TOKEN_METADATA, i - 1, 1, meta.toTooltipString(), Level.INFO));
			int w = meta.isSmallImage() ? array[i++] : toShort(array[i++], array[i++]);
			int h = meta.isSmallImage() ? array[i++] : toShort(array[i++], array[i++]);
			list.add(createToken(TOKEN_IMAGE_SIZE, i - (meta.isSmallImage() ? 2 : 4), meta.isSmallImage() ? 2 : 4, notice("size", w, h), Level.INFO));
			int palSize;
			if (meta.isUsingPalette()) {
				int palStart = i;
				palSize = Byte.toUnsignedInt(array[i++]);
				list.add(createToken(TOKEN_IMAGE_SIZE, i - 1, 1, notice("palette.size", palSize), Level.INFO));
				for (int e = 0; e < palSize; e++) {
					parseColor(list, meta, array, i);
					i += meta.getBytesPerPixel();
				}
				list.add(createToken(TOKEN_IMAGE_PALETTE, palStart, i - palStart));
			} else {
				palSize = 0;
			}
			
			int cmpixelsPerByte = 1;
			if (meta.isUsingCompressedPalette()) {
				for (; (int) Math.pow(palSize + 1, cmpixelsPerByte) * palSize < 256; cmpixelsPerByte++);
			}
			
			int dataStart = i;
			for (int x = 0; x < w; x++) {
				int off = 0, len = h;
				if (meta.hasClip()) {
					off = meta.isSmallImage() ? array[i++] : toShort(array[i++], array[i++]);
					len = meta.isSmallImage() ? array[i++] : toShort(array[i++], array[i++]);
					list.add(createToken(TOKEN_IMAGE_SIZE,
							i - (meta.isSmallImage() ? 2 : 4),
							meta.isSmallImage() ? 2 : 4,
							notice("clip", off, len),
							Level.INFO));
					if (len + off > h) {
						list.add(createToken(TOKEN_NONE,
								i - (meta.isSmallImage() ? 2 : 4),
								meta.isSmallImage() ? 2 : 4,
								notice("clipSize"),
								Level.ERROR));
					}
				}
				if (meta.isUsingCompressedPalette()) {
					int start = i;
					i++;
					int j = 0;
					for (int y = off; y < off + len; y++) {
						if (j == cmpixelsPerByte) {
							i++;
							j = 0;
						}
						j++;
					}
					list.add(createToken(TOKEN_IMAGE_COLOR, start, i - start));
				} else {
					for (int y = off; y < off + len; y++) {
						if (meta.isUsingPalette()) {
							int p = array[i++];
							list.add(createToken(TOKEN_IMAGE_COLOR, i - 1, 1));
							if (p >= palSize) {
								list.add(createToken(TOKEN_NONE, i - 1, 1, notice("palette.ioob", p), Level.ERROR));
							}
						} else {
							parseColor(list, meta, array, i);
							i += meta.getBytesPerPixel();
						}
					}
				}
			}
			list.add(createToken(TOKEN_IMAGE_DATA, dataStart, i - dataStart));
			if (i < array.length) {
				list.add(createToken(TOKEN_NONE, i, array.length - i, notice("dead"), Level.WARNING));
			}
		} catch (IndexOutOfBoundsException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, unexpectedEOFNotice(), Level.ERROR));
		}
		return list;
	}
	
	private static short toShort(byte b, byte c) {
		return toShort(b, c, ByteOrder.BIG_ENDIAN);
	}
	
	private void parseColor(List<Token> list, SNIMetadata meta, byte[] array, int i) {
		int c;
		if (meta.isGrayscaled()) {
			c = ((array[i] & 0xFF) << 16) | ((array[i] & 0xFF) << 8) | ((array[i++] & 0xFF) << 0);
		} else {
			c = ((array[i++] & 0xFF) << 16) | ((array[i++] & 0xFF) << 8) | ((array[i++] & 0xFF) << 0);
		}
		if (meta.hasAlpha()) {
			c |= ((array[i++] & 0xFF) << 24);
		}
		list.add(createToken(TOKEN_IMAGE_COLOR,
				i - meta.getBytesPerPixel(),
				meta.getBytesPerPixel(),
				notice("color", getHexString(Integer.toHexString(c & 0xFFFFFF), 6), getHexString(Integer.toHexString(c), 8)),
				Level.INFO));
	}
	
	@Override
	public String getName() {
		return "sni";
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
	
	@Override
	public byte[] getSignature() {
		return new byte[] { 0x53, 0x4d };
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[] { "sni" };
	}
	
	private class SNIMetadata {
		public static final byte ALPHA_MASK = 1, GRAYSCALE_MASK = 2, PALETTE_MASK = 8, PALETTE_COMPRESSION_MASK = 4, CLIP_MASK = 16,
				SMALL_IMAGE_MASK = 32;// 64, 128
		private byte val;
		
		public SNIMetadata(byte meta) {
			val = meta;
		}
		
		public String toTooltipString() {
			return notice("meta") + "<br/>" + notice("meta.grayscaled." + isGrayscaled()) + "<br/>" + notice("meta.alpha." + hasAlpha()) + "<br/>"
					+ notice("meta." + (isUsingCompressedPalette() ? "c" : "") + "palette." + isUsingPalette()) + "<br/>"
					+ notice("meta.clip." + hasClip());
		}
		
		public boolean hasAlpha() {
			return (val & ALPHA_MASK) != 0;
		}
		
		public boolean isGrayscaled() {
			return (val & GRAYSCALE_MASK) != 0;
		}
		
		public boolean isUsingPalette() {
			return (val & PALETTE_MASK) != 0;
		}
		
		public boolean isUsingCompressedPalette() {
			return (val & PALETTE_COMPRESSION_MASK) != 0;
		}
		
		public boolean hasClip() {
			return (val & CLIP_MASK) != 0;
		}
		
		public boolean isSmallImage() {
			return (val & SMALL_IMAGE_MASK) != 0;
		}
		
		public int getBytesPerPixel() {
			return ((isGrayscaled() ? 1 : 3) + (hasAlpha() ? 1 : 0));
		}
	}
}
