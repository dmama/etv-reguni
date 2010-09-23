package ch.vd.uniregctb.general.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

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
	public TiersGeneralView get(Tiers tiers, boolean full) ;

	/**
	 * Alimente un cartouche DPI étendu
	 *
	 * @param dpi
	 * @param etendu
	 * @return
	 */
	@Transactional(readOnly = true)
	public TiersGeneralView get(DebiteurPrestationImposable dpi, boolean etendu) ;

	/**
	 * Alimente le carouche d'une personne physique
	 *
	 * @param pp
	 * @param etendu
	 * @return
	 */
	@Transactional(readOnly = true)
	public TiersGeneralView get(PersonnePhysique pp, boolean etendu) ;
}
