package st.infos.elementalcube.snowhex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import st.infos.elementalcube.snowhex.SearchEngine;
import st.infos.elementalcube.snowylangapi.Lang;

public class FindFrame extends JDialog {
	private static final long serialVersionUID = -5479927946638950219L;
	private HexFrame parent;
	private SearchEngine engine;
	private JButton find, close;
	private JCheckBox backwards, selection;

	public FindFrame(HexFrame parent) {
		super(parent, Lang.getString("frame.find"), false);
		this.parent = parent;
		this.engine = new SearchEngine(parent.getEditor().getDocument(), null, 0, parent.getEditor().getDocument().getLength());
		JPanel content = new JPanel(new BorderLayout());
		
		JPanel center = new JPanel(new BorderLayout());
		
		SearchProvider[] providers = { new SearchBytes(), new SearchString(), new SearchNumber() };
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
				engine.end = parent.getEditor().getDocument().getLength();
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
			if (!selection.isSelected()) {
				engine.start = 0;
				engine.end = parent.getEditor().getDocument().getLength();
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
		rootPane.setDefaultButton(find);
		if (System.getProperty("os.name").contains("Mac"))
			setAlwaysOnTop(true);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
		getRootPane().getActionMap().put("close", new AbstractAction() {
			private static final long serialVersionUID = 5031761572638786701L;

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
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
	
	class SearchString extends JPanel implements SearchProvider {
		private static final long serialVersionUID = 2583361921577801618L;
		private JTextField text;

		public SearchString() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			JLabel lab = new JLabel("Enter an ASCII string to search:");
			add(lab);
			text = new JTextField();
			lab.setLabelFor(text);
			add(text);
			add(Box.createVerticalGlue());
		}
		
		@Override
		public String getTabName() {
			return "String";
		}

		@Override
		public byte[] getNeedle() {
			return text.getText().getBytes(StandardCharsets.UTF_8);
		}
	}
	
	class SearchNumber extends JPanel implements SearchProvider {
		private static final long serialVersionUID = 2583361921577801618L;
		private JFormattedTextField text;
		ButtonGroup sizes, endians;
		JRadioButton int8, int16, int32, int64, f32, f64, be, le;

		public SearchNumber() {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			JLabel lab = new JLabel("Enter a decimal number to search:");
			lab.setAlignmentX(0);
			add(lab);
			text = new JFormattedTextField(NumberFormat.getIntegerInstance());
			text.setAlignmentX(0);
			lab.setLabelFor(text);
			add(text);
			
			sizes = new ButtonGroup();
			int8 = new JRadioButton("8-bit integer");
			int8.addActionListener(this::switchToInt);
			sizes.add(int8);
			int16 = new JRadioButton("16-bit integer");
			int8.addActionListener(this::switchToInt);
			sizes.add(int16);
			int32 = new JRadioButton("32-bit integer", true);
			int8.addActionListener(this::switchToInt);
			sizes.add(int32);
			int64 = new JRadioButton("64-bit integer");
			int8.addActionListener(this::switchToInt);
			sizes.add(int64);
			f32 = new JRadioButton("32-bit float");
			f32.addActionListener(this::switchToFloating);
			sizes.add(f32);
			f64 = new JRadioButton("64-bit float");
			f64.addActionListener(this::switchToFloating);
			sizes.add(f64);
			JPanel psizes = new JPanel(new GridLayout(0, 2));
			psizes.setAlignmentX(0);
			psizes.add(int8);
			psizes.add(int16);
			psizes.add(int32);
			psizes.add(int64);
			psizes.add(f32);
			psizes.add(f64);
			add(psizes);
			
			add(new JSeparator());
			
			ByteOrder defaultEndianness = ByteOrder.nativeOrder();
			if (parent.getEditor().getColorer() != null &&
					parent.getEditor().getColorer().getEndianness() != null) {
				defaultEndianness = parent.getEditor().getColorer().getEndianness();
			}
			
			endians = new ButtonGroup();
			be = new JRadioButton("Big Endian", defaultEndianness.equals(ByteOrder.BIG_ENDIAN));
			endians.add(be);
			le = new JRadioButton("Little Endian", defaultEndianness.equals(ByteOrder.LITTLE_ENDIAN));
			endians.add(le);
			JPanel pend = new JPanel(new GridLayout(0, 2));
			pend.setAlignmentX(0);
			pend.add(be);
			pend.add(le);
			add(pend);
			
			add(Box.createVerticalGlue());
		}
		
		public void switchToInt(ActionEvent e) {
			NumberFormatter f = new NumberFormatter(NumberFormat.getIntegerInstance());
			int w = getNumberWidth();
			if (w != 64) {
				f.setMinimum(-(1L << (w - 1)));
				f.setMaximum((1L << (w - 1)) + 1);
			}
			text.setFormatterFactory(new DefaultFormatterFactory(f));
		}
		
		public void switchToFloating(ActionEvent e) {
			NumberFormatter f = new NumberFormatter(NumberFormat.getNumberInstance());
			text.setFormatterFactory(new DefaultFormatterFactory(f));
		}
		
		@Override
		public String getTabName() {
			return "Number";
		}
		
		public int getNumberWidth() {
			ButtonModel model = sizes.getSelection();
			if (model == int8.getModel()) {
				return 1;
			} else if (model == int16.getModel()) {
				return 2;
			} else if (model == int32.getModel() || model == f32.getModel()) {
				return 4;
			} else if (model == int64.getModel() || model == f64.getModel()) {
				return 8;
			}
			return 0;
		}

		@Override
		public byte[] getNeedle() {
			ByteBuffer bb = ByteBuffer.allocate(getNumberWidth());
			bb.order(be.isSelected() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
			ButtonModel model = sizes.getSelection();
			if (model == int8.getModel()) {
				bb.put(((Number) text.getValue()).byteValue());
			} else if (model == int16.getModel()) {
				bb.putShort(((Number) text.getValue()).shortValue());
			} else if (model == int32.getModel()) {
				bb.putInt(((Number) text.getValue()).intValue());
			} else if (model == int64.getModel()) {
				bb.putLong(((Number) text.getValue()).longValue());
			} else if (model == f32.getModel()) {
				bb.putFloat(((Number) text.getValue()).floatValue());
			} else if (model == f64.getModel()) {
				bb.putDouble(((Number) text.getValue()).doubleValue());
			}
			return bb.array();
		}
	}
}
