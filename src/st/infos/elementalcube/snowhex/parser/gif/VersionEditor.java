package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import st.infos.elementalcube.snowhex.ui.HexPanel;

public class VersionEditor extends JPanel {
	private static final long serialVersionUID = 3377734246008920485L;
	private JComboBox<String> combo;

	public VersionEditor(HexPanel panel) {
		super(new BorderLayout());
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		JLabel text = new JLabel("GIF File Header");
		text.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		text.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(text);
		combo = new JComboBox<>(new String[] { "Version 87a (May 1987)", "Version 89a (July 1989)" });
		combo.setAlignmentX(Component.LEFT_ALIGNMENT);
		combo.addActionListener(e -> {
			panel.getBytes()[panel.getClosestToken().getOffset() + 4] = (byte) (combo.getSelectedIndex() == 1 ? '9' : '7');
			panel.bytesDidChange();
		});
		content.add(combo);
		add(content, BorderLayout.PAGE_START);
	}

	public void updateValues(HexPanel panel) {
		// only have 87a and 89a
		combo.setSelectedIndex(panel.getBytes()[panel.getClosestToken().getOffset() + 4] == '9' ? 1 : 0);
	}
}