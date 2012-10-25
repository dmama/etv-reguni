package ch.vd.uniregctb.rt.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.rt.view.DebiteurListView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.rt.view.SourcierListView;
import ch.vd.uniregctb.type.Niveau;

public interface RapportPrestationEditManager {

	/**
	 * Alimente la vue RapportPrestationView
	 *
	 * @param numeroSrc
	 * @param numeroDpi
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	RapportPrestationView get (Long numeroSrc, Long numeroDpi, String provenance) ;

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(RapportPrestationView rapportView) ;


	/**
	 * Charge l'écran de recherche débiteurs pour un sourcier
	 *
	 * @param numeroSrc
	 * @return
	 */
	@Transactional(readOnly = true)
	DebiteurListView getDebiteurList(Long numeroSrc) ;


	/**
	 * Charge l'écran de recherche sourciers pour un debiteur
	 *
	 * @param numeroDpi
	 * @return
	 */
	@Transactional(readOnly = true)
	SourcierListView getSourcierList(Long numeroDpi) ;

	/**
	 * @param tiersId le numéro du tiers qui nous intéresse
	 * @return le niveau d'accès autorisé par l'utilisateur actuellement connecté au débiteur
	 */
	@Transactional(readOnly = true)
	Niveau getAccessLevel(long tiersId);

	/**
	 * @param tiersId le numéro du tiers dont on aimerait s'assurer de l'existence
	 * @return <code>true</code> si le tiers existe bien, <code>false</code> sinon
	 */
	@Transactional(readOnly = true)
	boolean isExistingTiers(long tiersId);
}
