package st.infos.elementalcube.snowhex;

/**
 * The name space for the token types constants
 */
public interface TokenTypes {
	
	// MARK: - special
	
	/**
	 * This token has no syntactic type. It should only render as a notice.
	 */
	public static final int TOKEN_NONE = 0;
//	public static final int TOKEN_ERRORED = 0;
	
	// MARK: - chunks / secondaries
	
	/**
	 * This secondary token is a color palette for an image
	 */
	public static final int TOKEN_IMAGE_PALETTE = 7;
	/**
	 * This secondary token is the main data
	 */
	public static final int TOKEN_IMAGE_DATA = 9;
	/**
	 * This secondary token is a misc data chunk
	 */
	public static final int TOKEN_CHUNK = 11;
	
	// MARK: - primaries
	
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
	 * This token corresponds to a color
	 */
	public static final int TOKEN_IMAGE_COLOR = 8;
	/**
	 * This token is the size of the image, or a position in an image
	 */
	public static final int TOKEN_IMAGE_SIZE = 10;
	/**
	 * This token is compressed data, difficult to read
	 */
	public static final int TOKEN_COMPRESSED_DATA = 12;
	/**
	 * This token is a checksum
	 */
	public static final int TOKEN_CHECKSUM = 13;
	/**
	 * This token is a keyword
	 */
	public static final int TOKEN_KEYWORD = 14;
}
