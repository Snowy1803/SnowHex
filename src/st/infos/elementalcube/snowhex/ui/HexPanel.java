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
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

import org.apache.commons.lang3.tuple.Pair;

import st.infos.elementalcube.snowhex.ByteSelection;
import st.infos.elementalcube.snowhex.Format;
import st.infos.elementalcube.snowhex.HexDocument;
import st.infos.elementalcube.snowhex.HexDocument.EditType;
import st.infos.elementalcube.snowhex.Theme;
import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.TokenTypes;
import st.infos.elementalcube.snowylangapi.Lang;

public class HexPanel extends JPanel implements Scrollable {
	private static final long serialVersionUID = 8016191606233812054L;
	private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 20);
	private HexDocument document;
	private TokenMaker colorer;
	private List<Token> tokens;
	private int length0, lineH;
	
	// Caret
	private HexCaret caret;
	private Token[] closestToken;
	private Pair<Integer, Integer> findRange;
	
	// Config
	private int addressCols;
	private boolean showDump = true;
	private boolean insert;
	
	private Timer recolorTimer;
	
	/**
	 * Called with sources:
	 * - A HexDocument when the document changes
	 * - HexPanel.this when mode changes
	 * - A HexCaret when the caret position changes
	 * - A List of Tokens when the token list changes
	 * - A TokenMaker when the colorer changes
	 */
	private ActionListener listener;
	
	public HexPanel(HexDocument document) {
		caret = new HexCaret(this);
		this.document = document;
		document.addEditListener(this::bytesChanged);
		recolorTimer = new Timer(200, e -> reloadColorsNow());
		recolorTimer.setRepeats(false);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setFocusable(true);
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					int caretIndex = getCoordsOffset(e.getX(), e.getY(), false);
					if (caretIndex == -1) {
						caret.removeCaretPosition();
					} else {
						int startX = (addressCols + 2) * length0;
						boolean caretAfter = Math.round((e.getX() - startX) / length0 % 3) >= 2;
						caret.setCaretPosition(caretIndex, caretAfter);
					}
					if (e.getClickCount() >= 2 && closestToken != null && closestToken.length > 0) {
						int index = e.getClickCount() - 2 >= closestToken.length ? closestToken.length - 1 : e.getClickCount() - 2;
						caret.setSelection(closestToken[index].getOffset(), closestToken[index].getLength());
					}
					document.pushFence();
					repaint(getVisibleRect());
					requestFocus();
				}
				if (e.isPopupTrigger())
					handlePopup(e);
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					handlePopup(e);
			}
			
			private void handlePopup(MouseEvent e) {
				PopupMenu menu = new PopupMenu();
				for (String s : new String[] { DefaultEditorKit.copyAction, DefaultEditorKit.cutAction, DefaultEditorKit.pasteAction }) {
					Action a = getActionMap().get(s);
					MenuItem mi = new MenuItem((String) a.getValue(Action.NAME));
					mi.addActionListener(a);
					mi.setEnabled(a.isEnabled());
					menu.add(mi);
				}
				if (colorer != null) {
					colorer.willShowPopup(HexPanel.this, menu);
				}
				HexPanel.this.add(menu);
				menu.show(HexPanel.this, e.getX(), e.getY());
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!caret.hasValidPosition() || !SwingUtilities.isLeftMouseButton(e))
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
					if ((insert || caretIndex == document.getLength() - 1) && caretAfter) {
						document.insertBytes(caretIndex + 1, new byte[] { (byte) Integer.parseInt(e.getKeyChar() + "0", 16) }, EditType.TYPING);
					} else {
						int i = caretAfter ? caretIndex + 1 : caretIndex;
						char[] cs = twoCharsHexByte(document.getByte(i)).toCharArray();
						cs[caretAfter ? 0 : 1] = e.getKeyChar();
						document.replaceBytes(i, document.getLength() == i ? 0 : 1,
								new byte[] { (byte) Integer.parseInt(new String(cs), 16) }, EditType.TYPING);
					}
					caret.moveCaretRight();
					repaint(getVisibleRect());
				}
			}
		});
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				document.pushFence();
			}
		});
		caret.addChangeListener(e -> caretDidMove());
		// use the system input map for text editors. this will not auto update this UIResource on L&F change
		getInputMap().setParent((InputMap) UIManager.get("EditorPane.focusInputMap"));
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert"); // Windows / Linux
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, getToolkit().getMenuShortcutKeyMaskEx()), "delByte");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert"); // Windows / Linux
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), "insert"); // macOS with non-Mac keyboards
		
		// character (1 position) = digit, or byte if selection
		// word = closest token
		// line = shown line, 16 bytes
		// paragraph = N/A
		
		getActionMap().put(DefaultEditorKit.backwardAction, new LambdaAction(caret::moveCaretLeft, true));
		getActionMap().put(DefaultEditorKit.forwardAction, new LambdaAction(caret::moveCaretRight, true));
		getActionMap().put(DefaultEditorKit.selectionBackwardAction, new LambdaAction(() -> caret.moveDot(caret.getDot() - 1), true));
		getActionMap().put(DefaultEditorKit.selectionForwardAction, new LambdaAction(() -> caret.moveDot(caret.getDot() + 1), true));
		
		Action up = new LambdaAction(() -> caret.setCaretPosition(caret.getDot() - 16, caret.isDotAfter()), true);
		Action down = new LambdaAction(() -> caret.setCaretPosition(caret.getDot() + 16, caret.isDotAfter()), true);
		getActionMap().put(DefaultEditorKit.upAction, up);
		getActionMap().put(DefaultEditorKit.downAction, down);
		getActionMap().put("aqua-move-up", up); // macOS
		getActionMap().put("aqua-move-down", down);
		getActionMap().put(DefaultEditorKit.selectionUpAction, new LambdaAction(() -> caret.moveDot(caret.getDot() - 16), true));
		getActionMap().put(DefaultEditorKit.selectionDownAction, new LambdaAction(() -> caret.moveDot(caret.getDot() + 16), true));
		
		getActionMap().put(DefaultEditorKit.selectAllAction, new LambdaAction(() -> caret.setSelection(0, document.getLength()), true));
		getActionMap().put("unselect", new LambdaAction(() -> caret.setCaretPosition(caret.getDot(), caret.isDotAfter()), true));
		
		getActionMap().put(DefaultEditorKit.beginLineAction, new LambdaAction(() -> caret.setCaretPosition(caret.getDot() / 16 * 16, false), true));
		getActionMap().put(DefaultEditorKit.endLineAction, new LambdaAction(() -> caret.setCaretPosition((caret.getDot() / 16 + 1) * 16 - 1, true), true));
		getActionMap().put(DefaultEditorKit.selectionBeginLineAction, new LambdaAction(() -> caret.moveDot(caret.getDot() / 16 * 16 - 1), true));
		getActionMap().put(DefaultEditorKit.selectionEndLineAction, new LambdaAction(() -> caret.moveDot(((caret.getDot() + 1) / 16 + 1) * 16 - 1), true));
		
		getActionMap().put(DefaultEditorKit.beginAction, new LambdaAction(() -> caret.setCaretPosition(-1, true), true));
		getActionMap().put(DefaultEditorKit.endAction, new LambdaAction(() -> caret.setCaretPosition(document.getLength() - 1, true), true));
		getActionMap().put(DefaultEditorKit.selectionBeginAction, new LambdaAction(() -> caret.moveDot(-1), true));
		getActionMap().put(DefaultEditorKit.selectionEndAction, new LambdaAction(() -> caret.moveDot(document.getLength() - 1), true));
		
		getActionMap().put(DefaultEditorKit.deletePrevCharAction, new LambdaAction(() -> {
			int i = caret.getDot();
			document.replaceBytes(i, 1, new byte[] { (byte) (document.getByte(i) & (caret.isDotAfter() ? 0xf0 : 0x0f)) }, EditType.DELETE);
			caret.moveCaretLeft();
		}, false));
		Action delByte = new LambdaAction(() -> {
			if (caret.hasSelection()) { // delete it all
				document.removeSelectedBytes(caret, EditType.DELETE);
			} else if (caret.isDotAfter() && caret.getDot() >= 0) {
				document.removeBytes(caret.getDot(), 1, EditType.DELETE);
				caret.setCaretPosition(caret.getDot() - 1, true);
			}
		}, false);
		delByte.putValue(Action.NAME, Lang.getString("menu.edit.delByte"));
		delByte.setEnabled(false);
		getActionMap().put("delByte", delByte);
		getActionMap().put("insert", new LambdaAction(this::toggleInsertMode, false));
		Action copy = new LambdaAction(() -> {
			getToolkit().getSystemClipboard().setContents(
					new ByteSelection(document.getSelectedBytes(caret)), null);
		}, false);
		copy.putValue(Action.NAME, Lang.getString("menu.edit.copy"));
		getActionMap().put(DefaultEditorKit.copyAction, copy);
		Action cut = new LambdaAction(() -> {
			getToolkit().getSystemClipboard().setContents(
					new ByteSelection(document.getSelectedBytes(caret)), null);
			document.removeSelectedBytes(caret, EditType.DELETE_CUT);
		}, false);
		cut.putValue(Action.NAME, Lang.getString("menu.edit.cut"));
		getActionMap().put(DefaultEditorKit.cutAction, cut);
		Action paste = new LambdaAction(() -> {
			try {
				byte[] b = (byte[]) getToolkit().getSystemClipboard().getContents(this).getTransferData(ByteSelection.BYTE_ARRAY);
				if (b == null)
					return;
				if (caret.hasSelection()) {
					document.replaceSelectedBytes(caret, b, EditType.INSERT_PASTE);
					// update caret position ?
				} else {
					document.insertBytes(caret.getDot() + 1, b, EditType.INSERT_PASTE);
					caret.setSelection(caret.getDot() + 1, b.length);
				}
			} catch (HeadlessException | UnsupportedFlavorException | IOException ex) {
				ex.printStackTrace();
			}
		}, false);
		paste.putValue(Action.NAME, Lang.getString("menu.edit.paste"));
		getActionMap().put(DefaultEditorKit.pasteAction, paste);
		addChangeListener(e -> {
			if (e.getSource() == caret) {
				delByte.putValue(Action.NAME, caret.hasSelection() ? Lang.getString("menu.edit.delBytes") : Lang.getString("menu.edit.delByte"));
				delByte.setEnabled(caret.hasValidPosition() && (caret.hasSelection() || (caret.isDotAfter() && caret.getDot() >= 0)));
				copy.setEnabled(caret.hasSelection());
				cut.setEnabled(caret.hasSelection());
			}
		});
	}
	
	protected void setDocumentModified() {
		if (getRootPane() != null)
			getRootPane().putClientProperty("Window.documentModified", document.canUndo());
	}
	
	private void updateClosestToken() {
		if (colorer == null) {
			closestToken = null;
			return;
		}
		closestToken = colorer.getClosestToken(document.getBytes(), tokens, caret.getFirstByte());
		if (caret.hasSelection() && !Arrays.equals(closestToken, colorer.getClosestToken(document.getBytes(), tokens, caret.getLastByte()))) {
			closestToken = null;
		}
	}
	
	// our document listener
	private void bytesChanged(ActionEvent e) {
		if ("compound".equals(e.getActionCommand()))
			return; // not done yet, let's wait for the end
		setDocumentModified();
		calculateAddressCols();
		reloadColors();
		repaint(getVisibleRect());
		if (listener != null)
			listener.actionPerformed(new ActionEvent(document, ActionEvent.ACTION_PERFORMED, null));
	}
	
	// our caret listener
	private void caretDidMove() {
		repaint(getVisibleRect());
		if (!caret.hasValidPosition()) {
			closestToken = null;
			return;
		}
		updateClosestToken();
		int x1 = startX() + ((caret.getDot() % 16) * 3 + (caret.isDotAfter() ? 2 : 1)) * length0;
		int y = ((caret.getDot() / 16) + 2) * lineH;
		scrollRectToVisible(new Rectangle(x1, y - lineH + 2, 2, lineH + 7));
		if (listener != null)
			listener.actionPerformed(new ActionEvent(caret, ActionEvent.ACTION_PERFORMED, null));
	}
	
	private int getCoordsOffset(int x, int y, boolean closest) {
		if (length0 == 0 || lineH == 0) return -1;
		int startX = startX();
		int startY = 2 * lineH;
		x -= startX;
		y -= startY;
		int rx = x / (length0 * 3);
		int ry = (y + lineH - 1) / lineH;
		if (rx < 0 || ry < 0 || rx > 15) {
			if (!closest)
				return -1;
			if (rx < 0) rx = -1;
			if (ry < 0) ry = 0;
			if (rx > 15) rx = 15;
		}
		int i = ry * 16 + rx;
		if (i >= document.getLength()) {
			if (!closest)
				return -1;
			return document.getLength() - 1;
		}
		return i;
	}
	
	private int startX() {
		return (addressCols + 2) * length0;
	}
	
	private void calculateAddressCols() {
		addressCols = (int) Math.ceil(Math.log(document.getLength()) / Math.log(16));
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
		List<Token> old = tokens;
		if (colorer == null) {
			tokens = null;
		} else {
			colorer.invalidateTokenPool();
			tokens = colorer.generateTokens(document.getBytes());
		}
		updateClosestToken();
		repaint(getVisibleRect());
		if (listener != null && (tokens != null || old != null))
			listener.actionPerformed(new ActionEvent(tokens == null ? old : tokens, ActionEvent.ACTION_PERFORMED, null));
	}
	
	public HexDocument getDocument() {
		return document;
	}
	
	public Token getClosestToken() {
		return closestToken != null && closestToken.length > 0 ? closestToken[0] : null;
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
			Object obj = colorer.getDump(document.getBytes());
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
	
	public void setFindRange(Pair<Integer, Integer> findRange) {
		this.findRange = findRange;
		repaint(getVisibleRect());
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setFont(FONT);
		g2d.setStroke(new BasicStroke(1));
		Rectangle2D r2d = FONT.getStringBounds("0", g2d.getFontRenderContext());
		length0 = (int) Math.ceil(r2d.getWidth() / 2) * 2;
		lineH = (int) Math.ceil(r2d.getHeight() / 2) * 2;
		setPreferredSize(new Dimension(
				(addressCols + (showDump ? 66 : 50)) * length0, 
				(((document.getLength() + 15) / 16 + 1) * lineH) + 5));
		if (g2d.getClip().intersects((int) ((addressCols + 2.5) * length0), 0, 54 * length0, lineH)) {
			g2d.drawString("0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f", (int) ((addressCols + 2.5) * length0), lineH);
			if (showDump)
				g2d.drawString("Dump", (addressCols + 50) * length0, lineH);
		}
		int ix = startX();
		int a = Math.max(0, (int) g2d.getClipBounds().getMinY() / lineH - 2);
		int count = Math.min((document.getLength() + 15) / 16, (int) Math.ceil(g2d.getClipBounds().getMaxY() / lineH));
		List<Token> formats = getTokensAt(tokens, a * 16, count * 16);
		for (; a < count; a++) {
			int y = (a + 2) * lineH;
			int x = ix;
			
			if (!g2d.getClip().intersects(0, y - lineH + 3, ix + 64 * length0, lineH))
				continue;
			
			String address = Integer.toHexString(a) + "X";
			g2d.setColor(Color.BLACK);
			g2d.drawString(address, (addressCols - address.length() + 1) * length0, y);
			for (int i = 0; i < 16; i++) {
				int index = a * 16 + i;
				if (document.getLength() <= index) break;
				byte b = document.getByte(index);
				Format f = getFormatAt(formats, index);
				g2d.setColor(f.getBackground());
				g2d.fillRect(x - length0 / 2, y - lineH + 3, length0 * 3, lineH);
				g2d.setColor(f.getForeground());
				g2d.drawString(twoCharsHexByte(b), x, y);
				Token closestToken = getClosestToken();
				if (closestToken != null && closestToken.at(index) && !caret.hasSelection()) {
					g2d.drawLine(x - length0 / 2, y + 2, x + (int) (length0 * 2.5), y + 2);
				}
				if (f.isStrikedThrough()) {
					g2d.setColor(f.getStrikeThroughColor());
					int ypos = y - lineH / 2 + 3;
					g2d.drawLine(x - 1, ypos, x - 1 + length0 * 2, ypos);
				}
				if (f.isSquiggly()) {
					g2d.setColor(f.getSquiggleColor());
					int h = 2;
					int ypos = y;
					int xend = x + length0 * 2 + length0 / 2;
					for (int xs = x - length0 / 2; xs < xend; xs += 2 * h) {
						g2d.drawArc(xs, ypos, h, h, 0, 180);
						g2d.drawArc(xs + h, ypos, h, h, 180, 181);
					}
				}
				if (caret.hasSelection() && caret.intersects(index)) {
					g2d.setColor(getForeground());
					if (i == 0 || index == caret.getFirstByte())
						g2d.drawLine(x - length0 / 2, y - lineH + 3, x - length0 / 2, y + 2);
					if (i == 15 || index == caret.getLastByte())
						g2d.drawLine(x + (int) (length0 * 2.5) - 1, y - lineH + 3, x + (int) (length0 * 2.5) - 1, y + 2);
					if (!caret.intersects(index + 16))
						g2d.drawLine(x - length0 / 2, y + 2, x + (int) (length0 * 2.5) - 1, y + 2);
					if (!caret.intersects(index - 16))
						g2d.drawLine(x - length0 / 2, y - lineH + 3, x + (int) (length0 * 2.5) - 1, y - lineH + 3);
				}
				x += 3 * length0;
				if (showDump) {
					int dx = ix + (48 + i) * length0;
					g2d.setColor(f.getBackground());
					g2d.fillRect(dx, y - lineH + 3, length0, lineH);
					g2d.setColor(f.getForeground());
					char[] s = {b >= 32 && b <= 127 ? (char) b : '.'};
					g2d.drawString(new String(s), dx, y);
					if (caret.intersects(index)) {
						g2d.drawLine(dx, y + 2, dx + length0, y + 2);
					}
				}
			}
			
			if (!caret.hasSelection() && caret.hasValidPosition() && caret.getDot() / 16 == a) {
				g2d.setColor(Color.BLACK);
				int x1 = ix + ((caret.getDot() % 16) * 3 + (caret.isDotAfter() ? 2 : 1)) * length0;
				g2d.fillRect(x1 - 1, y - lineH + 5, 2, lineH - 4);
			}
		}
	}
	
	public static void getDigitsInByte(byte b, char[] arr) {
		arr[0] = (char) (((b >>> 4) & 0xf) + '0');
		if (arr[0] > '9')
			arr[0] += 'a' - '0' - 10;
		arr[1] = (char) ((b & 0xf) + '0');
		if (arr[1] > '9')
			arr[1] += 'a' - '0' - 10;
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
	
	private List<Token> getTokensAt(List<Token> tokens, int start, int end) {
		if (tokens == null) return null;
		return tokens.stream().filter(t -> start <= t.getOffset() + t.getLength() && t.getOffset() <= end).collect(Collectors.toList());
	}
	
	private Iterator<Token> getTokensAt(List<Token> tokens, int index) {
		return tokens.stream().filter(t -> t.at(index)).iterator();
	}
	
	private Format getFormatAt(List<Token> tokens, int index) {
		Format f = Theme.DEFAULT.get(TokenTypes.TOKEN_NONE);
		if (tokens == null) return f;
		Iterator<Token> i = getTokensAt(tokens, index);
		Level highest = Level.INFO;
		while (i.hasNext()) {
			Token t = i.next();
			if (t.getType() != TokenTypes.TOKEN_NONE) {
				f = f.combine(Theme.DEFAULT.get(t.getType()));
			}
			if (t.getToolTipLevel() != null) {
				highest = highest.compareTo(t.getToolTipLevel()) > 0 ? highest : t.getToolTipLevel();
			}
		}
		f = f.combine(Theme.DEFAULT.get(highest));
		// have < alpha on non-'selected' bytes in find/replace
		if (findRange != null && !(findRange.getLeft() <= index && index <= findRange.getRight())) {
			f = f.faded();
		}
		return f;
	}
	
	@Override
	public String getToolTipText(MouseEvent e) {
		if (tokens == null) return null;
		int index = getCoordsOffset(e.getX(), e.getY(), false);
		if (index >= 0) {
			Iterator<Token> i = getTokensAt(tokens, index);
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
		boolean fence;
		
		public LambdaAction(Runnable r, boolean fence) {
			this.r = r;
			this.fence = fence;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			r.run();
			if (fence) {
				document.pushFence();
			}
		}
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return new Dimension(length0 * 64, lineH * (document.getLength() + 15 / 16));
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return direction == SwingConstants.HORIZONTAL ? length0 * 3 : lineH;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return direction == SwingConstants.HORIZONTAL ? length0 * 3 : (visibleRect.height / lineH * lineH);
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
