package ch.vd.uniregctb.admin;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.context.support.MessageSourceAccessor;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;

/**
 * Classe d'affichage pour la gestion des jobs quartz
 *
 * @author xcifde
 *
 */
public class GestionJob {

	private JobDefinition job = null;
	private MessageSourceAccessor messageSource;

	public GestionJob(JobDefinition def, MessageSourceAccessor messageSource) {
		this(def);
		this.messageSource = messageSource;
	}

	public GestionJob(JobDefinition def) {
		job = def;
	}

	public JobDefinition getJobDefinition() {
		return job;
	}

	public boolean isRunning() {
		return job.isRunning();
	}

	public String getName() {
		return job.getName();
	}

	public String getCategorie() {
		return job.getCategorie();
	}

	public String getDescription() {
		return job.getDescription();
	}

	/**
	 * @return la description et la valeur des paramètres de l'exécution courante du job, ou <b>null</b> si le job n'est pas en cours d'exécution ou
	 *         aucun paramètre n'a été spécifié.
	 */
	public Map<String, Object> getCurrentParametersDescription() {
		return job.getCurrentParametersDescription();
	}

	public String getRunningMessage() {
		String message = job.getRunningMessage();
		if (message == null)
			return "";
		return message;
	}

	public Integer getPercentProgression() {
		return job.getPercentProgression();
	}

	public String getStatus() {
		String status =  job.getStatut().toString();
		if ( this.messageSource != null) {
			status = messageSource.getMessage("option.batch.statut." + status);
		}
		return status;
	}

	public String getLastStart() {

		String str;
		Date date = job.getLastStart();

		if (date != null) {
			// Prends la date a 00:00:00
			Date today = DateHelper.dateWithoutTime(DateHelper.getCurrentDate());
			// Si on est before c'est qu'on est la jour précédent
			if (DateHelper.isBefore(date, today)) {
				// Donc on fomatte avec la date ET l'heure
				str = DateHelper.dateTimeToDisplayString(date);
			}
			else {
				// Sinon on formatte avec l'heure seule
				str = DateHelper.timeToString(date);
			}
		}
		else {
			str = "";
		}
		return str;
	}

	public String getDuration() {

		String str = "";

		final Date startDate = job.getLastStart();
		if (startDate != null) {
			Date endDate = job.getLastEnd();
			// Si le job est pas terminé, on prends l'heure coourante
			if (endDate == null) {
				endDate = DateHelper.getCurrentDate();
			}

			// On est tjrs en traiin de tourner
			final Calendar calStart = Calendar.getInstance();
			calStart.setTime(startDate);
			final Calendar calToday = Calendar.getInstance();
			calToday.setTime(endDate);

			final long d = calToday.getTimeInMillis() - calStart.getTimeInMillis();
			str = TimeHelper.formatDureeShort(d);
		}
		return str;
	}

	/**
	 * @return le document de rapport du dernier run, ou <b>null</b> si aucun rapport n'existe ou le job n'a pas été lancé
	 */
	public Document getLastRunReport() {
		return job.getLastRunReport();
	}

	public List<JobParam> getParameterDefintions() {
		return job.getParamDefinition();
	}


}
