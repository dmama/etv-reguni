package ch.vd.uniregctb.webservices.party3.data;

import ch.vd.unireg.xml.party.immovableproperty.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.immovableproperty.v1.PropertyShare;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

public class ImmovablePropertyBuilder {

	public static ImmovableProperty newImmovableProperty(Immeuble immeuble) {
		final ImmovableProperty immo = new ImmovableProperty();
		immo.setNumber(immeuble.getNumero());
		immo.setDateFrom(DataHelper.coreToWeb(immeuble.getDateDebut()));
		immo.setDateTo(DataHelper.coreToWeb(immeuble.getDateFin()));
		immo.setEstimatedTaxValue(immeuble.getEstimationFiscale());
		immo.setEstimatedTaxValueDate(DataHelper.coreToWeb(immeuble.getDateEstimationFiscale()));
		immo.setFormerEstimatedTaxValue(immeuble.getAncienneEstimationFiscale());
		immo.setNature(immeuble.getNature());
		immo.setType(EnumHelper.coreToWeb(immeuble.getGenrePropriete()));
		immo.setShare(coreToWeb(immeuble.getPartPropriete()));
		return immo;
	}

	private static PropertyShare coreToWeb(PartPropriete part) {
		if (part == null) {
			return null;
		}
		return new PropertyShare(part.getNumerateur(), part.getDenominateur());
	}
}
