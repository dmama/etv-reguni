package ch.vd.uniregctb.web.xt.handler;

import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.common.StringComparator;

public abstract class AbstractPredicate implements Predicate {

	/**
	 * texte saisi par l'utilisateur
	 */
	protected String filter = "";

	public void setFilter(String filter) {
		this.filter = toLowerCaseWithoutAccent(filter);
	}
	
	abstract public boolean evaluate(Object object);
	
	protected static String toLowerCaseWithoutAccent(String str) {
		if (str == null) {
			return StringUtils.EMPTY;
		}
		else {
			return StringComparator.removeAccents(str).toLowerCase();
		}
	}
}
