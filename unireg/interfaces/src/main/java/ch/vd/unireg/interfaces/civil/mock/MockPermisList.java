package ch.vd.unireg.interfaces.civil.mock;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.PermisListBase;

public class MockPermisList extends PermisListBase {

	public MockPermisList(long numeroIndividu) {
		super(numeroIndividu, Collections.<Permis>emptyList());
	}

	public MockPermisList(long numeroIndividu, List<Permis> list) {
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
