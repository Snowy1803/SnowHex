package st.infos.elementalcube.snowhex;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;

import st.infos.elementalcube.snowhex.ui.HexPanel;

public class SearchEngine {
	/**
	 * the byte sequence in which to search
	 */
	public byte[] haystack;
	/**
	 * the byte sequence to search
	 */
	public byte[] needle;
	/**
	 * the offset at which to begin searching
	 */
	public int start;
	/**
	 * the offset at which to stop/wrap
	 */
	public int end;
	/**
	 * the offset of the last search, to begin the next search
	 */
	public int offset;
	
	public SearchEngine(byte[] haystack, byte[] needle, int start, int end) {
		this.haystack = haystack;
		this.needle = needle;
		this.start = start;
		this.end = end;
		this.offset = start - 1;
	}
	
	public int nextOccurrence(boolean wrap) {
		for (int i = offset + 1; i < end - needle.length; i++) {
			if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) {
				offset = i;
				return i;
			}
		}
		if (wrap && offset != start) {
			offset = start - 1;
			return nextOccurrence(false);
		}
		return -1;
	}
	
	public int previousOccurrence(boolean wrap) {
		for (int i = offset - 1; i >= start; i--) {
			if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) {
				offset = i;
				return i;
			}
		}
		if (wrap && offset != start) {
			offset = end - needle.length + 1;
			return previousOccurrence(false);
		}
		return -1;
	}
	
	public static class BytesFormat extends java.text.Format {
		private static final long serialVersionUID = 1L;

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			ArrayList<Byte> bytes = new ArrayList<>();
			int lastDigit = -1;
			int i;
			for (i = pos.getIndex(); i < source.length(); i++) {
				int c = source.charAt(i);
				int digit;
				if (c >= '0' && c <= '9') {
					digit = c - '0';
				} else if (c >= 'a' && c <= 'f') {
					digit = c - 'a' + 10;
				} else if (c >= 'A' && c <= 'F') {
					digit = c - 'A' + 10;
				} else if (Character.isWhitespace(c)) {
					continue;
				} else {
					pos.setErrorIndex(i);
					return null;
				}
				if (lastDigit == -1) {
					lastDigit = digit;
				} else {
					bytes.add((byte) ((lastDigit << 4) | digit));
					lastDigit = -1;
				}
			}
			if (lastDigit != -1) {
				bytes.add((byte) (lastDigit << 4));
			}
			pos.setIndex(i);
			return bytes;
		}
		
		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			@SuppressWarnings("unchecked")
			ArrayList<Byte> bytes = (ArrayList<Byte>) obj;
			boolean space = false;
			for (byte b : bytes) {
				if (space) {
					toAppendTo.append(' ');
				} else {
					space = true;
				}
				toAppendTo.append(HexPanel.twoCharsHexByte(b));
			}
			return toAppendTo;
		}
	}
}
