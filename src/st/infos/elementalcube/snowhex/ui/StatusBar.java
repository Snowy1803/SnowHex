package st.infos.elementalcube.snowhex.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowylangapi.Lang;

public class StatusBar extends JPanel implements ActionListener {
	private static final long serialVersionUID = 4468240980330665079L;
	private HexPanel panel;
	private JLabel colorer, caret, mode, errors, warnings;
	
	public StatusBar(HexPanel panel) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.panel = panel;
		add(colorer = new JLabel());
		add(errors = new JLabel());
		errors.setIcon(new ImageIcon(StatusBar.class.getResource("/img/error.png")));
		errors.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		errors.setVisible(false);
		add(warnings = new JLabel());
		warnings.setIcon(new ImageIcon(StatusBar.class.getResource("/img/warning.png")));
		warnings.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		warnings.setVisible(false);
		add(Box.createHorizontalGlue());
		add(caret = new JLabel());
		caret.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
		add(newJSeparator(JSeparator.VERTICAL));
		add(mode = new JLabel());
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
		caret.setText(Lang.getString("frame.caretPos", panel.getCaret().getDot() / 16 + 1, panel.getCaret().getDot() % 16));
		mode.setText(Lang.getString("frame." + (panel.isInsertMode() ? "insert" : "overwrite")));

		if (panel.getTokens() != null) {
			long errcount = panel.getTokens().stream().filter(t -> t.getToolTipLevel() == Level.ERROR).count();
			errors.setText("" + errcount);
			errors.setVisible(errcount > 0);
		
			long warncount = panel.getTokens().stream().filter(t -> t.getToolTipLevel() == Level.WARNING).count();
			warnings.setText("" + warncount);
			warnings.setVisible(warncount > 0);
		} else {
			errors.setVisible(false);
			warnings.setVisible(false);
		}
	}
}
