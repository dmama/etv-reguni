package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Permis;

public class PermisListHost extends PermisListBase implements Serializable {

	private static final long serialVersionUID = -6499453915396649854L;

	public PermisListHost(long noIndividu) {
		super(noIndividu);
	}

	public PermisListHost(long numeroIndividu, List<Permis> list) {
		super(numeroIndividu, list);
	}

	protected void sort(List<Permis> list) {
		Collections.sort(list, new Comparator<Permis>() {
			@Override
			public int compare(Permis o1, Permis o2) {
				final PermisImpl p1 = (PermisImpl) o1;
				final PermisImpl p2 = (PermisImpl) o2;
				if (RegDateHelper.equals(p1.getDateDebut(), p2.getDateDebut())) {
					return p1.getNoSequence() - p2.getNoSequence();
				}
				else {
					return NullDateBehavior.EARLIEST.compare(p1.getDateDebut(), p2.getDateDebut());
				}
			}
		});
	}
}
