package ch.vd.uniregctb.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

/**
 * Mock du servlet service qui ne fait rien
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockServletService implements ServletService {

	public void downloadAsFile(String fileName, InputStream is, Integer contentLength, HttpServletResponse response) throws IOException {
		// on ne fait rien ici
	}

	public void downloadAsFile(String fileName, byte[] bytes, HttpServletResponse response) throws IOException {
		// on ne fait rien ici
	}

	public void downloadAsFile(String fileName, String content, HttpServletResponse response) throws IOException {
		// on ne fait rien ici
	}
}
