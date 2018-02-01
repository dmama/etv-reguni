package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;

/**
 * @author Raphaël Marmier, 2015-09-23
 */
public class BouclementHelper {

	/**
	 * Crée un bouclement selon la position dans l'année de la date. Au 31.12 de l'année correspondant
	 * si la date se situe dans le premier semestre. Au 31.12 de l'années suivante si la date se situe
	 * dans le second.
	 *
	 * @param creationDate La date de référence
	 */
	public static Bouclement createBouclement3112SelonSemestre(RegDate creationDate) {
		RegDate bouclementDebut = creationDate;

		// Si on a dépassé la moitié de l'année, on crée un bouclement pour l'années d'après.
		if (creationDate.isAfterOrEqual(RegDate.get(creationDate.year(), 7, 1))) {
			bouclementDebut = RegDate.get(creationDate.year() + 1, 1, 1); // Date au début de l'année pour éviter tout problème
		}

		return createBouclement3112(bouclementDebut);
	}

	@NotNull
	public static Bouclement createBouclement3112(RegDate bouclementDebut) {
		final Bouclement bouclement = new Bouclement();
		bouclement.setPeriodeMois(12);
		bouclement.setAncrage(DayMonth.get(12, 31));
		bouclement.setDateDebut(bouclementDebut);
		return bouclement;
	}

	/**
	 * Annule et recrée les bouclements qui vont bien pour correspondre à la nouvelle configuration
	 * @param entreprise entreprise dont on veut ré-initialiser les dates de bouclement
	 * @param nouveauxBouclements les nouveaux cycles de bouclements à respecter (ces bouclements ne doivent pas être déjà persistés !)
	 */
	public static void resetBouclements(Entreprise entreprise, Collection<Bouclement> nouveauxBouclements) {

		// petit test de blindage des conditions initiales
		for (Bouclement b : nouveauxBouclements) {
			if (b.getId() != null) {
				throw new IllegalStateException("Aucun bouclement déjà persisté ne devrait apparaître dans la collection des nouveaux bouclement (trouvé " + b.getId() + ") !");
			}
		}

		final Map<RegDate, Bouclement> indexAnciensBouclements = indexBouclementsParDateDebut(AnnulableHelper.sansElementsAnnules(entreprise.getBouclements()));
		final Map<RegDate, Bouclement> indexNouveauxBouclements = indexBouclementsParDateDebut(nouveauxBouclements);
		final SortedSet<RegDate> datesDebut = new TreeSet<>();
		datesDebut.addAll(indexAnciensBouclements.keySet());
		datesDebut.addAll(indexNouveauxBouclements.keySet());
		for (RegDate dateDebut : datesDebut) {
			final Bouclement ancien = indexAnciensBouclements.get(dateDebut);
			final Bouclement nouveau = indexNouveauxBouclements.get(dateDebut);
			if (ancien == null) {
				// à ajouter
				entreprise.addBouclement(nouveau);
			}
			else if (nouveau == null) {
				// à annuler
				ancien.setAnnule(true);
			}
			else {
				// on a deux bouclements qui commencent à la même date...
				// s'ils sont identiques, on continue
				// sinon, il faut annuler l'ancien et ajouter le nouveau
				if (ancien.getAncrage() != nouveau.getAncrage() || ancien.getPeriodeMois() != nouveau.getPeriodeMois()) {
					ancien.setAnnule(true);
					entreprise.addBouclement(nouveau);
				}
			}
		}
	}

	private static Map<RegDate, Bouclement> indexBouclementsParDateDebut(Collection<Bouclement> bouclementsNonAnnules) {
		final Map<RegDate, Bouclement> map = new HashMap<>(bouclementsNonAnnules.size());
		for (Bouclement bouclement : bouclementsNonAnnules) {
			map.put(bouclement.getDateDebut(), bouclement);
		}
		return map;
	}
}
