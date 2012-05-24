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
 * [UNIREG-1194] Liste spécialisée pour contenir les états-civil en provenance du host.
 * <p/>
 * Les états-civils sont ordonnées dans le sens croissant des numéros de séquence.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EtatCivilListHost implements Serializable, EtatCivilList {

	private static final long serialVersionUID = 533918768148428444L;

	final private long numeroIndividu; // pour le logging
	final private List<EtatCivil> list;

	public EtatCivilListHost(long numeroIndividu, Collection<EtatCivilImpl> listHost) {
		this.numeroIndividu = numeroIndividu;
		this.list = new ArrayList<EtatCivil>(listHost);
		sort();
	}

	private void sort() {

		// trie les états-civils par ordre croissant
		Collections.sort(this.list, new Comparator<EtatCivil>() {
			@Override
			public int compare(EtatCivil o1, EtatCivil o2) {
				final EtatCivilImpl e1 = (EtatCivilImpl) o1;
				final EtatCivilImpl e2 = (EtatCivilImpl) o2;
				return e1.getNoSequence() - e2.getNoSequence();
			}
		});

		// détermine la date de fin des états-civils
		EtatCivilImpl precedent = null;
		for (EtatCivil etatCivil : list) {
			if (precedent != null && etatCivil.getDateDebut() != null) {
				precedent.setDateFin(etatCivil.getDateDebut().getOneDayBefore());
			}
			precedent = (EtatCivilImpl) etatCivil;
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

			final RegDate debutValidite = e.getDateDebut();

			// Attention: les état-civils sont triés dans la collection par ordre de séquence, et ils n'ont pas de date de fin de validité
			// (= implicite à la date d'ouverture du suivant)
			if (RegDateHelper.isBetween(date, debutValidite, null, NullDateBehavior.LATEST)) {
				if (etat == null) {
					// premier état-civil trouvé
					etat = e;
				}
				else {
					// on a trouvé un état-civil qui ne respecte pas l'ordre chronologique des numéros de séquence
					if (debutValidite != null && etat.getDateDebut() != null && debutValidite.isBefore(etat.getDateDebut())) {
						throw new RuntimeException("L'état-civil n°" + ((EtatCivilImpl)e).getNoSequence() + " de l'individu n°" + numeroIndividu
								+ " ne respecte pas l'ordre chronologique des séquences.");
					}
					etat = e;
				}
			}
		}
		return etat;
	}

	public long getNumeroIndividu() {
		return numeroIndividu;
	}
}
