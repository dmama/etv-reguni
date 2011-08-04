package ch.vd.uniregctb.checker;

public interface ServiceChecker {

	/**
	 * @return le statut du service.
	 */
	Status getStatus();

	/**
	 * @return un message détaillant le problème lorsque le statut est KO.
	 */
	String getStatusDetails();
}
