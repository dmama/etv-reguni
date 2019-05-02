package ch.vd.unireg.ubr;

import javax.activation.DataHandler;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.transport.http.HTTPConduit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client qui se connecte au web-service 'batch' d'Unireg et qui permet de piloter (démarrer, arrêter, ...) les batches.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BatchRunnerClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchRunnerClient.class);

	private final String serviceUrl;
	private final String username;
	private final String password;

	private static final String CONTENT_DISPOSITION = "Content-Disposition";
	private static final String CONTENT_TYPE = "Content-Type";

	private static final String JOB_LIST = "/jobs";

	private enum JobAction {
		START("/job/%s/start"),
		STOP("/job/%s/stop"),
		STATUS("/job/%s/status"),
		WAIT("/job/%s/wait"),
		DESCRIPTION("/job/%s/description"),
		REPORT("/job/%s/report");

		private final String pattern;

		JobAction(String pattern) {
			this.pattern = pattern;
		}

		public String path(String jobName) {
			return String.format(pattern, jobName);
		}
	}

	private void setReceiveTimeout(WebClient client, long receiveTimeout) {
		final HTTPConduit conduit = (HTTPConduit) WebClient.getConfig(client).getConduit();
		conduit.getClient().setReceiveTimeout(receiveTimeout);
	}

	private WebClient createWebClient(long receiveTimeout) {
		final WebClient wc = WebClient.create(serviceUrl, username, password, null);
		setReceiveTimeout(wc, receiveTimeout);
		return wc;
	}

	private void reinitWebClient(WebClient client, long receiveTimeout) {
		client.reset();
		setReceiveTimeout(client, receiveTimeout);
	}

	private static void path(WebClient client, JobAction action, String jobName) {
		client.path(action.path(jobName));
	}

	private static void param(WebClient client, String paramName, Object paramValue) {
		client.query(paramName, paramValue);
	}

	public BatchRunnerClient(String serviceUrl, String username, String password) throws Exception {
		this.serviceUrl = serviceUrl;
		this.username = username;
		this.password = password;
	}

	/**
	 * Démarre le batch spécifié par son nom. L'appel retourne immédiatement.
	 * @param name le nom du batch
	 * @param params les paramètres à utiliser
	 * @throws BatchRunnerClientException en cas d'erreur lors du démarrage du batch
	 */
	public void startBatch(String name, Map<String, Object> params) throws BatchRunnerClientException {
		final WebClient client = createWebClient(60000);        // 60 secondes
		path(client, JobAction.START, name);

		final List<Attachment> attachments = new ArrayList<>();

		// pour les tests WIT (et plus généralement de toute façon), il vaut mieux blinder contre une map "nulle"
		params = Optional.ofNullable(params).orElseGet(Collections::emptyMap);

		// en gros, dans les paramètres, à ce niveau, il y a des String et des byte[]
		final Map<String, String> simpleParameters = new HashMap<>(params.size());
		for (Map.Entry<String, Object> paramEntry : params.entrySet()) {
			final String paramName = paramEntry.getKey();
			final Object paramValue = paramEntry.getValue();
			if (paramValue instanceof String) {
				simpleParameters.put(paramName, (String) paramValue);
			}
			else if (paramValue != null) {
				final DataHandler dh = new DataHandler(paramValue, MediaType.APPLICATION_OCTET_STREAM);
				final MultivaluedMap<String, String> headers = buildHeaders(MediaType.APPLICATION_OCTET_STREAM, paramName);
				final Attachment attachment = new Attachment(paramName, dh, headers);
				attachments.add(attachment);
			}
		}

		try {
			// paramètres "simples" (même vide, il faut renseigner le champ dans la requête)
			final ObjectMapper mapper = new ObjectMapper();
			final String json = mapper.writeValueAsString(simpleParameters);
			final DataHandler dh = new DataHandler(json, MediaType.APPLICATION_JSON);
			final MultivaluedMap<String, String> headers = buildHeaders(MediaType.APPLICATION_JSON, JobConstants.SIMPLE_PARAMETERS_PART_NAME);
			final Attachment attachment = new Attachment(JobConstants.SIMPLE_PARAMETERS_PART_NAME, dh, headers);
			attachments.add(attachment);

			final Response response = client.type(JobConstants.MULTIPART_MIXED).post(new MultipartBody(attachments, MediaType.valueOf(JobConstants.MULTIPART_MIXED), false));
			final int status = response.getStatus();
			if (status == HttpURLConnection.HTTP_PRECON_FAILED) {
				throw new BatchRunnerClientException("Job is already running...");
			}
			if (status >= 400) {
				final String message = response.readEntity(String.class);
				throw new WebApplicationException(message, response);
			}
			if (status != HttpURLConnection.HTTP_CREATED) {
				throw new BatchRunnerClientException(String.format("HTTP code %d received from the server", status));
			}
		}
		catch (WebApplicationException e) {
			throw new BatchRunnerClientException(String.format("HTTP error code %d received from the server", e.getResponse().getStatus()), e);
		}
		catch (BatchRunnerClientException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BatchRunnerClientException(e);
		}
	}

	private static MultivaluedMap<String, String> buildHeaders(String contentType, String name) {
		final MultivaluedMap<String, String> headers = new MetadataMap<>();
		headers.putSingle(CONTENT_DISPOSITION, String.format("form-data; name=\"%s\"", name));
		headers.putSingle(CONTENT_TYPE, contentType);
		return headers;
	}

	/**
	 * Démarre le batch spécifié par son nom et <b>attend</b> que le batch soit terminé (=exécution synchrone).
	 * @param name le nom du batch
	 * @param params les paramètres à utiliser
	 * @throws BatchRunnerClientException en cas d'erreur lors du démarrage du batch
	 */
	public void runBatch(String name, Map<String, Object> params) throws BatchRunnerClientException {
		// démarrage
		startBatch(name, params);

		// attendons un peu pour laisser le temps au job de démarrer
		try {
			Thread.sleep(TimeUnit.SECONDS.toMillis(10));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

		// on attend la fin du job
		attendreArretJob(name);
	}

	private void attendreArretJob(String jobName) throws BatchRunnerClientException {
		final Set<JobStatus> endStatuses = EnumSet.of(JobStatus.EXCEPTION,
		                                              JobStatus.INTERRUPTED,
		                                              JobStatus.OK);
		final JobWaitInformation waitInfo = waitForJobStatus(endStatuses, jobName);
		if (JobStatus.EXCEPTION == waitInfo.getJobStatus()) {
			// on essaie d'abord d'aller chercher le message de l'exception (qui se trouve dans le "runningMessage" du dernier run du batch)
			final String msg;
			final String runningMessage = waitInfo.getJobRunningMessage();
			if (StringUtils.isNotBlank(runningMessage)) {
				msg = String.format("%s (détails dans le log du serveur)", runningMessage);
			}
			else {
				msg = "Le job a lancé une exception - consulter le log du serveur";
			}
			throw new BatchRunnerClientException(msg);
		}
	}

	@NotNull
	private JobWaitInformation waitForJobStatus(Set<JobStatus> expectedStatuses, String jobName) throws BatchRunnerClientException {
		final long timeoutMetier = TimeUnit.SECONDS.toMillis(20);
		final long timeoutTechnique = timeoutMetier * 2;
		final WebClient client = createWebClient(timeoutTechnique);

		while (true) {
			try {
				reinitWebClient(client, timeoutTechnique);
				path(client, JobAction.WAIT, jobName);
				expectedStatuses.stream().map(Enum::name).forEach(s -> param(client, "status", s));
				param(client, "timeout", timeoutMetier);

				final JobWaitInformation info = getJSON(client, JobWaitInformation.class);
				if (info.getWaitStatus() == JobWaitInformation.JobWaitStatus.EXPECTED_STATUS_REACHED) {
					return info;
				}
			}
			catch (WebApplicationException | IOException e) {
				throw new BatchRunnerClientException(e);
			}
			catch (RuntimeException e) {
				if (causedByTimeout(e)) {
					LOGGER.warn("Timeout lors de la récupération du statut du batch, on va réessayer...");
				}
				else {
					LOGGER.error("Impossible de récupérer le statut du batch", e);
					throw e;
				}
			}
		}
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
	 * @param name le nom du batch à stopper.
	 * @throws BatchRunnerClientException en cas d'erreur lors de l'arrêt du batch
	 */
	public void stopBatch(String name) throws BatchRunnerClientException {
		final WebClient client = createWebClient(10000);   // 10 secondes
		path(client, JobAction.STOP, name);

		try {
			// demande d'arrêt
			final Response response = client.post(null);
			if (response.getStatus() >= 400) {
				final String message = response.readEntity(String.class);
				throw new WebApplicationException(message, response);
			}
			if (response.getStatus() != HttpURLConnection.HTTP_OK) {
				throw new BatchRunnerClientException(String.format("HTTP code %d received while trying to stop job %s.", response.getStatus(), name));
			}
		}
		catch (WebApplicationException e) {
			throw new BatchRunnerClientException(String.format("HTTP code %d received from the server", e.getResponse().getStatus()), e);
		}

		// attente de l'arrêt
		attendreArretJob(name);
	}

	/**
	 * @return les noms des batchs disponibles.
	 */
	public List<String> getBatchNames() throws BatchRunnerClientException {
		final WebClient client = createWebClient(10000);    // 10 secondes
		client.path(JOB_LIST);

		try {
			final JobNames names = getJSON(client, JobNames.class);
			return names.getJobs();
		}
		catch (WebApplicationException e) {
			throw new BatchRunnerClientException(String.format("HTTP error code %d received from the server", e.getResponse().getStatus()), e);
		}
		catch (Exception e) {
			throw new BatchRunnerClientException(e);
		}
	}

	private static <T> T getJSON(WebClient client, Class<T> clazz) throws IOException, WebApplicationException {
		final Response response = client.accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() >= 400) {
			final String message = response.readEntity(String.class);
			throw new WebApplicationException(message, response);
		}
		final ObjectMapper mapper = new ObjectMapper();
		try (InputStream is = (InputStream) response.getEntity()) {
			return mapper.readValue(is, clazz);
		}
	}

	private static String getString(WebClient client) throws IOException, WebApplicationException {
		return client.accept(MediaType.TEXT_PLAIN_TYPE).get(String.class);
	}

	/**
	 * @param name le nom du job à détailler.
	 * @return les informations complètes (paramètres, état, ...) d'un job.
	 */
	public JobDescription getBatchDescription(String name) throws BatchRunnerClientException {
		final WebClient client = createWebClient(10000);    // 10 secondes
		path(client, JobAction.DESCRIPTION, name);
		try {
			return getJSON(client, JobDescription.class);
		}
		catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
				return null;
			}
			throw new BatchRunnerClientException(e);
		}
		catch (Exception e) {
			throw new BatchRunnerClientException(e);
		}
	}

	public JobStatus getBatchStatus(String name) throws BatchRunnerClientException {
		final WebClient client = createWebClient(10000);    // 10 secondes
		path(client, JobAction.STATUS, name);
		try {
			final String status = getString(client);
			return JobStatus.valueOf(status);
		}
		catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
				return null;
			}
			throw new BatchRunnerClientException(e);
		}
		catch (Exception e) {
			throw new BatchRunnerClientException(e);
		}
	}

	/**
	 * @param name le nom du job dont on veut obtenir le dernier rapport.
	 * @return le dernier rapport d'exécution du job spécifié; <b>null</b> si le job n'a pas été exécuté depuis le démarrage de l'application.
	 */
	public Report getLastReport(String name) throws BatchRunnerClientException {
		final WebClient client = createWebClient(10000);    // 10 secondes
		path(client, JobAction.REPORT, name);

		try {
			final Response response = client.accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get();
			if (response.getStatus() >= 400) {
				final String message = response.readEntity(String.class);
				throw new WebApplicationException(message, response);
			}
			if (response.getStatus() == HttpURLConnection.HTTP_NO_CONTENT) {
				return null;
			}

			final ContentDisposition cd = new ContentDisposition((String) response.getMetadata().getFirst(CONTENT_DISPOSITION));
			final String filename = StringUtils.defaultIfBlank(cd.getParameter("filename"), String.format("%s-report-data", name));
			return new Report((InputStream) response.getEntity(), filename);
		}
		catch (WebApplicationException e) {
			throw new BatchRunnerClientException(String.format("HTTP error code %d received from the server", e.getResponse().getStatus()), e);
		}
		catch (Exception e) {
			throw new BatchRunnerClientException(e);
		}
	}
}
