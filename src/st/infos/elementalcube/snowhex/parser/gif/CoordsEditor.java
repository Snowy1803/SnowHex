package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import st.infos.elementalcube.snowhex.HexDocument.EditType;
import st.infos.elementalcube.snowhex.ui.HexPanel;

public class CoordsEditor extends JPanel {
	private static final long serialVersionUID = 3377734246008920485L;
	private TitledBorder label;
	private JSpinner x, y;
	private int bytesPerCoord;
	private ByteOrder endianness;
	private boolean paused;

	public CoordsEditor(HexPanel panel, boolean position, ByteOrder endianness) {
		super(new BorderLayout());
		this.endianness = endianness;
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipady = 5;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		
		c.gridwidth = 1;
		content.add(new JLabel(position ? "X position:" : "Width:"), c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		x = new JSpinner(new SpinnerNumberModel(Long.valueOf(0), Long.valueOf(0), null, Long.valueOf(1)));
		x.addChangeListener(e -> {
			if (paused)
				return;
			int offs = panel.getClosestToken().getOffset();
			panel.getDocument().replaceBytes(offs, bytesPerCoord, valueToByte((long) x.getValue()), EditType.PROPERTY_CHANGE);
		});
		content.add(x, c);
		
		c.gridwidth = 1;
		content.add(new JLabel(position ? "Y position:" : "Height:"), c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		y = new JSpinner(new SpinnerNumberModel(Long.valueOf(0), Long.valueOf(0), null, Long.valueOf(1)));
		y.addChangeListener(e -> {
			if (paused)
				return;
			int offs = panel.getClosestToken().getOffset();
			panel.getDocument().replaceBytes(offs + bytesPerCoord, bytesPerCoord, valueToByte((long) y.getValue()), EditType.PROPERTY_CHANGE);
		});
		content.add(y, c);
		
		label = BorderFactory.createTitledBorder(null, "");
		content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), label));
		add(content, BorderLayout.PAGE_START);
	}
	
	private byte[] valueToByte(long value) {
		ByteBuffer buf = ByteBuffer.allocate(bytesPerCoord).order(endianness);
		if (bytesPerCoord == 1) {
			buf.put((byte) value);
		} else if (bytesPerCoord == 2) {
			buf.putShort((short) value);
		} else if (bytesPerCoord == 4) {
			buf.putInt((int) value);
		}
		return buf.array();
	}
	
	private long bytesToValue(byte[] array, int offs) {
		ByteBuffer buf = ByteBuffer.wrap(array, offs, bytesPerCoord).order(endianness);
		if (bytesPerCoord == 1) {
			return Byte.toUnsignedInt(buf.get());
		} else if (bytesPerCoord == 2) {
			return Short.toUnsignedInt(buf.getShort());
		} else if (bytesPerCoord == 4) {
			return Integer.toUnsignedLong(buf.getInt());
		}
		return 0;
	}

	public void updateValues(HexPanel panel, String type) {
		paused = true;
		this.bytesPerCoord = panel.getClosestToken().getLength() / 2;
		this.label.setTitle(type);
		long maximum = (1L << (bytesPerCoord * 8L)) - 1L;
		((SpinnerNumberModel) x.getModel()).setMaximum(maximum);
		((SpinnerNumberModel) y.getModel()).setMaximum(maximum);
		
		int offs = panel.getClosestToken().getOffset();
		x.setValue(bytesToValue(panel.getDocument().getBytes(), offs));
		y.setValue(bytesToValue(panel.getDocument().getBytes(), offs + bytesPerCoord));
		paused = false;
	}
}