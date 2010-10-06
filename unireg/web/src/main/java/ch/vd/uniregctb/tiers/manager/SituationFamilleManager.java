package ch.vd.uniregctb.tiers.manager;

import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;


/**
 * Service à disposition du controller TiersSituationFamilleController
 *
 * @author xcifde
 *
 */
public interface SituationFamilleManager {

	/**
	 * Annule une situation de famille
	 *
	 * @param idSituationFamille
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerSituationFamille(Long idSituationFamille);

	/**
	 * Cree une nouvelle vue SituationFamilleView
	 *
	 * @param numeroCtb
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public SituationFamilleView create(Long numeroCtb) throws AdresseException;

	/**
	 * Sauvegarde de la situation de famille
	 *
	 * @param situationFamilleView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(SituationFamilleView situationFamilleView) ;

}
