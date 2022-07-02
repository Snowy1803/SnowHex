package st.infos.elementalcube.snowhex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import st.infos.elementalcube.snowhex.SearchEngine;
import st.infos.elementalcube.snowylangapi.Lang;

public class FindFrame extends JDialog {
	private static final long serialVersionUID = -5479927946638950219L;
//	private HexFrame parent;
	private SearchEngine engine;
	private JButton find, close;
	private JCheckBox backwards, selection;

	public FindFrame(HexFrame parent) {
		super(parent, Lang.getString("frame.find"), false);
//		this.parent = parent;
		this.engine = new SearchEngine(parent.getEditor().getBytes(), null, 0, parent.getEditor().getBytes().length);
		JPanel content = new JPanel(new BorderLayout());
		
		JPanel center = new JPanel(new BorderLayout());
		
		SearchProvider[] providers = { new SearchBytes() };
		JTabbedPane tabs = new JTabbedPane();
		
		for (SearchProvider provider : providers) {
			tabs.addTab(provider.getTabName(), (JComponent) provider);
		}
		center.add(tabs);
		
		JPanel boxes = new JPanel(new GridLayout(0, 1));
		
		backwards = new JCheckBox("Search backwards");
		boxes.add(backwards);
		
		selection = new JCheckBox("Search in selection");
		selection.addActionListener(e -> {
			if (selection.isSelected()) {
				engine.start = parent.getEditor().getCaret().getFirstByte();
				engine.end = parent.getEditor().getCaret().getLastByte() + 1;
				parent.getEditor().setFindRange(new ImmutablePair<>(engine.start, engine.end - 1));
			} else {
				engine.start = 0;
				engine.end = parent.getEditor().getBytes().length;
				parent.getEditor().setFindRange(null);
			}
			engine.offset = engine.start;
			parent.getEditor().repaint(parent.getEditor().getVisibleRect());
		});
		boxes.add(selection);

		center.add(boxes, BorderLayout.PAGE_END);
		content.add(center);
		
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		
		find = new JButton("Find");
		find.addActionListener(e -> {
			engine.needle = providers[tabs.getSelectedIndex()].getNeedle();
			engine.haystack = parent.getEditor().getBytes();
			if (!selection.isSelected()) {
				engine.start = 0;
				engine.end = parent.getEditor().getBytes().length;
			}
			int match = backwards.isSelected() ? engine.previousOccurrence(true) : engine.nextOccurrence(true);
			if (match == -1) {
				UIManager.getLookAndFeel().provideErrorFeedback(find);
			} else {
				parent.getEditor().getCaret().setSelection(match, engine.needle.length);
			}
		});
		getRootPane().setDefaultButton(find);
		buttons.add(find);
		
		close = new JButton("Close");
		close.addActionListener(e -> {
			dispose();
		});
		buttons.add(close);
		
		content.add(buttons, BorderLayout.PAGE_END);
		
		setContentPane(content);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				parent.getEditor().setFindRange(null);
			}
		});
		
		// utility style on macos
		rootPane.putClientProperty("Window.style", "small");
		if (System.getProperty("os.name").contains("Mac"))
			setAlwaysOnTop(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		pack();
		setPreferredSize(getPreferredSize());
		setVisible(true);
	}

	interface SearchProvider {
		byte[] getNeedle();
		String getTabName();
	}
	
	class SearchBytes extends JPanel implements SearchProvider {
		private static final long serialVersionUID = 2583361921577801618L;
		private JFormattedTextField text;

		public SearchBytes() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			JLabel lab = new JLabel("Enter bytes to search in hexadecimal:");
			add(lab);
			text = new JFormattedTextField(new SearchEngine.BytesFormat());
			lab.setLabelFor(text);
			add(text);
			add(Box.createVerticalGlue());
		}
		
		@Override
		public String getTabName() {
			return "Hex Bytes";
		}

		@SuppressWarnings("unchecked")
		@Override
		public byte[] getNeedle() {
			return ArrayUtils.toPrimitive(((ArrayList<Byte>) text.getValue()).toArray(new Byte[0]));
		}
	}
}
