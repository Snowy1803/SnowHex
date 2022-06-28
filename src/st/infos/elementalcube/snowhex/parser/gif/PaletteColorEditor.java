package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import st.infos.elementalcube.snowhex.TokenTypes;
import st.infos.elementalcube.snowhex.ui.HexPanel;

/**
 * The color chooser for an individual color inside a palette
 * @author Emil
 *
 */
public class PaletteColorEditor extends JPanel {
	private static final long serialVersionUID = 3377734246008920485L;
	private JLabel indexText, hexText;
	private JButton change;
	private JPanel preview;
	private JColorChooser chooser;
	private JDialog chooserDialog;

	public PaletteColorEditor(HexPanel panel) {
		super(new BorderLayout());
		JPanel content = new JPanel(new BorderLayout(10, 10));
		
		JPanel grid = new JPanel(new GridLayout(0, 1));
		
		indexText = new JLabel();
		grid.add(indexText);
		
		hexText = new JLabel();
		grid.add(hexText);
		
		change = new JButton("Edit");
		change.addActionListener(e -> {
			if (chooserDialog == null) {
				// ok and cancel actually do the same, as we update in real time
				chooserDialog = JColorChooser.createDialog(this, indexText.getText(), false, chooser, ok -> {
					commitColor(panel, chooser.getColor());
				}, cancel -> {
					commitColor(panel, chooser.getColor());
				});
			}
			chooserDialog.setVisible(true);
		});
		grid.add(change);
		
		content.add(grid, BorderLayout.CENTER);
		
		preview = new JPanel();
		preview.setPreferredSize(new Dimension(100, 100));
		content.add(preview, BorderLayout.LINE_START);
		
		chooser = new JColorChooser();
		chooser.setAlignmentX(Component.LEFT_ALIGNMENT);
		chooser.setBorder(null);
		chooser.getSelectionModel().addChangeListener(e -> commitColor(panel, chooser.getColor()));
		
		add(content, BorderLayout.PAGE_START);
	}
	
	private void commitColor(HexPanel panel, Color color) {
		GIFToken token = ((GIFToken) panel.getClosestToken());
		// as the color chooser dialog stays open, it can 'commit' when a color isn't selected
		if (token == null || token.getType() != TokenTypes.TOKEN_IMAGE_COLOR || token.getSubtype() != GIFToken.SUBTY_PALETTE_RGB)
			return;
		panel.getBytes()[token.getOffset()] = (byte) color.getRed();
		panel.getBytes()[token.getOffset() + 1] = (byte) color.getGreen();
		panel.getBytes()[token.getOffset() + 2] = (byte) color.getBlue();
		panel.bytesDidChange();
	}

	public void updateValues(HexPanel panel) {
		GIFToken token = ((GIFToken) panel.getClosestToken());
		Color color = new Color(panel.getBytes()[token.getOffset()] & 0xff,
				panel.getBytes()[token.getOffset() + 1] & 0xff,
				panel.getBytes()[token.getOffset() + 2] & 0xff);
		
		indexText.setText("Palette color #" + token.getIndex());
		if (chooserDialog != null)
			chooserDialog.setTitle(indexText.getText());
		hexText.setText("#" + GIFTokenMaker.parseColor(panel.getBytes(), token.getOffset()));
		preview.setBackground(color);
		chooser.setColor(color);
	}
}