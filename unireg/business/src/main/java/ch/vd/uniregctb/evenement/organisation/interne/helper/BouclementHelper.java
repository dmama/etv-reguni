package ch.vd.uniregctb.evenement.organisation.interne.helper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Bouclement;
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
	public static Bouclement createBouclementSelonSemestre(RegDate creationDate) {
		RegDate bouclementDebut = creationDate;

		// Si on a dépassé la moitié de l'année, on crée un bouclement pour l'années d'après.
		if (creationDate.isAfterOrEqual(RegDate.get(creationDate.year(), 7, 1))) {
			bouclementDebut = RegDate.get(creationDate.year() + 1, 1, 1); // Date au début de l'année pour éviter tout problème
		}
		final Bouclement bouclement = new Bouclement();
		bouclement.setPeriodeMois(12);
		bouclement.setAncrage(DayMonth.get(12, 31));
		bouclement.setDateDebut(bouclementDebut);

		return bouclement;
	}

}
