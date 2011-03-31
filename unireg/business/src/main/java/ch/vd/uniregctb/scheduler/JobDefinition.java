package ch.vd.uniregctb.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Classe regroupant les informations d'affichage du job quartz
 *
 */
public abstract class JobDefinition implements InitializingBean, Comparable<JobDefinition> {

	private final Logger LOGGER = Logger.getLogger(JobDefinition.class);

	public static final String DATE_TRAITEMENT = "DATE_TRAITEMENT"; // pour le testing uniquement

	public final static String KEY_JOB = "job";
	public final static String KEY_AUTH = "authentication";
	public final static String KEY_PARAMS = "params";

	// Params dynamic
	private String runningMessage;
	private Integer percentProgression;
	private JobStatut statut = JobStatut.JOB_OK;
	private Date lastStart;
	private Date lastEnd;
	private Document lastRunReport;
	private JobStatusManager statusManager = null;

	// Params static
	final private String name;
	final private String categorie;
	private JobSynchronousMode synchronousMode;
	final private int sortOrder;
	final private String description;
	final private Map<String, JobParam> paramDefinition = new LinkedHashMap<String, JobParam>();        // java.util.LinkedHashMap pour conserver l'ordre d'insertion des paramètres
	final private Map<String, Object> defaultParamWebValues = new HashMap<String, Object>();

	private boolean logDisabled = false;

	protected BatchScheduler batchScheduler;

	private Map<String, Object> currentParameters = null;

	public enum JobSynchronousMode {
		SYNCHRONOUS, ASYNCHRONOUS
	}

	public enum JobStatut {
		/**
		 * JOB_OK est l'état du test soit avant qu'il ait tourné, soit si le retour est OK
		 */
		JOB_OK,
		/**
		 * JOB_RUNNING veut dire que le job est en train de tourner en ce moment
		 */
		JOB_RUNNING,
		/**
		 * JOB_EXCEPTION est l'état du test s'il s'est terminé avec une erreur (Exception)
		 */
		JOB_EXCEPTION,
		/**
		 * Le job a reçu une demande d'interruption, et il est entrain d'interrompre son traitement.
		 */
		JOB_INTERRUPTING,
		/**
		 * JOB_INTERRUPTED est l'état du job terminé par l'utilisateur
		 */
		JOB_INTERRUPTED
	}

	public JobDefinition(String name, String categorie, int sortOrder, String description) {
		this.name = name;
		this.categorie = categorie;
		this.sortOrder = sortOrder;
		this.description = description;
	}

	public void afterPropertiesSet() throws Exception {
		if (isVisible()) {
			batchScheduler.register(this);
		}
	}

	protected final void refreshParameterDefinitions(List<JobParam> paramsDef) {
		paramDefinition.clear();
		defaultParamWebValues.clear();
		if (paramsDef != null) {
			for (JobParam paramDef : paramsDef) {
				addParameterDefinition(paramDef, null);
			}
		}
	}

	protected final void addParameterDefinition(JobParam param, Object defaultWebValue) {
		final JobParam oldParam = paramDefinition.put(param.getName(), param);
		if (oldParam != null) {
			throw new IllegalArgumentException(String.format("Paramètre '%s' défini deux fois", param.getName()));
		}
		defaultParamWebValues.put(param.getName(), defaultWebValue);
	}

	protected void doInitialize() {
	}

	protected final void initialize() {
		runningMessage = "Initialisation du job...";
		doInitialize();
	}

	/**
	 * Peut-être surchargé par les sous-classes
	 *
	 * @throws Exception
	 */
	protected void doTerminate() throws Exception {
	}

	protected final void terminate() throws Exception {
		doTerminate();
		currentParameters = null;
		lastEnd = DateHelper.getCurrentDate();
	}

	protected void execute(Map<String, Object> params) throws Exception {

		lastStart = DateHelper.getCurrentDate();
		lastEnd = null;

		runningMessage = "";
		percentProgression = null;
		currentParameters = params;

		// le batch tournera avec le nom d'utilisateur égal au nom du batch
		if (!logDisabled) {
			Audit.info(String.format("Démarrage du job %s", name));
		}
		AuthenticationHelper.pushPrincipal(name);
		try {
			doExecute(params);
		}
		finally {
			AuthenticationHelper.popPrincipal();
			if (!logDisabled) {
				Audit.info(String.format("Arrêt du job %s", name));
			}
		}
	}

