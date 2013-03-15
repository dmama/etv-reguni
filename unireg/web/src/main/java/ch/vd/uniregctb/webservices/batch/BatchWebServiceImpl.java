package ch.vd.uniregctb.webservices.batch;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.document.DocumentService.ReadDocCallback;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamType;

@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", name = "BatchPort", serviceName = "BatchService")
public class BatchWebServiceImpl implements BatchWebService {

	private static final String BATCH_VISA = "[Batch WS]";

	private BatchScheduler batchScheduler;
	private DocumentService documentService;

	protected final Logger LOGGER = Logger.getLogger(BatchWebServiceImpl.class);

	@Override
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public void start(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", partName = "params", name = "StartBatch") StartBatch params)
			throws BatchWSException {

		AuthenticationHelper.pushPrincipal(BATCH_VISA);
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(params);
			}

			final JobDefinition job = batchScheduler.getJob(params.name);
			if (job == null) {
				throw new BatchWSException("Batch Name incorrect");
			}

			final Map<String, Object> h;
			if (params.params != null && !params.params.isEmpty()) {

				h = new HashMap<>();
				for (ParamMapEntry entry : params.params.entries) {

					final String key = entry.key;

					final JobParam param = job.getParameterDefinition(key);
					if (param == null) {
						throw new BatchWSException("Le paramètre [" + key + "] est inconnu.");
					}

					final Object value;

					final JobParamType type = param.getType();
					if (type instanceof JobParamFile) {
						// paramètre de type fichier
						value = getFileBytes(entry.bytesValue);
					}
					else {
						// paramètre normal
						final String strValue = entry.value;
						value = type.stringToValue(strValue);
					}

					h.put(key, value);
				}
			}
			else {
				h = null;
			}
			batchScheduler.startJob(params.name, h);
		}
		catch (JobAlreadyStartedException e) {
			throw new BatchWSException("The job  is already started");
		}
		catch (BatchWSException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new BatchWSException(e.getMessage());
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private byte[] getFileBytes(DataHandler dataHandler) throws BatchWSException {
		final byte[] value;
		if (dataHandler == null) {
			value = null;
		}
		else {
	        InputStream is = null;
			try {
				is = dataHandler.getInputStream();
				value = IOUtils.toByteArray(is);
			}
			catch (IOException e) {
				throw new BatchWSException("Impossible de lire le paramètre de type fichier", e);
			}
			finally {
				IOUtils.closeQuietly(is);
			}
		}
		return value;
	}

	@Override
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public List<JobName> getListJobs() {

		AuthenticationHelper.pushPrincipal(BATCH_VISA);
		try {
			final List<JobName> listeJobDefinition = new ArrayList<>();
			final List<ch.vd.uniregctb.scheduler.JobDefinition> values = batchScheduler.getSortedJobs();

			for (ch.vd.uniregctb.scheduler.JobDefinition jobDefinition : values) {
				listeJobDefinition.add(new JobName(jobDefinition.getName()));
			}
			return listeJobDefinition;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public void stop(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", partName = "params", name = "StopBatch") StopBatch params)
			throws BatchWSException {

		AuthenticationHelper.pushPrincipal(BATCH_VISA);
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(params);
			}
			batchScheduler.stopJob(params.name);
		}
		catch (SchedulerException e) {
			throw new BatchWSException(e.getMessage());
		}
		catch (IllegalArgumentException e) {
			throw new BatchWSException("Nom du Batch incorrect");
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

	}

	@Override
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public ch.vd.uniregctb.webservices.batch.JobDefinition getJobDefinition(JobDefParam arg) {

		AuthenticationHelper.pushPrincipal(BATCH_VISA);
		try {
			final Map<String, JobDefinition> values = batchScheduler.getJobs();
			final JobDefinition jobDefinition = values.get(arg.name);
			if (jobDefinition == null) {
				return null;
			}

			final ch.vd.uniregctb.webservices.batch.JobDefinition def = new ch.vd.uniregctb.webservices.batch.JobDefinition(jobDefinition);
			return def;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public Report getLastReport(LastReport params) throws BatchWSException {

		AuthenticationHelper.pushPrincipal(BATCH_VISA);
		try {

			final JobDefinition job = batchScheduler.getJob(params.name);
			if (job == null) {
				return null;
			}

			final Document document = job.getLastRunReport();
			if (document == null) {
				return null;
			}

			// On rempli le rapport
			final Report report = new Report(document);
			try {
				documentService.readDoc(document, new ReadDocCallback<Document>() {
					@Override
					public void readDoc(Document doc, InputStream is) throws Exception {

						byte[] data = new byte[(int) doc.getFileSize()];
						is.read(data);

						report.contentByteStream = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
					}
				});
			}
			catch (Exception e) {
				throw new BatchWSException("Impossible de lire le rapport sur le dique", e);
			}

			return report;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}
}
