package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public abstract class AbstractEtatCivilList implements EtatCivilList, Serializable {

	private static final long serialVersionUID = -3575175760677132482L;

	protected final List<EtatCivil> list;

	protected AbstractEtatCivilList(List<EtatCivil> list) {
		if (list == null) {
			throw new NullPointerException("list");
		}
		this.list = new ArrayList<>(list);
	}

	@Override
	public EtatCivil getEtatCivilAt(RegDate date) {
		EtatCivil etat = null;
		final ListIterator<EtatCivil> iter = list.listIterator(list.size());
		while (iter.hasPrevious()) {
			final EtatCivil e = iter.previous();
			// les état-civils n'ont pas de date de fin de validité
			// (= implicite à la date d'ouverture du suivant)
			if (RegDateHelper.isBetween(date, e.getDateDebut(), null, NullDateBehavior.LATEST)) {
				etat = e;
				break;      // on prend le premier que l'on trouve
			}
		}
		return etat;
	}

	@Override
	public List<EtatCivil> asList() {
		return Collections.unmodifiableList(list);
	}
}
