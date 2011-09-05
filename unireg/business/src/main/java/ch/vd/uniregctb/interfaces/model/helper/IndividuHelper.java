package ch.vd.uniregctb.interfaces.model.helper;

import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;

public abstract class IndividuHelper {

	public static Permis getPermisActif(Individu individu, RegDate date) {

		Permis permis = null;

		final List<Permis> liste = individu.getPermis();
		if (liste != null) {

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
