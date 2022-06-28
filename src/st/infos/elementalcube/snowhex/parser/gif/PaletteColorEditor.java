package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import st.infos.elementalcube.snowhex.ui.HexPanel;

/**
 * The color chooser for an individual color inside a palette
 * @author Emil
 *
 */
public class PaletteColorEditor extends JPanel {
	private static final long serialVersionUID = 3377734246008920485L;
	private JLabel text;
	private JColorChooser chooser;

	public PaletteColorEditor(HexPanel panel) {
		super(new BorderLayout());
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		text = new JLabel();
		text.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		text.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(text);
		chooser = new JColorChooser();
		chooser.setAlignmentX(Component.LEFT_ALIGNMENT);
		chooser.setBorder(null);
		chooser.getSelectionModel().addChangeListener(e -> {
			Color color = chooser.getColor();
			GIFToken token = ((GIFToken) panel.getClosestToken());
			panel.getBytes()[token.getOffset()] = (byte) color.getRed();
			panel.getBytes()[token.getOffset() + 1] = (byte) color.getGreen();
			panel.getBytes()[token.getOffset() + 2] = (byte) color.getBlue();
			panel.bytesDidChange();
		});
		content.add(chooser);
		add(content, BorderLayout.PAGE_START);
	}

	public void updateValues(HexPanel panel) {
		GIFToken token = ((GIFToken) panel.getClosestToken());
		text.setText("Palette color #" + token.getIndex());
		chooser.setColor(panel.getBytes()[token.getOffset()] & 0xff,
				panel.getBytes()[token.getOffset() + 1] & 0xff,
				panel.getBytes()[token.getOffset() + 2] & 0xff);
	}
}