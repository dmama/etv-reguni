package ch.vd.uniregctb.web.xt.handler;

import org.apache.log4j.Logger;
import ch.vd.uniregctb.interfaces.model.Pays;

public class PaysPredicate extends AbstractPredicate {

	protected static final Logger LOGGER = Logger.getLogger(PaysPredicate.class);

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object object) {
		if (!(object instanceof Pays)) {
			return false;
		}
		Pays pays = (Pays) object;
		boolean result = toLowerCaseWithoutAccent(pays.getNomMinuscule()).startsWith(filter) ||
			(pays.getNoOFS() + "").startsWith(filter);
		return result;
	}
}
