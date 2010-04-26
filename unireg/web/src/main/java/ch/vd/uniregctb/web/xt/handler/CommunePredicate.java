package ch.vd.uniregctb.web.xt.handler;

import org.apache.log4j.Logger;
import ch.vd.uniregctb.interfaces.model.Commune;

public class CommunePredicate extends AbstractPredicate {

	protected static final Logger LOGGER = Logger.getLogger(CommunePredicate.class);

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object object) {
		if (!(object instanceof Commune)) {
			return false;
		}
		Commune commune = (Commune) object;
		boolean result = toLowerCaseWithoutAccent(commune.getNomMinuscule()).startsWith(filter) ||
			(commune.getNoOFS() + "").startsWith(filter);
		return result;
	}
}
