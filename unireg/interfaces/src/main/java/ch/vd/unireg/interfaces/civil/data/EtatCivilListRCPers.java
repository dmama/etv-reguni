package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
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
public class EtatCivilListRCPers implements EtatCivilList, Serializable {

	private static final long serialVersionUID = 4035484073324401539L;

	final private List<EtatCivil> list;

	public EtatCivilListRCPers() {
		this.list = new ArrayList<EtatCivil>();
	}

	public EtatCivilListRCPers(Collection<EtatCivil> list) {
		this.list = new ArrayList<EtatCivil>(list);
		sort();
	}

	private void sort() {
		Collections.sort(this.list, new Comparator<EtatCivil>() {
			@Override
			public int compare(EtatCivil o1, EtatCivil o2) {
				final RegDate debut1 = o1.getDateDebut();
				final RegDate debut2 = o2.getDateDebut();

				if (o1 == o2) {
					return 0;
				}

				// l'état-civil célibataire est TOUJOURS le premier
				if (o1.getTypeEtatCivil() == TypeEtatCivil.CELIBATAIRE) {
					if (o2.getTypeEtatCivil() == TypeEtatCivil.CELIBATAIRE) {
						throw new IllegalArgumentException("Trouvé deux états-civils de type 'célibataire' !");
					}
					return -1;
				}
				else if (o2.getTypeEtatCivil() == TypeEtatCivil.CELIBATAIRE) {
					return 1;
				}

				// pour les autres états-civil, on se base sur les dates de début de validité
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

		// détermine la date de fin des états-civils
		EtatCivilRCPers precedent = null;
		for (EtatCivil etatCivil : list) {
			if (precedent != null && etatCivil.getDateDebut() != null) {
				precedent.setDateFin(etatCivil.getDateDebut().getOneDayBefore());
			}
			precedent = (EtatCivilRCPers) etatCivil;
		}
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
			if (RegDateHelper.isBetween(date, e.getDateDebut(), null, NullDateBehavior.LATEST)) {
					etat = e;
			}
		}
		return etat;
	}
}
