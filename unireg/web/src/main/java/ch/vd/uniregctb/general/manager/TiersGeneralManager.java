package ch.vd.uniregctb.general.manager;

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
	 * @param tiers
	 * @return
	 */
	public TiersGeneralView get(Tiers tiers) ;

	/**
	 * Alimente un cartouche DPI étendu
	 *
	 * @param dpi
	 * @param etendu
	 * @return
	 */
	public TiersGeneralView get(DebiteurPrestationImposable dpi, boolean etendu) ;

	/**
	 * Alimente le carouche d'une personne physique
	 *
	 * @param pp
	 * @param etendu
	 * @return
	 */
	public TiersGeneralView get(PersonnePhysique pp, boolean etendu) ;
}
