package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.SwissGazetteOfCommercePublication;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;

public class BusinessPublicationConverter extends RangedToRangeBaseConverter<ch.vd.evd0022.v3.BusinessPublication, PublicationBusiness> {

	private static final TypeOfBusinessPublicationConverter TYPE_OF_BUSINESS_PUBLICATION_CONVERTER = new TypeOfBusinessPublicationConverter();
	private static final TypeOfFusionConverter TYPE_OF_FUSION_CONVERTER = new TypeOfFusionConverter();
	private static final TypeOfCapitalReductionConverter TYPE_OF_CAPITAL_REDUCTION_CONVERTER = new TypeOfCapitalReductionConverter();
	private static final TypeOfTransferConverter TYPE_OF_TRANSFER_CONVERTER = new TypeOfTransferConverter();
	private static final TypeOfLiquidationConverter TYPE_OF_LIQUIDATION_CONVERTER = new TypeOfLiquidationConverter();

	@NotNull
	@Override
	protected PublicationBusiness convert(@NotNull DateRangeHelper.Ranged<ch.vd.evd0022.v3.BusinessPublication> businessPublicationRange) {
		final BusinessPublication businessPublication = businessPublicationRange.getPayload();
		SwissGazetteOfCommercePublication fosc = null;
		if (businessPublication != null) {
			fosc = businessPublication.getSwissGazetteOfCommercePublication();
		}
		return new PublicationBusiness(businessPublicationRange.getDateDebut(),
		                               businessPublicationRange.getDateFin(),
		                               businessPublication == null ? null : businessPublication.getEventDate(),
		                               businessPublication == null ? null : TYPE_OF_BUSINESS_PUBLICATION_CONVERTER.apply(businessPublication.getTypeOfBusinessPublication()),
		                               fosc == null ? null : fosc.getDocumentNumber(),
		                               fosc == null ? null : fosc.getPublicationDate(),
		                               fosc == null ? null : fosc.getPublicationText(),
		                               businessPublication == null ? null : TYPE_OF_FUSION_CONVERTER.apply(businessPublication.getTypeOfFusion()),
		                               businessPublication == null ? null : TYPE_OF_CAPITAL_REDUCTION_CONVERTER.apply(businessPublication.getTypeOfCapitalReduction()),
		                               businessPublication == null ? null : TYPE_OF_TRANSFER_CONVERTER.apply(businessPublication.getTypeOfTransfer()),
		                               businessPublication == null ? null : TYPE_OF_LIQUIDATION_CONVERTER.apply(businessPublication.getTypeOfLiquidation())
		);
	}
}
