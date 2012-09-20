/**
 *
 */
package ch.vd.uniregctb.tiers.view;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ch.vd.uniregctb.common.GentilComparator;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Tri des adresses par type (Courrier, Représentation, Poursuite, Domicile), puis par dates croissantes.
 */
public final class AdresseViewComparator implements Comparator<AdresseView> {
	//UNIREG-1813 la liste des types d'adresse est donnée dans l'ordre suivant:
	//Domicile(P), Courrier(C), Secondaire(S).  P correspondant à Principal
	private static final List<TypeAdresseCivil> ordreUsageAdresseCivile = Arrays.asList(TypeAdresseCivil.PRINCIPALE,
			TypeAdresseCivil.COURRIER,
			TypeAdresseCivil.SECONDAIRE);
	private static final List<TypeAdresseTiers> ordreUsageAdresseFiscale = Arrays.asList(TypeAdresseTiers.COURRIER,
			TypeAdresseTiers.REPRESENTATION,
			TypeAdresseTiers.POURSUITE);
	private static final Comparator<TypeAdresseCivil> comparatorUsageAdresseCivile = new GentilComparator<TypeAdresseCivil>(ordreUsageAdresseCivile);

	private static final Comparator<TypeAdresseTiers> comparatorUsageAdresseFiscal = new GentilComparator<TypeAdresseTiers>(ordreUsageAdresseFiscale);

	private static <T extends Comparable<T>> int compareNullable(T o1, T o2, boolean nullAtEnd) {
		if (o1 == o2) {
			return 0;
		}
		else if (o1 == null) {
			return nullAtEnd ? -1 : 1;
		}
		else if (o2 == null) {
			return nullAtEnd ? 1 : -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}

	@Override
	public int compare(AdresseView o1, AdresseView o2) {
		int compare = Boolean.valueOf(o1.isAnnule()).compareTo(o2.isAnnule());

		if (compare == 0) {
			compare = comparatorUsageAdresseCivile.compare(o1.getUsageCivil(), o2.getUsageCivil());
		}
		if (compare == 0) {
			compare = comparatorUsageAdresseFiscal.compare(o1.getUsage(), o2.getUsage());
		}
		if (compare == 0) {
			compare = -compareNullable(o1.getDateDebut(), o2.getDateDebut(), true);
		}
		if (compare == 0) {
			compare = -compareNullable(o1.getDateFin(), o2.getDateFin(), true);
		}

		return compare;
	}

}