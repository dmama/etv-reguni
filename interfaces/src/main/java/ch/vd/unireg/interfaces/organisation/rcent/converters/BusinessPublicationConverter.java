package ch.vd.unireg.interfaces.organisation.rcent.converters;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.SwissGazetteOfCommercePublication;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;

public class BusinessPublicationConverter implements Function<BusinessPublication, PublicationBusiness> {

	private static final TypeOfBusinessPublicationConverter TYPE_OF_BUSINESS_PUBLICATION_CONVERTER = new TypeOfBusinessPublicationConverter();
	private static final TypeOfFusionConverter TYPE_OF_FUSION_CONVERTER = new TypeOfFusionConverter();
	private static final TypeOfCapitalReductionConverter TYPE_OF_CAPITAL_REDUCTION_CONVERTER = new TypeOfCapitalReductionConverter();
	private static final TypeOfTransferConverter TYPE_OF_TRANSFER_CONVERTER = new TypeOfTransferConverter();
	private static final TypeOfLiquidationConverter TYPE_OF_LIQUIDATION_CONVERTER = new TypeOfLiquidationConverter();

	@NotNull
	@Override
	public PublicationBusiness apply(@NotNull BusinessPublication businessPublication) {
		SwissGazetteOfCommercePublication fosc = null;
		fosc = businessPublication.getSwissGazetteOfCommercePublication();
		return new PublicationBusiness(businessPublication.getEventDate(),
		                               TYPE_OF_BUSINESS_PUBLICATION_CONVERTER.apply(businessPublication.getTypeOfBusinessPublication()),
		                               fosc == null ? null : fosc.getDocumentNumber(),
		                               fosc == null ? null : fosc.getPublicationDate(),
		                               fosc == null ? null : fosc.getPublicationText(),
		                               TYPE_OF_FUSION_CONVERTER.apply(businessPublication.getTypeOfFusion()),
		                               TYPE_OF_CAPITAL_REDUCTION_CONVERTER.apply(businessPublication.getTypeOfCapitalReduction()),
		                               TYPE_OF_TRANSFER_CONVERTER.apply(businessPublication.getTypeOfTransfer()),
		                               TYPE_OF_LIQUIDATION_CONVERTER.apply(businessPublication.getTypeOfLiquidation())
		);
	}
}
