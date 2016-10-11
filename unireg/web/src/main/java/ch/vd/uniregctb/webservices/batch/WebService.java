package ch.vd.uniregctb.webservices.batch;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;

import ch.vd.uniregctb.ubr.JobConstants;
import ch.vd.uniregctb.ubr.JobStatus;

public interface WebService {

	String APPLICATION_JSON_WITH_UTF8_CHARSET = MediaType.APPLICATION_JSON + "; charset=UTF-8";
	String TEXT_PLAIN_WITH_UTF8_CHARSET = MediaType.TEXT_PLAIN + "; charset=UTF-8";

	@GET
	@Produces(APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/jobs")
	@Description(value = "Liste des noms des jobs existants", target = DocTarget.METHOD)
	Response getJobs();

	@GET
	@Produces(APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/job/{name}/description")
	@Description(value = "Description d'un job, de ses paramètres", target = DocTarget.METHOD)
	Response getJobDescription(@PathParam("name") String jobName);

	@POST
	@Consumes(JobConstants.MULTIPART_MIXED)
	@Produces(APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/job/{name}/start")
	@Description(value = "Démarrage de job, la donnée postée est un multipart, chacune des parts étant nommée (au moyen de la propriété name du champ Content-Disposition)", target = DocTarget.METHOD)
	Response startJob(@PathParam("name") String jobName, MultipartBody body);

	@POST
	@Produces(APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/job/{name}/stop")
	@Description(value = "Demande d'arrêt d'un job en cours", target = DocTarget.METHOD)
	Response stopJob(@PathParam("name") String jobName);

	@GET
	@Produces(TEXT_PLAIN_WITH_UTF8_CHARSET)
	@Path("/job/{name}/status")
	@Description(value = "Etat d'un job", target = DocTarget.METHOD)
	Response getJobStatus(@PathParam("name") String jobName);

	@GET
	@Produces(APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/job/{name}/wait")
	@Description(value = "Attente d'un état spécifique sur un job", target = DocTarget.METHOD)
	Response waitJob(@PathParam("name") String jobName,
	                 @QueryParam("status") Set<JobStatus> statuses,
	                 @QueryParam("havingStatus") @DefaultValue("true") boolean havingStatus,
	                 @QueryParam("timeout") long timeout);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("/job/{name}/report")
	@Description(value = "Récupération du dernier rapport d'exécution (PDF) d'un job", target = DocTarget.METHOD)
	Response getLastJobReport(@PathParam("name") String jobName);
}
