package ch.vd.uniregctb.xml.party.v1;

import ch.vd.unireg.xml.party.immovableproperty.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.immovableproperty.v1.PropertyShare;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class ImmovablePropertyBuilder {

	public static ImmovableProperty newImmovableProperty(Immeuble immeuble) {
		final ImmovableProperty immo = new ImmovableProperty();
		immo.setNumber(immeuble.getNumero());
		immo.setDateFrom(DataHelper.coreToXMLv1(immeuble.getDateDebut()));
		immo.setDateTo(DataHelper.coreToXMLv1(immeuble.getDateFin()));
		immo.setEntryDate(DataHelper.coreToXMLv1(immeuble.getDateValidRF()));
		immo.setMunicipalityName(immeuble.getNomCommune());
		immo.setEstimatedTaxValue(immeuble.getEstimationFiscale());
		immo.setEstimatedTaxValueReference(immeuble.getReferenceEstimationFiscale());
		immo.setNature(immeuble.getNature());
		immo.setOwnershipType(EnumHelper.coreToXMLv1(immeuble.getGenrePropriete()));
		immo.setShare(coreToXML(immeuble.getPartPropriete()));
		immo.setType(EnumHelper.coreToXMLv1(immeuble.getTypeImmeuble()));
		immo.setLastMutationDate(DataHelper.coreToXMLv1(immeuble.getDateDerniereMutation()));
		immo.setLastMutationType(EnumHelper.coreToXMLv1(immeuble.getDerniereMutation()));
		return immo;
	}

	private static PropertyShare coreToXML(PartPropriete part) {
		if (part == null) {
			return null;
		}
		return new PropertyShare(part.getNumerateur(), part.getDenominateur());
	}
}
