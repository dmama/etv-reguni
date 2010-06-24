package ch.vd.uniregctb.web.xt.handler;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.interfaces.model.Localite;

public class LocalitePredicate extends AbstractPredicate {

	protected static final Logger LOGGER = Logger.getLogger(LocalitePredicate.class);

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object object) {
		if (!(object instanceof Localite)) {
			return false;
		}
		Localite localite = (Localite) object;
		
		boolean result = toLowerCaseWithoutAccent(localite.getNomAbregeMinuscule()).startsWith(filter)
			|| (localite.getNPA() + "").startsWith(filter);
		return result;
	}
}
