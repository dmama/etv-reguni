package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public abstract class PermisListBase implements PermisList, Serializable {

	private static final long serialVersionUID = 7962307714373650485L;

	final private List<Permis> list;

	protected PermisListBase(List<Permis> list) {
		this.list = new ArrayList<>(list);
		sort(this.list);
	}

	protected PermisListBase() {
		this(Collections.<Permis>emptyList());
	}

	protected abstract void sort(List<Permis> list);

	@Override
	public boolean add(Permis o) {
		boolean res = list.add(o);
		sort(list);
		return res;
	}

	@Override
	public void add(int index, Permis element) {
		list.add(index, element);
		sort(list);
	}

	@Override
	public boolean addAll(Collection<? extends Permis> c) {
		boolean res = list.addAll(c);
		sort(list);
		return res;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Permis> c) {
		boolean res = list.addAll(index, c);
		sort(list);
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
	public Permis get(int index) {
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
	public Iterator<Permis> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<Permis> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<Permis> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		boolean res = list.remove(o);
		sort(list);
		return res;
	}

	@Override
	public Permis remove(int index) {
		Permis res = list.remove(index);
		sort(list);
		return res;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean res = list.removeAll(c);
		sort(list);
		return res;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean res = list.retainAll(c);
		sort(list);
		return res;
	}

	@Override
	public Permis set(int index, Permis element) {
		Permis res = list.set(index, element);
		sort(list);
		return res;
	}

	@Override
	public int size() {
		return list.size();
	}

	@NotNull
	@Override
	public List<Permis> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@NotNull
	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@NotNull
	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public Permis getPermisActif(@Nullable RegDate date) {

		Permis permis = null;

		// null -> date du jour
		if (date == null) {
			date = RegDate.get();
		}

		// itération sur la liste des permis, dans l'ordre inverse de l'obtention
		// (on s'arrête sur le premier pour lequel les dates sont bonnes - et on ne prends pas en compte les permis annulés)
		final ListIterator<Permis> iter = list.listIterator(list.size());
		while (iter.hasPrevious()) {
			final Permis p = iter.previous();
			if (p.getDateAnnulation() != null) {
				continue;
			}
			if (RegDateHelper.isBetween(date, p.getDateDebut(), p.getDateFin(), NullDateBehavior.LATEST)) {
				permis = p;
				break;
			}
		}

		return permis;
	}

	@Override
	public Permis getPermisAnnule(@NotNull RegDate date) {
		for (Permis p : list) {
			if (p.getDateAnnulation() != null && p.getDateDebut() == date) {
				return p;
			}
		}
		return null;
	}
}
