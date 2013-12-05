package ch.vd.uniregctb.webservices.tiers2.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

import java.util.Set;

/**
* @author Manuel Siggen <manuel.siggen@vd.ch>
*/
public interface MapCallback {
	Object map(ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, RegDate date, Context context);
}
