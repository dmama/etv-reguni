package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Liste spécialisée des fors fiscaux revenus fortune, qui cache certaines informations comme les motifs de rattachement.
 * <p>
 * Les fors fiscaux stockés dans cette liste ne sont pas sensés changer au cours du temps, autrement les valeurs cachées peuvent se
 * désynchroniser.
 */
public class ForsList<T extends ForFiscalRevenuFortune> implements List<T> {

	private final List<T> list;
	private final Set<MotifRattachement> rattachements = new HashSet<MotifRattachement>();
	private final Set<TypeAutoriteFiscale> typesAutoritesFiscales = new HashSet<TypeAutoriteFiscale>();
	private RegDate minDateDebut = RegDate.getLateDate();
	private RegDate maxDateFin = RegDate.getEarlyDate();

	public ForsList() {
		this.list = new ArrayList<T>();
	}

	public ForsList(int initialCapacity) {
		this.list = new ArrayList<T>(initialCapacity);
	}

	public ForsList(List<T> subList) {
		this.list = subList;
		initSets();
	}

	private void initSets() {
		for (ForFiscalRevenuFortune f : list) {
			rattachements.add(f.getMotifRattachement());
			typesAutoritesFiscales.add(f.getTypeAutoriteFiscale());
			minDateDebut = RegDateHelper.minimum(minDateDebut, f.getDateDebut(), NullDateBehavior.EARLIEST);
			maxDateFin = RegDateHelper.maximum(maxDateFin, f.getDateFin(), NullDateBehavior.LATEST);
		}
	}

	/**
	 * @return <b>true</b> si le motif spécifié existe dans la liste des fors.
	 */
	public boolean contains(MotifRattachement rattachement) {
		return rattachements.contains(rattachement);
	}

	/**
	 * @return <b>true</b> si le type spécifié existe dans la liste des fors.
	 */
	public boolean contains(TypeAutoriteFiscale type) {
		return typesAutoritesFiscales.contains(type);
	}

	/**
	 * @return le premier for de la liste, ou <b>null</b> si la liste est vide
	 */
	public T first() {
		if (list.size() > 0) {
			return list.get(0);
		}
		else {
			return null;
		}
	}

	/**
	 * @return le dernier for de la liste, ou <b>null</b> si la liste est vide
	 */
	public T last() {
		int size = list.size();
		if (size > 0) {
			return list.get(size - 1);
		}
		else {
			return null;
		}
	}

	public boolean add(T o) {
		if (o != null) {
			rattachements.add(o.getMotifRattachement());
			typesAutoritesFiscales.add(o.getTypeAutoriteFiscale());
			minDateDebut = RegDateHelper.minimum(minDateDebut, o.getDateDebut(), NullDateBehavior.EARLIEST);
			maxDateFin = RegDateHelper.maximum(maxDateFin, o.getDateFin(), NullDateBehavior.LATEST);
		}
		return list.add(o);
	}

	public void add(int index, T element) {
		if (element != null) {
			rattachements.add(element.getMotifRattachement());
			typesAutoritesFiscales.add(element.getTypeAutoriteFiscale());
			minDateDebut = RegDateHelper.minimum(minDateDebut, element.getDateDebut(), NullDateBehavior.EARLIEST);
			maxDateFin = RegDateHelper.maximum(maxDateFin, element.getDateFin(), NullDateBehavior.LATEST);
		}
		list.add(index, element);
	}

	public boolean addAll(Collection<? extends T> c) {
		boolean r = list.addAll(c);
		initSets();
		return r;
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		boolean r = list.addAll(index, c);
		initSets();
		return r;
	}

	public void clear() {
		rattachements.clear();
		typesAutoritesFiscales.clear();
		list.clear();
		minDateDebut = RegDate.getLateDate();
		maxDateFin = RegDate.getEarlyDate();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public T get(int index) {
		return list.get(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator<T> iterator() {
		return list.iterator();
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator<T> listIterator() {
		return list.listIterator();
	}

	public ListIterator<T> listIterator(int index) {
		return list.listIterator(index);
	}

	public boolean remove(Object o) {
		throw new NotImplementedException();
	}

	public T remove(int index) {
		throw new NotImplementedException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new NotImplementedException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new NotImplementedException();
	}

	public T set(int index, T element) {
		throw new NotImplementedException();
	}

	public int size() {
		return list.size();
	}

	public List<T> subList(int fromIndex, int toIndex) {
		throw new NotImplementedException();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	/**
	 * @return la plus petite (= plus proche du big bang) des dates de début
	 */
	public RegDate getMinDateDebut() {
		return minDateDebut;
	}

	/**
	 * @return la plus grande (= plus proche du big crunch) des dates de fin
	 */
	public RegDate getMaxDateFin() {
		return maxDateFin;
	}

}
