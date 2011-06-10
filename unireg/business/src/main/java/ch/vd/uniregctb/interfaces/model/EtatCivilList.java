package ch.vd.uniregctb.interfaces.model;

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
 * <p>
 * Les états-civils sont ordonnées dans le sens croissant des numéros de séquence.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EtatCivilList implements List<EtatCivil>, Serializable {

	private static final long serialVersionUID = 5273477522204852303L;

	final private long numeroIndividu; // pour le logging
	final private List<EtatCivil> list;

	public EtatCivilList(long numeroIndividu, Collection<EtatCivil> listHost) {
		this.numeroIndividu = numeroIndividu;
		this.list = new ArrayList<EtatCivil>(listHost);
		sort();
	}

	private void sort() {
		Collections.sort(this.list, new Comparator<EtatCivil>() {
			@Override
			public int compare(EtatCivil o1, EtatCivil o2) {
				return o1.getNoSequence() - o2.getNoSequence();
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

	/**
	 * Détermine et retourne l'état-civil valide à la date spécifiée.
	 *
	 * @param date
	 *            la date de validité de l'état-civil souhaité; ou <b>null</b> pour obtenir le dernier état-civil.
	 * @return un état-civil; ou <b>null</b> si aucun état-civil n'existe.
	 */
	public EtatCivil getEtatCivilAt(RegDate date) {

		EtatCivil etat = null;

		for (EtatCivil e : list) {

			final RegDate debutValidite = e.getDateDebutValidite();

			// Attention: les état-civils sont triés dans la collection par ordre de séquence, et ils n'ont pas de date de fin de validité
			// (= implicite à la date d'ouverture du suivant)
			if (RegDateHelper.isBetween(date, debutValidite, null, NullDateBehavior.LATEST)) {
				if (etat == null) {
					// premier état-civil trouvé
					etat = e;
				}
				else {
					// on a trouvé un état-civil qui ne respecte pas l'ordre chronologique des numéros de séquence
					if (debutValidite != null && etat.getDateDebutValidite() != null && debutValidite.isBefore(etat.getDateDebutValidite())) {
						throw new RuntimeException("L'état-civil n°" + e.getNoSequence() + " de l'individu n°" + numeroIndividu
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
