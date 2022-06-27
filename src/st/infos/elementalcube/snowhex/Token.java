package st.infos.elementalcube.snowhex;

public interface Token {
	int getType();
	
	int getOffset();
	
	int getLength();
	
	default boolean hasToolTip() {
		return getToolTip() != null;
	}
	
	String getToolTip();
	
	Level getToolTipLevel();

	void init(int type, int offset, int length, String tooltip, Level tooltipLevel);
	
	default boolean at(int offset) {
		return getOffset() <= offset && offset < getOffset() + getLength();
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
