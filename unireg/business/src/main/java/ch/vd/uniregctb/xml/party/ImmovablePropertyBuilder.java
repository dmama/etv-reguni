package ch.vd.uniregctb.xml.party;

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
		immo.setDateFrom(DataHelper.coreToXML(immeuble.getDateDebut()));
		immo.setDateTo(DataHelper.coreToXML(immeuble.getDateFin()));
		immo.setEntryDate(DataHelper.coreToXML(immeuble.getDateValidRF()));
		immo.setMunicipalityName(immeuble.getNomCommune());
		immo.setEstimatedTaxValue(immeuble.getEstimationFiscale());
		immo.setEstimatedTaxValueReference(immeuble.getReferenceEstimationFiscale());
		immo.setNature(immeuble.getNature());
		immo.setOwnershipType(EnumHelper.coreToXML(immeuble.getGenrePropriete()));
		immo.setShare(coreToXML(immeuble.getPartPropriete()));
		immo.setType(EnumHelper.coreToXML(immeuble.getTypeImmeuble()));
		immo.setLastMutationDate(DataHelper.coreToXML(immeuble.getDateDerniereMutation()));
		immo.setLastMutationType(EnumHelper.coreToXML(immeuble.getDerniereMutation()));
		return immo;
	}

	private static PropertyShare coreToXML(PartPropriete part) {
		if (part == null) {
			return null;
		}
		return new PropertyShare(part.getNumerateur(), part.getDenominateur());
	}
}
