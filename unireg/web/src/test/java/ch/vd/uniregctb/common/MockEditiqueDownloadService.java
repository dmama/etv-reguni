package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import ch.vd.uniregctb.editique.EditiqueResultatDocument;

public class MockEditiqueDownloadService implements EditiqueDownloadService {

	@Override
	public void download(EditiqueResultatDocument resultat, String filenameRadical, HttpServletResponse response) throws IOException {
		// on ne fait rien, c'est un mock...
	}
}
