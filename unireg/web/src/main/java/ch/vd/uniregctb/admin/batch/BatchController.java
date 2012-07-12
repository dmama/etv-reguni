package ch.vd.uniregctb.admin.batch;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamType;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

@Controller
@RequestMapping(value = "/admin/batch/")
public class BatchController {

	private BatchScheduler batchScheduler;

	@SuppressWarnings("UnusedDeclaration")
	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	@RequestMapping(value = "/running.do", method = RequestMethod.GET)
	@ResponseBody
	public List<BatchView> running() {

		final List<BatchView> list = new ArrayList<BatchView>();

		final Date limit = DateUtils.addMinutes(DateHelper.getCurrentDate(), -10);

		final Collection<JobDefinition> jobs = batchScheduler.getJobs().values();
		for (JobDefinition job : jobs) {
			final Date lastStart = job.getLastStart();
			if (job.isRunning() || (lastStart != null && limit.before(lastStart))) {
				list.add(new BatchView(job));
			}
		}

		return list;
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	@ResponseBody
	public String start(@RequestParam(value = "name", required = true) String jobName, @Valid BatchStartParams startParams) {

		try {
			if (!SecurityProvider.isAnyGranted(Role.ADMIN, Role.TESTER)) {
				throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}

			// Récupère les données de la requête
			final JobDefinition job = batchScheduler.getJobs().get(jobName);
			if (job == null) {
				throw new IllegalArgumentException("Erreur: le batch [" + jobName + "] n'existe pas !");
			}

			if (!job.isWebStartable()) {
				throw new AccessDeniedException("Ce batch ne peut être lancé depuis l'IHM de l'application");
			}

			// Converti les strings des paramètres en objets
			final HashMap<String, Object> params = parseParams(startParams, job);

			// Démarre le batch
			batchScheduler.startJob(jobName, params);
			return null;
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}

	private HashMap<String, Object> parseParams(BatchStartParams startParams, JobDefinition job) {

		final HashMap<String, Object> params = new HashMap<String, Object>();

		final Map<String, Object> sp = startParams.getStartParams();
		if (sp != null) {
			for (Map.Entry<String, Object> param : sp.entrySet()) {
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
						throw new IllegalArgumentException("Type de paramètre inconnu = [" + value + ']');
					}
					params.put(param.getKey(), typedValue);
				}
			}
		}

		return params;
	}

	/**
	 * Stoppe le batch spécifié
	 *
	 * @param name le nom du batch à stopper
	 * @return <b>null</b> si la demande d'arrêt a bien été enregistrée; ou une message d'erreur si un problème est survenu.
	 */
	@RequestMapping(value = "/stop.do", method = RequestMethod.POST)
	@ResponseBody
	public String stop(@RequestParam(value = "name", required = true) String name) {

		try {
			if (!SecurityProvider.isAnyGranted(Role.ADMIN, Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}

			batchScheduler.stopJob(name);
			return null;
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}
}
