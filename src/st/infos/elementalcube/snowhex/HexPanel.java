package st.infos.elementalcube.snowhex;

import java.awt.AWTEventMulticaster;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
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

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowylangapi.Lang;

public class HexPanel extends JPanel {
	private static final long serialVersionUID = 8016191606233812054L;
	private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 20);
	private byte[] bytes;
	private TokenMaker colorer;
	private List<Token> tokens;
	private double length0, lineH;
	private int caretIndex = -1;
	private boolean caretAfter;
	
	// Config
	private int addressCols;
	private boolean showDump = true;
	private boolean insert;
	
	private ActionListener listener;
	
	public HexPanel(byte[] initialBytes) {
		setBytes(initialBytes);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setFocusable(true);
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Point pt = getLocationForIndex(caretIndex);
				caretIndex = getTokenAt(e.getX(), e.getY());
				int startX = (int) ((addressCols + 2) * length0);
				caretAfter = caretIndex == -1 ? false : Math.round((e.getX() - startX) / length0 % 3) >= 2;
				repaint(pt.x - (int) (length0 * 3), pt.y - (int) (lineH * 2), (int) (length0 * 8), (int) (lineH * 4));
				repaint(e.getX() - (int) (length0 * 3), e.getY() - (int) (lineH * 2), (int) (length0 * 8), (int) (lineH * 4));
				if (showDump) {
					int dx = startX() + (int) ((48 + 0) * length0);
					repaint(dx, pt.y - (int) (lineH * 1), (int) (length0 * 16), (int) (lineH * 2));
					repaint(dx, e.getY() - (int) (lineH * 1), (int) (length0 * 16), (int) (lineH * 2));
				}
				if (listener != null) listener.actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, null));
				requestFocus();
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() >= '0' && e.getKeyChar() <= '9' || e.getKeyChar() >= 'a' && e.getKeyChar() <= 'f' || e.getKeyChar() >= 'A' && e
						.getKeyChar() <= 'F') {
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
					moveCaretRight();
					reloadColors();
					if (listener != null) listener.actionPerformed(new ActionEvent(bytes, ActionEvent.ACTION_PERFORMED, null));
					repaint(getVisibleRect());
				}
			}
		});
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
		
		getActionMap().put("left", new LambdaAction(this::moveCaretLeft));
		getActionMap().put("right", new LambdaAction(this::moveCaretRight));
		getActionMap().put("up", new LambdaAction(this::moveCaretUp));
		getActionMap().put("down", new LambdaAction(this::moveCaretDown));
		getActionMap().put("lineStart", new LambdaAction(this::moveCaretLineStart));
		getActionMap().put("lineEnd", new LambdaAction(this::moveCaretLineEnd));
		getActionMap().put("start", new LambdaAction(() -> {
			caretIndex = -1;
			caretAfter = true;
		}));
		getActionMap().put("end", new LambdaAction(() -> {
			caretIndex = bytes.length - 1;
			caretAfter = true;
		}));
		getActionMap().put("back", new LambdaAction(() -> {
			int i = caretIndex;
			char[] cs = twoCharsHexByte(bytes[i]).toCharArray();
			cs[caretAfter ? 1 : 0] = '0';
			bytes[i] = (byte) Integer.parseInt(new String(cs), 16);
			moveCaretLeft();
			reloadColors();
			if (listener != null) listener.actionPerformed(new ActionEvent(bytes, ActionEvent.ACTION_PERFORMED, null));
		}));
		getActionMap().put("delByte", new LambdaAction(() -> {
			if (caretAfter && caretIndex >= 0) {
				bytes = ArrayUtils.remove(bytes, caretIndex);
				caretIndex--;
				reloadColors();
				if (listener != null) listener.actionPerformed(new ActionEvent(bytes, ActionEvent.ACTION_PERFORMED, null));
				repaint(getVisibleRect());
			}
		}));
		getActionMap().put("insert", new LambdaAction(() -> {
			insert = !insert;
		}));
	}
	
	protected void moveCaretUp() {
		if (caretIndex > 15) {
			caretIndex -= 16;
		} else {
			caretIndex = -1;
			caretAfter = true;
		}
	}
	
	protected void moveCaretDown() {
		caretIndex += 16;
		if (caretIndex >= bytes.length) {
			caretIndex = bytes.length - 1;
			caretAfter = true;
		}
	}
	
	protected void moveCaretRight() {
		if (caretAfter) {
			caretIndex++;
			caretAfter = false;
		} else {
			caretAfter = true;
		}
		if (caretIndex >= bytes.length) {
			caretIndex = bytes.length - 1;
			caretAfter = true;
		}
	}
	
	protected void moveCaretLeft() {
		if (caretAfter) {
			caretAfter = false;
		} else {
			caretIndex--;
			caretAfter = true;
		}
		if (caretIndex < 0) {
			caretIndex = -1;
			caretAfter = true;
		}
	}
	
	protected void moveCaretLineStart() {
		caretIndex = caretIndex / 16 * 16;
		caretAfter = false;
	}
	
	protected void moveCaretLineEnd() {
		caretIndex = (caretIndex / 16 + 1) * 16 - 1;
		caretAfter = true;
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
	
	private Point getLocationForIndex(int index) {
		if (length0 == 0 || lineH == 0 || index < 0) return new Point();
		int startX = startX();
		int startY = (int) (2 * lineH);
		int rx = index % 16;
		int ry = index / 16;
		return new Point(rx * (int) (length0 * 3) + startX, (int) (ry * lineH + startY));
	}
	
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
		if (bytes != null) {
			calculateAddressCols();
		}
		reloadColors();
		if (listener != null) listener.actionPerformed(new ActionEvent(bytes, ActionEvent.ACTION_PERFORMED, null));
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
		tokens = colorer == null ? null : colorer.generateTokens(bytes);
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public boolean isShowingDump() {
		return showDump;
	}
	
	public void setShowDump(boolean showDump) {
		this.showDump = showDump;
	}
	
	public void addChangeListener(ActionListener l) {
		if (l == null) {
			return;
		}
		listener = AWTEventMulticaster.add(listener, l);
	}
	
	public void removeChangeListener(ActionListener l) {
		if (l == null) {
			return;
		}
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
	
	public int getCaretPosition() {
		return caretIndex;
	}
	
	public TokenMaker getColorer() {
		return colorer;
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
		} else {
			int count = (int) Math.ceil(bytes.length / 16F);
			Rectangle2D r2d = FONT.getStringBounds("0", g2d.getFontRenderContext());
			length0 = r2d.getWidth();
			lineH = r2d.getHeight();
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
				if (g2d.getClip().intersects(x, y - lineH, 64 * length0, lineH + 2)) {
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
						if (f.isUnderlined()) g2d.drawLine(x - (int) (length0 / 2), y + 2, x + (int) (length0 * 2.5), y + 2);
						g2d.drawString(twoCharsHexByte(b), x, y);
						x += 3 * length0;
						if (showDump) {
							int dx = ix + (int) ((48 + i) * length0);
							g2d.setColor(f.getBackground());
							g2d.fillRect(dx, y - (int) lineH + 3, (int) (length0), (int) lineH);
							g2d.setColor(f.getForeground());
							String s = b >= 32 && b <= 127 ? "" + ((char) b) : ".";
							g2d.drawString(s, dx, y);
							if (index == caretIndex) {
								g2d.drawLine(dx, y + 2, dx + (int) (length0), y + 2);
							}
						}
					}
					
					if (validIndex() && caretIndex / 16 == a) {
						g2d.setStroke(new BasicStroke(2));
						g2d.setColor(Color.BLACK);
						int x1 = (int) (ix + ((caretIndex % 16) * 3 + (caretAfter ? 2 : 1)) * length0);
						g2d.drawLine(x1, y + 5, x1, y - (int) lineH + 2);
						g2d.setStroke(new BasicStroke(1));
					}
				}
			}
		}
	}
	
	private boolean validIndex() {
		return caretIndex >= 0 || caretIndex == -1 && caretAfter;
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
					desc.add("<img src=\"" + getClass().getResource("/img/" + t.getToolTipLevel().toString().toLowerCase() + ".png") + "\"/>" + t
							.getToolTip());
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
	
	private class LambdaAction implements Action {
		Runnable r;
		
		public LambdaAction(Runnable r) {
			this.r = r;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Point pt1 = getLocationForIndex(caretIndex);
			r.run();
			Point pt2 = getLocationForIndex(caretIndex);
			repaint(pt1.x - (int) (length0 * 3), pt1.y - (int) (lineH * 2), (int) (length0 * 10), (int) (lineH * 4));
			repaint(pt2.x - (int) (length0 * 3), pt2.y - (int) (lineH * 2), (int) (length0 * 10), (int) (lineH * 4));
			if (showDump) {
				int dx = startX() + (int) ((48 + 0) * length0);
				repaint(dx, pt1.y - (int) (lineH * 1), (int) (length0 * 16), (int) (lineH * 2));
				repaint(dx, pt2.y - (int) (lineH * 1), (int) (length0 * 16), (int) (lineH * 2));
			}
			if (listener != null) listener.actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, null));
		}
		
		@Override
		public Object getValue(String key) {
			return null;
		}
		
		@Override
		public void putValue(String key, Object value) {}
		
		@Override
		public void setEnabled(boolean b) {}
		
		@Override
		public boolean isEnabled() {
			return true;
		}
		
		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener) {}
		
		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {}
	}
}
