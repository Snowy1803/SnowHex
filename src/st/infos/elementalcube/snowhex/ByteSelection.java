package st.infos.elementalcube.snowhex;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import st.infos.elementalcube.snowhex.ui.HexPanel;

public class ByteSelection implements Transferable {
	
	public static final DataFlavor BYTE_ARRAY = new DataFlavor(byte[].class, "Byte array");
	private static final DataFlavor[] FLAVORS = { BYTE_ARRAY, DataFlavor.stringFlavor };
	
	private byte[] array;

	public ByteSelection(byte[] array) {
		this.array = array;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return FLAVORS;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Arrays.stream(FLAVORS).anyMatch(flavor::equals);
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor == DataFlavor.stringFlavor) {
			StringBuilder sb = new StringBuilder(array.length * 2);
			for (byte b : array) {
				sb.append(HexPanel.twoCharsHexByte(b));
			}
			return sb.toString();
		}
		return array;
	}

}
