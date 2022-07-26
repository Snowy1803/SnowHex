package st.infos.elementalcube.snowhex.parser.lsp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.GeneralClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PositionEncodingKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensCapabilities;
import org.eclipse.lsp4j.SemanticTokensClientCapabilitiesRequests;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TokenFormat;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WindowClientCapabilities;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;

import st.infos.elementalcube.snowhex.HexDocument.DocumentEdit;
import st.infos.elementalcube.snowhex.Token;
import st.infos.elementalcube.snowhex.Token.Level;
import st.infos.elementalcube.snowhex.TokenImpl;
import st.infos.elementalcube.snowhex.TokenMaker;
import st.infos.elementalcube.snowhex.ui.HexFrame;
import st.infos.elementalcube.snowhex.ui.HexPanel;

public class LSPTokenMaker extends TokenMaker implements ActionListener {
	
	private HexLanguageClient client;
	private Launcher<LanguageServer> server;
	private HexPanel panel;
	
	private List<Token> diagnostics = List.of();
	private List<Token> semanticTokens = List.of();
	
	private InitializeResult initResult;
	
	private VersionedTextDocumentIdentifier id;
	
	@Override
	public void setParent(HexPanel panel) {
		try {
			initialize(panel, new String[] { "/usr/bin/clangd" });
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void initialize(HexPanel panel, String[] args) throws IOException {
		this.panel = panel;
		Process proc = new ProcessBuilder(args).redirectError(Redirect.INHERIT).start();
		client = HexLanguageClient.INSTANCE;
		File f = ((HexFrame) SwingUtilities.getWindowAncestor(panel)).getFile();
		id = new VersionedTextDocumentIdentifier(f.toURI().normalize().toString(), 0);
		client.registerDocument(id.getUri(), this);
		server = LSPLauncher.createClientLauncher(client, proc.getInputStream(), proc.getOutputStream());
		server.startListening();
		InitializeParams init = new InitializeParams();
		init.setClientInfo(new ClientInfo("SnowHex", "2.0"));
		init.setCapabilities(new ClientCapabilities(new WorkspaceClientCapabilities(), new TextDocumentClientCapabilities(), new WindowClientCapabilities(), null));
		GeneralClientCapabilities gcc = new GeneralClientCapabilities();
		gcc.setPositionEncodings(List.of(PositionEncodingKind.UTF16, PositionEncodingKind.UTF8));
		init.getCapabilities().setGeneral(gcc);
		SemanticTokensCapabilities semtok = new SemanticTokensCapabilities(
				new SemanticTokensClientCapabilitiesRequests(true),
				List.of(SemanticTokenTypes.Comment, SemanticTokenTypes.String, SemanticTokenTypes.Keyword),
				List.of(), List.of(TokenFormat.Relative));
		semtok.setMultilineTokenSupport(true);
		semtok.setOverlappingTokenSupport(true);
		semtok.setAugmentsSyntaxTokens(false);
		init.getCapabilities().getTextDocument().setSemanticTokens(semtok);
		server.getRemoteProxy().initialize(init).thenAccept(i -> {
			initResult = i;
		});
		lastDoc = new String(panel.getDocument().getBytes(), StandardCharsets.UTF_8);
		lastDocB = panel.getDocument().getBytes();
		server.getRemoteProxy().getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
				new TextDocumentItem(id.getUri(), "c", id.getVersion(), lastDoc)));
		panel.getDocument().addEditListener(this);
	}
	
