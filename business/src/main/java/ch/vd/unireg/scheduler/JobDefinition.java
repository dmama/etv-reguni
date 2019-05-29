package ch.vd.unireg.scheduler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.InstantHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.Document;
import ch.vd.unireg.type.RestrictedAccess;
import ch.vd.unireg.utils.UniregModeHelper;

/**
 * Classe regroupant les informations d'affichage du job quartz
 *
 */
public abstract class JobDefinition implements InitializingBean, Comparable<JobDefinition> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobDefinition.class);

	public static final String DATE_TRAITEMENT = "DATE_TRAITEMENT"; // pour le testing uniquement

	public static final String KEY_JOB = "job";
	public static final String KEY_USER = "user";
	public static final String KEY_PARAMS = "params";

	// Params dynamic
	private String runningMessage;
	private Integer percentProgression;
	private JobStatut statut = JobStatut.JOB_OK;
	private Date lastStart;
	private Date lastEnd;
	private Document lastRunReport;
	private JobStatusManager statusManager = null;

	// suivi des status
	private final Lock statusLock = new ReentrantLock();
	private final Condition statusCondition = statusLock.newCondition();

	// Params static
	private final String name;
	private final JobCategory categorie;
	private JobSynchronousMode synchronousMode;
	private final int sortOrder;
	private final String description;
	private final Map<String, JobParam> paramDefinition = new LinkedHashMap<>();        // java.util.LinkedHashMap pour conserver l'ordre d'insertion des paramètres
	private final Map<String, Object> defaultParamWebValues = new HashMap<>();

	private boolean logDisabled = false;

	protected BatchScheduler batchScheduler;
	protected AuditManager audit;
	protected UniregModeHelper uniregModeHelper;

	/**
	 * La queue des exécutions du job en attente d'être exécutées.
	 */
	private final Queue<QueuedExecutionInfo> queuedExecutions = new LinkedBlockingQueue<>();

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

	public JobDefinition(String name, JobCategory categorie, int sortOrder, String description) {
		this.name = name;
		this.categorie = categorie;
		this.sortOrder = sortOrder;
		this.description = description;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isVisible()) {
			batchScheduler.register(this);
		}
	}

	protected final void refreshParameterDefinitions(List<Pair<JobParam, ?>> paramsDef) {
		paramDefinition.clear();
		defaultParamWebValues.clear();
		if (paramsDef != null) {
			for (Pair<JobParam, ?> paramDef : paramsDef) {
				addParameterDefinition(paramDef.getLeft(), paramDef.getRight());
			}
		}
	}

	protected final void addParameterDefinition(JobParam param, @Nullable Object defaultWebValue) {
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
			audit.info(String.format("Démarrage du job %s", name));
		}
		AuthenticationHelper.pushPrincipal(name);
		try {
			doExecute(params);
		}
		finally {
			AuthenticationHelper.popPrincipal();
			if (!logDisabled) {
				audit.info(String.format("Arrêt du job %s", name));
			}
		}
	}

	/**
	 * Ajoute les paramètres d'une exécution à effectuer dès que possible
	 * @param params les paramères en question
	 */
	public void addQueuedExecution(@NotNull String user, @NotNull Map<String, Object> params) {
		queuedExecutions.add(new QueuedExecutionInfo(user, params));
	}

	/**
	 * @return les prochains paramètres à exécuter ; ou <b>null</b> si aucune exécution n'a été mise-en-attente.
	 */
	@Nullable
	public QueuedExecutionInfo getNextQueuedExecution() {
		return queuedExecutions.poll();
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
			LOGGER.info('<' + name + "> interrupted flag set");
		}
	}

	public void toBeExecuted() {
		if (statut == JobStatut.JOB_INTERRUPTING || statut == JobStatut.JOB_RUNNING) {
			throw new IllegalStateException();
		}
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
			LOGGER.debug('<' + name + "> status changed from " + this.statut + " to " + statut);
		}

		// notifie tout le monde que le statut a changé
		statusLock.lock();
		try {
			this.statut = statut;
			statusCondition.signalAll();
		}
		finally {
			statusLock.unlock();
		}
	}

	/**
	 * @return le nom technique du batch
	 */
	public String getName() {
		return name;
	}

	public JobCategory getCategorie() {
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

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	public void setUniregModeHelper(UniregModeHelper uniregModeHelper) {
		this.uniregModeHelper = uniregModeHelper;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	@Override
	public int compareTo(@NotNull JobDefinition arg) {
		return Integer.compare(getSortOrder(), arg.getSortOrder());
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

		@Override
		public synchronized boolean isInterrupted() {
			return JobDefinition.this.isInterrupted();
		}

		@Override
		public synchronized void setMessage(String msg) {
			setRunningMessage(msg);
		}

		@Override
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
		return new ArrayList<>(paramDefinition.values());
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
		final Map<String, Object> result = new LinkedHashMap<>();
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
		final Boolean value = getOptionalBooleanValue(params, parameterDefinition, null);
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		return value != null ? value : false;
	}

	/**
	 * Extrait la valeur d'un paramètre de type boolean, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @param defaultValue la valeur à renvoyer si aucune valeur n'a été trouvée
	 * @return la valeur booléenne du paramètre
	 */
	protected final boolean getBooleanValue(Map<String, Object> params, String key, boolean defaultValue) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		final Boolean value = getOptionalBooleanValue(params, parameterDefinition, defaultValue);
		return value == null ? defaultValue : value;
	}

	/**
	 * Extrait la valeur d'un paramètre de type boolean, et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @param defaultValue la valeur à renvoyer si aucune valeur n'a été trouvée
	 * @return la valeur booléenne du paramètre
	 */
	@Nullable
	protected final Boolean getOptionalBooleanValue(Map<String, Object> params, String key, Boolean defaultValue) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		if (parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Le paramètre %s n'est pas optionnel", key));
		}
		return getOptionalBooleanValue(params, parameterDefinition, defaultValue);
	}

	@Nullable
	private static Boolean getOptionalBooleanValue(Map<String, Object> params, JobParam paramDefinition, @Nullable Boolean defaultValue) {
		if (paramDefinition == null) {
			throw new IllegalArgumentException();
		}
		Boolean value = null;
		if (params != null) {
			final Boolean b = (Boolean) params.get(paramDefinition.getName());
			value = (b == null ? defaultValue : b);
		}
		return value;
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
	 * @return la valeur entière du paramètre; ou <b>0</b> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, ou si aucun paramètre de ce nom n'a été défini, ou si la valeur est négative ou nulle
	 */
	protected final int getPositiveIntegerValue(Map<String, Object> params, String key) {
		final int value = getIntegerValue(params, key);
		if (value < 0) {
			throw new IllegalArgumentException(String.format("La valeur du paramètre %s doit être positive ou nulle", key));
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
	protected final @Nullable Integer getOptionalIntegerValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		if (parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Le paramètre %s n'est pas optionnel", key));
		}
		return getOptionalIntegerValue(params, parameterDefinition);
	}

	/**
	 * Extrait la valeur d'un paramètre de type integer (positif), et retourne la.
	 *
	 * @param params les paramètres
	 * @param key la clé du paramètre
	 * @return la valeur entière du paramètre; ou <code>null</code> si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire, ou si aucun paramètre de ce nom n'a été défini
	 */
	protected final @Nullable Integer getOptionalPositiveIntegerValue(Map<String, Object> params, String key) {
		final Integer value = getOptionalIntegerValue(params, key);
		if (value != null && value < 0) {
			throw new IllegalArgumentException(String.format("La valeur du paramètre %s doit être positive ou nulle", key));
		}
		return value;
	}

	private static @Nullable Integer getOptionalIntegerValue(Map<String, Object> params, JobParam paramDefinition) {
		if (paramDefinition == null) {
			throw new IllegalArgumentException();
		}
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
	protected final @Nullable Long getOptionalLongValue(Map<String, Object> params, String key) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		if (parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Le paramètre %s n'est pas optionnel", key));
		}
		return getOptionalLongValue(params, parameterDefinition);
	}

	private static @Nullable Long getOptionalLongValue(Map<String, Object> params, JobParam paramDefinition) {
		if (paramDefinition == null) {
			throw new IllegalArgumentException();
		}
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
	 * Extrait et retourne la valeur d'un paramètre de type énuméré.
	 *
	 * @param params les paramètres
	 * @param key    la clé du paramètre
	 * @param clazz  class du type énuméré
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
		if (value instanceof RestrictedAccess && !((RestrictedAccess) value).isAllowed()) {
			throw new IllegalArgumentException(String.format("Valeur invalide pour le paramètre %s : '%s'", key, value));
		}
		return value;
	}

	/**
	 * Extrait et retourne les valeurs d'un paramètre de type énuméré multi-select.
	 *
	 * @param params les paramètres
	 * @param key    la clé du paramètre
	 * @param clazz  class du type énuméré
	 * @return les valeurs du paramètre; ou une liste vide si le paramètre n'est pas renseigné.
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, si aucun paramètre de ce nom n'a été défini, ou la valeur est invalide
	 */
	@NotNull
	protected final <T extends Enum<T>> List<T> getMultiSelectEnumValue(Map<String, Object> params, @NotNull String key, @NotNull Class<T> clazz) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		final List<T> values = new ArrayList<>();
		if (params != null) {
			final Object v = params.get(key);
			if (v instanceof String) {
				final String s = (String) v;
				values.add(Enum.valueOf(clazz, s));
			}
			else if (v instanceof Iterable) {
				for (Object o : (Iterable) v) {
					if (o instanceof String) {
						values.add(Enum.valueOf(clazz, (String) o));
					}
					else {
						//noinspection unchecked
						values.add((T) o);
					}
				}
			}
			else if (v instanceof Object[]) {
				for (Object o : (Object[]) v) {
					if (o instanceof String) {
						values.add(Enum.valueOf(clazz, (String) o));
					}
					else {
						//noinspection unchecked
						values.add((T) o);
					}
				}
			}
			else if (v != null) {
				//noinspection unchecked
				values.add((T) v);
			}
		}
		if (values.isEmpty() && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		if (values instanceof RestrictedAccess && !((RestrictedAccess) values).isAllowed()) {
			throw new IllegalArgumentException(String.format("Valeur invalide pour le paramètre %s : '%s'", key, values));
		}
		return values;
	}

	/**
	 * Extrait la valeur d'un paramètre particulier
	 *
	 * @param params les paramètres du job
	 * @param key la clé du paramètre cherché
	 * @param clazz classe du paramètre cherché
	 * @param <T> type du paramètre cherché
	 * @return la valeur du paramètre, ou <b>null</b> si le paramètre n'est pas renseigné
	 * @throws IllegalArgumentException si le paramètre était noté comme obligatoire alors qu'il n'est pas renseigné, si aucun paramètre de ce nom n'a été défini, ou la valeur est invalide
	 */
	protected final <T> T getValue(Map<String, Object> params, String key, Class<T> clazz) {
		final JobParam parameterDefinition = getParameterDefinition(key, true);
		Object value = null;
		if (params != null) {
			final Object v = params.get(key);
			if (v instanceof String) {
				final String s = (String) v;
				value = parameterDefinition.getType().stringToValue(s);
			}
			else {
				value = v;
			}
		}
		if (value != null && !clazz.isInstance(value)) {
			throw new IllegalArgumentException("Paramètre invalide (" + clazz.getName() + " attendu, " + value.getClass().getName() + "reçu ");
		}
		if (value == null && parameterDefinition.isMandatory()) {
			throw new IllegalArgumentException(String.format("Paramètre obligatoire non renseigné : %s", key));
		}
		//noinspection unchecked
		return (T) value;
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

	protected boolean isTesting() {
		return uniregModeHelper.isTestMode();
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
	protected static List<Long> extractIdsFromCSV(byte[] csv) throws IOException {

		if (csv == null || csv.length == 0) {
			return Collections.emptyList();
		}

		final List<Long> ids = new LinkedList<>();
		try (InputStream is = new ByteArrayInputStream(csv);
		     Reader r = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(r)) {

			String s;
			while ((s = br.readLine()) != null) {
				final String[] lines = s.split("[;,]");
				for (String l : lines) {
					final String idAsString = l.replaceAll("[^0-9]", StringUtils.EMPTY); // supprime tous les caractères non-numériques
					if (idAsString.length() > 0) {
						final Long id = Long.valueOf(idAsString);
						ids.add(id);
					}
				}
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

	public static class TimeoutExpiredException extends Exception {
		public final JobStatut statut;
		public TimeoutExpiredException(JobStatut statut) {
			this.statut = statut;
		}
	}

	/**
	 * Attend le temps imparti que le job atteigne l'un des états attendus
	 * @param expected les états attendus
	 * @param timeout le temps d'attente maximal
	 * @return l'état atteint
	 * @throws TimeoutExpiredException si le timeout est atteint avant que l'un des états attendus soit atteint
	 * @throws InterruptedException si le thread a été interrompu
	 */
	public JobStatut waitForStatusIn(Set<JobStatut> expected, Duration timeout) throws TimeoutExpiredException, InterruptedException {
		final Instant now = InstantHelper.get();
		final Instant expiration = now.plus(timeout);
		statusLock.lock();
		try {
			while (!expected.contains(statut)) {
				final Duration remaningTime = Duration.between(InstantHelper.get(), expiration);
				if (remaningTime.isNegative() || remaningTime.isZero()) {
					throw new TimeoutExpiredException(statut);
				}
				statusCondition.awaitNanos(Math.max(remaningTime.toNanos(), 1L));
			}
			return statut;
		}
		finally {
			statusLock.unlock();
		}
	}
}
