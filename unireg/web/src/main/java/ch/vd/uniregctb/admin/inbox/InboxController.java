package ch.vd.uniregctb.admin.inbox;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.extraction.ExtractionService;
import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxElement;
import ch.vd.uniregctb.inbox.InboxService;
import ch.vd.uniregctb.print.PrintPCLManager;

/**
 * Contrôleur de la visualisation de l'inbox
 */
@Controller
@RequestMapping(value = "/admin/inbox")
public class InboxController implements InitializingBean {

	private static final String ID = "id";

	private InboxService inboxService;

	private ExtractionService extractionService;

	private PrintPCLManager pclManager;

	private Map<String, ContentDeliveryStrategy> contentDeliveryStrategies;
	private final ContentDeliveryStrategy defaultDeliveryStrategy = new PassThroughContentDeliveryStrategy();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPclManager(PrintPCLManager pclManager) {
		this.pclManager = pclManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExtractionService(ExtractionService extractionService) {
		this.extractionService = extractionService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// initialisation de la stratégie associée aux flux PCL
		final Map<String, ContentDeliveryStrategy> map = new HashMap<String, ContentDeliveryStrategy>();
		final PclContentDeliveryStrategy pclContentDeliveryStrategy = new PclContentDeliveryStrategy(pclManager);
		map.put(MimeTypeHelper.MIME_PCL, pclContentDeliveryStrategy);
		map.put(MimeTypeHelper.MIME_XPCL, pclContentDeliveryStrategy);
		map.put(MimeTypeHelper.MIME_HPPCL, pclContentDeliveryStrategy);
		contentDeliveryStrategies = map;
	}

	private Map<UUID, ExtractionJob> getKnownJobsEnAttenteForVisa(String visa) {
		final Map<UUID, ExtractionJob> map = new HashMap<UUID, ExtractionJob>();
		fillMap(map, extractionService.getQueueContent(visa));
		fillMap(map, extractionService.getExtractionsEnCours(visa));
		return map;
	}

	private static void fillMap(Map<UUID, ExtractionJob> map, List<ExtractionJob> src) {
		if (src != null && !src.isEmpty()) {
			for (ExtractionJob job : src) {
				map.put(job.getUuid(), job);
			}
		}
	}

	private ContentDeliveryStrategy getStrategy(String mimeType) {
		ContentDeliveryStrategy strategy = contentDeliveryStrategies.get(mimeType);
		if (strategy == null) {
			strategy = defaultDeliveryStrategy;
		}
		return strategy;
	}

	@RequestMapping(value = "/download.do", method = RequestMethod.GET)
	public String downloadAttachment(HttpServletResponse response, @RequestParam(value = ID, required = true) UUID uuid) throws Exception {

		final InboxElement elt = inboxService.getInboxElement(uuid);
		if (elt != null) {
			final InboxAttachment attachment = elt.getAttachment();
			final InputStream in = attachment.getContent();
			try {
				final String mimeType = attachment.getMimeType();

				final ServletOutputStream out = response.getOutputStream();
				response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

				final ContentDeliveryStrategy strategy = getStrategy(mimeType);
				final String actualMimeType = strategy.getMimeType(mimeType);
				response.setContentType(actualMimeType);
				final String filename = String.format("%s%s", attachment.getFilenameRadical(), MimeTypeHelper.getFileExtensionForType(actualMimeType));
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
			return show();
		}
	}

	@RequestMapping(value = "/show.do", method = RequestMethod.GET)
	public String show() {
		return "/admin/inbox";
	}

	@RequestMapping(value = "/jobs.do", method = RequestMethod.GET)
	public String showJobs(Model model) {
		// on met dans une liste pour trier par date de demande et ainsi avoir les éléments dans l'ordre de leur exécution
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final List<ExtractionJob> jobs = new ArrayList<ExtractionJob>(getKnownJobsEnAttenteForVisa(visa).values());
		Collections.sort(jobs, new Comparator<ExtractionJob>() {
			@Override
			public int compare(ExtractionJob o1, ExtractionJob o2) {
				return o1.getCreationDate().compareTo(o2.getCreationDate());
			}
		});
		model.addAttribute("content", jobs);
		return "/admin/inbox-jobs";
	}

	@RequestMapping(value = "/stopJob.do", method = RequestMethod.POST)
	public String stopJob(@RequestParam(value = ID) UUID id) {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Map<UUID, ExtractionJob> jobs = getKnownJobsEnAttenteForVisa(visa);
		final ExtractionJob job = jobs.get(id);
		if (job != null) {
			extractionService.cancelJob(job);
		}
		return "redirect:/admin/inbox/show.do";
	}

	@RequestMapping(value = "/content.do", method = RequestMethod.GET)
	public String showInboxContent(Model model) {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final List<InboxElement> elts = inboxService.getInboxContent(visa);
		model.addAttribute("content", elts);
		return "/admin/inbox-content";
	}

	@RequestMapping(value = "/removeElement.do", method = RequestMethod.POST)
	public String removeInboxElement(@RequestParam(value = ID) UUID id) {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		inboxService.removeDocument(id, visa);
		return "redirect:/admin/inbox/show.do";
	}

	@ResponseBody
	@RequestMapping(value = "/unreadSize.do", method = RequestMethod.GET)
	public int unreadSize() {
		final String visa = AuthenticationHelper.getCurrentPrincipal();

		final List<InboxElement> inboxContent = inboxService.getInboxContent(visa);
		int unread = 0;
		for (InboxElement elt : inboxContent) {
			if (!elt.isRead()) {
				++ unread;
			}
		}

		return unread;
	}
}
