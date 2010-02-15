package ch.vd.uniregctb.mouvement.manager;

import java.util.Comparator;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.mouvement.MouvementDossier;

/**
 * Les mouvements annulés sont de toute façon à la fin,
 * puis on tri dans l'ordre anti-chronologique (= plus récent d'abord) :
 * <ul>
 *  <li> on tri d'abord par date de mouvement (null est plus récent),</li>
 *  <li> et par date de modification du mouvement</li>
 * </ul>
 */
public class AntiChronologiqueMouvementComparator implements Comparator<MouvementDossier> {

	public int compare(MouvementDossier mvt1, MouvementDossier mvt2) {

		if (mvt1.isAnnule() || mvt2.isAnnule()) {
			if (!mvt1.isAnnule()) {
				return -1;
			}
			else if (!mvt2.isAnnule()) {
				return 1;
			}
			else {
				return compateSelonDates(mvt1, mvt2);
			}
		}
		else {
			return compateSelonDates(mvt1, mvt2);
		}
	}

	private static int compateSelonDates(MouvementDossier mvt1, MouvementDossier mvt2) {
		final RegDate dateMvt1 = mvt1.getDateMouvement();
		final RegDate dateMvt2 = mvt2.getDateMouvement();
		if (RegDateHelper.equals(dateMvt1, dateMvt2)) {
			return - mvt1.getLogModifDate().compareTo(mvt2.getLogModifDate());
		}
		else if (RegDateHelper.isAfterOrEqual(dateMvt1, dateMvt2, NullDateBehavior.LATEST)) {
			return -1;
		}
		else {
			return 1;
		}
	}
}
