package st.infos.elementalcube.snowhex;

import java.util.Stack;

import org.apache.commons.lang3.ArrayUtils;

/**
 * The document holding the array of bytes
 * @author emil
 *
 */
public class DefaultHexDocument extends HexDocument {
	private byte[] bytes;
	private Stack<DocumentEdit> undos = new Stack<>();
	private Stack<DocumentEdit> redos = new Stack<>();
	private boolean undoing, redoing;
	
	
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
		sendEvent(edit, actionCommand);
	}
	
	@Override
	public void pushEdit(DocumentEdit edit) {
		if (!edit.isFence() && edit.isNoOp()) {
			return; // no change
		}
		boolean shouldFastCoalesce = !undoing && !redoing && !undos.empty() && edit.canReplace(undos.peek());
		pushCompoundEdit(edit, "edit");
		if (shouldFastCoalesce) {
			undos.pop(); // what we just added is redundant
			if (undos.peek().isNoOp()) { // the edit we replace becomes useless
				undos.pop();
			}
			sendEvent(edit, "coalesce");
		}
	}
	
	@Override
	public void pushFence() {
		if (undos.empty() || undos.peek().isFence())
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
	@Override
	public void undo() {
		undoing = true;
		undoOrRedoStack(undos);
		undoing = false;
		sendEvent(this, "undo");
	}
	
	/**
	 * Redoes a possibly coalesced action from the redo stack
	 */
	@Override
	public void redo() {
		redoing = true;
		undoOrRedoStack(redos);
		redoing = false;
		sendEvent(this, "redo");
	}
	
	@Override
	public boolean canUndo() {
		return !undos.isEmpty();
	}
	
	@Override
	public boolean canRedo() {
		return !redos.isEmpty();
	}
	
	@Override
	public void replaceDocument(byte[] array) {
		this.bytes = array;
		undos.clear();
		redos.clear();
		sendEvent(this, "replace");
	}
	
	@Override
	public byte[] getBytes() {
		return bytes;
	}
}
