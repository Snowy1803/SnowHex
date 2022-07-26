package st.infos.elementalcube.snowhex.parser;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.io.File;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.parser.png.InflatedSubDocument;
import st.infos.elementalcube.snowhex.ui.HexFrame;
import st.infos.elementalcube.snowhex.ui.HexPanel;
import st.infos.elementalcube.snowylangapi.Lang;

public class GZTokenMaker extends TokenMaker {

	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		gen:
		try {
			if (array[0] != (byte) 0x1f || array[1] != (byte) 0x8b) {
				list.add(createToken(TOKEN_FILE_HEADER, 0, 2, notice("header"), Level.ERROR));
				break gen;
			}
			list.add(createToken(TOKEN_FILE_HEADER, 0, 2));
			if (array[2] != 0x08) {
				list.add(createToken(TOKEN_METADATA, 2, 1, notice("cm.error"), Level.ERROR));
				break gen;
			}
			list.add(createToken(TOKEN_METADATA, 2, 1, notice("cm"), Level.INFO));
			byte flg = array[3];
			boolean ftext = (flg & (1 << 0)) != 0;
			boolean fhcrc = (flg & (1 << 1)) != 0;
			boolean fextra = (flg & (1 << 2)) != 0;
			boolean fname = (flg & (1 << 3)) != 0;
			boolean fcomment = (flg & (1 << 4)) != 0;
			list.add(createToken(TOKEN_METADATA, 3, 1,
					notice("flg.ftext." + ftext) + "<br/>"
					+ notice("flg.fhcrc." + fhcrc) + "<br/>"
					+ notice("flg.fextra." + fextra) + "<br/>"
					+ notice("flg.fname." + fname) + "<br/>"
					+ notice("flg.fcomment." + fcomment), Level.INFO));
			int mtime = ByteBuffer.wrap(array, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
			list.add(createToken(TOKEN_DATE, 4, 4,
					mtime == 0 ? notice("mtime.none") : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).format(Instant.ofEpochSecond(mtime)), Level.INFO));
			byte xfl = array[8]; // deflate compression lvl
			list.add(createToken(TOKEN_METADATA, 8, 1)); // notice ?
			byte os = array[9];
			list.add(createToken(TOKEN_METADATA, 9, 1)); // notice ?
			ByteBuffer buf = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).position(10);
			if (fextra) {
				int xlen = buf.getShort();
				list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2));
				int start = buf.position();
				while (buf.position() < start + xlen) {
					buf.getShort();
					list.add(createToken(TOKEN_CHUNK_HEADER, buf.position() - 2, 2));
					int sublen = buf.getShort();
					list.add(createToken(TOKEN_LENGTH, buf.position() - 2, 2));
					// list.add(createToken(TOKEN_COMMENT, buf.position(), sublen));
					buf.position(buf.position() + sublen);
				}
			}
			if (fname) {
				int start = buf.position();
				while (buf.get() != 0);
				list.add(createToken(TOKEN_STRING, start, buf.position() - start));
			}
			if (fcomment) {
				int start = buf.position();
				while (buf.get() != 0);
				list.add(createToken(TOKEN_COMMENT, start, buf.position() - start));
			}
			if (fhcrc) {
				short crc16 = buf.getShort();
				// we could check it ? (cast CRC32 to short to get CRC16)
				list.add(createToken(TOKEN_CHECKSUM, buf.position() - 2, 2));
			}
			int datalen = array.length - buf.position() - 8;
			list.add(createToken(TOKEN_COMPRESSED_DATA, buf.position(), datalen));
			buf.position(buf.position() + datalen);
			int crc32 = buf.getInt();
			list.add(createToken(TOKEN_CHECKSUM, buf.position() - 4, 4));
			int isize = buf.getInt();
			list.add(createToken(TOKEN_LENGTH, buf.position() - 4, 4, notice("isize", isize), Level.INFO));
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, notice("ioob"), Level.ERROR));
		}
		return list;
	}

	@Override
	public String getName() {
		return "gz";
	}

	@Override
	public Object getDump(byte[] array) {
		return null;
	}
	
	@Override
	public void willShowPopup(HexPanel panel, PopupMenu menu) {
		Token closest = panel.getClosestToken();
		if (closest != null) {
			if (closest.getType() == TOKEN_COMPRESSED_DATA) {
				menu.addSeparator();
				MenuItem open = new MenuItem(Lang.getString("parser.png.decompress"));
				open.addActionListener(e -> {
					InflatedSubDocument doc = new InflatedSubDocument(panel.getDocument(), closest.getOffset(), closest.getLength(), -1, true);
					HexFrame frame = new HexFrame(doc);
					// maybe check fname property ?
					HexFrame parent = (HexFrame) SwingUtilities.getWindowAncestor(panel);
					File file = parent.getFile();
					if (file != null && file.getName().endsWith(".gz")) {
						frame.getEditor().setColorer(HexFrame.getColorer(file.getName().substring(0, file.getName().length() - 3)));
					}
				});
				menu.add(open);
			}
		}
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[] { "gz", "gzip" };
	}
	
	@Override
	public byte[] getSignature() {
		return new byte[] { 0x1f, (byte) 0x8b };
	}
}
