package ch.vd.uniregctb.xml.party.v3;

import ch.vd.unireg.interfaces.efacture.data.EtatDestinataire;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatus;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.xml.EnumHelper;

public class EBillingStatusBuilder {

	public static EbillingStatus newEBillingStatus(EtatDestinataire etat) {
		final EbillingStatus status = new EbillingStatus();
		status.setType(EnumHelper.coreToXMLv1(etat.getType()));
		status.setSince(XmlUtils.date2xmlcal(etat.getDateObtention()));
		return status;
	}
}
