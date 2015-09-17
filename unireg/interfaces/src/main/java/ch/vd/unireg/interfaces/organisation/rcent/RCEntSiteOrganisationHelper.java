package ch.vd.unireg.interfaces.organisation.rcent;

import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.rcent.converters.AddressConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CapitalConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CommercialRegisterEntryStatusConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CommercialRegisterStatusConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.KindOfLocationConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.OrganisationFunctionConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterLiquidationReasonConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterStatusConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterTypeOfOrganisationConverter;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class RCEntSiteOrganisationHelper {

	private static final KindOfLocationConverter KIND_OF_LOCATION_CONVERTER = new KindOfLocationConverter();
	private static final OrganisationFunctionConverter FUNCTION_CONVERTER = new OrganisationFunctionConverter();
	private static final AddressConverter ADDRESS_CONVERTER = new AddressConverter();
	private static final CommercialRegisterStatusConverter COMMERCIAL_REGISTER_STATUS_CONVERTER = new CommercialRegisterStatusConverter();
	private static final CommercialRegisterEntryStatusConverter COMMERCIAL_REGISTER_ENTRY_STATUS_CONVERTER = new CommercialRegisterEntryStatusConverter();
	private static final CapitalConverter CAPITAL_CONVERTER = new CapitalConverter();
	private static final UidRegisterStatusConverter UID_REGISTER_STATUS_CONVERTER = new UidRegisterStatusConverter();
	private static final UidRegisterTypeOfOrganisationConverter UID_REGISTER_TYPE_OF_ORGANISATION_CONVERTER = new UidRegisterTypeOfOrganisationConverter();
	private static final UidRegisterLiquidationReasonConverter UID_REGISTER_LIQUIDATION_REASON_CONVERTER = new UidRegisterLiquidationReasonConverter();

	public static SiteOrganisationRCEnt get(final OrganisationLocation rcEntLocation) {

		final OrganisationLocation.RCEntRCData rc = rcEntLocation.getRc();
		final OrganisationLocation.RCEntUIDData uid = rcEntLocation.getUid();

		return new SiteOrganisationRCEnt(
				RCEntHelper.convert(rcEntLocation.getName()),
				createDonneesRC(rc),
				createDonneesIDE(uid),
				RCEntHelper.convert(rcEntLocation.getIdentifiers()),
				RCEntHelper.convert(rcEntLocation.getOtherNames()),
				RCEntHelper.convertAndMap(rcEntLocation.getKindOfLocation(), KIND_OF_LOCATION_CONVERTER),
				RCEntHelper.convert(rcEntLocation.getSeat()),
				RCEntHelper.convertAndMap(rcEntLocation.getFunction(), FUNCTION_CONVERTER)
		);
	}

	private static DonneesRC createDonneesRC(OrganisationLocation.RCEntRCData rc) {
		return new DonneesRC(
				RCEntHelper.convertAndMap(rc.getLegalAddress(), ADDRESS_CONVERTER),
				RCEntHelper.convertAndMap(rc.getStatus(), COMMERCIAL_REGISTER_STATUS_CONVERTER),
				RCEntHelper.convert(rc.getName()),
				RCEntHelper.convertAndMap(rc.getEntryStatus(), COMMERCIAL_REGISTER_ENTRY_STATUS_CONVERTER),
				RCEntHelper.convertAndMap(rc.getCapital(), CAPITAL_CONVERTER)
		);
	}

	private static DonneesRegistreIDE createDonneesIDE(final OrganisationLocation.RCEntUIDData uid) {
		return new DonneesRegistreIDE(
				RCEntHelper.convertAndMap(uid.getPostOfficeBoxAddress(), ADDRESS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getStatus(), UID_REGISTER_STATUS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getTypeOfOrganisation(), UID_REGISTER_TYPE_OF_ORGANISATION_CONVERTER),
				RCEntHelper.convertAndMap(uid.getEffectiveAddress(), ADDRESS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getLiquidationReason(), UID_REGISTER_LIQUIDATION_REASON_CONVERTER)
		);
	}
}
