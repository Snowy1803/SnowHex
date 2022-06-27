package st.infos.elementalcube.snowhex;

import java.util.Objects;

public class TokenImpl implements Token {
	/**
	 * One of the TokenTypes constants
	 */
	private int type;
	/**
	 * The offset in bytes of the token
	 */
	private int offset;
	/**
	 * The length in bytes of the token
	 */
	private int length;
	/**
	 * The tooltip message
	 */
	private String tooltip;
	/**
	 * The tooltip level
	 */
	private Level level;
	
	public TokenImpl(int type, int offset, int length, String tooltip, Level tooltipLevel) {
		init(type, offset, length, tooltip, tooltipLevel);
	}
	
	public TokenImpl() {
		this(0, 0, 0, null, null);
	}

	public void init(int type, int offset, int length, String tooltip, Level tooltipLevel) {
		this.type = type;
		this.offset = offset;
		this.length = length;
		this.tooltip = tooltip;
		this.level = tooltipLevel;
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
		return Objects.hash(length, level, offset, tooltip, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TokenImpl other = (TokenImpl) obj;
		return length == other.length && level == other.level && offset == other.offset
				&& Objects.equals(tooltip, other.tooltip) && type == other.type;
	}
}
