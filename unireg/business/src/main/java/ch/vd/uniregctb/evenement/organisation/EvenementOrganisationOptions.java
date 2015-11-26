package ch.vd.uniregctb.evenement.organisation;

/**
 * Différentes options de comportement disponibles dans le traitement des événements organisation.
 */
public class EvenementOrganisationOptions {

	private final boolean seulementEvtFiscaux;

	public EvenementOrganisationOptions(boolean seulementEvtFiscaux) {
		this.seulementEvtFiscaux = seulementEvtFiscaux;
	}

	/**
	 * Signale que les événements internes ayant un impact Unireg ne doivent pas être exécutés.
	 * handle() doit retourner sans effet.
	 * @return
	 */
	public boolean isSeulementEvtFiscaux() {
		return seulementEvtFiscaux;
	}
}
