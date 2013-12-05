package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.Triplet;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

/**
 * Un for fiscal principal et son context, c'est-à-dire les fors fiscaux principaux qui précèdent et qui suivent immédiatement.
 */
public class ForFiscalPrincipalContext {

	public final ForFiscalPrincipal previousprevious;
	public final ForFiscalPrincipal previous;
	public final ForFiscalPrincipal current;
	public final ForFiscalPrincipal next;
	public final ForFiscalPrincipal nextnext;

	/**
	 * Construit le contact d'un for fiscal principal à partir du triplet de fors fiscaux (qui ne se touchent pas forcément).
	 *
	 * @param triplet le triplet de fors fiscaux initials
	 */
	public ForFiscalPrincipalContext(Triplet<ForFiscalPrincipal> triplet) {
		current = triplet.current;
		previous = (triplet.previous != null && DateRangeHelper.isCollatable(triplet.previous, current) ? triplet.previous : null);
		previousprevious = (previous != null && triplet.previousprevious != null && DateRangeHelper.isCollatable(triplet.previousprevious, previous) ? triplet.previousprevious : null);
		next = (triplet.next != null && DateRangeHelper.isCollatable(current, triplet.next) ? triplet.next : null);
		nextnext = (next != null && triplet.nextnext != null && DateRangeHelper.isCollatable(next, triplet.nextnext) ? triplet.nextnext : null);
	}
}
