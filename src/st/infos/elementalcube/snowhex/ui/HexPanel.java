package st.infos.elementalcube.snowhex.ui;

import java.awt.AWTEventMulticaster;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.Format;
import st.infos.elementalcube.snowhex.Theme;
import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowylangapi.Lang;

public class HexPanel extends JPanel {
	private static final long serialVersionUID = 8016191606233812054L;
	private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 20);
	private byte[] bytes;
	private TokenMaker colorer;
	private List<Token> tokens;
	private double length0, lineH;
	
	// Caret
	private HexCaret caret;
	private Token closestToken;
	
	// Config
	private int addressCols;
	private boolean showDump = true;
	private boolean insert;
	
	/**
	 * Called with sources:
	 * - A byte [] (bytes) when the bytes change
	 * - HexPanel.this when mode changes
	 * - A MouseEvent when a click changes the caret position
	 * - An ActionEvent when a keyboard shortcut changes the caret position
	 * - A TokenMaker when the colorer changes
	 */
	private ActionListener listener;
	
	public HexPanel(byte[] initialBytes) {
		this.caret = new HexCaret(this);
		setBytes(initialBytes);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setFocusable(true);
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int caretIndex = getTokenAt(e.getX(), e.getY());
				int startX = (int) ((addressCols + 2) * length0);
				if (caretIndex == -1) {
					caret.removeCaretPosition();
				} else {
					boolean caretAfter = Math.round((e.getX() - startX) / length0 % 3) >= 2;
					caret.setCaretPosition(caretIndex, caretAfter);
				}
				repaint(getVisibleRect());
				if (listener != null) listener.actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, null));
				requestFocus();
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() >= '0' && e.getKeyChar() <= '9' || e.getKeyChar() >= 'a' && e.getKeyChar() <= 'f' || e.getKeyChar() >= 'A' && e
						.getKeyChar() <= 'F') {
					int caretIndex = caret.getDot();
					boolean caretAfter = caret.isDotAfter();
					if (insert && caretAfter) {
						bytes = ArrayUtils.insert(caretIndex + 1, bytes, (byte) Integer.parseInt(e.getKeyChar() + "0", 16));
					} else {
						int i = caretAfter ? caretIndex + 1 : caretIndex;
						if (bytes.length == i) {
							bytes = Arrays.copyOf(bytes, bytes.length + 1);
							calculateAddressCols();
						}
						char[] cs = twoCharsHexByte(bytes[i]).toCharArray();
						cs[caretAfter ? 0 : 1] = e.getKeyChar();
						bytes[i] = (byte) Integer.parseInt(new String(cs), 16);
					}
					setDocumentModified();
					caret.moveCaretRight();
					bytesDidChange();
					repaint(getVisibleRect());
				}
			}
		});
		caret.addChangeListener(e -> caretDidMove());
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "lineStart");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "lineEnd");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, getToolkit().getMenuShortcutKeyMask()), "start");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_END, getToolkit().getMenuShortcutKeyMask()), "end");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "back");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, getToolkit().getMenuShortcutKeyMask()), "delByte");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert"); // Windows / Linux
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), "insert"); // macOS with non-Mac keyboards
		
		getActionMap().put("left", new LambdaAction(caret::moveCaretLeft));
		getActionMap().put("right", new LambdaAction(caret::moveCaretRight));
		getActionMap().put("up", new LambdaAction(() -> caret.setCaretPosition(caret.getDot() - 16, caret.isDotAfter())));
		getActionMap().put("down", new LambdaAction(() -> caret.setCaretPosition(caret.getDot() + 16, caret.isDotAfter())));
		getActionMap().put("lineStart", new LambdaAction(() -> caret.setCaretPosition(caret.getDot() / 16 * 16, false)));
		getActionMap().put("lineEnd", new LambdaAction(() -> caret.setCaretPosition((caret.getDot() / 16 + 1) * 16 - 1, true)));
		getActionMap().put("start", new LambdaAction(() -> caret.setCaretPosition(-1, true)));
		getActionMap().put("end", new LambdaAction(() -> caret.setCaretPosition(bytes.length - 1, true)));
		getActionMap().put("back", new LambdaAction(() -> {
			int i = caret.getDot();
			bytes[i] = (byte) (bytes[i] & (caret.isDotAfter() ? 0xf0 : 0x0f));
			caret.moveCaretLeft();
			bytesDidChange();
		}));
		getActionMap().put("delByte", new LambdaAction(() -> {
			if (caret.hasSelection()) { // delete it all
				byte[] copy = new byte[bytes.length - (caret.getLastByte() - caret.getFirstByte())];
				System.arraycopy(bytes, 0, copy, 0, caret.getFirstByte());
				System.arraycopy(bytes, caret.getLastByte() + 1, copy, caret.getFirstByte(), bytes.length - caret.getLastByte());
				bytes = copy;
				caret.setCaretPosition(caret.getFirstByte() - 1, true);
				bytesDidChange();
			} else if (caret.isDotAfter() && caret.getDot() >= 0) {
				bytes = ArrayUtils.remove(bytes, caret.getDot());
				caret.setCaretPosition(caret.getDot() - 1, true);
				bytesDidChange();
			}
		}));
		getActionMap().put("insert", new LambdaAction(() -> insert = !insert));
	}

	protected void setDocumentModified() {
		getRootPane().putClientProperty("Window.documentModified", true);
	}
	
	private void updateClosestToken() {
		if (colorer == null) {
			closestToken = null;
			return;
		}
		closestToken = colorer.getClosestToken(bytes, tokens, caret.getFirstByte());
		if (caret.hasSelection() && closestToken != colorer.getClosestToken(bytes, tokens, caret.getLastByte())) {
			closestToken = null;
		}
	}
	
	// public because can be changed from `getBytes()[...] = ...`
	public void bytesDidChange() {
		reloadColors();
		repaint(getVisibleRect());
		if (listener != null)
			listener.actionPerformed(new ActionEvent(bytes, ActionEvent.ACTION_PERFORMED, null));
	}
	
	// our caret listener
	private void caretDidMove() {
		if (!caret.hasValidPosition()) {
			closestToken = null;
			return;
		}
		updateClosestToken();
		int x1 = (int) (startX() + ((caret.getDot() % 16) * 3 + (caret.isDotAfter() ? 2 : 1)) * length0);
		int y = (int) (((caret.getDot() / 16) + 2) * lineH);
		scrollRectToVisible(new Rectangle(x1, y - (int) lineH + 2, 2, (int) lineH + 7));
	}
	
	private int getTokenAt(int x, int y) {
		if (length0 == 0 || lineH == 0) return -1;
		int startX = startX();
		int startY = (int) (2 * lineH);
		x -= startX;
		y -= startY;
		int rx = x / (int) (length0 * 3);
		int ry = (int) Math.ceil(y / lineH);
		if (rx < 0 || ry < 0 || rx > 15) return -1;
		int i = ry * 16 + rx;
		if (i >= bytes.length) return -1;
		return i;
	}
	
	private int startX() {
		return (int) ((addressCols + 2) * length0);
	}
	
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
		if (bytes != null) {
			calculateAddressCols();
		}
		bytesDidChange();
	}
	
	private void calculateAddressCols() {
		addressCols = (int) Math.ceil(Math.log(bytes.length) / Math.log(16));
		if (addressCols == 0) addressCols = 1;
	}
	
	public void setColorer(TokenMaker colorer) {
		if (this.colorer != colorer) {
			TokenMaker old = this.colorer;
			this.colorer = colorer;
			reloadColors();
			if (listener != null) listener.actionPerformed(new ActionEvent(colorer == null ? old : colorer, ActionEvent.ACTION_PERFORMED, null));
		}
	}
	
	public void reloadColors() {
		if (colorer == null) {
			tokens = null;
		} else {
			colorer.invalidateTokenPool();
			tokens = colorer.generateTokens(bytes);
		}
		updateClosestToken();
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public Token getClosestToken() {
		return closestToken;
	}
	
	public boolean isShowingDump() {
		return showDump;
	}
	
	public void setShowDump(boolean showDump) {
		this.showDump = showDump;
	}
	
	public void addChangeListener(ActionListener l) {
		listener = AWTEventMulticaster.add(listener, l);
	}
	
	public void removeChangeListener(ActionListener l) {
		listener = AWTEventMulticaster.remove(listener, l);
	}
	
	public Object getPreviewImage() {
		try {
			Object obj = colorer.getDump(bytes);
			return obj instanceof Image ? (Image) obj : null;
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public boolean isInsertMode() {
		return insert;
	}
	
	public void toggleInsertMode() {
		insert = !insert;
		if (listener != null) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
	}
	
	public HexCaret getCaret() {
		return caret;
	}
	
	public TokenMaker getColorer() {
		return colorer;
	}
	
	public List<Token> getTokens() {
		return tokens;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setFont(FONT);
		g2d.setStroke(new BasicStroke(1));
		if (bytes == null) {
			String s = Lang.getString("frame.empty");
			Rectangle2D r2d = FONT.getStringBounds(s, g2d.getFontRenderContext());
			g2d.drawString(s, (int) (getWidth() - r2d.getWidth()) / 2, (int) (getHeight() - r2d.getHeight()) / 2);
			return;
		}
		int count = (int) Math.ceil(bytes.length / 16F);
		Rectangle2D r2d = FONT.getStringBounds("0", g2d.getFontRenderContext());
		length0 = Math.ceil(r2d.getWidth());
		lineH = Math.ceil(r2d.getHeight());
		setPreferredSize(new Dimension((int) ((addressCols + (showDump ? 66 : 50)) * length0), (int) (Math.ceil(bytes.length / 16F + 1) * lineH)
				+ 5));
		if (g2d.getClip().intersects((int) ((addressCols + 2.5) * length0), 0, 54 * length0, lineH)) {
			g2d.drawString("0", (int) ((addressCols + 2.5) * length0), (int) lineH);
			g2d.drawString("1", (int) ((addressCols + 5.5) * length0), (int) lineH);
			g2d.drawString("2", (int) ((addressCols + 8.5) * length0), (int) lineH);
			g2d.drawString("3", (int) ((addressCols + 11.5) * length0), (int) lineH);
			g2d.drawString("4", (int) ((addressCols + 14.5) * length0), (int) lineH);
			g2d.drawString("5", (int) ((addressCols + 17.5) * length0), (int) lineH);
			g2d.drawString("6", (int) ((addressCols + 20.5) * length0), (int) lineH);
			g2d.drawString("7", (int) ((addressCols + 23.5) * length0), (int) lineH);
			g2d.drawString("8", (int) ((addressCols + 26.5) * length0), (int) lineH);
			g2d.drawString("9", (int) ((addressCols + 29.5) * length0), (int) lineH);
			g2d.drawString("a", (int) ((addressCols + 32.5) * length0), (int) lineH);
			g2d.drawString("b", (int) ((addressCols + 35.5) * length0), (int) lineH);
			g2d.drawString("c", (int) ((addressCols + 38.5) * length0), (int) lineH);
			g2d.drawString("d", (int) ((addressCols + 41.5) * length0), (int) lineH);
			g2d.drawString("e", (int) ((addressCols + 44.5) * length0), (int) lineH);
			g2d.drawString("f", (int) ((addressCols + 47.5) * length0), (int) lineH);
			if (showDump) g2d.drawString("Dump", (int) ((addressCols + 50) * length0), (int) lineH);
		}
		int ix = startX();
		for (int a = 0; a < count; a++) {
			int y = (int) ((a + 2) * lineH);
			int x = ix;
			
			if (!g2d.getClip().intersects(0, y - lineH + 3, ix + 64 * length0, lineH))
				continue;
			
			String address = Integer.toHexString(a) + "X";
			g2d.setColor(Color.BLACK);
			g2d.drawString(address, (int) ((addressCols - address.length() + 1) * length0), y);
			for (int i = 0; i < 16; i++) {
				int index = a * 16 + i;
				if (bytes.length <= index) break;
				byte b = bytes[index];
				Format f = getFormatAt(index);
				g2d.setColor(f.getBackground());
				g2d.fillRect(x - (int) (length0 / 2), y - (int) lineH + 3, (int) (length0 * 3), (int) lineH);
				g2d.setColor(f.getForeground());
				if (f.isUnderlined() || (closestToken != null && closestToken.at(index)))
					g2d.drawLine(x - (int) (length0 / 2), y + 2, x + (int) (length0 * 2.5), y + 2);
				g2d.drawString(twoCharsHexByte(b), x, y);
				x += 3 * length0;
				if (showDump) {
					int dx = ix + (int) ((48 + i) * length0);
					g2d.setColor(f.getBackground());
					g2d.fillRect(dx, y - (int) lineH + 3, (int) (length0), (int) lineH);
					g2d.setColor(f.getForeground());
					String s = b >= 32 && b <= 127 ? "" + ((char) b) : ".";
					g2d.drawString(s, dx, y);
					if (caret.intersects(index)) {
						g2d.drawLine(dx, y + 2, dx + (int) (length0), y + 2);
					}
				}
			}
			
			if (!caret.hasSelection() && caret.hasValidPosition() && caret.getDot() / 16 == a) {
				g2d.setStroke(new BasicStroke(2));
				g2d.setColor(Color.BLACK);
				int x1 = (int) (ix + ((caret.getDot() % 16) * 3 + (caret.isDotAfter() ? 2 : 1)) * length0);
				g2d.drawLine(x1, y + 5, x1, y - (int) lineH + 2);
				g2d.setStroke(new BasicStroke(1));
			}
		}
	}
	
	private static String twoCharsHexByte(byte b) {
		String s = Integer.toHexString(Byte.toUnsignedInt(b));
		return s.length() == 1 ? "0" + s : s;
	}
	
	@Override
	public void setPreferredSize(Dimension pref) {
		if (!pref.equals(getPreferredSize())) {
			super.setPreferredSize(pref);
			revalidate();
		}
	}
	
	public Level getHighestError() {
		if (tokens == null) return null;
		Optional<Token> max = tokens.stream().filter(t -> t.hasToolTip()).max((t1, t2) -> t1.getToolTipLevel().compareTo(t2.getToolTipLevel()));
		if (max.isPresent() && max.get().getToolTipLevel() != Level.INFO) {
			return max.get().getToolTipLevel();
		}
		return null;
	}
	
	private Iterator<Token> getTokensAt(int index) {
		return tokens.stream().filter(t -> t.at(index)).iterator();
	}
	
	private Format getFormatAt(int index) {
		Format f = Format.DEFAULT;
		if (tokens == null) return f;
		Iterator<Token> i = getTokensAt(index);
		while (i.hasNext()) {
			f = f.combine(Theme.DEFAULT.get(i.next().getType()));
		}
		return f;
	}
	
	@Override
	public String getToolTipText(MouseEvent e) {
		if (tokens == null) return null;
		int index = getTokenAt(e.getX(), e.getY());
		if (index >= 0) {
			Iterator<Token> i = getTokensAt(index);
			ArrayList<String> desc = new ArrayList<>(1);
			while (i.hasNext()) {
				Token t = i.next();
				if (t.hasToolTip()) {
					if (t.getToolTipLevel() == Level.INFO) {
						desc.add(t.getToolTip());
					} else {
						desc.add("<img src=\"" + getClass().getResource("/img/" + t.getToolTipLevel().toString().toLowerCase() + ".png") + "\"/>" + t
							.getToolTip());
					}
				}
			}
			if (desc.isEmpty()) return null;
			if (desc.size() == 1) {
				return "<html>" + desc.get(0) + "</html>";
			}
			StringBuilder sb = new StringBuilder("<html>");
			sb.append(Lang.getString("parser.multipleNotices"));
			for (String d : desc) {
				sb.append("<br/>&nbsp;&nbsp;&nbsp;&nbsp;").append(d);
			}
			return sb.append("</html>").toString();
		}
		return null;
	}
	
	private class LambdaAction extends AbstractAction {
		private static final long serialVersionUID = 3266426527475612562L;
		Runnable r;
		
		public LambdaAction(Runnable r) {
			this.r = r;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			r.run();
			repaint(getVisibleRect());
			if (listener != null) listener.actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, null));
		}
	}
}