	/**
	 * A réimplémenter par les sous-classes
	 *
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected abstract void doExecute(Map<String, Object> params) throws Exception;

	public void interrupt() {
		setStatut(JobStatut.JOB_INTERRUPTING);
		if (!logDisabled) {
			LOGGER.info("<" + name + "> interrupted flag set");
		}
	}

	public void toBeExecuted() {
		Assert.isTrue(statut != JobStatut.JOB_INTERRUPTING && statut != JobStatut.JOB_RUNNING);
		setStatut(JobStatut.JOB_RUNNING);
	}

	public void wasExecuted() {
		if (statut == JobStatut.JOB_RUNNING) {
			setStatut(JobStatut.JOB_OK);
			// Si le job s'est terminé correctement, on supprime le message
			runningMessage = "";
		}
		else if (statut == JobStatut.JOB_INTERRUPTING) {
			setStatut(JobStatut.JOB_INTERRUPTED);
		}
	}

	/**
	 * @return <b>vrai</b> si le job a été interrompu ou s'il est entrain de s'interrompre.
	 */
	protected boolean isInterrupted() {
		return statut == JobStatut.JOB_INTERRUPTING || statut == JobStatut.JOB_INTERRUPTED;
	}

	/**
	 * @return the statut
	 */
	public JobStatut getStatut() {
		return statut;
	}

	/**
	 * @param statut
	 *            the statut to set
	 */
	public void setStatut(JobStatut statut) {
		if (!logDisabled) {
			LOGGER.debug("<" + name + "> status changed from " + this.statut + " to " + statut);
		}
		this.statut = statut;
	}

	/**
	 * @return le nom technique du batch
	 */
	public String getName() {
		return name;
	}

	public String getCategorie() {
		return categorie;
	}

	/**
	 * Retourne le status du job (true => en cours d'execution)
	 *
	 * @return le status du job
	 */
	public boolean isRunning() {
		return statut == JobStatut.JOB_INTERRUPTING || statut == JobStatut.JOB_RUNNING;
	}

	public String getRunningMessage() {
		return runningMessage;
	}

	public Integer getPercentProgression() {
		return percentProgression;
	}

	public void setRunningMessage(String message) {
		this.runningMessage = message;
		this.percentProgression = null;
	}

	public void setRunningMessage(String message, int percentProgression) {
		this.runningMessage = message;
		this.percentProgression = percentProgression;
	}

	/**
	 * @return le document de rapport du dernier run, ou <b>null</b> si aucun rapport n'existe ou le job n'a pas été lancé
	 */
	public Document getLastRunReport() {
		return lastRunReport;
	}

	public void setLastRunReport(Document runReport) {
		this.lastRunReport = runReport;
	}

	public Map<String, Object> getDefaultParamWebValues() {
		return defaultParamWebValues;
	}

	public Object getDefaultWebValue(String key) {
		if (getDefaultParamWebValues() != null) {
			return getDefaultParamWebValues().get(key);
		}
		return null;
	}

	public JobSynchronousMode getSynchronousMode() {
		return synchronousMode;
	}

	public void setSynchronousMode(JobSynchronousMode synchronousMode) {
		this.synchronousMode = synchronousMode;
	}

	public void setSynchronous(boolean v) {
		if (v) {
			this.synchronousMode = JobSynchronousMode.SYNCHRONOUS;
		}
		else {
			this.synchronousMode = JobSynchronousMode.ASYNCHRONOUS;
		}
	}

	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregModeHelper(UniregModeHelper helper) {
		// Note: on accède au mode helper par une méthode statique, donc on a pas besoin de le mémoriser ici. Par contre il est
		// absolument nécessaire de garder la dépendence Spring pour être certain que le UniregModeHelper est initialisé avant les
		// JobDefinition.
		//this.uniregModeHelper = helper;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public int compareTo(JobDefinition arg) {
		return getSortOrder() - arg.getSortOrder();
	}

	@Override
	public String toString() {
		return "Job: " + name + " SortOrder:" + sortOrder + " Sync:" + synchronousMode;
	}

	public String getDescription() {
		return description;
	}

	public Date getLastStart() {
		return lastStart;
	}

	public void setLastStart(Date lastStart) {
		this.lastStart = lastStart;
	}

	public Date getLastEnd() {
		return lastEnd;
	}

	public void setLastEnd(Date lastEnd) {
		this.lastEnd = lastEnd;
	}

