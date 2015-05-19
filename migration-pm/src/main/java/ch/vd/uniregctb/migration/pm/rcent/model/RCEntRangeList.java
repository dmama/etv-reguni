package ch.vd.uniregctb.migration.pm.rcent.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.RegDate;

public class RCEntRangeList<E extends RCEntHistoryElement> implements List<E> {

	private List<E> elements;

	public RCEntRangeList(List<E> elements) {
		this.elements = elements;
	}

	public List<E> getValuesFor(RegDate date) {
		return stream().filter(range -> range.isValidAt(date)).collect(Collectors.toList());
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return elements.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return elements.iterator();
	}

	@Override
	public Object[] toArray() {
		return elements.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return elements.toArray(a);
	}

	@Override
	public boolean add(E e) {
		return elements.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return elements.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return elements.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return elements.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return elements.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return elements.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return elements.retainAll(c);
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public boolean equals(Object o) {
		return elements.equals(o);
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public E get(int index) {
		return elements.get(index);
	}

	@Override
	public E set(int index, E element) {
		return elements.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		elements.add(index, element);
	}

	@Override
	public E remove(int index) {
		return elements.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return elements.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return elements.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return elements.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return elements.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return elements.subList(fromIndex, toIndex);
	}
}
