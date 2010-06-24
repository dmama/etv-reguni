package ch.vd.uniregctb.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

public interface ServletService {

	/**
	 * Génère une réponse http qui provoque le téléchargement du contenu du stream sous forme d'un fichier.
	 *
	 * @param fileName
	 *            le nom du fichier qui sera proposé à l'utilisateur
	 * @param is
	 *            le contenu du fichier à télécharger
	 * @param contentLength
	 *            la taille du contenu du fichier, peut être <b>null</b>
	 * @throws IOException
	 */
	void downloadAsFile(String fileName, InputStream is, Integer contentLength, HttpServletResponse response) throws IOException;

	/**
	 * Génère une réponse http qui provoque le téléchargement du contenu d'un tableau de bytes sous forme d'un fichier.
	 *
	 * @param fileName
	 *            le nom du fichier qui sera proposé à l'utilisateur
	 * @param bytes
	 *            le contenu du fichier à télécharger
	 * @throws IOException
	 */
	void downloadAsFile(String fileName, byte[] bytes, HttpServletResponse response) throws IOException;

	/**
	 * Génère une réponse http qui provoque le téléchargement du contenu d'une string sous forme d'un fichier.
	 *
	 * @param fileName
	 *            le nom du fichier qui sera proposé à l'utilisateur
	 * @param content
	 *            le contenu du fichier à télécharger
	 * @throws IOException
	 */
	void downloadAsFile(String fileName, String content, HttpServletResponse response) throws IOException;
}
