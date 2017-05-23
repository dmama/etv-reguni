package ch.vd.uniregctb.webservices.batch;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamDynamicEnum;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamType;
import ch.vd.uniregctb.ubr.ErrorData;
import ch.vd.uniregctb.ubr.JobConstants;
import ch.vd.uniregctb.ubr.JobDescription;
import ch.vd.uniregctb.ubr.JobNames;
import ch.vd.uniregctb.ubr.JobParamDescription;
import ch.vd.uniregctb.ubr.JobStatus;
import ch.vd.uniregctb.ubr.JobWaitInformation;

public class WebServiceEndPoint implements WebService {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceEndPoint.class);
	private static final Logger GET_LOGGER = LoggerFactory.getLogger("ws.batch.get");
	private static final Logger POST_LOGGER = LoggerFactory.getLogger("ws.batch.post");
	private static final Logger OTHER_LOGGER = LoggerFactory.getLogger("ws.batch.other");

	private static final String BATCH_VISA = "[Batch WS]";

	private BatchScheduler batchScheduler;
	private DocumentService documentService;

	@Context
	private MessageContext messageContext;

	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

	private Response doAndTrace(Supplier<Response> executor) {
		final long start = System.nanoTime();
		final HttpServletRequest request = messageContext.getHttpServletRequest();
		Response response = null;
		try {
			response = executor.get();
		}
		catch (Throwable e) {
			response = Response.serverError().build();
			LOGGER.error(String.format("Erreur lors de l'exécution de la méthode %s", getURL(request)), e);
		}
		finally {
			final long end = System.nanoTime();
			final Response.Status status = response != null ? Response.Status.fromStatusCode(response.getStatus()) : Response.Status.NO_CONTENT;
			final String statusString = (status == null ? StringUtils.EMPTY : String.format(", status='%d %s'", status.getStatusCode(), status.getReasonPhrase()));
			final Logger logger = getLogger(request);
			logger.info(String.format("[%s] (%d ms) %s%s",
			                          getBasicAuthenticationUser(request),
			                          TimeUnit.NANOSECONDS.toMillis(end - start),
			                          getURL(request),
			                          statusString));
		}
		return response;
	}

	private static String getBasicAuthenticationUser(HttpServletRequest request) {
		final Principal principal = request.getUserPrincipal();
		return principal == null ? "n/a" : principal.getName();
	}

	private static String getURL(HttpServletRequest request) {
		final String queryString = request.getQueryString();
		if (StringUtils.isBlank(queryString)) {
			return String.format("%s%s", request.getServletPath(), request.getPathInfo());
		}
		else {
			return String.format("%s%s?%s", request.getServletPath(), request.getPathInfo(), queryString);
		}
	}

	private static Logger getLogger(HttpServletRequest request) {
		switch (request.getMethod()) {
		case "GET":
			return GET_LOGGER;
		case "POST":
			return POST_LOGGER;
		default:
			return OTHER_LOGGER;
		}
	}

	@Override
	public Response getJobs() {
		return doAndTrace(() -> {
			final List<JobDefinition> jobs = batchScheduler.getSortedJobs();
			final List<String> names = new ArrayList<>(jobs.size());
			for (JobDefinition job : jobs) {
				if (job.isVisible()) {
					names.add(job.getName());
				}
			}
			return Response.ok(new JobNames(names), APPLICATION_JSON_WITH_UTF8_CHARSET).build();
		});
	}

	@Override
	public Response getJobDescription(final String jobName) {
		return doAndTrace(() -> {
			final JobDefinition job = batchScheduler.getJob(jobName);
			if (job == null) {
				return buildUnknownBatchNameResponse(jobName);
			}

			final JobDescription description = buildDescription(job);
			return Response.ok(description, APPLICATION_JSON_WITH_UTF8_CHARSET).build();
		});
	}

	private static JobDescription buildDescription(JobDefinition job) {

		// d'abord les paramètres
		final List<JobParamDescription> destParams;
		final List<JobParam> paramDefinitions = job.getParamDefinition();
		if (paramDefinitions.isEmpty()) {
			destParams = Collections.emptyList();
		}
		else {
			destParams = new ArrayList<>(paramDefinitions.size());
			for (JobParam param : paramDefinitions) {
				if (param.isEnabled()) {
					final Collection<String> allowedValues;
					final JobParamType type = param.getType();
					if (type instanceof JobParamEnum) {
						allowedValues = Arrays.stream(type.getConcreteClass().getEnumConstants())
								.map(Enum.class::cast)
								.map(Enum::name)
								.collect(Collectors.toList());
					}
					else if (type instanceof JobParamDynamicEnum) {
						allowedValues = ((JobParamDynamicEnum<?>) type).getAllowedValues().stream()
								.map(type::valueToString)
								.collect(Collectors.toList());
					}
					else {
						allowedValues = null;
					}
					destParams.add(new JobParamDescription(param.getName(), param.isMandatory(), type.getConcreteClass(), allowedValues));
				}
			}
		}

		// puis le reste
		return new JobDescription(job.getName(), job.getDescription(),
		                          mapStatus(job.getStatut()),
		                          job.getLastStart(),
		                          job.getLastEnd(),
		                          job.getRunningMessage(),
		                          destParams);
	}

	private static JobStatus mapStatus(JobDefinition.JobStatut statut) {
		if (statut == null) {
			return null;
		}
		switch (statut) {
		case JOB_EXCEPTION:
			return JobStatus.EXCEPTION;
		case JOB_INTERRUPTED:
			return JobStatus.INTERRUPTED;
		case JOB_INTERRUPTING:
			return JobStatus.INTERRUPTING;
		case JOB_OK:
			return JobStatus.OK;
		case JOB_RUNNING:
			return JobStatus.RUNNING;
		default:
			throw new IllegalArgumentException("Unknown value : " + statut);
		}
	}

	private static JobDefinition.JobStatut mapStatut(JobStatus status) {
		if (status == null) {
			return null;
		}
		switch (status) {
		case EXCEPTION:
			return JobDefinition.JobStatut.JOB_EXCEPTION;
		case INTERRUPTED:
			return JobDefinition.JobStatut.JOB_INTERRUPTED;
		case INTERRUPTING:
			return JobDefinition.JobStatut.JOB_INTERRUPTING;
		case OK:
			return JobDefinition.JobStatut.JOB_OK;
		case RUNNING:
			return JobDefinition.JobStatut.JOB_RUNNING;
		default:
			throw new IllegalArgumentException("Unknown value : " + status);
		}
	}

	private static Response buildUnknownBatchNameResponse(String jobName) {
		return Response.status(Response.Status.NOT_FOUND)
				.type(APPLICATION_JSON_WITH_UTF8_CHARSET)
				.entity(new ErrorData(String.format("Job '%s' not found", jobName)))
				.build();
	}

	@Override
	public Response startJob(final String jobName, final MultipartBody body) {
		return doAndTrace(() -> {
			final JobDefinition job = batchScheduler.getJob(jobName);
			if (job == null) {
				return buildUnknownBatchNameResponse(jobName);
			}

			// le body posté peut contenir les "parts" suivantes :
			// - une part au format application/json nommée "simpleParameters" pour les paramètres dits simples (= normaux : entiers, dates, strings, enums...)
			// - une part de type application/octet-stream pour chaque paramètre de type "file" nommée comme le paramètre

			AuthenticationHelper.pushPrincipal(BATCH_VISA);
			try {
				final List<Attachment> attachments = body.getAllAttachments();
				final Map<String, Object> parameters = new HashMap<>();
				for (Attachment attachment : attachments) {
					final ContentDisposition disposition = attachment.getContentDisposition();
					final String name = disposition != null ? disposition.getParameter("name") : null;
					if (StringUtils.isBlank(name)) {
						return Response.status(Response.Status.BAD_REQUEST)
								.type(APPLICATION_JSON_WITH_UTF8_CHARSET)
								.entity(new ErrorData("Parts in multipart data should be named using the 'name' field in the 'Content-Disposition' header"))
								.build();
					}
					if (JobConstants.SIMPLE_PARAMETERS_PART_NAME.equals(name)) {
						//noinspection unchecked
						final Map<String, Object> simple = attachment.getObject(HashMap.class);
						parameters.putAll(simple);
					}
					else {
						try (InputStream is = attachment.getDataHandler().getInputStream();
						     ByteArrayOutputStream os = new ByteArrayOutputStream()) {

							IOUtils.copy(is, os);
							os.flush();
							parameters.put(name, os.toByteArray());
						}
					}
				}

				// reconstitution d'une map d'éléments typés comme attendu par le job
				final Map<String, Object> typedParameters = new HashMap<>(parameters.size());
				for (Map.Entry<String, Object> untyped : parameters.entrySet()) {
					final JobParam paramDefinition = job.getParameterDefinition(untyped.getKey());
					final Object parameterValue = untyped.getValue();
					if (paramDefinition != null && parameterValue != null) {
						final JobParamType parameterType = paramDefinition.getType();
						final Class<?> expectedType = parameterType.getConcreteClass();
						final Object typedValue;
						if (expectedType.isAssignableFrom(parameterValue.getClass())) {
							typedValue = parameterValue;
						}
						else if (expectedType == byte[].class) {
							// on attendait un fichier, mais autre chose nous arrive...
							return Response.status(Response.Status.BAD_REQUEST)
									.type(APPLICATION_JSON_WITH_UTF8_CHARSET)
									.entity(new ErrorData("File-typed parameter " + untyped.getKey() + " is not well-formed"))
									.build();
						}
						else {
							// on passe par la représentation en String
							final String stringReprentation = parameterValue instanceof String ? (String) parameterValue : parameterValue.toString();
							typedValue = parameterType.stringToValue(stringReprentation);
						}
						typedParameters.put(untyped.getKey(), typedValue);
					}
					else {
						// on ignore les paramètres donnés dont le batch ne veut pas, et ceux dont la valeur est "null"
					}
				}

				batchScheduler.startJob(jobName, typedParameters);
				return Response.status(Response.Status.CREATED).build();
			}
			catch (JobAlreadyStartedException e) {
				return Response.status(Response.Status.PRECONDITION_FAILED).build();
			}
			catch (Exception e) {
				LOGGER.error("Exception levée au démarrage du job " + jobName, e);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		});
	}

	@Override
	public Response stopJob(final String jobName) {
		return doAndTrace(() -> {
			final JobDefinition job = batchScheduler.getJob(jobName);
			if (job == null) {
				return buildUnknownBatchNameResponse(jobName);
			}

			try {
				batchScheduler.stopJob(jobName, null);
				return Response.ok().build();
			}
			catch (Exception e) {
				LOGGER.error("Erreur lors de l'arrêt du job " + jobName, e);
				return Response.serverError().build();
			}
		});
	}

	@Override
	public Response getJobStatus(final String jobName) {
		return doAndTrace(() -> {
			final JobDefinition job = batchScheduler.getJob(jobName);
			if (job == null || job.getStatut() == null) {
				return buildUnknownBatchNameResponse(jobName);
			}
			return Response.ok(mapStatus(job.getStatut()).name(), TEXT_PLAIN_WITH_UTF8_CHARSET).build();
		});
	}

	@Override
	public Response waitJob(String jobName, Set<JobStatus> statuses, boolean havingStatus, long timeout) {
		return doAndTrace(() -> {
			final Set<JobStatus> given = EnumSet.noneOf(JobStatus.class);
			if (statuses != null) {
				given.addAll(statuses);
			}

			final Set<JobStatus> having;
			if (havingStatus) {
				having = given;
			}
			else {
				having = EnumSet.allOf(JobStatus.class);
				having.removeAll(given);
			}

			final JobDefinition job = batchScheduler.getJob(jobName);
			if (job == null) {
				return buildUnknownBatchNameResponse(jobName);
			}

			final Set<JobDefinition.JobStatut> internalHaving = having.stream()
					.map(WebServiceEndPoint::mapStatut)
					.collect(Collectors.toCollection(() -> EnumSet.noneOf(JobDefinition.JobStatut.class)));
			try {
				final JobDefinition.JobStatut etatAtteint = job.waitForStatusIn(internalHaving, Duration.ofMillis(timeout));
				return Response.ok(new JobWaitInformation(JobWaitInformation.JobWaitStatus.EXPECTED_STATUS_REACHED, mapStatus(etatAtteint), job.getRunningMessage()), APPLICATION_JSON_WITH_UTF8_CHARSET).build();
			}
			catch (JobDefinition.TimeoutExpiredException e) {
				return Response.ok(new JobWaitInformation(JobWaitInformation.JobWaitStatus.TIMEOUT_OCCURRED, mapStatus(e.statut), job.getRunningMessage()), APPLICATION_JSON_WITH_UTF8_CHARSET).build();
			}
			catch (RuntimeException | Error e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Response getLastJobReport(final String jobName) {
		return doAndTrace(() -> {
			final JobDefinition job = batchScheduler.getJob(jobName);
			if (job == null) {
				return buildUnknownBatchNameResponse(jobName);
			}

			final Document document = job.getLastRunReport();
			if (document == null) {
				return Response.noContent().build();
			}

			final Mutable<byte[]> content = new MutableObject<>();
			try {
				documentService.readDoc(document, new DocumentService.ReadDocCallback<Document>() {
					@Override
					public void readDoc(Document doc, InputStream is) throws Exception {
						try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
							IOUtils.copy(is, bos);
							bos.flush();
							content.setValue(bos.toByteArray());
						}
					}
				});
			}
			catch (Exception e) {
				LOGGER.error("Erreur à la récupération du dernier rapport du job " + jobName, e);
				return Response.serverError().build();
			}

			return Response.ok(content.getValue(), MediaType.APPLICATION_OCTET_STREAM)
					.header("Content-Disposition", String.format("attachment; filename=\"%s\"", document.getFileName()))
					.header("Content-Length", document.getFileSize())
					.build();
		});
	}
}
