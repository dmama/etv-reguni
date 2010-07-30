package ch.vd.uniregctb.web.xt.handler;

import org.apache.log4j.Logger;
import ch.vd.securite.model.Operateur;

public class UtilisateurPredicate extends AbstractPredicate {

	protected static final Logger LOGGER = Logger.getLogger(UtilisateurPredicate.class);

	/**
	 * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object object) {
		if (!(object instanceof Operateur)) {
			return false;
		}
		Operateur operateur = (Operateur) object;
		boolean result1 = operateur.getCode().toLowerCase().startsWith(filter);
		boolean result2 = false;
		if (operateur.getNom() != null) {
			result2 = toLowerCaseWithoutAccent(operateur.getNom()).startsWith(filter);
		}
		boolean result3 = false;
		if (operateur.getPrenom() != null) {
			result3 = toLowerCaseWithoutAccent(operateur.getPrenom()).startsWith(filter);
		}
		return (result1 || result2 || result3);
	}

}
