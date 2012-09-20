package ch.vd.uniregctb.tiers.view;

import java.util.Arrays;
import java.util.Comparator;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.GentilComparator;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Tri des adresses civiles par type (Courrier, Représentation, Poursuite, Domicile), puis par dates croissantes.
 */
public final class AdresseCivilViewComparator implements Comparator<AdresseCivilView> {

	//UNIREG-1813 la liste des types d'adresse est donnée dans l'ordre suivant: Domicile(P), Courrier(C), Secondaire(S).  P correspondant à Principal
	private static final Comparator<TypeAdresseCivil> usageComparator =
			new GentilComparator<TypeAdresseCivil>(Arrays.asList(TypeAdresseCivil.PRINCIPALE, TypeAdresseCivil.COURRIER, TypeAdresseCivil.SECONDAIRE));

	@Override
	public int compare(AdresseCivilView o1, AdresseCivilView o2) {

		int compare = usageComparator.compare(o1.getUsageCivil(), o2.getUsageCivil());

		if (compare == 0) {
			// on trie par ordre décroissant des dates
			compare = - DateRangeComparator.compareRanges(o1, o2);
		}

		return compare;
	}

}