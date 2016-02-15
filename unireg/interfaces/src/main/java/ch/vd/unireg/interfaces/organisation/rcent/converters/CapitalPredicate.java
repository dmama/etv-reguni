package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.apache.commons.collections4.Predicate;

import ch.vd.evd0022.v3.Capital;

/**
 * @author Raphaël Marmier, 2015-11-05
 */
public class CapitalPredicate implements Predicate<Capital> {
	@Override
	public boolean evaluate(Capital capital) {
		return capital.getCashedInAmount() != null;
	}
}
