package ch.vd.uniregctb.admin.inbox;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.StreamUtils;
import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxElement;
import ch.vd.uniregctb.inbox.InboxService;
import ch.vd.uniregctb.print.PrintPCLManager;

/**
 * Contrôleur qui renvoie le contenu d'un attachement d'élément de la boîte de réception
 */
public class InboxContentController extends AbstractController implements InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(InboxContentController.class);

	private static final String ACTION = "action";
	private static final String ID = "id";

	private static final String ACTION_DOWNLOAD = "dl";

	private Map<String, ContentDeliveryStrategy> contentDeliveryStrategies;
	private ContentDeliveryStrategy defaultDeliveryStrategy;

	private InboxService inboxService;

	private PrintPCLManager pclManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPclManager(PrintPCLManager pclManager) {
		this.pclManager = pclManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final Map<String, ContentDeliveryStrategy> map = new HashMap<String, ContentDeliveryStrategy>();
		final PclContentDeliveryStrategy pclContentDeliveryStrategy = new PclContentDeliveryStrategy(pclManager);
		map.put("application/pcl", pclContentDeliveryStrategy);
		map.put("application/x-pcl", pclContentDeliveryStrategy);
		map.put("application/vnd.hp-pcl", pclContentDeliveryStrategy);
		contentDeliveryStrategies = map;

		defaultDeliveryStrategy = new PassThroughContentDeliveryStrategy();
	}

	/**
	 * Différentes stratégies pour reconstituer le flux à télécharger
	 */
	private static interface ContentDeliveryStrategy {

		/**
		 * @param mimeType le type MIME original
		 * @return le type MIME définitif du contenu (en cas d'encapsulation du flux initial dans autre chose, ce type n'est pas forcément celui du flux initial)
		 */
		String getMimeType(String mimeType);

		/**
		 * @return <code>true</code> si la "content-disposition" doit être "attachment", <code>false</code> s'il doit être "inline"
		 */
		boolean isAttachment();

		/**
		 * Copie du flux initial dans le flux de la réponse HTTP
		 * @param in flux initial
		 * @param out flux de la réponse HTTP
		 * @throws IOException en cas de problème à la recopie
		 */
		void copyToOutputStream(InputStream in, OutputStream out) throws IOException;
	}

	/**
	 * Stratégie par défaut, sans aucune encapsulation
	 */
	private static class PassThroughContentDeliveryStrategy implements ContentDeliveryStrategy {

		@Override
		public String getMimeType(String mimeType) {
			return mimeType;
		}

		@Override
		public boolean isAttachment() {
			return true;
		}

		@Override
		public void copyToOutputStream(InputStream in, OutputStream out) throws IOException {
			StreamUtils.copy(in, out);
		}
	}

	/**
	 * Stratégie utilisée par les flux PCL (avec ou sans encapsulation localapp)
	 */
	private static class PclContentDeliveryStrategy implements ContentDeliveryStrategy {

		private final PrintPCLManager pclManager;

		private PclContentDeliveryStrategy(PrintPCLManager pclManager) {
			this.pclManager = pclManager;
		}

		@Override
		public String getMimeType(String mimeType) {
			return pclManager.getActualMimeType();
		}

		@Override
		public boolean isAttachment() {
			return pclManager.isAttachmentContent();
		}

		@Override
		public void copyToOutputStream(InputStream in, OutputStream out) throws IOException {
			pclManager.copyToOutputStream(in, out);
		}
	}

	private ContentDeliveryStrategy getStrategy(String mimeType) {
		ContentDeliveryStrategy strategy = contentDeliveryStrategies.get(mimeType);
		if (strategy == null) {
			strategy = defaultDeliveryStrategy;
		}
		return strategy;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final String action = request.getParameter(ACTION);
		boolean actionConnue = false;

		// Téléchargement de l'attachement
		if (ACTION_DOWNLOAD.equals(action)) {

			actionConnue = true;
			final String idElement = request.getParameter(ID);

			// récupération de document contenu dans l'inbox
			final UUID uuid = UUID.fromString(idElement);

			final InboxElement elt = inboxService.getInboxElement(uuid);
			if (elt != null) {
				final InboxAttachment attachment = elt.getAttachment();
				final InputStream in = attachment.getContent();
				try {
					final String mimeType = attachment.getMimeType();
					final String filename = attachment.getFilename();

					final ServletOutputStream out = response.getOutputStream();
					response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

					final ContentDeliveryStrategy strategy = getStrategy(mimeType);
					response.setContentType(strategy.getMimeType(mimeType));
					response.setHeader("Content-disposition", String.format("%s; filename=\"%s\"", strategy.isAttachment() ? "attachment" : "inline", filename));
					response.setHeader("Pragma", "public");
					response.setHeader("cache-control", "no-cache");
					response.setHeader("Cache-control", "must-revalidate");

					try {
						strategy.copyToOutputStream(in, out);
						out.flush();
					}
					finally {
						out.close();
					}
				}
				finally {
					in.close();
				}

				elt.setRead(true);
				return null;
			}
			else {
				// l'élément a expiré et a été nettoyé...
			}
		}

		if (!actionConnue) {
			LOGGER.error(String.format("Action '%s' inconnue!", action));
		}
		return new ModelAndView(new RedirectView("/admin/inbox.do"));
	}
}
