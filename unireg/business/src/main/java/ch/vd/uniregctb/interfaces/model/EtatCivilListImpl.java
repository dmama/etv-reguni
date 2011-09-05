package ch.vd.uniregctb.interfaces.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Liste générique d'états civils qui sont triés par ordre croissant des dates de début de validité.
 */
public class EtatCivilListImpl implements EtatCivilList {

	final private List<EtatCivil> list;

	public EtatCivilListImpl() {
		this.list = new ArrayList<EtatCivil>();
	}

	public EtatCivilListImpl(Collection<EtatCivil> list) {
		this.list = new ArrayList<EtatCivil>(list);
		sort();
	}

	private void sort() {
		Collections.sort(this.list, new Comparator<EtatCivil>() {
			@Override
			public int compare(EtatCivil o1, EtatCivil o2) {
				final RegDate debut1 = o1.getDateDebutValidite();
				final RegDate debut2 = o2.getDateDebutValidite();

				if (debut1 == null && debut2 == null) {
					return 0;
				}
				else if (debut1 == null) {
					return -1;
				}
				else if (debut2 == null) {
					return 1;
				}
				else {
					return debut1.compareTo(debut2);
				}
			}
		});
	}

	@Override
	public boolean add(EtatCivil o) {
		boolean res = list.add(o);
		sort();
		return res;
	}

	@Override
	public void add(int index, EtatCivil element) {
		list.add(index, element);
		sort();
	}

	@Override
	public boolean addAll(Collection<? extends EtatCivil> c) {
		boolean res = list.addAll(c);
		sort();
		return res;
	}

	@Override
	public boolean addAll(int index, Collection<? extends EtatCivil> c) {
		boolean res = list.addAll(index, c);
		sort();
		return res;
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public EtatCivil get(int index) {
		return list.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<EtatCivil> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<EtatCivil> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<EtatCivil> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		boolean res = list.remove(o);
		sort();
		return res;
	}

	@Override
	public EtatCivil remove(int index) {
		EtatCivil res = list.remove(index);
		sort();
		return res;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean res = list.removeAll(c);
		sort();
		return res;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean res = list.retainAll(c);
		sort();
		return res;
	}

	@Override
	public EtatCivil set(int index, EtatCivil element) {
		EtatCivil res = list.set(index, element);
		sort();
		return res;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public List<EtatCivil> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public EtatCivil getEtatCivilAt(RegDate date) {

		EtatCivil etat = null;
		for (EtatCivil e : list) {
			// les état-civils n'ont pas de date de fin de validité
			// (= implicite à la date d'ouverture du suivant)
			if (RegDateHelper.isBetween(date, e.getDateDebutValidite(), null, NullDateBehavior.LATEST)) {
					etat = e;
			}
		}
		return etat;
	}
}
