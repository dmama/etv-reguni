package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

public interface EvenementsCivilsEchJmxBean extends EvenementsCivilsJmxBean {

	@ManagedAttribute(description = "Total number of individuals currently waiting to be processed")
	int getNbIndividualsAwaitingTreatment();

}
