package st.infos.elementalcube.snowhex;

import java.awt.AWTEventMulticaster;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.ui.HexCaret;

public abstract class HexDocument {

	/**
	 * Action Commands:
	 * - edit: some standalone edit was made
	 * - coalesce: some standalone edit replaced previous edits in the stack (may also remove all edits)
	 * - compound: one of a series of edits was made
	 * - undo: an edit was undone
	 * - redo: an edit was redone
	 * - replace: the whole document was replaced, clearing all stacks
	 */
	private ActionListener listener;

	public HexDocument() {
		super();
	}

	public abstract byte[] getBytes();

	// MARK: - undo manager
	
	/**
	 * Redoes a possibly coalesced action from the redo stack
	 */
	public abstract void redo();

	/**
	 * Undoes a possibly coalesced action from the undo stack
	 */
	public abstract void undo();

	public abstract boolean canRedo();
	public abstract boolean canUndo();
	
	// MARK: - edits
	
	// utility function for applying an edit to a byte array
	protected byte[] applyEditToArray(DocumentEdit edit, byte[] input) {
		if (edit.length == edit.replace.length) {
			// simple case, just overwrite
			System.arraycopy(edit.replace, 0, input, edit.start, edit.length);
			return input;
		} else {
			byte[] copy = new byte[input.length - edit.length + edit.replace.length];
			System.arraycopy(input, 0, copy, 0, edit.start);
			System.arraycopy(edit.replace, 0, copy, edit.start, edit.replace.length);
			System.arraycopy(input, edit.start + edit.length, copy, edit.start + edit.replace.length, input.length - (edit.start + edit.length));
			return copy;
		}
	}

	public abstract void pushEdit(DocumentEdit edit);

	// MARK: - events

	protected void sendEvent(Object sender, String actionCommand) {
		if (listener == null)
			return;
		listener.actionPerformed(new ActionEvent(sender, ActionEvent.ACTION_PERFORMED, actionCommand));
	}

	/**
	 * Pushes a fence to the undo stack, stopping previous and next edits from coalescing together
	 * 
	 * This method should be called when arrow keys or a click changes the caret position, or when we lose focus
	 * 
	 * Note that this doesn't dispatch an event, as the document is not changed
	 */
	public abstract void pushFence();

	public abstract void replaceDocument(byte[] array);

	// MARK: - convenience APIs

	public void insertBytes(int offset, byte[] b, EditType type) {
		pushEdit(new DocumentEdit(offset, 0, b, System.currentTimeMillis(), type));
	}

	public void replaceBytes(int offset, int length, byte[] b, EditType type) {
		pushEdit(new DocumentEdit(offset, length, b, System.currentTimeMillis(), type));
	}

	public void setByte(int offset, byte b, EditType type) {
		pushEdit(new DocumentEdit(offset, 1, new byte[] { b }, System.currentTimeMillis(), type));
	}

	public void removeBytes(int offset, int length, EditType type) {
		pushEdit(new DocumentEdit(offset, length, ArrayUtils.EMPTY_BYTE_ARRAY, System.currentTimeMillis(), type));
	}

	public void replaceSelectedBytes(HexCaret caret, byte[] b, EditType type) {
		pushEdit(new DocumentEdit(caret.getFirstByte(), caret.getLastByte() - caret.getFirstByte() + 1,
				b, System.currentTimeMillis(), type));
	}

	public void removeSelectedBytes(HexCaret caret, EditType type) {
		pushEdit(new DocumentEdit(caret.getFirstByte(), caret.getLastByte() - caret.getFirstByte() + 1,
				ArrayUtils.EMPTY_BYTE_ARRAY, System.currentTimeMillis(), type));
	}

	// MARK: - getters

	public byte getByte(int i) {
		return getBytes()[i];
	}

	public int getLength() {
		return getBytes().length;
	}

	public byte[] getBytes(int start, int length) {
		return ArrayUtils.subarray(getBytes(), start, start + length);
	}

	public byte[] getSelectedBytes(HexCaret caret) {
		return ArrayUtils.subarray(getBytes(), caret.getFirstByte(), caret.getLastByte() + 1);
	}

	public void addEditListener(ActionListener l) {
		listener = AWTEventMulticaster.add(listener, l);
	}

	public void removeEditListener(ActionListener l) {
		listener = AWTEventMulticaster.remove(listener, l);
	}
	
	/**
	 * An edit entry with information on how to undo it
	 */
	public class DocumentEdit {
		/**
		 * the start index of the change
		 */
		public final int start;
		/**
		 * the length of the area to replace
		 */
		public final int length;
		/**
		 * the byte array to replace the area with (not necessarily the same length)
		 */
		public final byte[] replace;
		/**
		 * the timestamp of the change (in ms)
		 */
		public final long time;
		/**
		 * the type of edit
		 */
		public final EditType type;
		
		public DocumentEdit(int start, int length, byte[] replace, long time, EditType type) {
			this.start = start;
			this.length = length;
			this.replace = replace;
			this.time = time;
			this.type = type;
		}
		
		public boolean canCoalesceWith(DocumentEdit edit) {
			if (isFence())
				return false; // blocks
			if (edit.isFence())
				return true; // last one did nothing, we need to
			if (type != edit.type)
				return false;
			if (Math.abs(time - edit.time) > 30000)
				return false; // 30s is the standard time to stop coalescing
			if (type == EditType.INSERT_PASTE || type == EditType.DELETE_CUT)
				return false; // cut/paste never get coalesced
			return true;
		}
		
		public boolean canReplace(DocumentEdit edit) {
			if (edit.isFence())
				return false; // can't replace a fence
			if (!canCoalesceWith(edit))
				return false;
			if (edit.replace.length != edit.length || replace.length != length)
				return false; // insertions/deletions not supported
			if (start != edit.start || length != edit.length)
				return false; // not same area
			return true;
		}

		protected DocumentEdit reversed() {
			byte[] newReplace = ArrayUtils.subarray(getBytes(), start, start + length);
			return new DocumentEdit(start, replace.length, newReplace, time, type);
		}
		
		/**
		 * @return true if this edit doesn't change anything
		 */
		public boolean isFence() {
			return replace.length == 0 && length == 0;
		}
		
		/**
		 * @return true if this edit, if applied now, doesn't change anything
		 */
		public boolean isNoOp() {
			return replace.length == length && Arrays.equals(replace, 0, replace.length, getBytes(), start, start + length);
		}
		
		@Override
		public String toString() {
			return "DocumentEdit [start=" + start + ", length=" + length + ", replace=" + Arrays.toString(replace)
					+ ", time=" + time + ", type=" + type + "]";
		}
	}

	public enum EditType {
		/**
		 * A property was changed from the PropertiesFrame
		 */
		PROPERTY_CHANGE,
		/**
		 * User typed it (inserts & overwrites)
		 */
		TYPING,
		/**
		 * User deleted text (zeroing & byte deletions)
		 */
		DELETE,
		/**
		 * User pasted data
		 */
		INSERT_PASTE,
		/**
		 * User cut data
		 */
		DELETE_CUT,
		/**
		 * Document lost focus. Blocks coalescing
		 */
		FENCE,
	}
}