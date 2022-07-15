package st.infos.elementalcube.snowhex.parser.png;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterInputStream;

import st.infos.elementalcube.snowhex.HexDocument;

/**
 * A document for an inflated PNG IDAT chunk. All modifications are reflected on the parent document and vice-versa.
 * The chunk length and chunk checksum are both also updated at each change.
 * 
 * @author emil
 *
 */
public class InflatedSubDocument extends HexDocument implements ActionListener {
	private HexDocument parent;
	private int parentOffset, parentLength;
	private byte[] inflated;
	private CRC32 crc = new CRC32();
	private boolean paused;
	
	/**
	 * Creates a document for an inflated PNG IDAT chunk.
	 * 
	 * @param parent the parent PNG document
	 * @param offset the offset of the IDAT content token (right after the chunk header)
	 * @param length the length of the chunk
	 */
	public InflatedSubDocument(HexDocument parent, int offset, int length) {
		this.parent = parent;
		this.parentOffset = offset;
		this.parentLength = length;
		parent.addEditListener(this);
		updateInflate();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "compound")
			return;
		updateInflate();
		sendEvent(e.getSource(), e.getActionCommand());
	}
	
	private void updateInflate() {
		if (paused)
			return; // its our own change
		ByteArrayInputStream in = new ByteArrayInputStream(parent.getBytes(), parentOffset, parentLength);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InflaterInputStream inflater = new InflaterInputStream(in)) {
            int c;
            while ((c = inflater.read()) != -1) {
                baos.write(c);
            }
        } catch (IOException e) {
			e.printStackTrace();
		}
        inflated = baos.toByteArray();
	}
	
	private void updateDeflate(EditType type) {
		ByteArrayInputStream in = new ByteArrayInputStream(inflated);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterInputStream deflater = new DeflaterInputStream(in)) {
            int c;
            while ((c = deflater.read()) != -1) {
                baos.write(c);
            }
        } catch (IOException e) {
			e.printStackTrace();
		}
        paused = true;
        byte[] change = baos.toByteArray();
        parent.replaceBytes(parentOffset, parentLength, change, type);
        parentLength = change.length;
        {
	        byte[] size = new byte[4];
	        ByteBuffer.wrap(size).putInt(parentLength);
	        parent.replaceBytes(parentOffset - 8, 4, size, type);
        }
        {
	        byte[] checksum = new byte[4];
			crc.reset();
			crc.update(parent.getBytes(), parentOffset - 4, parentLength + 4);
	        ByteBuffer.wrap(checksum).putInt((int) crc.getValue());
	        parent.replaceBytes(parentOffset + parentLength, 4, checksum, type);
        }
        paused = false;
	}

	@Override
	public byte[] getBytes() {
		return inflated;
	}

	@Override
	public void redo() {
		parent.redo();
	}

	@Override
	public void undo() {
		parent.undo();
	}

	@Override
	public boolean canRedo() {
		return parent.canRedo();
	}

	@Override
	public boolean canUndo() {
		return parent.canUndo();
	}

	@Override
	public void pushEdit(DocumentEdit edit) {
		inflated = applyEditToArray(edit, inflated);
		updateDeflate(edit.type);
		sendEvent(edit, "edit");
	}

	@Override
	public void pushFence() {
		parent.pushFence();
	}

	@Override
	public void replaceDocument(byte[] array) {
		throw new UnsupportedOperationException("Replacing an InflatedSubDocument is illegal");
	}
}
