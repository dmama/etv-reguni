package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;

public class PermisListRcPers extends PermisListBase implements Serializable {

	private static final long serialVersionUID = 4776065381997664389L;

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
