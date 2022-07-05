package st.infos.elementalcube.snowhex;

import java.awt.AWTEventMulticaster;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;

import org.apache.commons.lang3.ArrayUtils;

import st.infos.elementalcube.snowhex.ui.HexCaret;

/**
 * The document holding the array of bytes
 * @author emil
 *
 */
public class HexDocument {
	private byte[] bytes;
	private Stack<DocumentEdit> undos = new Stack<>();
	private Stack<DocumentEdit> redos = new Stack<>();
	private boolean undoing, redoing;
	/**
	 * Action Commands:
	 * - edit: some standalone edit was made
	 * - compound: one of a series of edits was made
	 * - undo: an edit was undone
	 * - redo: an edit was redone
	 * - replace: the whole document was replaced, clearing all stacks
	 */
	private ActionListener listener;
	
	public HexDocument() {
		addEditListener(e -> {
			for (DocumentEdit edit : undos) {
				System.out.println(edit);
			}
			System.out.println("----");
			for (DocumentEdit edit : redos) {
				System.out.println(edit);
			}
			System.out.println("====");
		});
	}
	
	// MARK: - Edit handling
	
	// Applies an edit, without pushing it to any stack
	private void applyEdit(DocumentEdit edit) {
		if (edit.length == edit.replace.length) {
			// simple case, just overwrite
			System.arraycopy(edit.replace, 0, bytes, edit.start, edit.length);
		} else {
			byte[] copy = new byte[bytes.length - edit.length + edit.replace.length];
			System.arraycopy(bytes, 0, copy, 0, edit.start);
			System.arraycopy(edit.replace, 0, copy, edit.start, edit.replace.length);
			System.arraycopy(bytes, edit.start + edit.length, copy, edit.start + edit.replace.length, bytes.length - (edit.start + edit.length));
			bytes = copy;
		}
	}
	
	private void pushCompoundEdit(DocumentEdit edit, String actionCommand) {
		if (undoing) {
			redos.push(edit.reversed());
		} else {
			undos.push(edit.reversed());
			if (!redoing)
				redos.clear();
		}
		applyEdit(edit);
		listener.actionPerformed(new ActionEvent(edit, ActionEvent.ACTION_PERFORMED, actionCommand));
	}
	
	public void pushEdit(DocumentEdit edit) {
		pushCompoundEdit(edit, "edit");
	}
	
	/**
	 * Pushes a fence to the undo stack, stopping previous and next edits from coalescing together
	 * 
	 * This method should be called when arrow keys or a click changes the caret position, or when we lose focus
	 * 
	 * Note that this doesn't dispatch an event, as the document is not changed
	 */
	public void pushFence() {
		if (undos.peek().isFence())
			return;
		undos.push(new DocumentEdit(0, 0, ArrayUtils.EMPTY_BYTE_ARRAY, System.currentTimeMillis(), EditType.FENCE));
	}
	
	// MARK: - Undo/Redo handling
	
	private void undoOrRedoStack(Stack<DocumentEdit> stack) {
		if (stack.empty())
			return;
		DocumentEdit edit = stack.pop();
		pushCompoundEdit(edit, "compound");
		while (!stack.empty() && stack.peek().canCoalesceWith(edit)) {
			edit = stack.pop();
			pushCompoundEdit(edit, "compound");
		}
	}
	
	/**
	 * Undoes a possibly coalesced action from the undo stack
	 */
	public void undo() {
		undoing = true;
		undoOrRedoStack(undos);
		undoing = false;
		listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "undo"));
	}
	
	/**
	 * Redoes a possibly coalesced action from the redo stack
	 */
	public void redo() {
		redoing = true;
		undoOrRedoStack(redos);
		redoing = false;
		listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "redo"));
	}
	
	public boolean canUndo() {
		return !undos.isEmpty();
	}
	
	public boolean canRedo() {
		return !redos.isEmpty();
	}
	
	// MARK: - convenience APIs
	
	public void replaceDocument(byte[] array) {
		this.bytes = array;
		undos.clear();
		redos.clear();
		listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "replace"));
	}
	
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
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public byte getByte(int i) {
		return bytes[i];
	}
	
	public int getLength() {
		return bytes.length;
	}
	
	public byte[] getBytes(int start, int length) {
		return ArrayUtils.subarray(bytes, start, start + length);
	}
	
	public byte[] getSelectedBytes(HexCaret caret) {
		return ArrayUtils.subarray(bytes, caret.getFirstByte(), caret.getLastByte() + 1);
	}
	
	// MARK: - events
	
	public void addEditListener(ActionListener l) {
		listener = AWTEventMulticaster.add(listener, l);
	}
	
	public void removeEditListener(ActionListener l) {
		listener = AWTEventMulticaster.remove(listener, l);
	}
	
	/**
	 * An edit entry with information on how to undo it
	 */
	class DocumentEdit {
		/**
		 * the start index of the change
		 */
		protected final int start;
		/**
		 * the length of the area to replace
		 */
		protected final int length;
		/**
		 * the byte array to replace the area with (not necessarily the same length)
		 */
		protected final byte[] replace;
		/**
		 * the timestamp of the change (in ms)
		 */
		protected final long time;
		/**
		 * the type of edit
		 */
		protected final EditType type;
		
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

		private DocumentEdit reversed() {
			byte[] newReplace = ArrayUtils.subarray(bytes, start, start + length);
			return new DocumentEdit(start, replace.length, newReplace, time, type);
		}
		
		/**
		 * @return true if this edit doesn't change anything
		 */
		public boolean isFence() {
			return replace.length == 0 && length == 0;
		}
	}
	
	public enum EditType {
		/**
		 * A property was changed from the PropertiesFrame
		 */
		PROPERTY_CHANGE,
		/**
		 * User typed it
		 */
		TYPING,
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
