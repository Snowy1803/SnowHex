package st.infos.elementalcube.snowhex.ui;

import java.awt.Dialog.ModalityType;
import java.awt.FileDialog;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import st.infos.elementalcube.snowhex.DefaultHexDocument;
import st.infos.elementalcube.snowylangapi.Lang;
import st.infos.elementalcube.snowylangapi.LangLoader;

public class InitialFrame extends SnowHexFrame {
	private static final long serialVersionUID = 125057354483869125L;
	
	public InitialFrame() {
		super(Lang.getString("frame.title"));
		JLabel label = new JLabel(Lang.getString("frame.empty"));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(label);
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
		
	private void createJMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu(Lang.getString("menu.file")),
				view = new JMenu(Lang.getString("menu.view")),
				lang = new JMenu(Lang.getString("menu.view.lang"));
		
		JMenuItem create = new JMenuItem(Lang.getString("menu.file.new")),
				open = new JMenuItem(Lang.getString("menu.file.open"));
						
		HashMap<String, String> map = new LangLoader().langList();
		for (String key : map.keySet()) {
			JMenuItem item = new JMenuItem(map.get(key));
			item.addActionListener(e -> {
				Lang.getInstance().setLocale(key);
				try {
					dispose();
					new InitialFrame();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
			lang.add(item);
		}
		
		create.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, getToolkit().getMenuShortcutKeyMaskEx()));
		create.addActionListener(e -> {
			DefaultHexDocument doc = new DefaultHexDocument();
			doc.replaceDocument(new byte[1]);
			dispose();
			new HexFrame(doc);
		});
		
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, getToolkit().getMenuShortcutKeyMaskEx()));
		open.addActionListener(e -> {
			if (HexFrame.USE_NATIVE_FILE_DIALOG) {
				FileDialog fc = new FileDialog(InitialFrame.this, "", FileDialog.LOAD);
				fc.setModalityType(ModalityType.DOCUMENT_MODAL);
				fc.setVisible(true);
				if (fc.getFile() != null)
					open(fc.getFiles()[0]);
			} else {
				JFileChooser fc = new JFileChooser();
				fc.addChoosableFileFilter(null);
				fc.showOpenDialog(InitialFrame.this);
				if (fc.getSelectedFile() != null) {
					open(fc.getSelectedFile());
				}
			}
		});
		
		file.add(create);
		file.add(open);
		
		view.add(lang);
		
		bar.add(file);
		bar.add(view);
		
		setJMenuBar(bar);
	}
	
	@Override
	protected void open(File file) {
		dispose();
		new HexFrame(file);
	}
}
