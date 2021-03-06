package st.infos.elementalcube.snowhex.ui;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
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

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultEditorKit;

import org.apache.commons.io.FileUtils;

import st.infos.elementalcube.snowhex.DefaultHexDocument;
import st.infos.elementalcube.snowhex.HexDocument;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowylangapi.Lang;
import st.infos.elementalcube.snowylangapi.LangLoader;
import st.infos.elementalcube.snowymage.SnowImageReaderSpi;
import st.infos.elementalcube.snowymage.SnowImageWriterSpi;

public class HexFrame extends SnowHexFrame {
	private static final long serialVersionUID = 125057354483869125L;
	private File file;
	private HexPanel editor;
	private PreviewFrame preview;
	private PropertiesFrame props;
	private FindFrame findDialog;
	
	public HexFrame(File file, HexPanel panel) {
		super(Lang.getString("frame.title"));
		this.file = file;
		editor = panel;
		JScrollPane pane = new JScrollPane(editor);
		JPanel border = new JPanel(new BorderLayout());
		border.add(pane);
		StatusBar bar = new StatusBar(editor);
		editor.addChangeListener(bar);
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
		setLocationByPlatform(true);
		setVisible(true);
	}
	
	public HexFrame(HexDocument document) {
		this(null, new HexPanel(document));
	}
	
	public HexFrame(File file) {
		this(new DefaultHexDocument());
		open(file);
	}
	
	private void createJMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu(Lang.getString("menu.file")),
				edit = new JMenu(Lang.getString("menu.edit")),
				view = new JMenu(Lang.getString("menu.view")),
				coloring = new JMenu(Lang.getString("menu.view.coloring")),
				lang = new JMenu(Lang.getString("menu.view.lang"));
		
