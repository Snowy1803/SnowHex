package st.infos.elementalcube.snowhex;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dialog.ModalityType;
import java.awt.FileDialog;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;

import st.infos.elementalcube.snowylangapi.Lang;
import st.infos.elementalcube.snowylangapi.LangLoader;
import st.infos.elementalcube.snowymage.SnowImageReaderSpi;
import st.infos.elementalcube.snowymage.SnowImageWriterSpi;

public class HexFrame extends JFrame {
	private static final long serialVersionUID = 125057354483869125L;
	public static final File APPDATA = new File((System.getenv("APPDATA") == null ? FileSystemView.getFileSystemView().getDefaultDirectory().getPath()
		: System.getenv("APPDATA") + "/") + "ElementalCube/snowhex");
	public static final boolean USE_NATIVE_FILE_DIALOG = System.getProperty("os.name", "").contains("Mac");
	private File file;
	private HexPanel editor;
	private PreviewFrame preview;
	
	public HexFrame(HexPanel panel) {
		super(Lang.getString("frame.title"));
		JScrollPane pane = new JScrollPane(editor = panel);
		pane.getVerticalScrollBar().setUnitIncrement(16);
		JPanel border = new JPanel(new BorderLayout());
		border.add(pane);
		StatusBar bar = new StatusBar(panel);
		panel.addChangeListener(bar);
		border.add(bar, BorderLayout.PAGE_END);
		setContentPane(border);
		createJMenuBar();
		setTransferHandler(new OpenFileTransferHandler());
		try {
			setIconImage(ImageIO.read(getClass().getResourceAsStream("/img/icon.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(920, 480);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public HexFrame(File file) {
		this();
		open(file);
	}
	
	public HexFrame() {
		this(new HexPanel(null));
	}
	
	private void createJMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu(Lang.getString("menu.file")), view = new JMenu(Lang.getString("menu.view")), coloring = new JMenu(Lang.getString(
				"menu.view.coloring")), lang = new JMenu(Lang.getString("menu.view.lang"));
		
		JMenuItem create = new JMenuItem(Lang.getString("menu.file.new")), open = new JMenuItem(Lang.getString("menu.file.open")),
				save = new JMenuItem(Lang.getString("menu.file.save")), showDump = new JCheckBoxMenuItem(Lang.getString("menu.view.showDump")),
				showResult = new JCheckBoxMenuItem(Lang.getString("menu.view.showResult"));
		
		ArrayList<String> tokenmakers = new ArrayList<>(TokenMaker.getParsers());
		tokenmakers.sort(null);
		ButtonGroup group = new ButtonGroup();
		for (String ext : tokenmakers) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(Lang.getString("parser." + ext));
			item.addActionListener(e -> {
				editor.setColorer(TokenMaker.getTokenMaker(ext));
				editor.repaint();
			});
			group.add(item);
			coloring.add(item);
		}
		JRadioButtonMenuItem nocolor = new JRadioButtonMenuItem(Lang.getString("parser.none"));
		nocolor.addActionListener(e -> {
			editor.setColorer(null);
			editor.repaint();
		});
		group.add(nocolor);
		coloring.add(nocolor);
				
		HashMap<String, String> map = new LangLoader().langList();
		for (String key : map.keySet()) {
			JMenuItem item = new JMenuItem(map.get(key));
			item.addActionListener(e -> {
				Lang.getInstance().setLocale(key);
				try {
					setVisible(false);
					new HexFrame(editor);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
			lang.add(item);
		}
		
		create.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, getToolkit().getMenuShortcutKeyMask()));
		create.addActionListener(e -> {
			if (this.file != null) {
				new HexFrame(new HexPanel(new byte[1]));
			} else {
				editor.setBytes(new byte[1]);
				editor.repaint();
			}
		});
		
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, getToolkit().getMenuShortcutKeyMask()));
		open.addActionListener(e -> {
			if (USE_NATIVE_FILE_DIALOG) {
				FileDialog fc = new FileDialog(HexFrame.this, "", FileDialog.LOAD);
				fc.setModalityType(ModalityType.DOCUMENT_MODAL);
				fc.setDirectory(this.file == null ? null : this.file.getParentFile().getAbsolutePath());
				fc.setVisible(true);
				if (fc.getFile() != null)
					open(fc.getFiles()[0]);
			} else {
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(this.file == null ? null : this.file.getParentFile());
				for (String ext : tokenmakers) {
					fc.addChoosableFileFilter(new FileNameExtensionFilter(Lang.getString("parser." + ext), ext));
				}
				fc.addChoosableFileFilter(null);
				fc.showOpenDialog(HexFrame.this);
				if (fc.getSelectedFile() != null) {
					open(fc.getSelectedFile());
				}
			}
		});
		
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, getToolkit().getMenuShortcutKeyMask()));
		save.addActionListener(e -> {
			File saveFile;
			if (USE_NATIVE_FILE_DIALOG) {
				FileDialog fc = new FileDialog(HexFrame.this, "", FileDialog.SAVE);
				fc.setModalityType(ModalityType.DOCUMENT_MODAL);
				fc.setFile(this.file.getAbsolutePath());
				fc.setVisible(true);
				if (fc.getFile() != null)
					saveFile = fc.getFiles()[0];
				else
					return;
			} else {
				JFileChooser fc = new JFileChooser();
				fc.setSelectedFile(this.file);
				fc.showSaveDialog(HexFrame.this);
				if (fc.getSelectedFile() != null)
					saveFile = fc.getSelectedFile();
				else
					return;
			}
			this.setFile(saveFile);
			try {
				FileUtils.writeByteArrayToFile(this.file, editor.getBytes());
				getRootPane().putClientProperty("Window.documentModified", false);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		showDump.setSelected(true);
		showDump.addActionListener(e -> {
			editor.setShowDump(!editor.isShowingDump());
			editor.repaint(editor.getVisibleRect());
		});
		
		showResult.setSelected(false);
		showResult.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, getToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK));
		showResult.addActionListener(e -> {
			if (preview == null) {
				preview = new PreviewFrame(this);
				preview.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						showResult.setSelected(false);
						preview = null;
					}
				});
				editor.addChangeListener(preview);
			} else {
				showResult.setSelected(false);
				preview.dispose();
				preview = null;
			}
		});
		
		file.add(create);
		file.add(open);
		file.add(save);
		
		view.add(coloring);
		view.add(lang);
		view.add(showDump);
		view.add(showResult);
		
		bar.add(file);
		bar.add(view);
		
		setJMenuBar(bar);
	}
	
	private void open(File file) {
		if (this.file != null) {
			HexFrame f = new HexFrame(file);
			f.editor.setColorer(getColorer(file));
			f.editor.repaint();
		} else {
			this.setFile(file);
			setTitle(Lang.getString("frame.title.file", file.getAbsolutePath()));
			try {
				editor.setBytes(FileUtils.readFileToByteArray(file));
				editor.setColorer(getColorer(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
			editor.repaint();
		}
	}
	
	private void setFile(File file) {
		this.file = file;
		// macOS draggable icon
		rootPane.putClientProperty("Window.documentFile", file);
	}
	
	public static TokenMaker getColorer(File f) {
		String ext = f.getName();
		ext = (ext.contains(".") ? ext.substring(ext.indexOf('.') + 1) : null);
		return TokenMaker.getTokenMaker(ext);
	}
	
	public HexPanel getEditor() {
		return editor;
	}
	
	public static void main(String[] args) {
		IIORegistry.getDefaultInstance().registerServiceProvider(new SnowImageReaderSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new SnowImageWriterSpi());
		HexFrame f = new HexFrame();
		for (String arg : args) {
			f.open(new File(arg));
		}
		try {
			File path = new File(APPDATA, "path.txt");
			if (!path.exists()) path.createNewFile();
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path)));
			pw.println(URLDecoder.decode(HexFrame.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class OpenFileTransferHandler extends TransferHandler {
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
