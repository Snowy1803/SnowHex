package st.infos.elementalcube.snowhex.parser;

import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenImpl;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.ui.HexPanel;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.OIDMap;

public class ASN1TokenMaker extends TokenMaker {

	private static final String[] UNIVERSAL_TYPENAMES = {
			"End of Content",
			"Boolean",
			"Integer",
			"Bit String",
			"Octet String",
			"Null",
			"Object Identifier",
			"Object Descriptor",
			"External",
			"Real number",
			"Enumerated",
			"Embedded-PDV",
			"UTF8 String",
			"Relative Object Identifier",
			"Time",
			null,
			"Sequence",
			"Set",
			"Numeric String",
			"Printable String",
			"T61 String",
			"Videotex String",
			"IA5 String",
			"UTC Time",
			"Generalized Time",
			"Graphic String",
			"Visible String",
			"General String",
			"Universal String",
			"Character String",
			"BMP String",
			"Date",
			"Time Of Day",
			"Date/Time",
			"Duration",
			"OID IRI",
			"Relative OID IRI"
	};
	
	@Override
	public List<Token> generateTokens(byte[] array) {
		ArrayList<Token> list = new ArrayList<>();
		ByteBuffer buf = ByteBuffer.wrap(array);
		try {
			while (buf.hasRemaining()) {
				readChunk(list, buf);
			}
		} catch (IndexOutOfBoundsException | BufferUnderflowException e) {
			list.add(createToken(TOKEN_NONE, array.length - 1, 1, unexpectedEOFNotice(), Level.ERROR));
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
		if (tag == 0 && type < UNIVERSAL_TYPENAMES.length && UNIVERSAL_TYPENAMES[(int) type] != null) { // universal
			list.add(createToken(TOKEN_CHUNK_HEADER, chunkstart, buf.position() - chunkstart,
					notice("id.0.named", notice("id.constructed." + constructed), type, UNIVERSAL_TYPENAMES[(int) type]), Level.INFO));
		} else {
			list.add(createToken(TOKEN_CHUNK_HEADER, chunkstart, buf.position() - chunkstart,
					notice("id." + tag, notice("id.constructed." + constructed), type), Level.INFO));
		}
		int lengthstart = buf.position();
		byte lengthb = buf.get();
		int length;
		boolean indefinite;
		if (lengthb < 0) {
			int rest = lengthb & 0x7f;
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
		int contentstart = buf.position();
		if (!constructed) { // primitive
			parsePrimitive(tag, type, length, list, buf);
			buf.position(contentstart + length);
			return id == 0;
		}
		if (indefinite) {
			while (!readChunk(list, buf));
		} else {
			while (buf.position() < contentstart + length) {
				readChunk(list, buf);
			}
		}
		list.add(createToken(TOKEN_HIERARCHICAL_CHUNK, chunkstart, buf.position() - chunkstart));
		return false;
	}

	private void parsePrimitive(int tag, long type, int length, ArrayList<Token> list, ByteBuffer buf) {
		if (tag == 0) { // universal
			switch ((int) type) {
			case 1: // BOOL
				if (length != 1) {
					list.add(createToken(TOKEN_METADATA, buf.position(), length, notice("invalid"), Level.ERROR));
				} else {
					byte b = buf.get();
					if (b == 0) {
						list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("boolean.false"), Level.INFO));
					} else if (b == -1) {
						list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("boolean.true"), Level.INFO));
					} else {
						list.add(createToken(TOKEN_METADATA, buf.position() - 1, 1, notice("boolean.truthy"), Level.WARNING));
					}
				}
				return;
			case 2: // INT
			case 10: // enumerated
				BigInteger value = new BigInteger(buf.array(), buf.position(), length);
				list.add(createToken(TOKEN_METADATA, buf.position(), length, notice("integer", value), Level.INFO));
				return;
			case 6: // OID
				ArrayList<Long> values = new ArrayList<>();
				long val = 0;
				for (int i = 0; i < length; i++) {
					byte b = buf.get();
					val = (val << 7) | (b & 0x7f);
					if (b >= 0) {
						values.add(val);
						val = 0;
					}
				}
				StringJoiner sj = new StringJoiner(".");
				if (values.size() > 0) {
					long first = values.get(0);
					long s0 = first / 40;
					long s1 = first % 40;
					if (s0 > 2) {
						s1 += (s0 - 2) * 40;
						s0 = 2;
					}
					sj.add(Long.toString(s0));
					sj.add(Long.toString(s1));
					for (int i = 1; i < values.size(); i++) {
						sj.add(Long.toString(values.get(i)));
					}
				}
				String str = sj.toString();
				try {
					ObjectIdentifier oid;
					try {
						// Linux
						oid = (ObjectIdentifier) ObjectIdentifier.class.getDeclaredMethod("of", String.class).invoke(null, str);
					} catch (NoSuchMethodException e) {
						// macOS
						oid = ObjectIdentifier.class.getConstructor(String.class).newInstance(str);
					}
					String name = OIDMap.getName(oid);
					list.add(createToken(TOKEN_KEYWORD, buf.position() - length, length,
							name == null ? notice("oid.unknown", str) : notice("oid", name, str), Level.INFO).withOid(str));
				} catch (InvocationTargetException | InstantiationException | NoSuchMethodException e) {
					e.printStackTrace();
					list.add(createToken(TOKEN_KEYWORD, buf.position() - length, length, notice("oid.unknown", str), Level.INFO).withOid(str));
				} catch (IllegalAccessError | IllegalAccessException e) { // java 9 disallows 
					e.printStackTrace();
					list.add(createToken(TOKEN_KEYWORD, buf.position() - length, length, notice("oid.unknown", str), Level.INFO).withOid(str));
					list.add(createToken(TOKEN_NONE, buf.position() - length, length,
							notice("oid.java9", "--add-exports=java.base/sun.security.util=ALL-UNNAMED --add-exports=java.base/sun.security.x509=ALL-UNNAMED"), Level.WARNING));
				}
				return;
			case 13: // RELATIVE OID
				sj = new StringJoiner(".");
				val = 0;
				for (int i = 0; i < length; i++) {
					byte b = buf.get();
					val = (val << 7) | (b & 0x7f);
					if (b >= 0) {
						sj.add(Long.toString(val));
						val = 0;
					}
				}
				str = sj.toString();
				list.add(createToken(TOKEN_KEYWORD, buf.position() - length, length, notice("oid.relative", str), Level.INFO));
				return;
			case 12: // utf8 string
			case 19: // printable string (ascii)
			case 22: // IA5 string (ascii)
				str = new String(buf.array(), buf.position(), length, StandardCharsets.UTF_8);
				list.add(createToken(TOKEN_STRING, buf.position(), length, str, Level.INFO));
				return;
			case 23: // UTC time
				str = new String(buf.array(), buf.position(), length, StandardCharsets.UTF_8);
				int year = Integer.parseInt(str.substring(0, 2));
				ZonedDateTime time = ZonedDateTime.of(
						year > 50 ? 1900 + year : 2000 + year, // RFC 5280 states UTCTime will not be used after 2050
						Integer.parseInt(str.substring(2, 4)),
						Integer.parseInt(str.substring(4, 6)),
						Integer.parseInt(str.substring(6, 8)),
						Integer.parseInt(str.substring(8, 10)),
						Integer.parseInt(str.substring(10, 12)),
						0,
						ZoneId.of(str.substring(12)));
				list.add(createToken(TOKEN_DATE, buf.position(), length,
						time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)), Level.INFO));
				return;
			case 24: // Generalized time
				str = new String(buf.array(), buf.position(), length, StandardCharsets.UTF_8);
				int endindex = Math.max(Math.max(str.indexOf('+'), str.indexOf('-')), str.indexOf('Z'));
				if (endindex >= 0) {
					time = ZonedDateTime.of(
							Integer.parseInt(str.substring(0, 4)),
							Integer.parseInt(str.substring(4, 6)),
							Integer.parseInt(str.substring(6, 8)),
							Integer.parseInt(str.substring(8, 10)),
							endindex < 12 ? 0 : Integer.parseInt(str.substring(10, 12)),
							endindex < 14 ? 0 : Integer.parseInt(str.substring(12, 14)),
							endindex < 18 ? 0 : Integer.parseInt(str.substring(15, 18)) * 1_000_000,
							ZoneId.of(str.substring(endindex)));
					list.add(createToken(TOKEN_DATE, buf.position(), length,
							time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)), Level.INFO));
				} else {
					endindex = str.length();
					LocalDateTime local = LocalDateTime.of(
							Integer.parseInt(str.substring(0, 4)),
							Integer.parseInt(str.substring(4, 6)),
							Integer.parseInt(str.substring(6, 8)),
							Integer.parseInt(str.substring(8, 10)),
							endindex < 12 ? 0 : Integer.parseInt(str.substring(10, 12)),
							endindex < 14 ? 0 : Integer.parseInt(str.substring(12, 14)),
							endindex < 18 ? 0 : Integer.parseInt(str.substring(15, 18)) * 1_000_000);
					list.add(createToken(TOKEN_DATE, buf.position(), length,
							local.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)), Level.INFO));
				}
				return;
			}
		}
		list.add(createToken(TOKEN_METADATA, buf.position(), length));
	}

	@Override
	public String getName() {
		return "asn1";
	}
	
	@Override
	public byte[] getSignature() {
		return null;
	}
	
	@Override
	public String[] getFileExtensions() {
		return new String[] {"asn1", "cer", "crt", "der"};
	}
	
	@Override
	public ByteOrder getEndianness() {
		return ByteOrder.BIG_ENDIAN; // for INT
	}
	
	@Override
	public void willShowPopup(HexPanel panel, PopupMenu menu) {
		ASN1Token token = (ASN1Token) panel.getClosestToken();
		if (token != null && token.getType() == TOKEN_KEYWORD && token.oid != null) {
			menu.addSeparator();
			MenuItem item = new MenuItem(notice("oid.repository"));
			item.addActionListener(e -> {
				try {
					Desktop.getDesktop().browse(new URI("https://oid-rep.orange-labs.fr/get/" + token.oid));
				} catch (IOException | URISyntaxException | UnsupportedOperationException ex) {
					ex.printStackTrace();
				}
			});
			menu.add(item);
		}
	}
	
	@Override
	public Token allocToken() {
		return new ASN1Token();
	}
	
	@Override
	public ASN1Token createToken(int type, int offset, int length, String desc, Level lvl) {
		return (ASN1Token) super.createToken(type, offset, length, desc, lvl);
	}
	
	private class ASN1Token extends TokenImpl {
		private String oid;
		
		public ASN1Token withOid(String oid) {
			this.oid = oid;
			return this;
		}
	}
}