	private String lastDoc = "";
	private byte[] lastDocB = null;
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "compound":
		case "edit":
			// take the edit
			if (initResult.getCapabilities().getTextDocumentSync().getLeft() == TextDocumentSyncKind.Incremental
			|| initResult.getCapabilities().getTextDocumentSync().getRight().getChange() == TextDocumentSyncKind.Incremental) {
				// incremental change
				id.setVersion(id.getVersion() + 1);
				DocumentEdit edit = (DocumentEdit) e.getSource();
				String currDoc = new String(panel.getDocument().getBytes(), StandardCharsets.UTF_8);
				server.getRemoteProxy().getTextDocumentService().didChange(
						new DidChangeTextDocumentParams(id, List.of(
								new TextDocumentContentChangeEvent(
										new Range(byteOffsetToPosition(lastDocB, edit.start), 
												byteOffsetToPosition(lastDocB, edit.start + edit.length)),
										new String(edit.replace, StandardCharsets.UTF_8)))));
				lastDoc = currDoc;
				lastDocB = panel.getDocument().getBytes();
				break;
			}
		case "replace":
			// send replace
			id.setVersion(id.getVersion() + 1);
			String currDoc = new String(panel.getDocument().getBytes(), StandardCharsets.UTF_8);
			server.getRemoteProxy().getTextDocumentService().didChange(
					new DidChangeTextDocumentParams(id, List.of(
							new TextDocumentContentChangeEvent(
									new Range(byteOffsetToPosition(lastDocB, 0), 
											byteOffsetToPosition(lastDocB, lastDocB.length)), currDoc))));
			lastDoc = currDoc;
			lastDocB = panel.getDocument().getBytes();
			break;
		case "coalesce":
		case "undo":
		case "redo":
			break; // don't care
		}
	}
	
	int lastLoadVersion = -1;

	@Override
	public List<Token> generateTokens(byte[] array) {
		if (id.getVersion() != lastLoadVersion) {
			lastLoadVersion = id.getVersion();
			server.getRemoteProxy().getTextDocumentService().semanticTokensFull(new SemanticTokensParams(new TextDocumentIdentifier(id.getUri()))).thenAccept(tok -> {
				setSemanticTokens(tok);
			}).exceptionally(e -> {
				e.printStackTrace();
				return null;
			});
		}
		return Stream.concat(diagnostics.stream(), semanticTokens.stream()).collect(Collectors.toList());
	}
	
	public void setDiagnostics(List<Diagnostic> diagnostics) {
		String doc = new String(panel.getDocument().getBytes(), StandardCharsets.UTF_8);
		this.diagnostics = diagnostics.stream().map(d -> {
			// we can't use the pool here
			Level l;
			switch (d.getSeverity()) {
			case Error:
				l = Level.ERROR;
				break;
			case Warning:
				l = Level.WARNING;
				break;
			case Information:
				l = Level.NOTICE;
				break;
			case Hint:
				l = Level.INFO;
				break;
			default:
				l = Level.INFO;
				break;
			}
			int start = positionToByteOffset(doc, d.getRange().getStart());
			Token t = new TokenImpl(TOKEN_NONE, start, positionToByteOffset(doc, d.getRange().getEnd()) - start, d.getMessage(), l);
			return t;
		}).collect(Collectors.toList());
		panel.reloadColorsNow();
	}
	
	public void setSemanticTokens(SemanticTokens tok) {
		ArrayList<Token> tokens = new ArrayList<>();
		List<Integer> data = tok.getData();
		int line = 0;
		int offs = 0;
		super.invalidateTokenPool();
		for (int i = 0; i < data.size(); i += 5) {
			int dl = data.get(i);
			int ds = data.get(i + 1);
			int len = data.get(i + 2);
			int tt = data.get(i + 3);
			line += dl;
			if (dl == 0) {
				offs += ds;
			} else {
				offs = ds;
			}
			int offset = positionToByteOffset(lastDoc, new Position(line, offs));
			int length = lastDoc.substring(offset, offset + len).getBytes(StandardCharsets.UTF_8).length;
			String type = initResult.getCapabilities().getSemanticTokensProvider().getLegend().getTokenTypes().get(tt);
			switch (type) {
			case SemanticTokenTypes.Keyword:
				tt = TOKEN_KEYWORD;
				break;
			case SemanticTokenTypes.Comment:
				tt = TOKEN_COMMENT;
				break;
			case SemanticTokenTypes.String:
				tt = TOKEN_STRING;
				break;
			case SemanticTokenTypes.Function:
				tt = TOKEN_METADATA;
				break;
			case SemanticTokenTypes.Variable:
				tt = TOKEN_LENGTH;
				break;
			default:
				tt = TOKEN_NONE;
				break;
			}
			tokens.add(createToken(tt, offset, length));
		}
		setSemanticTokens(tokens);
	}
	
	@Override
	public void invalidateTokenPool() {
	}
	
	public void setSemanticTokens(List<Token> semanticTokens) {
		this.semanticTokens = semanticTokens;
		panel.reloadColorsNow();
	}

	@Override
	public String getName() {
		return "lsp";
	}
	
	@Override
	public String getLocalizedName() {
		if (initResult == null || initResult.getServerInfo() == null)
			return super.getLocalizedName();
		return initResult.getServerInfo().getName();
	}

	@Override
	public Object getDump(byte[] array) {
		return null;
	}
	
	public static int positionToByteOffset(String doc, Position pos) {
		int index = 0;
		int line = 0;
		while (line < pos.getLine()) {
			while (doc.charAt(index) != '\r' && doc.charAt(index) != '\n') {
				index++;
				if (index > doc.length())
					return -1;
			}
			if (doc.charAt(index) == '\r' && doc.charAt(index + 1) == '\n')
				index++;
			line++;
			index++;
		}
		return doc.substring(0, index + pos.getCharacter()).getBytes(StandardCharsets.UTF_8).length;
	}
	
	public static Position byteOffsetToPosition(byte[] doc, int offset) {
		String subs = new String(doc, 0, offset);
		int index = 0;
		int line = 0;
		int last = 0;
		while (index < subs.length()) {
			if (subs.charAt(index) == '\n') {
				line++;
				last = index + 1;
			} else if (subs.charAt(index) == '\r') {
				line++;
				if (subs.charAt(index + 1) == '\n')
					index++;
				last = index + 1;
			}
			index++;
		}
		return new Position(line, subs.length() - last);
	}
}
