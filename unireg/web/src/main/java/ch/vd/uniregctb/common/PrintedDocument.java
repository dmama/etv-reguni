package ch.vd.uniregctb.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Contenu d'un document imprimé en provenance d'éditique
 */
public class PrintedDocument extends TypedDataContainer {

	private static final String PREFIX = "ur-print-";

	public PrintedDocument(String mimeType, InputStream content, String filenameRadical) throws IOException {
		super(mimeType, content, PREFIX, filenameRadical);
	}

	public PrintedDocument(String mimeType, byte[] content, String filenameRadical) throws IOException {
		super(mimeType, content, PREFIX, filenameRadical);
	}
}
