/**
 *
 */
package ch.vd.uniregctb.tiers.view;

import ch.vd.uniregctb.common.BaseComparator;

/**
 * Tri des adresses par type (Courrier, Représentation, Poursuite, Domicile), puis par dates croissantes.
 */
public final class AdresseViewComparator extends BaseComparator<AdresseView> {

	public AdresseViewComparator() {
		super(new String[] { "usage", "dateDebut" },
                new Boolean[] { true, false});
	}

}