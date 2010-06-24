package ch.vd.uniregctb.web.xt.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.uniregctb.interfaces.model.Rue;


public class RuePredicate extends AbstractPredicate {

	protected static final Logger LOGGER = Logger.getLogger(RuePredicate.class);

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object object) {
		if (!(object instanceof Rue)) {
			return false;
		}
		Rue rue = (Rue) object;
		boolean result = StringUtils.contains(toLowerCaseWithoutAccent(rue.getDesignationCourrier()), filter);

		return result;
	}
}