	public class JobStatusManager implements StatusManager {

		public JobStatusManager() {
		}

		public synchronized boolean interrupted() {
			return isInterrupted();
		}

		public synchronized void setMessage(String msg) {
			setRunningMessage(msg);
		}

		public void setMessage(String msg, int percentProgression) {
			setRunningMessage(msg, percentProgression);
		}
	}

	protected StatusManager getStatusManager() {
		if (statusManager == null) {
			statusManager = new JobStatusManager();
		}
		return statusManager;
	}

	public List<JobParam> getParamDefinition() {
		return new ArrayList<JobParam>(paramDefinition.values());
	}

	/**
	 * @param key nom du paramètre
	 * @return la définition du paramètre nommé, ou <code>null</code> si aucune définition n'est connue
	 */
	public JobParam getParameterDefinition(String key) {
		return getParameterDefinition(key, false);
	}

	/**
	 * @param key nom du paramètre
	 * @param mustBeThere <code>true</code> si la méthode doit lancer une exception lorsque le paramètre est inconnu
	 * @return la définition du paramètre nommé, ou <code>null</code> si aucune définition n'est connue mais que ce n'est pas grave
	 * @throws IllegalArgumentException si aucun paramètre de ce nom n'est connu et qu'il doit pourtant être là
	 */
	protected JobParam getParameterDefinition(String key, boolean mustBeThere) {
		final JobParam param = paramDefinition.get(key);
		if (param == null && mustBeThere) {
			throw new IllegalArgumentException(String.format("Paramètre inconnu : %s", key));
		}
		return param;
	}

	/**
	 * @return the currentParameters
	 */
	public Map<String, Object> getCurrentParameters() {
		return currentParameters;
	}

	/**
	 * @return la description et la valeur des paramètres de l'exécution courante du job, ou <b>null</b> si le job n'est pas en cours d'exécution ou
	 *         aucun paramètre n'a été spécifié.
	 */
	public Map<String, Object> getCurrentParametersDescription() {

		if (this.currentParameters == null || this.currentParameters.isEmpty()) {
			return null;
		}

		// linked hash map pour conserver le même ordre des paramètres un peu partout
		final Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, JobParam> entry : this.paramDefinition.entrySet()) {
			final Object value = this.currentParameters.get(entry.getKey());
			if (value != null) {
				final JobParam paramDef = entry.getValue();
				result.put(paramDef.getDescription(), value);
			}
		}

