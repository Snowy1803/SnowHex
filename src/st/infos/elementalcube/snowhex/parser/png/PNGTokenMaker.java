package st.infos.elementalcube.snowhex.parser.png;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.ByteSelection;
import st.infos.elementalcube.snowhex.HexDocument.EditType;
import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.parser.gif.CoordsEditor;
import st.infos.elementalcube.snowhex.parser.gif.PaletteColorEditor;
import st.infos.elementalcube.snowhex.ui.HexFrame;
import st.infos.elementalcube.snowhex.ui.HexPanel;
import st.infos.elementalcube.snowylangapi.Lang;

public class PNGTokenMaker extends TokenMaker {
	private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
	
	int width, height;
	byte bitDepth, colorType;
	
	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			if (!Arrays.equals(array, 0, 8, SIGNATURE, 0, 8)) {
				list.add(createToken(TOKEN_FILE_HEADER, 0, 8, notice("header"), Level.ERROR));
				break gen;
			}
			list.add(createToken(TOKEN_FILE_HEADER, 0, 8));
			readChunks(list, array);
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, notice("ioob"), Level.ERROR));
		}
		return list;
	}
	
	private void readChunks(ArrayList<Token> list, byte[] array) {
		ByteBuffer buf = ByteBuffer.wrap(array);
		buf.position(8);
		buf.order(ByteOrder.BIG_ENDIAN);
		CRC32 crc = new CRC32();
		byte[] chunkType = new byte[4];
		while (true) {
			int length = buf.getInt();
			list.add(createToken(TOKEN_LENGTH, buf.position() - 4, 4));
			crc.reset();
			crc.update(array, buf.position(), length + 4);
			buf.get(chunkType);
			int type = buf.getInt(buf.position() - 4); // use ints instead of byte[4]
			list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 4, 4,
					notice("chunk", new String(chunkType), 
							notice("chunk." + ((chunkType[0] & 32) != 0 ? "ancillary" : "critical")), 
							notice("chunk." + ((chunkType[1] & 32) != 0 ? "private" : "public")), 
							notice("chunk." + ((chunkType[3] & 32) != 0 ? "safe" : "unsafe"))),
					Level.INFO));
			int datastart = buf.position();
			readChunk(list, buf, type, length);
			buf.position(datastart + length);
			int checksum = buf.getInt();
			int expected = (int) crc.getValue();
			if (expected != checksum) {
				list.add(createToken(TOKEN_CHECKSUM, buf.position() - 4, 4, notice("invalidCrc"), Level.ERROR).expectedCrc(expected));
			} else {
				list.add(createToken(TOKEN_CHECKSUM, buf.position() - 4, 4));
			}
			if (type == 0x49_45_4e_44) { // IEND
				return; // last block, skip the rest
			}
		}
	}
	
	private void readChunk(ArrayList<Token> list, ByteBuffer buf, int type, int length) {
		int start = buf.position();
		switch (type) {
		case 0x49_48_44_52: // IHDR
			width = buf.getInt();
			height = buf.getInt();
			list.add(createToken(TOKEN_IMAGE_SIZE, buf.position() - 8, 8, notice("size", width, height), Level.INFO));
			bitDepth = buf.get();
			list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("bitDepth", bitDepth), Level.INFO));
			colorType = buf.get();
			if (Arrays.binarySearch(new byte[] {0, 2, 3, 4, 6}, colorType) >= 0) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("colorType", notice("colorType." + colorType)), Level.INFO));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("colorType", notice("colorType.error")), Level.ERROR));	
			}
			byte compression = buf.get();
			if (compression == 0) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("compression." + compression), Level.INFO));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("compression.error"), Level.ERROR));	
			}
			byte filter = buf.get();
			if (filter == 0) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("filter." + filter), Level.INFO));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("filter.error"), Level.ERROR));	
			}
			byte interlace = buf.get();
			if (interlace == 0 || interlace == 1) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("interlace." + interlace), Level.INFO));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("interlace.error"), Level.ERROR));	
			}
			break;
		case 0x49_44_41_54: // IDAT
			list.add(createToken(TOKEN_IMAGE_DATA, buf.position() - 8, length + 12));
			list.add(createToken(TOKEN_COMPRESSED_DATA, buf.position(), length)
					.withSubtype(PNGToken.COMPRESSED_IDAT).withIndex(8));
			break;
		case 0x50_4c_54_45: // PLTE
			list.add(createToken(TOKEN_IMAGE_PALETTE, buf.position() - 8, length + 12));
			for (int i = 0; i < length / 3; i++) {
				list.add(createToken(TOKEN_IMAGE_COLOR, buf.position(), 3, Lang.getString("parser.gif.color", parseColor(buf.array(), buf.position())), Level.INFO)
						.withPLTEIndex(i));
				buf.position(buf.position() + 3);
			}
			break;
		case 0x74_49_4d_45: // tIME
			list.add(createToken(TOKEN_CHUNK, buf.position() - 8, length + 12));
			int year = buf.getShort();
			int month = buf.get();
			int day = buf.get();
			int hour = buf.get();
			int minute = buf.get();
			int second = buf.get();
			ZonedDateTime time = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("Z"));
			list.add(createToken(TOKEN_DATE, buf.position() - length, length,
					time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)), Level.INFO));
			break;
		case 0x74_45_58_74: // tEXt
			list.add(createToken(TOKEN_CHUNK, buf.position() - 8, length + 12));
			while (buf.get() != 0);
			list.add(createToken(TOKEN_KEYWORD, start, buf.position() - start));
			list.add(createToken(TOKEN_COMMENT, buf.position(), length - (buf.position() - start)));
			break;
		case 0x7a_54_58_74: // zTXt
			list.add(createToken(TOKEN_CHUNK, buf.position() - 8, length + 12));
			while (buf.get() != 0);
			list.add(createToken(TOKEN_KEYWORD, start, buf.position() - start));
			compression = buf.get();
			if (compression == 0) {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("compression." + compression), Level.INFO));
			} else {
				list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("compression.error"), Level.ERROR));	
			}
			list.add(createToken(TOKEN_COMPRESSED_DATA, buf.position(), length - (buf.position() - start))
					.withSubtype(PNGToken.COMPRESSED_ZTXT).withIndex(buf.position() - start + 8));
			break;
		case 0x69_54_58_74: // iTXt
			list.add(createToken(TOKEN_CHUNK, buf.position() - 8, length + 12));
			while (buf.get() != 0);
			list.add(createToken(TOKEN_KEYWORD, start, buf.position() - start));
			byte compressed = buf.get();
			list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1)); // compressed on/off. no notice ?
			compression = buf.get();
			if (compressed != 0) {
				if (compression == 0) {
					list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("compression." + compression), Level.INFO));
				} else {
					list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("compression.error"), Level.ERROR));	
				}
			} else {
				list.add(createToken(TOKEN_RESERVED, buf.position() - 1, 1));	
			}
			int langtagstart = buf.position();
			while (buf.get() != 0);
			list.add(createToken(TOKEN_METADATA, langtagstart, buf.position() - langtagstart));
			int transkwstart = buf.position();
			while (buf.get() != 0);
			list.add(createToken(TOKEN_KEYWORD, transkwstart, buf.position() - transkwstart));
			if (compressed != 0) {
				list.add(createToken(TOKEN_COMPRESSED_DATA, buf.position(), length - (buf.position() - start))
						.withSubtype(PNGToken.COMPRESSED_ZTXT).withIndex(buf.position() - start + 8));
			} else {
				list.add(createToken(TOKEN_COMMENT, buf.position(), length - (buf.position() - start)));
			}
			break;
		case 0x70_48_59_73: // pHYs
			list.add(createToken(TOKEN_IMAGE_SIZE, buf.position(), 8)); // subtype ? notice ?
			list.add(createToken(TOKEN_METADATA, buf.position() + 8, 1)); // unit, no notice ?
			list.add(createToken(TOKEN_CHUNK, buf.position() - 8, length + 12));
			break;
		case 0x62_4b_47_44: // bKGD
			// standardise and use palette / use 8bit instead of 16 bit
			list.add(createToken(TOKEN_IMAGE_COLOR, buf.position(), length,
					"Color: #" + new ByteSelection(ArrayUtils.subarray(buf.array(), buf.position(), buf.position() + length)), Level.INFO));
			list.add(createToken(TOKEN_CHUNK, buf.position() - 8, length + 12));
			break;
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
	
	@Override
	public byte[] getSignature() {
		return SIGNATURE;
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[] { "png" };
	}
	
	@Override
	public void willShowPopup(HexPanel panel, PopupMenu menu) {
		PNGToken closest = (PNGToken) panel.getClosestToken();
		if (closest != null) {
			if (closest.subtype == PNGToken.ERROR_CRC) {
				menu.addSeparator();
				MenuItem fix = new MenuItem(notice("invalidCrc.fix"));
				fix.addActionListener(e -> {
					byte[] b = new byte[4];
					ByteBuffer buf = ByteBuffer.wrap(b);
					buf.putInt(closest.expectedCrc);
					panel.getDocument().replaceBytes(closest.getOffset(), closest.getLength(), b, EditType.PROPERTY_CHANGE);
				});
				menu.add(fix);
			}
			if (closest.getType() == TOKEN_COMPRESSED_DATA) {
				menu.addSeparator();
				MenuItem open = new MenuItem(notice("decompress"));
				open.addActionListener(e -> {
					InflatedSubDocument doc = new InflatedSubDocument(panel.getDocument(), closest.getOffset(), closest.getLength(), closest.getIndex(), false);
					HexFrame frame = new HexFrame(doc);
					if (closest.getSubtype() == PNGToken.COMPRESSED_IDAT) {
						frame.getEditor().setColorer(new IDATTokenMaker(this, doc));
					}
				});
				menu.add(open);
			}
		}
	}
	
	@Override
	public Token allocToken() {
		return new PNGToken();
	}
	
	@Override
	public PNGToken createToken(int type, int offset, int length, String desc, Level lvl) {
		return (PNGToken) super.createToken(type, offset, length, desc, lvl);
	}
	
	@Override
	public PNGToken createToken(int type, int offset, int length) {
		return (PNGToken) super.createToken(type, offset, length);
	}
	
	private PaletteColorEditor paletteColorEditor;
	private PaletteColorEditor getPaletteColorEditor(HexPanel panel) {
		if (paletteColorEditor == null) {
			paletteColorEditor = new PaletteColorEditor(panel);
		}
		paletteColorEditor.updateValues(panel);
		return paletteColorEditor;
	}
	private CoordsEditor sizeEditor;
	private CoordsEditor getSizeEditor(HexPanel panel, String type) {
		if (sizeEditor == null) {
			sizeEditor = new CoordsEditor(panel, false, ByteOrder.BIG_ENDIAN);
		}
		sizeEditor.updateValues(panel, type);
		return sizeEditor;
	}
	
	@Override
	public JComponent getTokenProperties(HexPanel panel) {
		if (panel.getClosestToken() == null)
			return null;
		switch (panel.getClosestToken().getType()) {
		case TOKEN_IMAGE_COLOR:
			return getPaletteColorEditor(panel);
		case TOKEN_IMAGE_SIZE:
			return getSizeEditor(panel, "Size of the image");
		}
		return null;
	}
}
