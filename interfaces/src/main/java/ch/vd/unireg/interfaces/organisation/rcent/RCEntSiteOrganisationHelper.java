package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.function.Predicate;

import ch.vd.evd0022.v3.Capital;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRCRCEnt;
import ch.vd.unireg.interfaces.organisation.data.DonneesREE;
import ch.vd.unireg.interfaces.organisation.data.DonneesREERCEnt;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.OrganisationLocation;
import ch.vd.unireg.interfaces.organisation.rcent.converters.AddressConverters;
import ch.vd.unireg.interfaces.organisation.rcent.converters.BurRegistrationDataConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.BusinessPublicationConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CapitalConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CapitalPredicate;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CommercialRegisterDiaryEntryConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CommercialRegisterRegistrationDataConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.LegalFormConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.OrganisationFunctionConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.SeatConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfLocationConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterDeregistrationReasonConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterStatusConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterTypeOfOrganisationConverter;

public class RCEntSiteOrganisationHelper {

	private static final TypeOfLocationConverter TYPE_OF_LOCATION_CONVERTER = new TypeOfLocationConverter();
	private static final OrganisationFunctionConverter FUNCTION_CONVERTER = new OrganisationFunctionConverter();
	private static final AddressConverters.LegalAddressConverter ADDRESS_LEGALE_CONVERTER = new AddressConverters.LegalAddressConverter();
	private static final AddressConverters.POBoxAddressConverter ADDRESS_BOITE_POSTALE_CONVERTER = new AddressConverters.POBoxAddressConverter();
	private static final AddressConverters.EffectiveAddressConverter ADDRESS_EFFECIVE_CONVERTER = new AddressConverters.EffectiveAddressConverter();
	private static final CommercialRegisterRegistrationDataConverter COMMERCIAL_REGISTER_DATA_CONVERTER = new CommercialRegisterRegistrationDataConverter();
	private static final CapitalConverter CAPITAL_CONVERTER = new CapitalConverter();
	private static final CommercialRegisterDiaryEntryConverter DIARY_ENTRY_CONVERTER = new CommercialRegisterDiaryEntryConverter();
	private static final BusinessPublicationConverter BUSINESS_PUBLICATION_CONVERTER = new BusinessPublicationConverter();
	private static final UidRegisterStatusConverter UID_REGISTER_STATUS_CONVERTER = new UidRegisterStatusConverter();
	private static final UidRegisterTypeOfOrganisationConverter UID_REGISTER_TYPE_OF_ORGANISATION_CONVERTER = new UidRegisterTypeOfOrganisationConverter();
	private static final UidRegisterDeregistrationReasonConverter UID_REGISTER_LIQUIDATION_REASON_CONVERTER = new UidRegisterDeregistrationReasonConverter();
	private static final Predicate<Capital> CAPITAL_PREDICATE = new CapitalPredicate();
	private static final BurRegistrationDataConverter BUR_REGISTRATION_DATA_CONVERTER = new BurRegistrationDataConverter();

	public static SiteOrganisationRCEnt get(OrganisationLocation rcEntLocation, ServiceInfrastructureRaw infraService) {

		final OrganisationLocation.RCEntRCData rc = rcEntLocation.getRc();
		final OrganisationLocation.RCEntUIDData uid = rcEntLocation.getUid();
		final OrganisationLocation.RCEntBURData bur = rcEntLocation.getBur();

		return new SiteOrganisationRCEnt(
				rcEntLocation.getCantonalId(),
				RCEntHelper.convert(rcEntLocation.getIdentifiers()),
				RCEntHelper.convert(rcEntLocation.getName()),
				RCEntHelper.convert(rcEntLocation.getAdditionalName()),
				RCEntHelper.convertAndMap(rcEntLocation.getTypeOfLocation(), TYPE_OF_LOCATION_CONVERTER),
				RCEntHelper.convertAndMap(rcEntLocation.getLegalForm(), new LegalFormConverter()),
				RCEntHelper.convertAndDerange(rcEntLocation.getSeat(), new SeatConverter(infraService)),
				RCEntHelper.convertAndMap(rcEntLocation.getFunction(), FUNCTION_CONVERTER),
				createDonneesRC(rc),
				createDonneesIDE(uid),
				createDonneesREE(bur),
				RCEntHelper.convert(rcEntLocation.getBusinessPublication(), BUSINESS_PUBLICATION_CONVERTER),
				RCEntHelper.convert(rcEntLocation.getUidReplacedBy()),
				RCEntHelper.convert(rcEntLocation.getUidInReplacementOf()),
				RCEntHelper.convert(rcEntLocation.getBurTransferTo()),
				RCEntHelper.convert(rcEntLocation.getBurTransferFrom())
		);
	}

	private static DonneesRC createDonneesRC(OrganisationLocation.RCEntRCData rc) {
		return new DonneesRCRCEnt(
				RCEntHelper.convertAndDerange(rc.getLegalAddress(), ADDRESS_LEGALE_CONVERTER),
				RCEntHelper.convertAndMap(rc.getRegistrationData(), COMMERCIAL_REGISTER_DATA_CONVERTER),
				RCEntHelper.convertAndDerange(rc.getCapital(), CAPITAL_CONVERTER, CAPITAL_PREDICATE),
				RCEntHelper.convert(rc.getPurpose()),
				RCEntHelper.convert(rc.getByLawsDate()),
				RCEntHelper.convert(rc.getDiaryEntries(), DIARY_ENTRY_CONVERTER)
		);
	}

	private static DonneesRegistreIDE createDonneesIDE(final OrganisationLocation.RCEntUIDData uid) {
		return new DonneesRegistreIDERCEnt(
				RCEntHelper.convertAndDerange(uid.getPostOfficeBoxAddress(), ADDRESS_BOITE_POSTALE_CONVERTER),
				RCEntHelper.convertAndMap(uid.getStatus(), UID_REGISTER_STATUS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getTypeOfOrganisation(), UID_REGISTER_TYPE_OF_ORGANISATION_CONVERTER),
				RCEntHelper.convertAndDerange(uid.getEffectiveAddress(), ADDRESS_EFFECIVE_CONVERTER),
				RCEntHelper.convertAndMap(uid.getLiquidationReason(), UID_REGISTER_LIQUIDATION_REASON_CONVERTER)
		);
	}

	private static DonneesREE createDonneesREE(OrganisationLocation.RCEntBURData bur) {
		return new DonneesREERCEnt(
				RCEntHelper.convertAndMap(bur.getRegistrationData(), BUR_REGISTRATION_DATA_CONVERTER)
		);
	}
}
