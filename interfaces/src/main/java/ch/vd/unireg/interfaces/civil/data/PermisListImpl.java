package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CollectionsUtils;

public class PermisListImpl implements PermisList, Serializable {

	private static final long serialVersionUID = 5377477549461551838L;

	protected static final Comparator<Permis> COMPARATOR_PERMIS = (o1, o2) -> NullDateBehavior.EARLIEST.compare(o1.getDateValeur(), o2.getDateValeur());

	private final List<Permis> list;

	public PermisListImpl(List<Permis> list) {
		this.list = new ArrayList<>(list);
		//sort(this.list);
	}

	public PermisListImpl() {
		this(Collections.emptyList());
	}

	protected void sort(List<Permis> list) {
		list.sort(COMPARATOR_PERMIS);
	}

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
	public boolean addAll(@NotNull Collection<? extends Permis> c) {
		boolean res = list.addAll(c);
		sort(list);
		return res;
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends Permis> c) {
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
	public boolean containsAll(@NotNull Collection<?> c) {
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

	@NotNull
	@Override
	public Iterator<Permis> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@NotNull
	@Override
	public ListIterator<Permis> listIterator() {
		return list.listIterator();
	}

	@NotNull
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
	public boolean removeAll(@NotNull Collection<?> c) {
		boolean res = list.removeAll(c);
		sort(list);
		return res;
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
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
	public <T> T[] toArray(@NotNull T[] a) {
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
		for (Permis candidate : CollectionsUtils.revertedOrder(list)) {
			if (candidate.getDateAnnulation() != null) {
				continue;
			}
			//SIFISC-161109 Utiliser la reporting Date(date valeur et non la date de début)

			if (RegDateHelper.isBetween(date, candidate.getDateValeur(), candidate.getDateFin(), NullDateBehavior.LATEST)) {
				permis = candidate;
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
