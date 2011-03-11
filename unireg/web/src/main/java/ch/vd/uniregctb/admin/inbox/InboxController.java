package ch.vd.uniregctb.admin.inbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxHandler;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ExecuteJavascriptFunctionAction;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Anchor;
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
import ch.vd.uniregctb.extraction.ExtractionServiceMonitoring;
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

	private ExtractionServiceMonitoring extractionService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExtractionService(ExtractionServiceMonitoring extractionService) {
		this.extractionService = extractionService;
	}

	@Override
	public AjaxResponse handle(AjaxEvent event) {
		if ("loadJobsEnAttente".equals(event.getEventId())) {
			return loadJobsEnAttente((AjaxActionEvent) event);
		}
		else if ("loadInboxContent".equals(event.getEventId())) {
			return loadInboxContent((AjaxActionEvent) event);
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
		return ("loadJobsEnAttente".equals(id) || ("loadInboxContent".equals(id)));
	}

	/**
	 * Appelé pour rafraîchir la liste des travaux en attente ou en cours
	 * @param event requête Ajax
	 * @return le contenu de la DIV qui affiche les travaux en cours
	 */
	private AjaxResponse loadJobsEnAttente(AjaxActionEvent event) {

		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final AjaxResponse response = new AjaxResponseImpl();

		// on passe d'abord par un set pour éliminer les doublons (qui passeraient juste de la queue d'attente
		// aux extractions en cours entre les deux appels aux addAll)
		final Set<ExtractionJob> jobSet = new HashSet<ExtractionJob>();
		jobSet.addAll(extractionService.getQueueContent(visa));
		jobSet.addAll(extractionService.getExtractionsEnCours(visa));

		// puis on met dans une liste pour trier par date de demande et ainsi avoir les éléments dans l'ordre de leur exécution
		final List<ExtractionJob> jobs = new ArrayList<ExtractionJob>(jobSet);
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
					getMessageResource("label.inbox.date.demande"), getMessageResource("label.inbox.description"),
					getMessageResource("label.inbox.etat"), getMessageResource("label.inbox.temps.execution"),
					getMessageResource("label.inbox.progression")
			});

			final Table table = new Table(header);

			int index = 0;
			for (ExtractionJob job : jobs) {
				final TableRow row = createRowForJobEnAttente(job);
				if (row != null) {
					final String rowClass = ((index++ % 2) == 1 ? "even" : "odd");
					row.addAttribute("class", rowClass);
					table.addTableRow(row);
				}
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
					getMessageResource("label.inbox.doc")
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

	private TableRow createRowForJobEnAttente(ExtractionJob job) {
		final TableRow row = new TableRow();
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
		return row;
	}
}
