package ch.vd.uniregctb.webservices.batch.impl;

import java.util.ArrayList;
import java.util.Collection;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.batch.JobStatut;
import ch.vd.uniregctb.webservices.batch.Param;


/**
 * Cette helper effectue la traduction des classes venant de 'core' en classes 'web'.
 * <p/>
 * De manière naturelle, ces méthodes auraient dû se trouver dans les classes 'web' correspondantes, mais cela provoque des erreurs (les classes 'core' sont aussi inspectées et le fichier se retrouve
 * avec des structures ayant le même nom définies plusieurs fois) lors la génération du WSDL par CXF.
 */
public class DataHelper {

	public static boolean coreToWeb(Boolean value) {
		return value != null && value;
	}

	public static Collection<Param> coreToWeb(ch.vd.uniregctb.scheduler.JobDefinition job) {

		if (job == null) {
			return null;
		}

		Collection<Param> listeParamWeb = new ArrayList<Param>();
		for (ch.vd.uniregctb.scheduler.JobParam param : job.getParamDefinition()) {

			Param parametre = new Param(job, param);
			listeParamWeb.add(parametre);
		}

		return listeParamWeb;
	}

	public static JobStatut coreToWeb(ch.vd.uniregctb.scheduler.JobDefinition.JobStatut jobStatut) {
		if (jobStatut == null) {
			return null;
		}

		final JobStatut value = JobStatut.fromValue(jobStatut.name());
		Assert.notNull(value);
		return value;
	}
}