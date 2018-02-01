package ch.vd.unireg.webservices.party3.impl;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.v1.PartyPart;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;

/**
* @author Manuel Siggen <manuel.siggen@vd.ch>
*/
public interface MapCallback {
	Object map(Tiers tiers, Set<PartyPart> parts, RegDate date, Context context);
}
