package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;

/**
* @author Manuel Siggen <manuel.siggen@vd.ch>
*/
public interface MapCallback {
	Object map(Tiers tiers, Set<TiersPart> parts, RegDate date, Context context);
}
