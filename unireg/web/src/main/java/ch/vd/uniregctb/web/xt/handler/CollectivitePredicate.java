package ch.vd.uniregctb.web.xt.handler;

import org.apache.log4j.Logger;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;

public class CollectivitePredicate extends AbstractPredicate {

	protected static final Logger LOGGER = Logger.getLogger(CollectivitePredicate.class);

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object object) {
		if (!(object instanceof CollectiviteAdministrative)) {
			return false;
		}
		CollectiviteAdministrative collectivite = (CollectiviteAdministrative) object;
		boolean result = toLowerCaseWithoutAccent(collectivite.getNomCourt()).startsWith(filter);
		return result;
	}

}
