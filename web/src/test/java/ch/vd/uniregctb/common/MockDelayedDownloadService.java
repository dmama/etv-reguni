package ch.vd.uniregctb.common;

import java.io.IOException;
import java.util.UUID;

import ch.vd.uniregctb.editique.EditiqueResultatDocument;

public class MockDelayedDownloadService implements DelayedDownloadService {

	@Override
	public UUID putDocument(EditiqueResultatDocument document, String filenameRadical) throws IOException {
		return null;
	}

	@Override
	public TypedDataContainer fetchDocument(UUID id, boolean remove) {
		return null;
	}

	@Override
	public void eraseDocument(UUID id) {
	}

	@Override
	public int getPendingSize() {
		return 0;
	}
}
