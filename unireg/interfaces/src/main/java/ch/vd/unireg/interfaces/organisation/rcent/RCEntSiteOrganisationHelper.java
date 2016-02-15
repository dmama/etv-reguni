package ch.vd.unireg.interfaces.organisation.rcent;

import org.apache.commons.collections4.Predicate;

import ch.vd.evd0022.v3.Capital;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRCRCEnt;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.rcent.converters.AddressConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CapitalConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CapitalPredicate;
import ch.vd.unireg.interfaces.organisation.rcent.converters.CommercialRegisterStatusConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.LegalFormConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.OrganisationFunctionConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.SeatConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfLocationConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterRaisonDeRadiationRegistreIDEConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterStatusConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterTypeOfOrganisationConverter;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class RCEntSiteOrganisationHelper {

	private static final TypeOfLocationConverter TYPE_OF_LOCATION_CONVERTER = new TypeOfLocationConverter();
	private static final OrganisationFunctionConverter FUNCTION_CONVERTER = new OrganisationFunctionConverter();
	private static final AddressConverter ADDRESS_CONVERTER = new AddressConverter();
	private static final CommercialRegisterStatusConverter COMMERCIAL_REGISTER_STATUS_INSCRIPTION_CONVERTER = new CommercialRegisterStatusConverter();
	private static final CapitalConverter CAPITAL_CONVERTER = new CapitalConverter();
	private static final UidRegisterStatusConverter UID_REGISTER_STATUS_CONVERTER = new UidRegisterStatusConverter();
	private static final UidRegisterTypeOfOrganisationConverter UID_REGISTER_TYPE_OF_ORGANISATION_CONVERTER = new UidRegisterTypeOfOrganisationConverter();
	private static final UidRegisterRaisonDeRadiationRegistreIDEConverter UID_REGISTER_LIQUIDATION_REASON_CONVERTER = new UidRegisterRaisonDeRadiationRegistreIDEConverter();
	private static final Predicate<Capital> CAPITAL_PREDICATE = new CapitalPredicate();

	public static SiteOrganisationRCEnt get(OrganisationLocation rcEntLocation, ServiceInfrastructureRaw infraService) {

		final OrganisationLocation.RCEntRCData rc = rcEntLocation.getRc();
		final OrganisationLocation.RCEntUIDData uid = rcEntLocation.getUid();

		return new SiteOrganisationRCEnt(
				rcEntLocation.getCantonalId(),
				RCEntHelper.convert(rcEntLocation.getIdentifiers()),
				RCEntHelper.convert(rcEntLocation.getName()),
				RCEntHelper.convert(rcEntLocation.getAdditionalName()),
				RCEntHelper.convertAndMap(rcEntLocation.getTypeOfLocation(), TYPE_OF_LOCATION_CONVERTER),
				RCEntHelper.convertAndMap(rcEntLocation.getLegalForm(), new LegalFormConverter()),
				RCEntHelper.convertAndFlatmap(rcEntLocation.getSeat(), new SeatConverter(infraService)),
				RCEntHelper.convertAndMap(rcEntLocation.getFunction(), FUNCTION_CONVERTER),
				createDonneesRC(rc),
				createDonneesIDE(uid),
				RCEntHelper.convert(rcEntLocation.getUidReplacedBy()),
				RCEntHelper.convert(rcEntLocation.getUidInReplacementOf()),
				RCEntHelper.convert(rcEntLocation.getBurTransferTo()),
				RCEntHelper.convert(rcEntLocation.getBurTransferFrom())
		);
	}

	private static DonneesRC createDonneesRC(OrganisationLocation.RCEntRCData rc) {
		return new DonneesRCRCEnt(
				RCEntHelper.convertAndFlatmap(rc.getLegalAddress(), ADDRESS_CONVERTER),
				RCEntHelper.convertAndMap(rc.getRegistrationStatus(), COMMERCIAL_REGISTER_STATUS_INSCRIPTION_CONVERTER),
				RCEntHelper.convert(rc.getRegistrationDate()),
				RCEntHelper.convertAndFlatmap(rc.getCapital(), CAPITAL_CONVERTER, CAPITAL_PREDICATE),
				RCEntHelper.convert(rc.getPurpose()),
				RCEntHelper.convert(rc.getByLawsDate()),
				RCEntHelper.convert(rc.getDeregistrationDate()));
	}

	private static DonneesRegistreIDE createDonneesIDE(final OrganisationLocation.RCEntUIDData uid) {
		return new DonneesRegistreIDERCEnt(
				RCEntHelper.convertAndFlatmap(uid.getPostOfficeBoxAddress(), ADDRESS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getStatus(), UID_REGISTER_STATUS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getTypeOfOrganisation(), UID_REGISTER_TYPE_OF_ORGANISATION_CONVERTER),
				RCEntHelper.convertAndFlatmap(uid.getEffectiveAddress(), ADDRESS_CONVERTER),
				RCEntHelper.convertAndMap(uid.getLiquidationReason(), UID_REGISTER_LIQUIDATION_REASON_CONVERTER)
		);
	}
}
