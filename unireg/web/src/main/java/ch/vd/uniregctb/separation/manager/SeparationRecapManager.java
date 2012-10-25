package ch.vd.uniregctb.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.separation.view.SeparationRecapView;


public interface SeparationRecapManager {

	/**
	 * Alimente la vue SeparationRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	SeparationRecapView get(Long numero);


	/**
	 * Persiste le rapport
	 *
	 * @param separationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(SeparationRecapView separationRecapView) throws MetierServiceException;

	/**
	 * @param noTiers le numéro du tiers dont on veut connaître l'activité au niveau des fors principaux
	 * @return <code>true</code> si le tiers possède un for principal actif (= non-fermé), <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isAvecForFiscalPrincipalActif(long noTiers);

}
