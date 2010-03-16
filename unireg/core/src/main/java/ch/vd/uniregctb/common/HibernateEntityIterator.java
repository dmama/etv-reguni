package ch.vd.uniregctb.common;

import java.util.Iterator;

/**
 * Cette itérateur permet de contourner les proxys retournés par Hibernate dans certaines situation et d'attaquer directement les entités
 * réelles.
 *
 * @param <T>
 *            le type d'entité considérées.
 */
public class HibernateEntityIterator<T extends HibernateEntity> implements Iterator<T> {

	private final Iterator<T> iter;
	private final RefParam<HibernateEntity> param = new RefParam<HibernateEntity>();

	public HibernateEntityIterator(Iterator<T> iter) {
		this.iter = iter;
	}

	public boolean hasNext() {
		return iter.hasNext();
	}

	@SuppressWarnings("unchecked")
	public T next() {

		T p = iter.next();
		if (p == null) {
			return null;
		}

		// Récupère l'objet réel (pas le proxy)
		p.tellMeAboutYou(param);
		T n = (T)param.ref;

		return n;
	}

	public void remove() {
		iter.remove();
	}
}
