package ch.vd.uniregctb.admin.batch;

import javax.validation.Valid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.admin.BatchList;
import ch.vd.uniregctb.common.EncodingFixHelper;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamType;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;

@Controller
@RequestMapping(value = "/admin/")
public class BatchController {

	private BatchScheduler batchScheduler;
	private SecurityProviderInterface securityProvider;
	private MessageSource messageSource;

	@SuppressWarnings("UnusedDeclaration")
	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@RequestMapping(value = "/batch.do", method = RequestMethod.GET)
	public String list(Model model) {

		final List<JobDefinition> jobs = batchScheduler.getSortedJobs();
		model.addAttribute("command", new BatchList(jobs, new MessageSourceAccessor(messageSource)));

		return "admin/batch";
	}

	@RequestMapping(value = "/batch/running.do", method = RequestMethod.GET)
	@ResponseBody
	public List<BatchView> running() {

		final List<BatchView> list = new ArrayList<>();

		final Date limit = DateUtils.addMinutes(DateHelper.getCurrentDate(), -10);

		final Collection<JobDefinition> jobs = batchScheduler.getJobs().values();
		final Long offsetJVM = new Long(DateConstants.TIME_OFFSET);
		for (JobDefinition job : jobs) {
			final Date lastEnd = job.getLastEnd();
			if (job.isRunning() || (lastEnd != null && limit.before(lastEnd))) {
				list.add(new BatchView(job, offsetJVM));
			}
		}

		return list;
	}

	@RequestMapping(value = "/batch/start.do", method = RequestMethod.POST)
	@ResponseBody
	public String start(@RequestParam(value = "name", required = true) String jobName, @Valid BatchStartParams startParams) {

		try {
			if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
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
			final Map<String, Object> params = parseParams(startParams, job);

			// Démarre le batch
			batchScheduler.startJob(jobName, params);
			return null;
		}
		catch (Exception e) {
			return EncodingFixHelper.breakToIso(e.getMessage());
		}
	}

	private Map<String, Object> parseParams(BatchStartParams startParams, JobDefinition job) {

		final Map<String, Object> params = new HashMap<>();

		final Map<String, Object> sp = startParams.getStartParams();
		if (sp != null) {
			for (Map.Entry<String, Object> param : sp.entrySet()) {
				final JobParam jobparam = job.getParameterDefinition(param.getKey());
				if (jobparam != null) { // il arrive que IE foire un peu et reposte les paramètres d'un formulaire posté précédemment...
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
	@RequestMapping(value = "/batch/stop.do", method = RequestMethod.POST)
	@ResponseBody
	public String stop(@RequestParam(value = "name", required = true) String name) {

		try {
			if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}

			batchScheduler.stopJob(name, Duration.ofSeconds(30));
			return null;
		}
		catch (Exception e) {
			return EncodingFixHelper.breakToIso(e.getMessage());
		}
	}
}
