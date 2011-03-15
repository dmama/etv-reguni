package ch.vd.uniregctb.admin.inbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxHandler;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ExecuteJavascriptFunctionAction;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Anchor;
import org.springmodules.xt.ajax.component.Container;
import org.springmodules.xt.ajax.component.Image;
import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableHeader;
import org.springmodules.xt.ajax.component.TableRow;
import org.springmodules.xt.ajax.support.UnsupportedEventException;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.admin.JobPercentIndicator;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.extraction.ExtractionService;
import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxElement;
import ch.vd.uniregctb.inbox.InboxService;
import ch.vd.uniregctb.taglibs.JspTagDocumentIcon;
import ch.vd.uniregctb.taglibs.JspTagDuration;
import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Contrôleur de la visualisation de l'inbox
 */
public class InboxController extends ParameterizableViewController implements AjaxHandler {

	private static final String NBSP = "&nbsp;";

	private InboxService inboxService;

	private ExtractionService extractionService;

	private static interface AjaxActionHandler {
		AjaxResponse handle(AjaxActionEvent event);
	}

	private final Map<String, AjaxActionHandler> ajaxHandlers = buildAjaxHandlerMap();

	private Map<String, AjaxActionHandler> buildAjaxHandlerMap() {
		final Map<String, AjaxActionHandler> ajaxHandlers = new HashMap<String, AjaxActionHandler>();
		ajaxHandlers.put("updateInboxUnreadSize", new AjaxActionHandler() {
			@Override
			public AjaxResponse handle(AjaxActionEvent event) {
				return updateInboxUnreadSize(event);
			}
		});
		ajaxHandlers.put("loadJobsEnAttente", new AjaxActionHandler() {
			@Override
			public AjaxResponse handle(AjaxActionEvent event) {
				return loadJobsEnAttente(event);
			}
		});
		ajaxHandlers.put("loadInboxContent", new AjaxActionHandler() {
			@Override
			public AjaxResponse handle(AjaxActionEvent event) {
				return loadInboxContent(event);
			}
		});
		ajaxHandlers.put("stopJobEnAttente", new AjaxActionHandler() {
			@Override
			public AjaxResponse handle(AjaxActionEvent event) {
				return stopJobEnAttente(event);
			}
		});
		ajaxHandlers.put("removeInboxContent", new AjaxActionHandler() {
			@Override
			public AjaxResponse handle(AjaxActionEvent event) {
				return removeInboxContent(event);
			}
		});
		return ajaxHandlers;
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
	public AjaxResponse handle(AjaxEvent event) {
		final AjaxActionHandler handler = ajaxHandlers.get(event.getEventId());
		if (handler != null && event instanceof AjaxActionEvent) {
			return handler.handle((AjaxActionEvent) event);
		}

		logger.error("You need to call the supports() method first!");
		throw new UnsupportedEventException("You need to call the supports() method first!");
	}

	@Override
	public boolean supports(AjaxEvent event) {
		if (!(event instanceof AjaxActionEvent)) {
			return false;
		}
		final String id = event.getEventId();
		return ajaxHandlers.containsKey(id);
	}

	private AjaxResponse updateInboxUnreadSize(AjaxActionEvent event) {

		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final AjaxResponse response = new AjaxResponseImpl();

		final List<InboxElement> inboxContent = inboxService.getInboxContent(visa);
		int unread = 0;
		for (InboxElement elt : inboxContent) {
			if (!elt.isRead()) {
				++ unread;
			}
		}

		final String baseMsg = getMessageResource("title.inbox");
		if (unread == 0) {
			response.addAction(new ReplaceContentAction(event.getElementId(), new SimpleText(baseMsg)));
		}
		else {
			final Container span = new Container(Container.Type.SPAN);
			span.addAttribute("style", "font-weight: bold;");
			span.addComponent(new SimpleText(String.format("%s%s(%d)", baseMsg, NBSP, unread)));
			response.addAction(new ReplaceContentAction(event.getElementId(), span));
		}

		response.addAction(new ExecuteJavascriptFunctionAction("onReceivedInboxSize", null));
		return response;
	}

	private Map<UUID, ExtractionJob> getKnownJobsEnAttenteForVisa(String visa) {
		final Map<UUID, ExtractionJob> map = new HashMap<UUID, ExtractionJob>();
		fillMap(map, extractionService.getQueueContent(visa));
		fillMap(map, extractionService.getExtractionsEnCours(visa));
		return map;
	}

	private static void fillMap(Map<UUID, ExtractionJob> map, List<ExtractionJob> src) {
		if (src != null && src.size() > 0) {
			for (ExtractionJob job : src) {
				map.put(job.getUuid(), job);
			}
		}
	}

	/**
	 * Appelé pour rafraîchir la liste des travaux en attente ou en cours
	 * @param event requête Ajax
	 * @return le contenu de la DIV qui affiche les travaux en cours
	 */
	private AjaxResponse loadJobsEnAttente(AjaxActionEvent event) {

		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final AjaxResponse response = new AjaxResponseImpl();

		// on met dans une liste pour trier par date de demande et ainsi avoir les éléments dans l'ordre de leur exécution
		final List<ExtractionJob> jobs = new ArrayList<ExtractionJob>(getKnownJobsEnAttenteForVisa(visa).values());
		Collections.sort(jobs, new Comparator<ExtractionJob>() {
			@Override
			public int compare(ExtractionJob o1, ExtractionJob o2) {
				return o1.getCreationDate().compareTo(o2.getCreationDate());
			}
		});

		final String elementId = event.getElementId();
		if (jobs.size() == 0) {
			final ReplaceContentAction action = new ReplaceContentAction(elementId, new SimpleText(getMessageResource("label.inbox.empty.attente")));
			response.addAction(action);
		}
		else {
			final TableHeader header = new TableHeader(new String[] {
					getMessageResource("label.action"),
					getMessageResource("label.inbox.date.demande"), getMessageResource("label.inbox.description"),
					getMessageResource("label.inbox.etat"), getMessageResource("label.inbox.temps.execution"),
					getMessageResource("label.inbox.progression")
			});

			final Table table = new Table(header);

			int index = 0;
			for (ExtractionJob job : jobs) {
				final TableRow row = new TableRow();
				if (job.wasInterrupted()) {
					row.addTableData(new TableData(new SimpleText(NBSP)));
				}
				else {
					final Anchor stopLink = new Anchor(String.format("javascript:stopJobEnAttente('%s');", job.getUuid()));
					stopLink.addAttribute("class", "stop iepngfix");
					row.addTableData(new TableData(stopLink));
				}
				row.addTableData(new TableData(new SimpleText(DateHelper.dateTimeToDisplayString(job.getCreationDate()))));
				row.addTableData(new TableData(new SimpleText(job.getDescription())));
				row.addTableData(new TableData(new SimpleText(job.getRunningMessage())));
				if (job.isRunning()) {
					final Long jobDuration = job.getDuration();
					row.addTableData(new TableData(new SimpleText(jobDuration != null ? TimeHelper.formatDureeShort(jobDuration) : NBSP)));

					final Integer progression = job.getPercentProgression();
					if (progression != null) {
						row.addTableData(new TableData(new JobPercentIndicator(progression)));
					}
					else {
						row.addTableData(new TableData(new SimpleText(getMessageResource("label.inbox.en.cours"))));
					}
				}
				else {
					final TableData data = new TableData(new SimpleText(NBSP));
					data.addAttribute("colspan", "2");      // temps d'exécution et progression
					row.addTableData(data);
				}

				final String rowClass = ((index++ % 2) == 1 ? "even" : "odd");
				row.addAttribute("class", rowClass);
				table.addTableRow(row);
			}

			final ReplaceContentAction action = new ReplaceContentAction(elementId, table);
			response.addAction(action);
		}

		response.addAction(new ExecuteJavascriptFunctionAction("onReceivedJobsEnAttente", null));
		return response;
	}

	/**
	 * Appelé pour rafraîchir la liste du contenu de l'inbox
	 * @param event requête Ajax
	 * @return le contenu de la DIV qui affiche les travaux en cours
	 */
	private AjaxResponse loadInboxContent(AjaxActionEvent event) {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final AjaxResponse response = new AjaxResponseImpl();

		final String elementId = event.getElementId();
		final List<InboxElement> content = inboxService.getInboxContent(visa);
		if (content == null || content.size() == 0) {
			final ReplaceContentAction action = new ReplaceContentAction(elementId, new SimpleText(getMessageResource("label.inbox.empty.inbox")));
			response.addAction(action);
		}
		else {
			final TableHeader header = new TableHeader(new String[] {
					getMessageResource("label.inbox.date.reception"), getMessageResource("label.inbox.nom"),
					getMessageResource("label.inbox.description"), getMessageResource("label.inbox.expiration"),
					getMessageResource("label.inbox.doc"), getMessageResource("label.action")
			});

			final String contextPath = event.getHttpRequest().getContextPath();
			final Table table = new Table(header);
			table.addTableAttribute("class", "inbox");

			int index = 0;
			for (final InboxElement elt : content) {
				final TableRow row = new TableRow();
				row.addAttribute("class", buildRowClassForInboxContent(elt, ++ index));
				row.addTableData(new TableData(new SimpleText(DateHelper.dateTimeToDisplayString(elt.getIncomingDate()))));
				row.addTableData(new TableData(new SimpleText(elt.getName())));
				row.addTableData(new TableData(new SimpleText(elt.getDescription())));
				row.addTableData(new TableData(new SimpleText(JspTagDuration.buildDisplayText(elt.getTimeToExpiration(), getMessageResource("label.inbox.sans.expiration"), getMessageResource("label.inbox.expire"), true))));
				final InboxAttachment attachment = elt.getAttachment();
				if (attachment != null) {
					final Image img = new Image(JspTagDocumentIcon.getImageUri(contextPath, attachment.getMimeType()), attachment.getFilename());
					final Anchor link = new Anchor(String.format("%s/admin/inbox-content.do?action=dl&amp;id=%s", contextPath, elt.getUuid()), img);
					final TableData td = new TableData(link);
					td.addAttribute("style", "text-align: center;");
					row.addTableData(td);
				}
				else {
					row.addTableData(new TableData(new SimpleText(NBSP)));
				}

				final Anchor removeLink = new Anchor(String.format("javascript:removeInboxContent('%s');", elt.getUuid()), new SimpleText(NBSP));
				removeLink.addAttribute("class", "delete iepngfix");
				row.addTableData(new TableData(removeLink));

				table.addTableRow(row);
			}

			final ReplaceContentAction action = new ReplaceContentAction(elementId, table);
			response.addAction(action);
		}

		response.addAction(new ExecuteJavascriptFunctionAction("onReceivedInboxContent", null));
		return response;
	}

	private String getMessageResource(String key) {
		return this.getMessageSourceAccessor().getMessage(key);
	}

	private String buildRowClassForInboxContent(InboxElement elt, int rowIndex) {
		return String.format("%s %s", rowIndex % 2 == 1 ? "odd" : "even", elt.isRead() ? "read" : "unread");
	}

	/**
	 * Appelé pour interrompre un job en cours (ou annuler une demande pas encore prise en compte)
	 * @param event requête Ajax
	 * @return les actions à exécuter une fois l'appel terminé
	 */
	private AjaxResponse stopJobEnAttente(AjaxActionEvent event) {

		final AjaxResponse response = new AjaxResponseImpl();

		final Map<String, String> params = event.getParameters();
		if (params != null) {
			final UUID uuid = UUID.fromString(params.get("uuid"));
			final String visa = AuthenticationHelper.getCurrentPrincipal();
			final Map<UUID, ExtractionJob> jobs = getKnownJobsEnAttenteForVisa(visa);
			final ExtractionJob job = jobs.get(uuid);
			if (job != null) {
				extractionService.cancelJob(job);
				response.addAction(new ExecuteJavascriptFunctionAction("refreshInboxPage", null));
			}
		}

		return response;
	}

	/**
	 * Appelé pour effacer un élément de la boîte de réception
	 * @param event requête Ajax
	 * @return les actions à exécuter une fois l'appel terminé
	 */
	private AjaxResponse removeInboxContent(AjaxActionEvent event) {

		final AjaxResponse response = new AjaxResponseImpl();

		final Map<String, String> params = event.getParameters();
		if (params != null) {
			final UUID uuid = UUID.fromString(params.get("uuid"));
			final String visa = AuthenticationHelper.getCurrentPrincipal();
			inboxService.removeDocument(uuid, visa);
			response.addAction(new ExecuteJavascriptFunctionAction("refreshInboxPage", null));
		}

		return response;
	}
}
