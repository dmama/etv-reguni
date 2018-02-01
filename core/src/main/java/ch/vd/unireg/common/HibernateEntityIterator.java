package ch.vd.unireg.common;

import java.util.Iterator;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * Cette itérateur permet de contourner les proxys retournés par Hibernate dans certaines situation et d'attaquer directement les entités
 * réelles.
 *
 * @param <T>
 *            le type d'entité considérées.
 */
public class HibernateEntityIterator<T extends HibernateEntity> implements Iterator<T> {

	private final Iterator<T> iter;
	private final Mutable<HibernateEntity> param = new MutableObject<>();

	public HibernateEntityIterator(Iterator<T> iter) {
		this.iter = iter;
	}

	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T next() {

		T p = iter.next();
		if (p == null) {
			return null;
		}

		// Récupère l'objet réel (pas le proxy)
		p.tellMeAboutYou(param);
		return (T) param.getValue();
	}

	@Override
	public void remove() {
		iter.remove();
	}
}
