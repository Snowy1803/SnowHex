package st.infos.elementalcube.snowhex.ui;

import java.awt.HeadlessException;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileSystemView;

public abstract class SnowHexFrame extends JFrame {
	private static final long serialVersionUID = -8095398602614431283L;
	public static final File APPDATA = new File((System.getenv("APPDATA") == null ? FileSystemView.getFileSystemView().getDefaultDirectory().getPath()
			: System.getenv("APPDATA") + "/") + "ElementalCube/snowhex");
	public static final boolean USE_NATIVE_FILE_DIALOG = System.getProperty("os.name", "").contains("Mac");


	public SnowHexFrame(String title) throws HeadlessException {
		super(title);
	}

	protected abstract void open(File file);
	
	protected class OpenFileTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 2749553292520468494L;
		
		@Override
		public boolean canImport(TransferSupport ts) {
			if ((COPY & ts.getSourceDropActions()) == COPY) {// Force copy
				ts.setDropAction(COPY);
			} else {
				return false;
			}
			return ts.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		}
		
		@Override
		public boolean importData(TransferSupport ts) {
			try {
				if (ts.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					@SuppressWarnings("unchecked")
					List<File> files = (List<File>) ts.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					for (File f : files) {
						open(f);
					}
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}
}