package st.infos.elementalcube.snowhex.parser.gif;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.ui.HexPanel;

public class DelayEditor extends JPanel {
	private static final long serialVersionUID = 3377734246008920485L;
	private JSpinner delay;

	public DelayEditor(HexPanel panel) {
		super(new BorderLayout());
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
		JLabel text = new JLabel("Delay before next frame");
		text.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		text.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(text);
		delay = new JSpinner(new SpinnerNumberModel(0, 0, Short.toUnsignedInt((short) -1) / 100.0, 0.01));
		delay.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		delay.setAlignmentX(Component.LEFT_ALIGNMENT);
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(delay, "0.00 s");
		delay.setEditor(editor);
		delay.addChangeListener(e -> {
			ByteBuffer bb = ByteBuffer.wrap(panel.getBytes(), panel.getClosestToken().getOffset(), 2);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			bb.putShort((short) Math.round((double) delay.getValue() * 100d));
			panel.bytesDidChange();
		});
		content.add(delay);
		add(content, BorderLayout.PAGE_START);
	}

	public void updateValues(HexPanel panel) {
		byte[] bytes = panel.getBytes();
		int offset = panel.getClosestToken().getOffset();
		short s = TokenMaker.toShort(bytes[offset], bytes[offset + 1], ByteOrder.LITTLE_ENDIAN);
		double d = (double) Short.toUnsignedInt(s) / 100d;
		delay.setValue(d);
	}
}