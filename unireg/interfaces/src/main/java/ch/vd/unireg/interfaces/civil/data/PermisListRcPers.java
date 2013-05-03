package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;

public class PermisListRcPers extends PermisListBase implements Serializable {

	private static final long serialVersionUID = 7001930116163643695L;

	public PermisListRcPers() {
		super();
	}

	public PermisListRcPers(List<Permis> list) {
		super(list);
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
