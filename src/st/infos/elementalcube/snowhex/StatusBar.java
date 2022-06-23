package st.infos.elementalcube.snowhex;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import st.infos.elementalcube.snowylangapi.Lang;

public class StatusBar extends JPanel implements ActionListener {
	private static final long serialVersionUID = 4468240980330665079L;
	private HexPanel panel;
	private JLabel colorer, caret, mode;
	
	public StatusBar(HexPanel panel) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.panel = panel;
		add(colorer = new JLabel(panel.getColorer() == null ? Lang.getString("parser.none") : panel.getColorer().getLocalizedName()));
		add(Box.createHorizontalGlue());
		add(caret = new JLabel(Lang.getString("frame.caretPos", panel.getCaretPosition() / 16, panel.getCaretPosition() % 16)));
		caret.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
		add(newJSeparator(JSeparator.VERTICAL));
		add(mode = new JLabel(Lang.getString("frame." + (panel.isInsertMode() ? "insert" : "overwrite"))));
		mode.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				panel.toggleInsertMode();
			}
		});
		mode.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
	}
	
	private static JSeparator newJSeparator(int orientation) {
		JSeparator sep = new JSeparator(orientation);
		Dimension dim = new Dimension(2, 20);
		sep.setPreferredSize(dim);
		sep.setMinimumSize(dim);
		sep.setMaximumSize(dim);
		return sep;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		colorer.setText(panel.getColorer() == null ? Lang.getString("parser.none") : panel.getColorer().getLocalizedName());
		caret.setText(Lang.getString("frame.caretPos", panel.getCaretPosition() / 16 + 1, panel.getCaretPosition() % 16));
		mode.setText(Lang.getString("frame." + (panel.isInsertMode() ? "insert" : "overwrite")));
	}
}
