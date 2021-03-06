package ch.vd.unireg.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.print.PrintPCLManager;
import ch.vd.unireg.servlet.ServletService;

/**
 * Service qui permet de factoriser la gestion IHM de réception et du téléchargement
 * des retours d'impression éditique
 */
public class EditiqueDownloadServiceImpl implements EditiqueDownloadService, InitializingBean {

	private PrintPCLManager printPCLManager;

	private ServletService servletService;

	private Map<String, Downloader> downloaders;
	private Downloader defaultDownloader;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPrintPCLManager(PrintPCLManager printPCLManager) {
		this.printPCLManager = printPCLManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		defaultDownloader = new DefaultDownloader(servletService);

		final Downloader pclDownloader = new PclDownloader(printPCLManager);
		downloaders = new HashMap<>();
		downloaders.put(MimeTypeHelper.MIME_HPPCL, pclDownloader);
		downloaders.put(MimeTypeHelper.MIME_XPCL, pclDownloader);
		downloaders.put(MimeTypeHelper.MIME_PCL, pclDownloader);
	}

	/**
	 * Interface qui permet de spécialiser la manière de télécharger un retour d'éditique
	 * en fonction du type de contenu
	 */
	private interface Downloader {

		/**
		 * Remplit la réponse HTTP avec le document à charger
		 * @param contenu contenu du document
		 * @param mimeType type de contenu
		 * @param filenameRadical radical (sans l'extension) du nom de fichier présenté dans la réponse HTTP
		 * @param response réponse à remplir
		 * @throws IOException en cas de problème
		 */
		void download(byte[] contenu, String mimeType, String filenameRadical, HttpServletResponse response) throws IOException;

		/**
		 * Remplit la réponse HTTP avec le document à charger
		 * @param contenu le stream du contenu à télécharger
		 * @param size (optionnelle) taille du contenu, en bytes
		 * @param mimeType type de contenu
		 * @param filenameRadical radical (sans l'extension) du nom de fichier présenté dans la réponse HTTP
		 * @param response réponse à remplir
		 * @throws IOException en cas de problème
		 */
		void download(InputStream contenu, @Nullable Integer size, String mimeType, String filenameRadical, HttpServletResponse response) throws IOException;
	}

	/**
	 * Classe de downloader direct (sans manipulation)
	 */
	private static final class DefaultDownloader implements Downloader {

		private final ServletService service;

		public DefaultDownloader(ServletService service) {
			this.service = service;
		}

		@Override
		public void download(byte[] contenu, String mimeType, String filenameRadical, HttpServletResponse response) throws IOException {
			final String filename = String.format("%s%s", filenameRadical, MimeTypeHelper.getFileExtensionForType(mimeType));
			service.downloadAsFile(filename, contenu, response);
		}

		@Override
		public void download(InputStream contenu, @Nullable Integer size, String mimeType, String filenameRadical, HttpServletResponse response) throws IOException {
			final String filename = String.format("%s%s", filenameRadical, MimeTypeHelper.getFileExtensionForType(mimeType));
			service.downloadAsFile(filename, contenu, size, response);
		}
	}

	/**
	 * Classe de downloader spécifique PCL
	 */
	private static final class PclDownloader implements Downloader {

		private final PrintPCLManager pclManager;

		public PclDownloader(PrintPCLManager pclManager) {
			this.pclManager = pclManager;
		}

		@Override
		public void download(byte[] contenu, String mimeType, String filenameRadical, HttpServletResponse response) throws IOException {
			pclManager.openPclStream(response, filenameRadical, contenu);
		}

		@Override
		public void download(InputStream contenu, @Nullable Integer size, String mimeType, String filenameRadical, HttpServletResponse response) throws IOException {
			pclManager.openPclStream(response, filenameRadical, contenu);
		}
	}

	/**
	 * Choix du downloader et activation
	 * @param resultat résultat contenant un document à télécharger
	 * @param filenameRadical radical du nom de fichier à présenter dans la réponse HTTP
	 * @param response réponse HTTP à remplir avec le contenu du fichier
	 * @throws IOException en cas de procblème
	 */
	@Override
	public void download(EditiqueResultatDocument resultat, String filenameRadical, HttpServletResponse response) throws IOException {
		Downloader downloader = downloaders.get(resultat.getContentType());
		if (downloader == null) {
			downloader = defaultDownloader;
		}
		downloader.download(resultat.getDocument(), resultat.getContentType(), filenameRadical, response);
	}

	/**
	 * Choix du downloader et activation
	 * @param container résultat contenant un document à télécharger
	 * @param response réponse HTTP à remplir avec le contenu du fichier
	 * @throws IOException en cas de procblème
	 */
	@Override
	public void download(TypedDataContainer container, HttpServletResponse response) throws IOException {
		Downloader downloader = downloaders.get(container.getMimeType());
		if (downloader == null) {
			downloader = defaultDownloader;
		}
		if (container.getSize() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Cannot download data which length does not fit into an int (" + container.getSize() + ")");
		}

		try (InputStream is = container.getContent()) {
			downloader.download(is, (int) container.getSize(), container.getMimeType(), container.getFilenameRadical(), response);
		}
	}
}
