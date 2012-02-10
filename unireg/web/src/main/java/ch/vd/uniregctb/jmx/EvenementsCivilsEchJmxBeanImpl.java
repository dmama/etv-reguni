package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchReceptionMonitor;

@ManagedResource
public class EvenementsCivilsEchJmxBeanImpl extends EvenementsCivilsJmxBeanImpl implements EvenementsCivilsEchJmxBean {

	private EvenementCivilEchReceptionMonitor monitor;

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(EvenementCivilEchReceptionMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	@ManagedAttribute
	public int getNbMeaningfullEventsReceived() {
		return monitor.getNombreEvenementsNonIgnores();
	}

	@Override
	@ManagedAttribute
	public int getNbIndividualsAwaitingTreatment() {
		return monitor.getNombreIndividusEnAttenteDeTraitement();
	}
}
