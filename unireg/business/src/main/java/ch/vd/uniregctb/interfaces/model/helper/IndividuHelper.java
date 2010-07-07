package ch.vd.uniregctb.interfaces.model.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;

public abstract class IndividuHelper {

	public static Permis getPermisActif(Individu individu, RegDate date) {

		Permis permis = null;

		final Collection<Permis> coll = individu.getPermis();
		if (coll != null) {

			// tri des permis par leur date de début et numéro de séquence (utile si les dates de début sont nulles)
			final List<Permis> liste = new ArrayList<Permis>(coll);
			Collections.sort(liste, new Comparator<Permis>() {
				public int compare(Permis o1, Permis o2) {
					if (RegDateHelper.equals(o1.getDateDebutValidite(), o2.getDateDebutValidite())) {
						return o1.getNoSequence() - o2.getNoSequence();
					}
					else {
						return RegDateHelper.isBeforeOrEqual(o1.getDateDebutValidite(), o2.getDateDebutValidite(), NullDateBehavior.EARLIEST) ? -1 : 1;
					}
				}
			});

			// itération sur la liste des permis, dans l'ordre inverse de l'obtention
			// (on s'arrête sur le premier pour lequel les dates sont bonnes - et on ne prends pas en compte les permis annulés)
			for (int i = liste.size() - 1; i >= 0; --i) {
				final Permis e = liste.get(i);
				if (e.getDateAnnulation() != null) {
					continue;
				}
				if (RegDateHelper.isBetween(date, e.getDateDebutValidite(), e.getDateFinValidite(), NullDateBehavior.LATEST)) {
					permis = e;
					break;
				}
			}
		}

		return permis;
	}
}
