package ch.vd.uniregctb.tiers.view;

import java.util.Arrays;
import java.util.Comparator;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.GentilComparator;
import ch.vd.uniregctb.type.TypeAdresseCivil;

/**
 * Tri des adresses civiles par type (Courrier, Représentation, Poursuite, Domicile), puis par dates décroissantes.
 */
public final class AdresseCivilViewComparator implements Comparator<AdresseCivilView> {

	//UNIREG-1813 la liste des types d'adresse est donnée dans l'ordre suivant: Domicile(P), Courrier(C), Secondaire(S), Case Postale (B).  P correspondant à Principal
	private static final Comparator<TypeAdresseCivil> usageComparator =
			new GentilComparator<>(Arrays.asList(TypeAdresseCivil.PRINCIPALE, TypeAdresseCivil.COURRIER, TypeAdresseCivil.SECONDAIRE, TypeAdresseCivil.CASE_POSTALE));

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