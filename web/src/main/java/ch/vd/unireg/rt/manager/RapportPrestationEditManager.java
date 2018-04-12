package ch.vd.unireg.rt.manager;

import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.tiers.view.RapportsPrestationView;

public interface RapportPrestationEditManager {

	/**
	 * Alimente la vue RapportPrestationView
	 */
	RapportPrestationView get (Long numeroSrc, Long numeroDpi, String provenance) ;

	/**
	 * Persiste le rapport de travail
	 */
	void save(RapportPrestationView rapportView) ;

	/**
	 * @param tiersId le numéro du tiers dont on aimerait s'assurer de l'existence
	 * @return <code>true</code> si le tiers existe bien, <code>false</code> sinon
	 */
	boolean isExistingTiers(long tiersId);

	/**
	 * @return  le nombre de rapports prestation imposable pour un débiteur
	 */
	int countRapportsPrestationImposable(Long numeroDebiteur, boolean rapportsPrestationHisto);

	void fillRapportsPrestationView(long noDebiteur, RapportsPrestationView view);
}
