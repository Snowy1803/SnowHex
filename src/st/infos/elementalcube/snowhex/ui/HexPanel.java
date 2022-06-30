package st.infos.elementalcube.snowhex.ui;

import java.awt.AWTEventMulticaster;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.ByteSelection;
import st.infos.elementalcube.snowhex.Format;
import st.infos.elementalcube.snowhex.Theme;
import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowylangapi.Lang;

public class HexPanel extends JPanel implements Scrollable {
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
	
	private Timer recolorTimer;
	
	/**
	 * Called with sources:
	 * - A byte [] (bytes) when the bytes change
	 * - HexPanel.this when mode changes
	 * - A HexCaret when the caret position changes
	 * - A TokenMaker when the colorer changes
	 */
	private ActionListener listener;
	
	public HexPanel(byte[] initialBytes) {
		this.caret = new HexCaret(this);
		recolorTimer = new Timer(200, e -> reloadColorsNow());
		recolorTimer.setRepeats(false);
		setBytes(initialBytes);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setFocusable(true);
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int caretIndex = getCoordsOffset(e.getX(), e.getY(), false);
				if (caretIndex == -1) {
					caret.removeCaretPosition();
				} else {
					int startX = (int) ((addressCols + 2) * length0);
					boolean caretAfter = Math.round((e.getX() - startX) / length0 % 3) >= 2;
					caret.setCaretPosition(caretIndex, caretAfter);
				}
				if (e.getClickCount() >= 2 && closestToken != null) {
					caret.setSelection(closestToken.getOffset(), closestToken.getLength());
				}
				repaint(getVisibleRect());
				requestFocus();
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!caret.hasValidPosition())
					return;
				int caretIndex = getCoordsOffset(e.getX(), e.getY(), true);
				caret.moveDot(caretIndex);
				repaint(getVisibleRect());
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
		// use the system input map for text editors. this will not auto update this UIResource on L&F change
		getInputMap().setParent((InputMap) UIManager.get("EditorPane.focusInputMap"));
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert"); // Windows / Linux
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, getToolkit().getMenuShortcutKeyMask()), "delByte");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert"); // Windows / Linux
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), "insert"); // macOS with non-Mac keyboards
		
		// character (1 position) = digit, or byte if selection
		// word = closest token
		// line = shown line, 16 bytes
		// paragraph = N/A
		
		getActionMap().put(DefaultEditorKit.backwardAction, new LambdaAction(caret::moveCaretLeft));
		getActionMap().put(DefaultEditorKit.forwardAction, new LambdaAction(caret::moveCaretRight));
		getActionMap().put(DefaultEditorKit.selectionBackwardAction, new LambdaAction(() -> caret.moveDot(caret.getDot() - 1)));
		getActionMap().put(DefaultEditorKit.selectionForwardAction, new LambdaAction(() -> caret.moveDot(caret.getDot() + 1)));
		
		Action up = new LambdaAction(() -> caret.setCaretPosition(caret.getDot() - 16, caret.isDotAfter()));
		Action down = new LambdaAction(() -> caret.setCaretPosition(caret.getDot() + 16, caret.isDotAfter()));
		getActionMap().put(DefaultEditorKit.upAction, up);
		getActionMap().put(DefaultEditorKit.downAction, down);
		getActionMap().put("aqua-move-up", up); // macOS
		getActionMap().put("aqua-move-down", down);
		getActionMap().put(DefaultEditorKit.selectionUpAction, new LambdaAction(() -> caret.moveDot(caret.getDot() - 16)));
		getActionMap().put(DefaultEditorKit.selectionDownAction, new LambdaAction(() -> caret.moveDot(caret.getDot() + 16)));
		
		getActionMap().put(DefaultEditorKit.selectAllAction, new LambdaAction(() -> caret.setSelection(0, bytes.length)));
		getActionMap().put("unselect", new LambdaAction(() -> caret.setCaretPosition(caret.getDot(), caret.isDotAfter())));
		
		getActionMap().put(DefaultEditorKit.beginLineAction, new LambdaAction(() -> caret.setCaretPosition(caret.getDot() / 16 * 16, false)));
		getActionMap().put(DefaultEditorKit.endLineAction, new LambdaAction(() -> caret.setCaretPosition((caret.getDot() / 16 + 1) * 16 - 1, true)));
		getActionMap().put(DefaultEditorKit.selectionBeginLineAction, new LambdaAction(() -> caret.moveDot(caret.getDot() / 16 * 16 - 1)));
		getActionMap().put(DefaultEditorKit.selectionEndLineAction, new LambdaAction(() -> caret.moveDot(((caret.getDot() + 1) / 16 + 1) * 16 - 1)));
		
		getActionMap().put(DefaultEditorKit.beginAction, new LambdaAction(() -> caret.setCaretPosition(-1, true)));
		getActionMap().put(DefaultEditorKit.endAction, new LambdaAction(() -> caret.setCaretPosition(bytes.length - 1, true)));
		getActionMap().put(DefaultEditorKit.selectionBeginAction, new LambdaAction(() -> caret.moveDot(-1)));
		getActionMap().put(DefaultEditorKit.selectionEndAction, new LambdaAction(() -> caret.moveDot(bytes.length - 1)));
		
		getActionMap().put(DefaultEditorKit.deletePrevCharAction, new LambdaAction(() -> {
			int i = caret.getDot();
			bytes[i] = (byte) (bytes[i] & (caret.isDotAfter() ? 0xf0 : 0x0f));
			caret.moveCaretLeft();
			bytesDidChange();
		}));
		Action delByte = new LambdaAction(() -> {
			if (caret.hasSelection()) { // delete it all
				deleteSelectedBytes();
			} else if (caret.isDotAfter() && caret.getDot() >= 0) {
				bytes = ArrayUtils.remove(bytes, caret.getDot());
				caret.setCaretPosition(caret.getDot() - 1, true);
				bytesDidChange();
			}
		});
		getActionMap().put("delByte", delByte);
		getActionMap().put("insert", new LambdaAction(() -> insert = !insert));
		Action copy = new LambdaAction(() -> {
			getToolkit().getSystemClipboard().setContents(
					new ByteSelection(ArrayUtils.subarray(bytes, caret.getFirstByte(), caret.getLastByte() + 1)), null);
		});
		getActionMap().put(DefaultEditorKit.copyAction, copy);
		Action cut = new LambdaAction(() -> {
			getToolkit().getSystemClipboard().setContents(
					new ByteSelection(ArrayUtils.subarray(bytes, caret.getFirstByte(), caret.getLastByte() + 1)), null);
			deleteSelectedBytes();
		});
		getActionMap().put(DefaultEditorKit.cutAction, cut);
		getActionMap().put(DefaultEditorKit.pasteAction, new LambdaAction(() -> {
			try {
				byte[] b = (byte[]) getToolkit().getSystemClipboard().getContents(this).getTransferData(ByteSelection.BYTE_ARRAY);
				if (b == null)
					return;
				if (caret.hasSelection())
					deleteSelectedBytes();
				insertBytes(b);
			} catch (HeadlessException | UnsupportedFlavorException | IOException ex) {
				ex.printStackTrace();
			}
		}));
		addChangeListener(e -> {
			if (e.getSource() == caret) {
				delByte.putValue(Action.NAME, caret.hasSelection() ? Lang.getString("menu.edit.delBytes") : Lang.getString("menu.edit.delByte"));
				delByte.setEnabled(caret.hasValidPosition() && (caret.hasSelection() || (caret.isDotAfter() && caret.getDot() >= 0)));
				copy.setEnabled(caret.hasSelection());
				cut.setEnabled(caret.hasSelection());
			}
		});
	}
	
	private void deleteSelectedBytes() {
		byte[] copy = new byte[bytes.length - (caret.getLastByte() - caret.getFirstByte() + 1)];
		System.arraycopy(bytes, 0, copy, 0, caret.getFirstByte());
		System.arraycopy(bytes, caret.getLastByte() + 1, copy, caret.getFirstByte(), bytes.length - (caret.getLastByte() + 1));
		bytes = copy;
		caret.setCaretPosition(caret.getFirstByte() - 1, true);
		bytesDidChange();
	}
	
	private void insertBytes(byte[] insert) {
		bytes = ArrayUtils.insert(caret.getDot() + 1, bytes, insert);
		caret.setSelection(caret.getDot() + 1, insert.length);
		bytesDidChange();
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
		repaint(getVisibleRect());
		if (!caret.hasValidPosition()) {
			closestToken = null;
			return;
		}
		updateClosestToken();
		int x1 = (int) (startX() + ((caret.getDot() % 16) * 3 + (caret.isDotAfter() ? 2 : 1)) * length0);
		int y = (int) (((caret.getDot() / 16) + 2) * lineH);
		scrollRectToVisible(new Rectangle(x1, y - (int) lineH + 2, 2, (int) lineH + 7));
		if (listener != null)
			listener.actionPerformed(new ActionEvent(caret, ActionEvent.ACTION_PERFORMED, null));
	}
	
	private int getCoordsOffset(int x, int y, boolean closest) {
		if (length0 == 0 || lineH == 0) return -1;
		int startX = startX();
		int startY = (int) (2 * lineH);
		x -= startX;
		y -= startY;
		int rx = (int) Math.floor(x / (length0 * 3));
		int ry = (int) Math.ceil(y / lineH);
		if (rx < 0 || ry < 0 || rx > 15) {
			if (!closest)
				return -1;
			if (rx < 0) rx = -1;
			if (ry < 0) ry = 0;
			if (rx > 15) rx = 15;
		}
		int i = ry * 16 + rx;
		if (i >= bytes.length) {
			if (!closest)
				return -1;
			return bytes.length - 1;
		}
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
			reloadColorsNow();
			if (listener != null) listener.actionPerformed(new ActionEvent(colorer == null ? old : colorer, ActionEvent.ACTION_PERFORMED, null));
		}
	}
	
	public void reloadColors() {
		recolorTimer.restart();
	}
	
	public void reloadColorsNow() {
		if (colorer == null) {
			tokens = null;
		} else {
			colorer.invalidateTokenPool();
			tokens = colorer.generateTokens(bytes);
		}
		updateClosestToken();
		repaint(getVisibleRect());
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
				if (f.isUnderlined() || (closestToken != null && closestToken.at(index) && !caret.hasSelection()))
					g2d.drawLine(x - (int) (length0 / 2), y + 2, x + (int) (length0 * 2.5), y + 2);
				g2d.drawString(twoCharsHexByte(b), x, y);
				if (caret.hasSelection() && caret.intersects(index)) {
					g2d.setColor(getForeground());
					if (i == 0 || index == caret.getFirstByte())
						g2d.drawLine(x - (int) (length0 / 2), y - (int) lineH + 3, x - (int) (length0 / 2), y + 3);
					if (i == 15 || index == caret.getLastByte())
						g2d.drawLine(x + (int) (length0 * 2.5) - 1, y - (int) lineH + 3, x + (int) (length0 * 2.5) - 1, y + 3);
					if (!caret.intersects(index + 16))
						g2d.drawLine(x - (int) (length0 / 2), y + 2, x + (int) (length0 * 2.5) - 1, y + 2);
					if (!caret.intersects(index - 16))
						g2d.drawLine(x - (int) (length0 / 2), y - (int) lineH + 3, x + (int) (length0 * 2.5) - 1, y - (int) lineH + 3);
				}
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
	
	public static String twoCharsHexByte(byte b) {
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
		int index = getCoordsOffset(e.getX(), e.getY(), false);
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
		}
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return new Dimension((int) length0 * 64, (int) (lineH * bytes.length / 16));
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return direction == SwingConstants.HORIZONTAL ? (int) length0 * 3 : (int) lineH;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return direction == SwingConstants.HORIZONTAL ? (int) length0 * 3 : (int) (visibleRect.height / lineH * lineH);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