		JMenuItem create = new JMenuItem(Lang.getString("menu.file.new")),
				open = new JMenuItem(Lang.getString("menu.file.open")),
				save = new JMenuItem(Lang.getString("menu.file.save")),
				find = new JMenuItem(Lang.getString("frame.find")),
				showDump = new JCheckBoxMenuItem(Lang.getString("menu.view.showDump")),
				showResult = new JCheckBoxMenuItem(Lang.getString("menu.view.showResult")),
				showProps = new JCheckBoxMenuItem(Lang.getString("menu.view.showProps"));
		
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
					dispose();
					new HexFrame(this.file, editor);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
			lang.add(item);
		}
		
		create.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, getToolkit().getMenuShortcutKeyMaskEx()));
		create.addActionListener(e -> {
			if (this.file != null) {
				DefaultHexDocument doc = new DefaultHexDocument();
				doc.replaceDocument(new byte[1]);
				new HexFrame(doc);
			} else {
				editor.getDocument().replaceDocument(new byte[1]);
				editor.repaint();
			}
		});
		
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, getToolkit().getMenuShortcutKeyMaskEx()));
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
		
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, getToolkit().getMenuShortcutKeyMaskEx()));
		save.addActionListener(e -> {
			File saveFile;
			if (USE_NATIVE_FILE_DIALOG) {
				FileDialog fc = new FileDialog(HexFrame.this, "", FileDialog.SAVE);
				fc.setModalityType(ModalityType.DOCUMENT_MODAL);
				fc.setDirectory(this.file.getParent());
				fc.setFile(this.file.getName());
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
				FileUtils.writeByteArrayToFile(this.file, editor.getDocument().getBytes());
				getRootPane().putClientProperty("Window.documentModified", false);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});

		AbstractAction undoAction = new AbstractAction() {
			private static final long serialVersionUID = -5865428512521623261L;
			@Override
			public void actionPerformed(ActionEvent e) {
				editor.getDocument().undo();
			}
		};
		undoAction.setEnabled(false);
		undoAction.putValue(Action.NAME, Lang.getString("menu.edit.undo"));
		JMenuItem undo = new JMenuItem(undoAction);
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, getToolkit().getMenuShortcutKeyMaskEx()));
		
		AbstractAction redoAction = new AbstractAction() {
			private static final long serialVersionUID = -5865428512521623261L;
			@Override
			public void actionPerformed(ActionEvent e) {
				editor.getDocument().redo();
			}
		};
		redoAction.setEnabled(false);
		redoAction.putValue(Action.NAME, Lang.getString("menu.edit.redo"));
		JMenuItem redo = new JMenuItem(redoAction);
		if (UIManager.getLookAndFeel().getID().equals("Aqua")) {
			redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, getToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
		} else {
			redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, getToolkit().getMenuShortcutKeyMaskEx()));
		}
		
		editor.getDocument().addEditListener(e -> {
			undoAction.setEnabled(editor.getDocument().canUndo());
			redoAction.setEnabled(editor.getDocument().canRedo());
		});
		
		JMenuItem cut = new JMenuItem(editor.getActionMap().get(DefaultEditorKit.cutAction));
		cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, getToolkit().getMenuShortcutKeyMaskEx()));
		cut.setText(Lang.getString("menu.edit.cut"));
		JMenuItem copy = new JMenuItem(editor.getActionMap().get(DefaultEditorKit.copyAction));
		copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, getToolkit().getMenuShortcutKeyMaskEx()));
		copy.setText(Lang.getString("menu.edit.copy"));
		JMenuItem paste = new JMenuItem(editor.getActionMap().get(DefaultEditorKit.pasteAction));
		paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, getToolkit().getMenuShortcutKeyMaskEx()));
		paste.setText(Lang.getString("menu.edit.paste"));
		JMenuItem delByte = new JMenuItem(editor.getActionMap().get("delByte"));
		delByte.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, getToolkit().getMenuShortcutKeyMaskEx()));
		JMenuItem selectAll = new JMenuItem(editor.getActionMap().get(DefaultEditorKit.selectAllAction));
		selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, getToolkit().getMenuShortcutKeyMaskEx()));
		selectAll.setText(Lang.getString("menu.edit.selectAll"));
		
		find.setSelected(false);
		find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, getToolkit().getMenuShortcutKeyMaskEx()));
		find.addActionListener(e -> {
			if (findDialog == null || !findDialog.isVisible()) {
				findDialog = new FindFrame(this);
			} else {
				findDialog.requestFocus();
			}
		});
		
		showDump.setSelected(true);
		showDump.addActionListener(e -> {
			editor.setShowDump(!editor.isShowingDump());
			editor.repaint(editor.getVisibleRect());
		});
		
		showResult.setSelected(false);
		showResult.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
		showResult.addActionListener(e -> {
			if (preview == null) {
				preview = new PreviewFrame(this);
				preview.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						showResult.setSelected(false);
					}
				});
				editor.addChangeListener(preview);
			} else {
				boolean value = !preview.isVisible();
				showResult.setSelected(value);
				preview.setVisible(value);
			}
		});
		
		showProps.setSelected(false);
		showProps.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		showProps.addActionListener(e -> {
			if (props == null) {
				props = new PropertiesFrame(this);
				props.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						showProps.setSelected(false);
					}
				});
				editor.addChangeListener(props);
			} else {
				boolean value = !props.isVisible();
				showProps.setSelected(value);
				props.setVisible(value);
			}
		});
		
		file.add(create);
		file.add(open);
		file.add(save);

		edit.add(undo);
		edit.add(redo);
		edit.addSeparator();
		edit.add(cut);
		edit.add(copy);
		edit.add(paste);
		edit.add(delByte);
		edit.add(selectAll);
		edit.addSeparator();
		edit.add(find);
		
		view.add(coloring);
		view.add(lang);
		view.add(showDump);
		view.add(showResult);
		view.add(showProps);
		
		bar.add(file);
		bar.add(edit);
		bar.add(view);
		
		setJMenuBar(bar);
	}
	
	@Override
	protected void open(File file) {
		if (this.file != null) {
			new HexFrame(file);
		} else {
			this.setFile(file);
			try {
				editor.getDocument().replaceDocument(FileUtils.readFileToByteArray(file));
				editor.setColorer(getColorer(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
			editor.repaint();
		}
	}
	
	private void setFile(File file) {
		this.file = file;
		setTitle(Lang.getString("frame.title.file", file.getName()));
		// macOS draggable icon
		rootPane.putClientProperty("Window.documentFile", file);
	}
	
	public static TokenMaker getColorer(File f) {
		return getColorer(f.getName());
	}
	
	public static TokenMaker getColorer(String filename) {
		String ext = filename;
		ext = (ext.contains(".") ? ext.substring(ext.lastIndexOf('.') + 1) : null);
		return TokenMaker.getTokenMaker(ext);
	}
	
	public HexPanel getEditor() {
		return editor;
	}
	
	public File getFile() {
		return file;
	}
	
	public static void main(String[] args) {
		IIORegistry.getDefaultInstance().registerServiceProvider(new SnowImageReaderSpi());
		IIORegistry.getDefaultInstance().registerServiceProvider(new SnowImageWriterSpi());
		for (String arg : args) {
			new HexFrame(new File(arg));
		}
		if (args.length == 0) {
			new InitialFrame();
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
}
