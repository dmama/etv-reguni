package ch.vd.uniregctb.ubr;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.uniregctb.webservices.batch.BatchPort;
import ch.vd.uniregctb.webservices.batch.BatchService;
import ch.vd.uniregctb.webservices.batch.BatchWSException;
import ch.vd.uniregctb.webservices.batch.JobDefParam;
import ch.vd.uniregctb.webservices.batch.JobDefinition;
import ch.vd.uniregctb.webservices.batch.JobName;
import ch.vd.uniregctb.webservices.batch.JobNameArray;
import ch.vd.uniregctb.webservices.batch.JobStatut;
import ch.vd.uniregctb.webservices.batch.LastReport;
import ch.vd.uniregctb.webservices.batch.ParamMap;
import ch.vd.uniregctb.webservices.batch.ParamMapEntry;
import ch.vd.uniregctb.webservices.batch.Report;
import ch.vd.uniregctb.webservices.batch.StartBatch;
import ch.vd.uniregctb.webservices.batch.StopBatch;

/**
 * Client qui se connecte au web-service 'batch' d'Unireg et qui permet de piloter (démarrer, arrêter, ...) les batches.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BatchRunnerClient {

	private final BatchPort service;
	
	static private final Logger LOGGER = Logger.getLogger(BatchRunnerClient.class);

	public BatchRunnerClient(String serviceUrl, String username, String password) throws Exception {
		service = initWebService(serviceUrl, username, password);
	}

	/**
	 * Démarre le batch spécifié par son nom. En fonction du type de batch, l'appel retourne immédiatement (=exécution asynchrone) ou attend
	 * que le batch soit terminé (=exécution synchrone).
	 *
	 * @param name
	 *            le nom du batch
	 * @param params
	 *            les paramètres à utiliser
	 * @throws BatchWSException
	 *             en cas d'erreur lors du démarrage du batch
	 */
	public void startBatch(String name, Map<String, Object> params) throws BatchWSException {
		StartBatch p = new StartBatch();
		p.setName(name);
		p.setParams(util2web(params));
		service.start(p);
	}

	/**
	 * Démarre le batch spécifié par son nom et <b>attend</b> que le batch soit terminé (=exécution synchrone).
	 *
	 * @param name
	 *            le nom du batch
	 * @param params
	 *            les paramètres à utiliser
	 * @throws BatchWSException
	 *             en cas d'erreur lors du démarrage du batch
	 */
	public void runBatch(String name, Map<String, Object> params) throws BatchWSException {
		StartBatch p = new StartBatch();
		p.setName(name);
		p.setParams(util2web(params));
		service.start(p);

		final JobDefParam pp = new JobDefParam();
		pp.setName(name);

		JobStatut status = JobStatut.JOB_READY;
		while (isRunning(status) || JobStatut.JOB_READY.equals(status)) {
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			try {
				JobDefinition def = service.getJobDefinition(pp);
				status = def.getStatut();
			}
			catch (RuntimeException e) {
				if (causedByTimeout(e)) {
					LOGGER.warn("Timeout lors de la récupération du statut du batch, on va réessayer...");
					status = JobStatut.JOB_RUNNING; // on suppose que le job tourne toujours
				}
				else {
					LOGGER.error("Impossible de récupérer le statut du batch", e);
					throw e;
				}
			}
		}

		if (JobStatut.JOB_EXCEPTION.equals(status)) {
			throw new BatchWSException("Le job a lancé une exception - consulter le log du serveur");
		}
	}

	private boolean isRunning(JobStatut status) {
		return status == JobStatut.JOB_RUNNING || status == JobStatut.JOB_INTERRUPTING;
	}

	/**
	 * Détermine si l'exception spécifiée a été levée à cause d'un timeout.
	 *
	 * @param exception une exception
	 * @return <b>vrai</b> si l'exception a été levée à cause d'un timeout; <b>faux</b> autrement.
	 */
	private boolean causedByTimeout(Throwable exception) {
		Throwable e = exception;
		while (e != null) {
			if (e instanceof SocketTimeoutException) {
				return true;
			}
			e = e.getCause();
		}
		return false;
	}

	/**
	 * Stoppe le batch spécifié. La méthode ne retourne que lorsque le batch est véritablement stoppé.
	 *
	 * @param name
	 *            le nom du batch à stopper.
	 * @throws BatchWSException
	 *             en cas d'erreur lors de l'arrêt du batch
	 */
	public void stopBatch(String name) throws BatchWSException {
		StopBatch p = new StopBatch();
		p.setName(name);
		service.stop(p);
	}

	/**
	 * @return les noms des batchs disponibles.
	 */
	public List<String> getBatchNames() {
		final JobNameArray array = service.getListJobs();
		return web2util(array);
	}

	/**
	 * @param name
	 *            le nom du job à détailler.
	 * @return les informations complètes (paramètres, état, ...) d'un job.
	 */
	public JobDefinition getBatchDefinition(String name) {
		JobDefParam p = new JobDefParam();
		p.setName(name);
		return service.getJobDefinition(p);
	}

	/**
	 * @param name
	 *            le nom du job dont on veut obtenir le dernier rapport.
	 * @return le dernier rapport d'exécution du job spécifié; <b>null</b> si le job n'a pas été exécuté depuis le démarrage de
	 *         l'application.
	 */
	public Report getLastReport(String name) throws BatchWSException {
		LastReport p = new LastReport();
		p.setName(name);
		return service.getLastReport(p);
	}

	private static BatchPort initWebService(String serviceUrl, String username, String password) throws Exception {
		URL wsdlUrl = ResourceUtils.getURL("classpath:BatchService.wsdl");
		BatchService ts = new BatchService(wsdlUrl);
		BatchPort service = ts.getBatchPortPort();
		Map<String, Object> context = ((BindingProvider) service).getRequestContext();
		if (StringUtils.isNotBlank(username)) {
			context.put(BindingProvider.USERNAME_PROPERTY, username);
			context.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);

		// Désactive la validation du schéma (= ignore silencieusement les éléments inconnus), de manière à permettre l'évolution ascendante-compatible du WSDL.
		context.put(Message.SCHEMA_VALIDATION_ENABLED, false);
		context.put("set-jaxb-validation-event-handler", false);
		
		return service;
	}

	/**
	 * Converti une java.util.HashMap en MyHashMapType.
	 */
	private ParamMap util2web(Map<String, Object> params) {

		ParamMap map = new ParamMap();

		if (params != null) {
			final List<ParamMapEntry> entries = map.getEntries();
			for (Entry<String, Object> e : params.entrySet()) {
				ParamMapEntry entry = new ParamMapEntry();
				entry.setKey(e.getKey());
				final Object value = e.getValue();
				if (value instanceof String) {
					entry.setValue((String) value);
				}
				else if (value instanceof byte[]) {
					entry.setBytesValue(new DataHandler(new ByteArrayDataSource((byte[]) value, "application/octet-stream")));
				}
				else {
					throw new IllegalArgumentException("Type de paramètre inconnu = [" + value + "]");
				}
				entries.add(entry);
			}
		}

		return map;
	}

	/**
	 * Converti un array JobNameArray en liste de strings.
	 */
	private List<String> web2util(final JobNameArray array) {
		if (array == null) {
			return Collections.emptyList();
		}
		final List<JobName> items = array.getItem();
		List<String> list = new ArrayList<String>(items.size());
		for (JobName n : items) {
			list.add(n.getName());
		}
		return list;
	}
}