		return result;
	}

	/**
	 * Extrait la valeur d'un paramètre de type boolean, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur booléenne du paramètre; ou <b>false</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final boolean getBooleanValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		Boolean value = null;
		if (params != null) {
			value = (Boolean) params.get(key);
		}
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value != null ? value : false;
	}

	/**
	 * Extrait la valeur d'un paramètre de type integer, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur entière du paramètre; ou <b>0</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final int getIntegerValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		final Integer value = getOptionalIntegerValue(params, parameterDefinition);
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value != null ? value : 0;
	}

	/**
	 * Extrait la valeur d'un paramètre de type integer, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur entière du paramètre; ou <b>0</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, ou si aucun paramètre de ce nom n'a été défini, ou si la valeur est négative ou nulle
	 */
	protected final int getStrictlyPositiveIntegerValue(Map<String, Object> params, String key) {
		final int value = getIntegerValue(params, key);
		if (value <= 0) {
			throw new IllegalArgumentException(String.format("La valeur du paramètre %s doit être strictement positive", key));
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type integer, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur entière du paramètre; ou <code>null</code> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final Integer getOptionalIntegerValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		if (parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Le paramètre %s n'est pas optionnel", key));
		}
		return getOptionalIntegerValue(params, parameterDefinition);
	}

	private static Integer getOptionalIntegerValue(Map<String, Object> params, JobParam paramDefinition) {
		Assert.notNull(paramDefinition);
		Integer value = null;
		if (params != null) {
			final Number i = (Number) params.get(paramDefinition.getName());
			if (i != null) {
				value = i.intValue();
			}
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type String, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur du paramètre (<code>null</code> si le paramètre n'était pas renseigné)
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final String getStringValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		String value = null;
		if (params != null) {
			value = (String) params.get(key);
		}
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type long, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur entière du paramètre; ou <b>0</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final long getLongValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		final Long value = getOptionalLongValue(params, parameterDefinition);
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value != null ? value : 0L;
	}

	/**
	 * Extrait la valeur d'un paramètre de type long, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur entière du paramètre; ou <code>null</code> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final Long getOptionalLongValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		if (parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Le paramètre %s n'est pas optionnel", key));
		}
		return getOptionalLongValue(params, parameterDefinition);
	}

	private static Long getOptionalLongValue(Map<String, Object> params, JobParam paramDefinition) {
		Assert.notNull(paramDefinition);
		Long value = null;
		if (params != null) {
			final Number i = (Number) params.get(paramDefinition.getName());
			if (i != null) {
				value = i.longValue();
			}
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type RegDate, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur du paramètre; ou <b>null</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, si aucun paramètre de ce nom n'a été défini, ou la date est invalide
	 */
	protected final RegDate getRegDateValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		RegDate value = null;
		if (params != null) {
			final Object v = params.get(key);
			if (v instanceof String) {
				final String s = (String) v;
				value = RegDateHelper.dashStringToDate(s);
				if (value == null) {
					throw new IllegalArgumentException(String.format("La date spécifiée '%s' n'est pas valide", s));
				}
			}
			else {
				value = (RegDate) v;
			}
		}
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type File, et retourne le contenu du fichier
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur du paramètre; ou <b>null</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, si aucun paramètre de ce nom n'a été défini
	 */
	protected final byte[] getFileContent(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		byte[] content = null;
		if (params != null) {
			content = (byte[]) params.get(key);
		}
		if (content == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return content;
	}

	/**
	 * Extrait la valeur d'un paramètre de type énuméré, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @param clazz class du type énuméré
	 * @return la valeur du paramètre; ou <b>null</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, si aucun paramètre de ce nom n'a été défini, ou la valeur est invalide
	 */
	protected final <T extends Enum<T>> T getEnumValue(Map<String, Object> params, String key, Class<T> clazz) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		T value = null;
		if (params != null) {
			final Object v = params.get(key);
			if (v instanceof String) {
				final String s = (String) v;
				value = Enum.valueOf(clazz, s);
			}
			else {
				//noinspection unchecked
				value = (T) v;
			}
		}
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value;
	}

	/**
	 * Retourne la date de traitement; c'est-à-dire la date du jour ou la date spécifiée en paramètre si Unireg est en mode testing.
	 *
	 * @param params
	 *            les paramètres de déclenchement du job
	 * @return une date
	 */
	protected RegDate getDateTraitement(Map<String, Object> params) {

		RegDate dateTraitement = RegDate.get();
		if (isTesting() && getParameterDefinition(DATE_TRAITEMENT) != null) {
			final RegDate date = getRegDateValue(params, DATE_TRAITEMENT);
			if (date != null) {
				dateTraitement = date;
			}
		}
		return dateTraitement;
	}

	protected static boolean isTesting() {
		return UniregModeHelper.isTestMode();
	}

	public boolean isVisible() {
		return true;
	}

	/**
	 * @return <code>true</code> si on doit pouvoir lancer ce job depuis l'IHM, <code>false</code> sinon
	 */
	public final boolean isWebStartable() {
		return isVisible() && (isTesting() || isWebStartableInProductionMode());
	}

	/**
	 * @return <code>true</code> si on doit pouvoir lancer ce job depuis l'IHM en dehors du mode "testing", <code>false</code> pour les jobs qui ne peuvent être lancés en production que par l'exploitation
	 */
	protected boolean isWebStartableInProductionMode() {
		return false;
	}

	/**
	 * Extrait les ids d'un fichier CSV contenant des ids séparés par des virgules, des points-virgules ou des retours de ligne.
	 *
	 * @param csv
	 *            le contenu d'un fichier CSV
	 * @return une liste d'ids
	 */
	protected static List<Long> extractIdsFromCSV(byte[] csv) {

		final String s = new String(csv);
		final String[] lines = s.split("[;,\n]");

		final List<Long> ids = new ArrayList<Long>(lines.length);
		for (String l : lines) {
			final String idAsString = l.replaceAll("[^0-9]", ""); // supprime tous les caractères non-numériques
			if (idAsString.length() > 0) {
				final Long id = Long.valueOf(idAsString);
				ids.add(id);
			}
		}

		return ids;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLogDisabled(boolean logDisabled) {
		this.logDisabled = logDisabled;
	}

	public boolean isLogDisabled() {
		return logDisabled;
	}
}
