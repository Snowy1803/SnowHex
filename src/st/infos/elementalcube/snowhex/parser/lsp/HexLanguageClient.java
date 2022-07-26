package st.infos.elementalcube.snowhex.parser.lsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

public class HexLanguageClient implements LanguageClient {
	
	public static final HexLanguageClient INSTANCE = new HexLanguageClient();
	
	private HashMap<URI, LSPTokenMaker> documents = new HashMap<>();
	
	private HexLanguageClient() {}
	
	public void registerDocument(String uri, LSPTokenMaker tm) {
		try {
			documents.put(new URI(uri), tm);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void telemetryEvent(Object object) {
		System.out.println("Telemetry Event: " + object);
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		try {
			documents.get(new URI(diagnostics.getUri())).setDiagnostics(diagnostics.getDiagnostics());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void showMessage(MessageParams messageParams) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(null, messageParams.getMessage());
		});
	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
		CompletableFuture<MessageActionItem> result = new CompletableFuture<>();
		SwingUtilities.invokeLater(() -> {
			Object[] arr = requestParams.getActions().stream().map(MessageActionItem::getTitle).toArray();
			Object select = JOptionPane.showInputDialog(null, requestParams.getMessage(), null, JOptionPane.PLAIN_MESSAGE, null, arr, null);
			int ind = -1;
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] == select) {
					ind = i;
					break;
				}
			}
			result.complete(requestParams.getActions().get(ind));
		});
		return result;
	}

	@Override
	public void logMessage(MessageParams message) {
		System.out.println("[LSP]" + message.getMessage());
	}

}
