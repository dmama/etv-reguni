package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.tiers.Tiers;

/**
* @author Manuel Siggen <manuel.siggen@vd.ch>
*/
public interface MapCallback {
	Object map(Tiers tiers, Set<TiersPart> parts, RegDate date, Context context);
}
