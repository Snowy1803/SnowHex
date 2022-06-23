package st.infos.elementalcube.snowhex;

/**
 * The name space for the token types constants
 */
public interface TokenTypes {
	/**
	 * This token is invalid, it will be red underlined
	 */
	public static final int TOKEN_ERRORED = 0;
	/**
	 * This token is a magic number, the identifier to this file type
	 */
	public static final int TOKEN_FILE_HEADER = 1;
	/**
	 * This is a comment, its content is not meaningful
	 */
	public static final int TOKEN_COMMENT = 2;
	/**
	 * This token is a chunk identifier
	 */
	public static final int TOKEN_CHUNK_HEADER = 3;
	/**
	 * This token is miscellaneous metadata
	 */
	public static final int TOKEN_METADATA = 4;
	/**
	 * This token corresponds to unused bytes, reserved for future use
	 */
	public static final int TOKEN_RESERVED = 5;
	/**
	 * This token is a chunk length
	 */
	public static final int TOKEN_LENGTH = 6;
	/**
	 * This secondary token is a color palette for an image
	 */
	public static final int TOKEN_IMAGE_PALETTE = 7;
	/**
	 * This token corresponds to a color
	 */
	public static final int TOKEN_IMAGE_COLOR = 8;
	/**
	 * This secondary token is the main data
	 */
	public static final int TOKEN_IMAGE_DATA = 9;
	/**
	 * This token is the size of the image, or a position in an image
	 */
	public static final int TOKEN_IMAGE_SIZE = 10;
	/**
	 * This secondary token is a data chunk
	 */
	public static final int TOKEN_CHUNK = 11;
}
