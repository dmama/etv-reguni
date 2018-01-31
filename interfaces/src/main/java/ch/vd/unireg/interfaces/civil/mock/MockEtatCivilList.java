package ch.vd.unireg.interfaces.civil.mock;

import java.util.Collections;
import java.util.List;

import ch.vd.unireg.interfaces.civil.data.AbstractEtatCivilList;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;

/**
 * Liste générique d'états civils qui sont triés par ordre croissant des dates de début de validité.
 */
public class MockEtatCivilList extends AbstractEtatCivilList {

	private static final long serialVersionUID = -3037552810670227205L;

	private final boolean frozen;

	public MockEtatCivilList() {
		this(Collections.emptyList(), false);
	}

	public MockEtatCivilList(List<EtatCivil> list, boolean frozen) {
		super(list);
		this.frozen = frozen;
	}

	public boolean add(EtatCivil etatCivil) {
		if (frozen) {
			throw new UnsupportedOperationException();
		}
		return list.add(etatCivil);
	}

	public boolean remove(EtatCivil etatCivil) {
		if (frozen) {
			throw new UnsupportedOperationException();
		}
		return list.remove(etatCivil);
	}

	public void clear() {
		if (frozen) {
			throw new UnsupportedOperationException();
		}
		list.clear();
	}
}
