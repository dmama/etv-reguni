package ch.vd.uniregctb.webservices.batch;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", name = "BatchPort", serviceName = "BatchService")
public interface BatchWebService {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "StartBatch")
	public class StartBatch {

		@XmlElement(required = true)
		public String name;

		@XmlElement(required = true)
		public ParamMap params;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "StopBatch")
	public class StopBatch {

		@XmlElement(required = true)
		public String name;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "JobDefParam")
	public class JobDefParam {

		@XmlElement(required = true)
		public String name;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "LastReport")
	public class LastReport {

		@XmlElement(required = true)
		public String name;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "JobName")
	public class JobName {

		@XmlElement(required = true)
		public String name;

		public JobName() {
		}

		public JobName(String name) {
			this.name = name;
		}
	}

	/**
	 * Démarre le job spécifié. En fonction du type de job, l'appel retourne immédiatement (=exécution asynchrone) ou attend que le job soit
	 * terminé (=exécution synchrone).
	 *
	 * @param params
	 *            le nom du job et les paramètres à utiliser
	 * @throws BatchWSException
	 *             en cas d'erreur lors du démarrage du job
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public void start(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", partName = "params", name = "StartBatch") StartBatch params)
			throws BatchWSException;

	/**
	 * Stoppe le job spécifié. La méthode ne retourne que lorsque le job est véritablement stoppé.
	 *
	 * @param params
	 *            le nom du job à stopper.
	 * @throws BatchWSException
	 *             en cas d'erreur lors de l'arrêt du job
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public void stop(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", partName = "params", name = "StopBatch") StopBatch params)
			throws BatchWSException;

	/**
	 * @return la liste des jobs disponibles.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public List<JobName> getListJobs();

	/**
	 * @param params
	 *            le nom du job à détailler.
	 * @return les informations complètes (paramètres, état, ...) d'un job.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public JobDefinition getJobDefinition(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", partName = "params", name = "JobDefParam") JobDefParam params);

	/**
	 * @param params
	 *            le nom du job dont on veut obtenir le dernier rapport.
	 * @return le dernier rapport d'exécution du job spécifié; <b>null</b> si le job n'a pas été exécuté depuis le démarrage de
	 *         l'application.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch")
	public Report getLastReport(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/batch", partName = "params", name = "LastReport") LastReport params) throws BatchWSException;
}
