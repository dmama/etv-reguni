package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.uniregctb.interfaces.model.Permis;

public class PermisListRcPers extends PermisListBase implements Serializable {

	private static final long serialVersionUID = -8707553226907735643L;

	public PermisListRcPers(long noIndividu) {
		super(noIndividu);
	}

	public PermisListRcPers(long numeroIndividu, List<Permis> list) {
		super(numeroIndividu, list);
	}

	protected void sort(List<Permis> list) {
		Collections.sort(list, new Comparator<Permis>() {
			@Override
			public int compare(Permis o1, Permis o2) {
				return NullDateBehavior.EARLIEST.compare(o1.getDateDebut(), o2.getDateDebut());
			}
		});
	}
}
