package ch.vd.uniregctb.xml.party.v5;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.xml.party.v5.AdministrativeAuthorityLink;
import ch.vd.unireg.xml.party.v5.PartyLabel;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.xml.DataHelper;

public class LabelBuilder {

	public static PartyLabel newLabel(EtiquetteTiers etiquetteTiers, boolean virtual) {
		return newLabel(etiquetteTiers, etiquetteTiers.getEtiquette(), virtual);
	}

	public static PartyLabel newLabel(DateRange range, Etiquette etiquette, boolean virtual) {

		final PartyLabel label = new PartyLabel();
		label.setDateFrom(DataHelper.coreToXMLv2(range.getDateDebut()));
		label.setDateTo(DataHelper.coreToXMLv2(range.getDateFin()));

		label.setDisplayLabel(etiquette.getLibelle());
		label.setLabel(etiquette.getCode());
		label.setVirtual(virtual);

		final CollectiviteAdministrative ca = etiquette.getCollectiviteAdministrative();
		if (ca != null) {
			label.setAdministrativeAuthority(new AdministrativeAuthorityLink(null, ca.getNumero().intValue(), ca.getNumeroCollectiviteAdministrative()));
		}

		return label;

	}
}
