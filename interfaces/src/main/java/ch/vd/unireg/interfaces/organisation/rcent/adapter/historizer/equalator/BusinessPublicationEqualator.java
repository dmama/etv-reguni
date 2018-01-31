package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;


import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.SwissGazetteOfCommercePublication;
import ch.vd.uniregctb.common.Equalator;

public class BusinessPublicationEqualator implements Equalator<BusinessPublication> {

	@Override
	public boolean test(BusinessPublication biz1, BusinessPublication biz2) {
		if (biz1 == biz2) return true;
		if (biz2 == null) return false;

		if (biz1.getTypeOfBusinessPublication() != null ? !biz1.getTypeOfBusinessPublication().equals(biz2.getTypeOfBusinessPublication()) : biz2.getTypeOfBusinessPublication() != null) return false;
		if (biz1.getEventDate() != null ? !biz1.getEventDate().equals(biz2.getEventDate()) : biz2.getEventDate() != null) return false;
		if (biz1.getSwissGazetteOfCommercePublication() != null ? !compare(biz1.getSwissGazetteOfCommercePublication(), biz2.getSwissGazetteOfCommercePublication()) : biz2.getSwissGazetteOfCommercePublication() != null)
			return false;
		if (biz1.getTypeOfFusion() != biz2.getTypeOfFusion()) return false;
		if (biz1.getTypeOfCapitalReduction() != biz2.getTypeOfCapitalReduction()) return false;
		if (biz1.getTypeOfTransfer() != biz2.getTypeOfTransfer()) return false;
		if (biz1.getTypeOfLiquidation() != biz2.getTypeOfLiquidation()) return false;

		return true;
	}

	public boolean compare(SwissGazetteOfCommercePublication fosc1, SwissGazetteOfCommercePublication fosc2) {
		if (fosc1 == fosc2) return true;
		if (fosc2 == null) return false;

		if (fosc1.getDocumentNumber() != null ? !fosc1.getDocumentNumber().equals(fosc2.getDocumentNumber()) : fosc2.getDocumentNumber() != null) return false;
		if (fosc1.getPublicationDate() != null ? !fosc1.getPublicationDate().equals(fosc2.getPublicationDate()) : fosc2.getPublicationDate() != null) return false;
		if (fosc1.getPublicationText() != null ? !fosc1.getPublicationText().equals(fosc2.getPublicationText()) : fosc2.getPublicationText() != null) return false;

		return true;
	}

}
