package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ExecuteJavascriptFunctionAction;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Anchor;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableHeader;
import org.springmodules.xt.ajax.component.TableRow;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamType;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;
import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class GestionBatchController extends AbstractEnhancedSimpleFormController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(GestionBatchController.class);

	/**
	 * Le BatchHelper
	 */
	private BatchScheduler batchScheduler;

	/**
	 * @return the batchScheduler
	 */
	public BatchScheduler getBatchScheduler() {
		return batchScheduler;
	}

	/**
	 * @param batchScheduler
	 *            the batchScheduler to set
	 */
	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		List<JobDefinition> jobs = batchScheduler.getSortedJobs();
		BatchList list = new BatchList(jobs, this.getMessageSourceAccessor());
		return list;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		return showForm(request, response, errors);
	}

	/**
	 * Cette méthode est appelée par une requête Ajax pour mettre-à-jour la liste des jobs actifs dans l'écran de gestion des batches.
	 */
	public AjaxResponse loadJobActif(AjaxActionEvent event) {
		return doLoadJobActif(event, false);
	}

	public AjaxResponse doLoadJobActif(AjaxActionEvent event, boolean readonly) {
		AjaxResponse response = new AjaxResponseImpl();

		TableHeader head = new TableHeader(new String[] {
				getMessageResource("label.batch.action"), getMessageResource("label.batch.nom"),
				getMessageResource("label.batch.progression"), getMessageResource("label.batch.statut"),
				getMessageResource("label.batch.lastStart"), getMessageResource("label.batch.duration")
		});

		Table table = new Table(head);
		boolean rowExist = false;
		Date now = DateUtils.addMinutes(DateHelper.getCurrentDate(), -10);
		int index = 0;

		final Collection<JobDefinition> jobs = batchScheduler.getJobs().values();
		for (JobDefinition job : jobs) {
			final Date lastStart = job.getLastStart();
			if (job.isRunning() || (lastStart != null && now.before(lastStart))) {
				final String rowClass = ((index++ % 2) == 1 ? "even" : "odd");
				addJob(table, new GestionJob(job, this.getMessageSourceAccessor()), rowClass, readonly);
				rowExist = true;
			}
		}

		final String elementId = event.getElementId();
		if (rowExist) {
			ReplaceContentAction replaceTableAction = new ReplaceContentAction(elementId, table);
			response.addAction(replaceTableAction);
		}
		else {
			ReplaceContentAction replaceAction = new ReplaceContentAction(elementId, new SimpleText(
					"Aucun job n'est en exécution actuellement."));
			response.addAction(replaceAction);
		}

		response.addAction(new ExecuteJavascriptFunctionAction("onRecieved", null));
		return response;
	}

	/**
	 * Ajoute la description complète (avec les paramètres) d'un job à la table spécifiée.
	 *
	 * @param readonly
	 *            si <code>faux</code> et que le job tourne toujours, affiche un bouton d'interruption; si <code>vrai</code> n'affiche aucun
	 *            bouton dans tous les cas.
	 */
	private void addJob(Table table, GestionJob job, String rowClass, boolean readonly) {

		// description du job
		TableRow row = new TableRow();
		row.addAttribute("class", rowClass);
		if (job.isRunning() && !readonly) {
			Anchor stopAction = new Anchor("javascript:stopJob('" + job.getName() + "');");
			stopAction.addAttribute("class", "stop iepngfix");
			row.addTableData(new TableData(stopAction));
		}
		else {
			row.addTableData(new TableData(new SimpleText("&nbsp;")));
		}

		row.addTableData(new TableData(new SimpleText(job.getDescription())));
		row.addTableData(new TableData(new SimpleText(job.getRunningMessage())));
		final Integer percent = job.getPercentProgression();
		final TableData status;
		if (percent == null) {
			status = new TableData(new SimpleText(job.getStatus()));
		}
		else {
			status = new TableData(new JobPercentIndicator(percent));
		}
		status.addAttribute("align", "left");
		row.addTableData(status);
		TableData lastStart = new TableData(new SimpleText(job.getLastStart()));
		lastStart.addAttribute("nowrap", "nowrap");
		row.addTableData(lastStart);
		TableData duration = new TableData(new SimpleText(job.getDuration()));
		duration.addAttribute("nowrap", "nowrap");
		row.addTableData(duration);
		table.addTableRow(row);

		// paramètres du job (sur une seconde ligne)
		final Map<String, Object> currentParams = job.getCurrentParametersDescription();
		if (currentParams != null) {
			row = new TableRow();
			row.addAttribute("class", rowClass);
			row.addTableData(new TableData(new SimpleText("&nbsp;")));
			TableData paramTable = new TableData(new JobParametersTable(currentParams));
			paramTable.addAttribute("colspan", "5");
			row.addTableData(paramTable);
			table.addTableRow(row);
		}
	}

	/**
	 * Table spécialisée pour l'affichage des paramètres de la dernière exécution d'un job
	 */
	private static class JobParametersTable extends Table {

		private static final long serialVersionUID = -8857731698878141582L;

		public JobParametersTable(Map<String, Object> parameters) {

			addAttribute("class", "jobparams");
			Assert.notNull(parameters);

			for (Map.Entry<String, Object> entry : parameters.entrySet()) {
				String description = entry.getKey();
				Object value = entry.getValue();
				TableRow row = new TableRow();
				row.addTableData(new TableData(new SimpleText(description)));
				row.addTableData(new TableData(new SimpleText("➭ " + value.toString())));
				addTableRow(row);
			}

		}
	}

	/**
	 * Table spécialisée pour l'affichage de la progression d'un job en pourcent
	 */
	private static class JobPercentIndicator implements Component {

		private static final long serialVersionUID = -8857731698878141582L;

		private final int percent;

		public JobPercentIndicator(int percent) {
			this.percent = percent;
		}

		public String render() {
			int width = 100;
			final int pixels = (width * percent) / 100;
			StringBuilder s = new StringBuilder();
			s.append("<div class=\"progress-bar\" style=\"width: ").append(width).append("px\">");
			s.append("<div class=\"progress-bar-fill\" style=\"width: ").append(pixels).append("px\"></div>");
			s.append("<div class=\"progress-bar-text\" style=\"width: ").append(width).append("px\">").append(percent).append("%</div></div>");
			return s.toString();
		}
	}

	public AjaxResponse stopJob(AjaxActionEvent event) {
		String jobName = event.getParameters().get("jobName");

		AjaxResponse response = new AjaxResponseImpl();

		try {
			if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}

			batchScheduler.stopJob(jobName);
		}
		catch (Exception e) {
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("message", e.getMessage());
			ExecuteJavascriptFunctionAction action = new ExecuteJavascriptFunctionAction("showAlert", options);
			response.addAction(action);
		}
		return response;
	}

	public AjaxResponse startJob(AjaxSubmitEvent event) {

		// Récupère les données de la requête
		final BatchList batchList = (BatchList) event.getCommandObject();
		final String jobName = event.getElementId();
		final JobDefinition job = batchScheduler.getJobs().get(jobName);

		// Converti les strings des paramètres en objets
		final HashMap<String, Object> params = new HashMap<String, Object>();
		final Map<String, Object> startParams = batchList.getStartParams();
		for (Entry<String, Object> param : startParams.entrySet()) {
			final JobParam jobparam = job.getParameterDefinition(param.getKey());
			if (jobparam != null) { // il arrive que IE6 foire un peu et reposte les paramètres d'un formulaire posté précédemment...
				final Object value = param.getValue();
				final Object typedValue;
				if (value instanceof String) {
					final String stringValue = (String) value;
					if (StringUtils.isEmpty(stringValue)) {
						typedValue = null;
					}
					else {
						final JobParamType type = jobparam.getType();
						typedValue = type.stringToValue(stringValue.trim());
					}
				}
				else if (value instanceof CommonsMultipartFile) {
					final CommonsMultipartFile file = (CommonsMultipartFile) value;
					typedValue = file.getBytes();
				}
				else {
					throw new IllegalArgumentException("Type de paramètre inconnu = [" + value + "]");
				}
				params.put(param.getKey(), typedValue);
			}
		}

		AjaxResponse response = new AjaxResponseImpl();

		// Démarre le batch
		try {
			if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}

			batchScheduler.startJob(jobName, params);
			response.addAction(new NullAction());
		}
		catch (Exception e) {
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("message", e.getMessage());
			ExecuteJavascriptFunctionAction action = new ExecuteJavascriptFunctionAction("showAlert", options);
			response = new AjaxResponseImpl();
			response.addAction(action);
		}

		return response;
	}

	private String getMessageResource(String key) {
		return this.getMessageSourceAccessor().getMessage(key);
	}
}
