package st.infos.elementalcube.snowhex;

public class Token {
	private final int type;
	private final int offset;
	private final int length;
	private String tooltip;
	private Level level;
	
	public Token(int type, int offset, int length, String tooltip, Level tooltipLevel) {
		this.type = type;
		this.offset = offset;
		this.length = length;
		this.tooltip = tooltip;
		this.level = tooltipLevel;
	}
	
	public Token(int type, int offset, int length) {
		this(type, offset, length, null, null);
	}
	
	public int getType() {
		return type;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public boolean hasToolTip() {
		return tooltip != null;
	}
	
	public String getToolTip() {
		return tooltip;
	}
	
	public Level getToolTipLevel() {
		return level;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + length;
		result = prime * result + offset;
		result = prime * result + type;
		result = prime * result + (tooltip == null ? 0 : tooltip.hashCode());
		result = prime * result + (level == null ? 0 : level.ordinal());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Token)) return false;
		Token other = (Token) obj;
		if (!(tooltip == null ^ other.tooltip == null)) return false;
		return type == other.type && offset == other.offset && length == other.length
				&& (tooltip != null ? level.equals(other.level) && tooltip.equals(other.tooltip) : true);
	}
	
	public boolean is(int type, int offset, int length) {
		return this.tooltip == null && this.type == type && this.offset == offset && this.length == length;
	}
	
	public boolean is(int type, int offset, int length, String desc, Level lvl) {
		return this.tooltip != null && this.type == type && this.offset == offset && this.length == length && this.tooltip.equals(desc)
				&& level.equals(lvl);
	}
	
	public boolean at(int offset) {
		return this.offset <= offset && offset < this.offset + this.length;
	}
	
	public enum Level {
		/**
		 * Just information about the token. Shows no icon
		 */
		INFO,
		/**
		 * An unimportant notice
		 */
		NOTICE,
		/**
		 * A warning about the token
		 */
		WARNING,
		/**
		 * This token is invalid
		 */
		ERROR;
	}
}
