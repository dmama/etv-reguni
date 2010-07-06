package ch.vd.uniregctb.scheduler;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.utils.UniregModeHelper;
import org.apache.log4j.Logger;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;

/**
 * Classe regroupant les informations d'affichage du job quartz
 *
 */
public abstract class JobDefinition implements InitializingBean, Comparable<Object> {

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
	private String name;
	private String categorie;
	private Map<String, Object> defaultParams = null;
	private JobSynchronousMode synchronousMode;
	private int sortOrder;
	private String description;
	private List<JobParam> paramDefinition = Collections.emptyList();

	protected BatchScheduler batchScheduler;

	private HashMap<String, Object> currentParameters = null;

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

	public JobDefinition() {
	}

	public JobDefinition(String name, String categorie, int sortOrder, String description) {
		this.name = name;
		this.categorie = categorie;
		this.sortOrder = sortOrder;
		this.description = description;
	}

	public JobDefinition(String name, String categorie, int sortOrder, String description, List<JobParam> paramsDef) {
		this(name, categorie, sortOrder, description);
		if (paramsDef == null) {
			this.paramDefinition = Collections.emptyList();
		}
		else {
			this.paramDefinition = paramsDef;
		}
	}

	public JobDefinition(String name, String categorie, int sortOrder, String description, List<JobParam> paramsDef,
			Map<String, Object> defaultParams) {
		this(name, categorie, sortOrder, description, paramsDef);
		this.defaultParams = defaultParams;
	}

	public void afterPropertiesSet() throws Exception {
		if (isVisible()) {
			batchScheduler.register(this);
		}
		/*
		 * Surcharge les paramètres par défaut créés par cette instance
		 * avec ceux spécifiés dans le constructeur.
		 */
		defaultParams = combineDefaultParams(createDefaultParams(), defaultParams);
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
		lastEnd = new Date();
	}

	protected void execute(HashMap<String, Object> params) throws Exception {

		lastStart = new Date();
		lastEnd = null;

		runningMessage = "";
		percentProgression = null;
		currentParameters = params;

		// le batch tournera avec le nom d'utilisateur égal au nom du batch
		Audit.info(String.format("Démarrage du job %s", name));
		AuthenticationHelper.pushPrincipal(name);
		try {
			doExecute(params);
		}
		finally {
			AuthenticationHelper.popPrincipal();
			Audit.info(String.format("Arrêt du job %s", name));
		}
	}

	/**
	 * Remplis une HashMap avec les paramètres par défaut définis par programmation.
	 * Les valeurs renseignées dans cette Map sont surchargées avec les default params passées au constructeur.
	 *
	 * @return
	 */
	protected HashMap<String, Object> createDefaultParams() {
		return null;
	}

	/**
	 * A réimplémenter par les sous-classes
	 *
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected abstract void doExecute(HashMap<String, Object> params) throws Exception;

	public void interrupt() {
		setStatut(JobStatut.JOB_INTERRUPTING);
		LOGGER.info("<" + name + "> interrupted flag set");
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
	 * Surcharge les paramètres par défaut avec des paramètres additionnels.
	 *
	 * @param defaultParams
	 *            paramètres par défaut
	 * @param additionalParams
	 *            paramètres additionnels
	 * @return la nouvelle HashMap contenant la combinaison de paramètres
	 */
	protected static HashMap<String, Object> combineDefaultParams(Map<String, Object> defaultParams, Map<String, Object> additionalParams) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		if (defaultParams != null) {
			result.putAll(defaultParams);
		}
		if (additionalParams != null && additionalParams != defaultParams) {
			result.putAll(additionalParams);
		}
		return result;
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
		LOGGER.debug("<" + name + "> status changed from " + this.statut + " to " + statut);
		this.statut = statut;
	}

	/**
	 * @return the nom
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

	public Map<String, Object> getDefaultParams() {
		return defaultParams;
	}

	public Object getDefaultValue(String key) {
		if (getDefaultParams() != null) {
			return getDefaultParams().get(key);
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

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public int compareTo(Object arg) {

		JobDefinition other = (JobDefinition) arg;
		return getSortOrder() - other.getSortOrder();
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
		return paramDefinition;
	}

	public void setParamDefinition(List<JobParam> paramDef) {
		this.paramDefinition = paramDef;
	}

	public JobParam getParameterDefintion(String key) {

		if (key == null || this.paramDefinition == null || this.paramDefinition.isEmpty()) {
			return null;
		}
		for (JobParam param : this.paramDefinition) {
			if (key.equals(param.getName()))
				return param;
		}
		return null;
	}

	/**
	 * @return the currentParameters
	 */
	public HashMap<String, Object> getCurrentParameters() {
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

		Map<String, Object> result = new HashMap<String, Object>();

		for (String parameterName : this.currentParameters.keySet()) {
			Object value = this.currentParameters.get(parameterName);
			if (value != null) {
				JobParam param = this.getParameterDefintion(parameterName);
				result.put(param.getDescription(), value);
			}
		}

		return result;
	}

	/**
	 * Extrait la valeur d'un paramètre de type boolean, et retourne-là.
	 *
	 * @param params
	 *            les paramètres
	 * @param key
	 *            la clé du paramètre
	 * @return la valeur booléenne du paramètre; ou <b>false</b> si le paramètre n'est pas renseigné.
	 */
	protected static boolean getBooleanValue(HashMap<String, Object> params, String key) {
		boolean value = false;
		if (params != null) {
			Boolean b = (Boolean) params.get(key);
			if (b != null) {
				value = b;
			}
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type integer, et retourne-là.
	 *
	 * @param params
	 *            les paramètres
	 * @param key
	 *            la clé du paramètre
	 * @return la valeur entière du paramètre; ou <b>0</b> si le paramètre n'est pas renseigné.
	 */
	protected static int getIntegerValue(HashMap<String, Object> params, String key) {
		int value = 0;
		if (params != null) {
			Number i = (Number) params.get(key);
			if (i != null) {
				value = i.intValue();
			}
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type integer, et retourne-là.
	 *
	 * @param params
	 *            les paramètres
	 * @param key
	 *            la clé du paramètre
	 * @return la valeur entière du paramètre; ou <b>0</b> si le paramètre n'est pas renseigné.
	 */
	protected static long getLongValue(HashMap<String, Object> params, String key) {
		long value = 0;
		if (params != null) {
			Number i = (Number) params.get(key);
			if (i != null) {
				value = i.longValue();
			}
		}
		return value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type boolean, et retourne-là.
	 *
	 * @param params
	 *            les paramètres
	 * @param key
	 *            la clé du paramètre
	 * @return la valeur booléenne du paramètre; ou <b>false</b> si le paramètre n'est pas renseigné.
	 */
	protected static RegDate getRegDateValue(HashMap<String, Object> params, String key) {
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
				value = (RegDate)v;
			}
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
	protected RegDate getDateTraitement(HashMap<String, Object> params) {

		RegDate dateTraitement = RegDate.get();
		if (isTesting()) {
			final RegDate date = getRegDateValue(params, DATE_TRAITEMENT);
			if (date != null) {
				dateTraitement = date;
			}
		}
		return dateTraitement;
	}

	public boolean isTesting(){
		return UniregModeHelper.isTestMode();
	}

	public boolean isVisible() {
		return true;
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

}
