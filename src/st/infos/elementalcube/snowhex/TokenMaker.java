package st.infos.elementalcube.snowhex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowylangapi.Lang;

public abstract class TokenMaker {
	public static final int TOKEN_ERRORED = 0, TOKEN_FILE_HEADER = 1, TOKEN_COMMENT = 2, TOKEN_CHUNK_HEADER = 3, TOKEN_METADATA = 4,
			TOKEN_RESERVED = 5, TOKEN_LENGTH = 6, TOKEN_IMAGE_PALETTE = 7, TOKEN_IMAGE_COLOR = 8, TOKEN_IMAGE_DATA = 9, TOKEN_IMAGE_SIZE = 10,
			TOKEN_CHUNK = 11, TOKEN_LAST = 11;
	private static ArrayList<Token> cache = new ArrayList<>();
	private static HashMap<String, Class<? extends TokenMaker>> subclasses = new HashMap<>();
	
	static {
		subclasses.put("sni", SNITokenMaker.class);
		subclasses.put("gif", GIFTokenMaker.class);
		subclasses.put("ant3", Ant3TokenMaker.class);
		subclasses.put("ant1", Ant1TokenMaker.class);
		subclasses.put("cor", CORTokenMaker.class);
		// TODO subclasses.put("ser", SERTokenMaker.class);
	}
	
	public abstract List<Token> generateTokens(byte[] array);
	
	public Token createToken(int type, int offset, int length) {
		Optional<Token> t = cache.stream().filter(o -> o.is(type, offset, length)).findAny();
		if (t.isPresent()) {
			return t.get();
		}
		Token c = new Token(type, offset, length);
		cache.add(c);
		return c;
	}
	
	public Token createToken(int type, int offset, int length, String desc, Level lvl) {
		Optional<Token> t = cache.stream().filter(o -> o.is(type, offset, length, desc, lvl)).findAny();
		if (t.isPresent()) {
			return t.get();
		}
		Token c = new Token(type, offset, length, desc, lvl);
		cache.add(c);
		return c;
	}
	
	/**
	 * Returns a short name for the language (ex: sni, gif, png...)
	 * 
	 * @return the simple name of the language
	 */
	public abstract String getName();
	
	public String getLocalizedName() {
		return Lang.getString("parser." + getName());
	}
	
	public String notice(String type, Object... params) {
		return Lang.getString("parser." + getName() + "." + type, params);
	}
	
	/**
	 * Returns the object to print as a dump.<br/>
	 * If null, shows the ascii text for the bytes.<br/>
	 * If instanceof BufferedImage, shows the image.
	 * 
	 * @return the object to show to the user
	 */
	public abstract Object getDump(byte[] array);
	
	// Static utilities
	
	public static short toShort(byte b1, byte b2, ByteOrder bo) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(bo);
		bb.put(b1);
		bb.put(b2);
		return bb.getShort(0);
	}
	
	public static String getHexString(String hexString, int len) {
		StringBuilder sb = new StringBuilder(hexString);
		while (sb.length() < len)
			sb.insert(0, '0');
		return sb.toString();
	}
	
	public static void registerTokenMaker(String ext, Class<? extends TokenMaker> c) {
		subclasses.put(ext.toLowerCase(), c);
	}
	
	public static TokenMaker getTokenMaker(String ext) {
		if (ext == null) return null;
		Class<? extends TokenMaker> c = subclasses.get(ext.toLowerCase());
		if (c != null) {
			try {
				return c.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				System.err.println("Unable to initialize TokenMaker: " + c);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static Set<String> getParsers() {
		return subclasses.keySet();
	}
}
