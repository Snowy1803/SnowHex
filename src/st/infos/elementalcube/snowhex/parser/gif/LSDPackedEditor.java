package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import st.infos.elementalcube.snowhex.HexDocument.EditType;
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
				panel.getDocument().setByte(offs, (byte) (panel.getDocument().getByte(offs) | (1 << 7)), EditType.PROPERTY_CHANGE);
			} else {
				panel.getDocument().setByte(offs, (byte) (panel.getDocument().getByte(offs) & ~(1 << 7)), EditType.PROPERTY_CHANGE);
			}
		});
		content.add(gctPresent, c);

		sorted = new JCheckBox("GCT is sorted by importance of colors");
		sorted.addActionListener(e -> {
			int offs = panel.getClosestToken().getOffset();
			if (sorted.isSelected()) {
				panel.getDocument().setByte(offs, (byte) (panel.getDocument().getByte(offs) | (1 << 3)), EditType.PROPERTY_CHANGE);
			} else {
				panel.getDocument().setByte(offs, (byte) (panel.getDocument().getByte(offs) & ~(1 << 3)), EditType.PROPERTY_CHANGE);
			}
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
			panel.getDocument().setByte(offs, (byte) ((panel.getDocument().getByte(offs) & ~0b111) | ints.indexOf(size.getValue())),
					EditType.PROPERTY_CHANGE);
		});
		content.add(size, c);
		
		JLabel lres = new JLabel("Bit depth:");
		c.gridwidth = 1;
		content.add(lres, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		res = new JSpinner(new SpinnerNumberModel(8, 0b000 + 1, 0b111 + 1, 1));
		res.addChangeListener(e -> {
			int offs = panel.getClosestToken().getOffset();
			panel.getDocument().setByte(offs, (byte) ((panel.getDocument().getByte(offs) & ~(0b111 << 4)) | (((int) res.getValue() - 1) << 4)),
					EditType.PROPERTY_CHANGE);
		});
		content.add(res, c);
		
		content.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5), 
				BorderFactory.createTitledBorder(null, "Logical Screen Descriptor packed fields")));
		add(content, BorderLayout.PAGE_START);
	}

	public void updateValues(HexPanel panel) {
		byte b = panel.getDocument().getByte(panel.getClosestToken().getOffset());
		gctPresent.setSelected((b & (1 << 7)) != 0);
		sorted.setSelected((b & (1 << 3)) != 0);
		res.setValue(((b >> 4) & 0b111) + 1);
		size.setValue(1 << ((b & 0b111) + 1));
	}
}