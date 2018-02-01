package ch.vd.unireg.general.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;

/**
 * Methodes relatives au cartouche contribuable
 * @author xcifde
 *
 */
public interface TiersGeneralManager {

	/**
	 * Alimente TiersGeneralView en fonction du tiers
	 * @param tiers tiers dont on veut connaître les détails
	 * @param full <code>true</code> si les états-civils, événements civils, validation... sont aussi concernés, <code>false</code> si on ne s'intéresse en gros qu'à son nom, son rôle, son adresse
	 * @return
	 */
	@Transactional(readOnly = true)
	TiersGeneralView getTiers(Tiers tiers, boolean full) ;

	/**
	 * Alimente un cartouche DPI étendu
	 *
	 * @param dpi
	 * @param etendu
	 * @return
	 */
	@Transactional(readOnly = true)
	TiersGeneralView getDebiteur(DebiteurPrestationImposable dpi, boolean etendu) ;

	/**
	 * Alimente le cartouche d'une personne physique
	 *
	 * @param pp
	 * @param etendu
	 * @return
	 */
	@Transactional(readOnly = true)
	TiersGeneralView getPersonnePhysique(PersonnePhysique pp, boolean etendu) ;
}
