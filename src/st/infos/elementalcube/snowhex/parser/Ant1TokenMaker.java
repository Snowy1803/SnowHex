package st.infos.elementalcube.snowhex.parser;

import java.util.Arrays;
import java.util.List;

import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.TokenMaker;

public class Ant1TokenMaker extends TokenMaker {

	@Override
	public List<Token> generateTokens(byte[] array) {
		return Arrays.asList(
				createToken(TOKEN_IMAGE_PALETTE, 0, 8),
				createToken(TOKEN_IMAGE_DATA, 8, 16),
				createToken(TOKEN_CHUNK, 24, 32)
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
