package st.infos.elementalcube.snowhex;

import java.util.Arrays;
import java.util.List;

public class Ant1TokenMaker extends TokenMaker {

	@Override
	public List<Token> generateTokens(byte[] array) {
		return Arrays.asList(
				new Token(TOKEN_IMAGE_PALETTE, 0, 8),
				new Token(TOKEN_IMAGE_DATA, 8, 16),
				new Token(TOKEN_CHUNK, 24, 32)
		);
	}

	@Override
	public String getName() {
		return "ant1";
	}

	@Override
	public Object getDump(byte[] array) {
		return null;
	}

}
