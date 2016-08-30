package ch.vd.uniregctb.webservices.batch;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collection;

import ch.vd.uniregctb.scheduler.JobDefinition.JobSynchronousMode;
import ch.vd.uniregctb.webservices.batch.impl.DataHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "jobDefinition", propOrder = {
		"lastEnd",
		"lastStart",
		"runningMessage",
		"sortOrder",
		"statut",
		"name",
		"description",
		"synchronousMode",
		"params"
})
public class JobDefinition {

	@XmlElement(required = true)
	public String name;

	@XmlElement(required = true)
	public JobStatut statut;

	@XmlElement(required = false)
	public java.util.Date lastEnd;

	@XmlElement(required = false)
	public java.util.Date lastStart;

	@XmlElement(required = false)
	public String runningMessage;

	@XmlElement(required = false)
	public int sortOrder;

	@XmlElement(required = false)
	public String description;

	@XmlElement(required = false)
	public JobSynchronousMode synchronousMode;

	@XmlElement(required = false)
	public Collection<ch.vd.uniregctb.webservices.batch.Param> params;


	public JobDefinition(ch.vd.uniregctb.scheduler.JobDefinition job) {
		lastEnd = job.getLastEnd();
		lastStart = job.getLastStart();
		runningMessage = job.getRunningMessage();
		sortOrder = job.getSortOrder();
		statut = DataHelper.coreToWeb(job.getStatut());
		name = job.getName();
		synchronousMode = job.getSynchronousMode();
		description = job.getDescription();
		params = DataHelper.coreToWeb(job);
	}

	public JobDefinition() {
	}

}
