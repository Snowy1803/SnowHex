package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.ui.HexPanel;

public class LSDPackedEditor extends JPanel {
	private static final long serialVersionUID = 3377734246008920485L;
	private JSpinner res, size;
	private JCheckBox gctPresent, sorted;

	public LSDPackedEditor(HexPanel panel) {
		super(new BorderLayout());
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipady = 5;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		gctPresent = new JCheckBox("Image has a Global Color Table (GCT)");
		gctPresent.addActionListener(e -> {
			int offs = panel.getClosestToken().getOffset();
			if (gctPresent.isSelected()) {
				panel.getBytes()[offs] |= 1 << 7;
			} else {
				panel.getBytes()[offs] &= ~(1 << 7);
			}
			panel.bytesDidChange();
		});
		content.add(gctPresent, c);

		sorted = new JCheckBox("GCT is sorted by importance of colors");
		sorted.addActionListener(e -> {
			int offs = panel.getClosestToken().getOffset();
			if (sorted.isSelected()) {
				panel.getBytes()[offs] |= 1 << 3;
			} else {
				panel.getBytes()[offs] &= ~(1 << 3);
			}
			panel.bytesDidChange();
		});
		content.add(sorted, c);

		JLabel lsize = new JLabel("GCT size:");
		c.gridwidth = 1;
		content.add(lsize, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		ArrayList<Integer> ints = new ArrayList<>(8);
		for (int i = 0; i < 8; i++) {
			ints.add(1 << (i + 1));
		}
		size = new JSpinner(new SpinnerListModel(ints));
		((JSpinner.ListEditor)size.getEditor()).getTextField().setHorizontalAlignment(JTextField.TRAILING);
		size.addChangeListener(e -> {
			int offs = panel.getClosestToken().getOffset();
			panel.getBytes()[offs] &= ~(0b111);
			panel.getBytes()[offs] |= ints.indexOf(size.getValue());
			panel.bytesDidChange();
		});
		content.add(size, c);
		
		JLabel lres = new JLabel("Bit depth:");
		c.gridwidth = 1;
		content.add(lres, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		res = new JSpinner(new SpinnerNumberModel(8, 0b000 + 1, 0b111 + 1, 1));
		res.addChangeListener(e -> {
			int offs = panel.getClosestToken().getOffset();
			panel.getBytes()[offs] &= ~(0b111 << 4);
			panel.getBytes()[offs] |= ((int) res.getValue() - 1) << 4;
			panel.bytesDidChange();
		});
		content.add(res, c);
		
		content.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5), 
				BorderFactory.createTitledBorder(null, "Logical Screen Descriptor packed fields")));
		add(content, BorderLayout.PAGE_START);
	}

	public void updateValues(HexPanel panel) {
		byte b = panel.getBytes()[panel.getClosestToken().getOffset()];
		gctPresent.setSelected((b & (1 << 7)) != 0);
		sorted.setSelected((b & (1 << 3)) != 0);
		res.setValue(((b >> 4) & 0b111) + 1);
		size.setValue(1 << ((b & 0b111) + 1));
	}
}