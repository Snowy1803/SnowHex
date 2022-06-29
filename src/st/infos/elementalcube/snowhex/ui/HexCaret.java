package st.infos.elementalcube.snowhex.ui;

import java.awt.AWTEventMulticaster;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HexCaret {
	/**
	 * The position of the caret
	 */
	private int dot = -1;
	/**
	 * The mark left behind, when selecting something
	 */
	private int mark = -1;
	/**
	 * True if the dot is after the current byte, false if it is in the middle
	 * When there is a selection, it is unused
	 */
	private boolean dotAfter = false;
	/**
	 * The component the caret is attached to
	 */
	private HexPanel component;
	
	private ActionListener listener;
	private ActionEvent sharedEvent;
	
	public HexCaret(HexPanel component) {
		this.component = component;
		this.sharedEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null);
	}
	
	/**
	 * Resets selection, and sets the caret at the given position
	 * @param position the caret byte offset
	 * @param afterByte true if the dot is after the current byte, false if it is in the middle
	 */
	public void setCaretPosition(int position, boolean afterByte) {
		int pos = normalizePos(position);
		dot = pos;
		mark = pos;
		dotAfter = pos != position || pos == -1 ? true : afterByte;
		listener.actionPerformed(sharedEvent);
	}
	
	/**
	 * Removes the caret from the visible view
	 */
	public void removeCaretPosition() {
		dot = -1;
		mark = -1;
		dotAfter = false;
		listener.actionPerformed(sharedEvent);
	}
	
	/**
	 * Moves the dot, leaving the mark behind, creating a selection
	 * @param dot the dot byte offset
	 */
	public void moveDot(int dot) {
		dot = normalizePos(dot);
		if (this.mark == this.dot && dot != this.dot) { // create the selection
			if (!this.dotAfter) {
				if (dot > mark) {
					this.mark--;
				}
				this.dotAfter = true;
			}
		}
		this.dot = dot;
		listener.actionPerformed(sharedEvent);
	}
	
	public int normalizePos(int pos) {
		if (pos >= component.getBytes().length) {
			return component.getBytes().length - 1;
		} else if (pos < 0) {
			return -1;
		} else {
			return pos;
		}
	}
	
	public void moveCaretLeft() {
		if (dotAfter) {
			setCaretPosition(dot, false);
		} else {
			setCaretPosition(dot - 1, true);
		}
	}
	
	public void moveCaretRight() {
		if (dotAfter) {
			setCaretPosition(dot + 1, false);
		} else {
			setCaretPosition(dot, true);
		}
	}
	
	// MARK: - event

	public void addChangeListener(ActionListener l) {
		listener = AWTEventMulticaster.add(listener, l);
	}
	
	public void removeChangeListener(ActionListener l) {
		listener = AWTEventMulticaster.remove(listener, l);
	}
	
	// MARK: - getters
	
	public int getDot() {
		return dot;
	}
	
	public int getMark() {
		return mark;
	}
	
	public int getFirstByte() {
		if (!hasSelection()) {
			return dot;
		}
		if (dot < mark) {
			return dot + 1;
		} else {
			return mark + 1;
		}
	}
	
	public int getLastByte() {
		if (!hasSelection()) {
			return dot;
		}
		if (dot < mark) {
			return mark;
		} else {
			return dot;
		}
	}
	
	public boolean isDotAfter() {
		return dotAfter;
	}
	
	public boolean hasValidPosition() {
		return dot != mark || dot >= 0 || dotAfter;
	}
	
	public boolean hasSelection() {
		return dot != mark;
	}

	public boolean intersects(int index) {
		if (hasSelection()) {
			return getFirstByte() <= index && index <= getLastByte();
		}
		return dot == index;
	}
}
