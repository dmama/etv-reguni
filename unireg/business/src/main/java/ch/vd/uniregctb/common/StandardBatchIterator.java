package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Iterateur qui découpe en batches de taille déterminée un ensemble des données.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class StandardBatchIterator<E> implements Iterator<List<E>>, BatchIterator<E> {

	private final int batchSize;
	private final Iterator<E> sourceIterator;
	private List<E> next;

	private int percent;
	private int i;
	private final int size;


	public StandardBatchIterator(Iterator<E> iterator, int batchSize) {
		Assert.isTrue(batchSize > 0);
		this.sourceIterator = iterator;
		this.batchSize = batchSize;
		this.next = buildNext();

		this.percent = -1;
		this.i = -1;
		this.size = -1;
	}

	public StandardBatchIterator(Collection<E> list, int batchSize) {
		Assert.isTrue(batchSize > 0);
		this.sourceIterator = list.iterator();
		this.batchSize = batchSize;
		this.next = buildNext();

		this.percent = 0;
		this.i = 0;
		this.size = calculateSize(list.size(), batchSize);
	}

	/**
	 * @param listSize  la taille de la liste de départ
	 * @param batchSize la taille des lots
	 * @return le nombre de batches nécessaire au processing d'un liste contenant <i>listSize</i> éléments en utilisant des batches de taille <i>batchSize</i>.
	 */
	protected static int calculateSize(int listSize, int batchSize) {
		int size = listSize / batchSize;
		if (listSize % batchSize != 0) {
			++size;
		}
		return size;
	}

	public boolean hasNext() {
		return next != null;
	}

	public List<E> next() {
		List<E> result = next;
		next = buildNext();
		if (size > 0) {
			percent = (++i * 100) / size;
		}
		return result;
	}

	/**
	 * @return le pourcentage de progression; ou <b>-1</b> si l'itérateur a été construit à partir d'un autre itérateur.
	 */
	public int getPercent() {
		return percent;
	}

	private List<E> buildNext() {
		List<E> list = new ArrayList<E>();
		for (int i = 0; i < batchSize && sourceIterator.hasNext(); ++i) {
			E e = sourceIterator.next();
			list.add(e);
		}
		if (list.isEmpty()) {
			return null;
		}
		else {
			return list;
		}
	}

	public void remove() {
		throw new NotImplementedException();
	}
}
