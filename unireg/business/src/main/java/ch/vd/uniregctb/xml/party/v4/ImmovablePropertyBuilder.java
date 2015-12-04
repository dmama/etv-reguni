package ch.vd.uniregctb.xml.party.v4;

import ch.vd.unireg.xml.party.immovableproperty.v2.ImmovableProperty;
import ch.vd.unireg.xml.party.immovableproperty.v2.PropertyShare;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class ImmovablePropertyBuilder {

	public static ImmovableProperty newImmovableProperty(Immeuble immeuble) {
		final ImmovableProperty immo = new ImmovableProperty();
		immo.setNumber(immeuble.getNumero());
		immo.setDateFrom(DataHelper.coreToXMLv2(immeuble.getDateDebut()));
		immo.setDateTo(DataHelper.coreToXMLv2(immeuble.getDateFin()));
		immo.setEntryDate(DataHelper.coreToXMLv2(immeuble.getDateValidRF()));
		immo.setMunicipalityName(immeuble.getNomCommune());
		immo.setEstimatedTaxValue(immeuble.getEstimationFiscale());
		immo.setEstimatedTaxValueReference(immeuble.getReferenceEstimationFiscale());
		immo.setNature(immeuble.getNature());
		immo.setOwnershipType(EnumHelper.coreToXMLv2(immeuble.getGenrePropriete()));
		immo.setShare(coreToXML(immeuble.getPartPropriete()));
		immo.setType(EnumHelper.coreToXMLv2(immeuble.getTypeImmeuble()));
		immo.setLastMutationDate(DataHelper.coreToXMLv2(immeuble.getDateDerniereMutation()));
		immo.setLastMutationType(EnumHelper.coreToXMLv2(immeuble.getDerniereMutation()));
		return immo;
	}

	private static PropertyShare coreToXML(PartPropriete part) {
		if (part == null) {
			return null;
		}
		return new PropertyShare(part.getNumerateur(), part.getDenominateur());
	}
}
