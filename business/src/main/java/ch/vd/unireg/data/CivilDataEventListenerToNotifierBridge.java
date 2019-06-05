package ch.vd.unireg.data;

/**
 * Bridge pour convertir les événements de modification reçus par l'interface {@link CivilDataEventListener} en appels de méthodes sur un bean qui implémente l'interface {@link CivilDataEventNotifier}.
 */
public class CivilDataEventListenerToNotifierBridge implements CivilDataEventListener {

	private CivilDataEventNotifier target;

	public void setTarget(CivilDataEventNotifier target) {
		this.target = target;
	}

	@Override
	public void onEntrepriseChange(long id) {
		target.notifyEntrepriseChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		target.notifyIndividuChange(id);
	}
}
