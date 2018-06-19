package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import java.util.function.Predicate;

import ch.vd.evd0022.v3.Capital;

/**
 * @author Raphaël Marmier, 2015-11-05
 */
public class CapitalPredicate implements Predicate<Capital> {
	@Override
	public boolean test(Capital capital) {
		return capital.getCashedInAmount() != null;
	}
}
