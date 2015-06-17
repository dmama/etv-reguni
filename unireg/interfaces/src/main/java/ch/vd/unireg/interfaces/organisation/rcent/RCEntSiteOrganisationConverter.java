package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.UidRegisterLiquidationReason;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.unireg.interfaces.organisation.data.Adresse;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.Fonction;
import ch.vd.unireg.interfaces.organisation.data.RaisonLiquidationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOfCapital;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class RCEntSiteOrganisationConverter {

	public static SiteOrganisation get(final OrganisationLocation rcEntLocation) {

		DonneesRC donneesRC = createDonneesRC(rcEntLocation.getRc());
		DonneesRegistreIDE donneesIDE = createDonneesIDE(rcEntLocation.getUid());

		return new SiteOrganisation(
				rcEntLocation.getCantonalId(),
				RCEntHelper.convert(rcEntLocation.getName()),
				donneesRC,
				donneesIDE,
				RCEntHelper.convert(rcEntLocation.getIdentifiers()),
				RCEntHelper.convert(rcEntLocation.getOtherNames()),
				RCEntHelper.convertAndMap(rcEntLocation.getKindOfLocation(), new Function<ch.vd.evd0022.v1.KindOfLocation, TypeDeSite>() {
					@Override
					public TypeDeSite apply(ch.vd.evd0022.v1.KindOfLocation kindOfLocation) {
						return TypeDeSite.valueOf(kindOfLocation.toString());
					}
				}),
				RCEntHelper.convert(rcEntLocation.getSeat()),
				RCEntHelper.convertAndMap(rcEntLocation.getFunction(), new Function<ch.vd.evd0022.v1.Function, Fonction>() {
					@Override
					public Fonction apply(ch.vd.evd0022.v1.Function function) {
						return RCEntFonctionConverter.get(function);
					}
				}),
				RCEntHelper.convert(rcEntLocation.getReplacedBy()),
				RCEntHelper.convert(rcEntLocation.getInReplacementOf())
		);
	}

	@NotNull
	private static DonneesRC createDonneesRC(OrganisationLocation.RCEntRCData rc) {
		return new DonneesRC(
					RCEntHelper.convertAndMap(rc.getLegalAddress(), new Function<Address, Adresse>() {
						@Override
						public Adresse apply(Address address) {
							return RCEntAddressHelper.fromRCEntAddress(address);
						}
					}),
					RCEntHelper.convertAndMap(rc.getStatus(), new Function<ch.vd.evd0022.v1.CommercialRegisterStatus, StatusRC>() {
						@Override
						public StatusRC apply(ch.vd.evd0022.v1.CommercialRegisterStatus commercialRegisterStatus) {
							return StatusRC.valueOf(commercialRegisterStatus.toString());
						}
					}),
					RCEntHelper.convert(rc.getName()),
					RCEntHelper.convertAndMap(rc.getEntryStatus(), new Function<ch.vd.evd0022.v1.CommercialRegisterEntryStatus, StatusInscriptionRC>() {
						@Override
						public StatusInscriptionRC apply(ch.vd.evd0022.v1.CommercialRegisterEntryStatus commercialRegisterEntryStatus) {
							return StatusInscriptionRC.valueOf(commercialRegisterEntryStatus.toString());
						}
					}),
					RCEntHelper.convertAndMap(rc.getCapital(), new Function<ch.vd.evd0022.v1.Capital, Capital>() {
						@Override
						public Capital apply(ch.vd.evd0022.v1.Capital capital) {
							return new Capital(TypeOfCapital.valueOf(capital.getTypeOfCapital().toString()), capital.getCurrency(), capital.getCapitalAmount(), capital.getCashedInAmount(), capital.getDivision());
						}
					})
			);
	}

	private static DonneesRegistreIDE createDonneesIDE(final OrganisationLocation.RCEntUIDData uid) {
		return new DonneesRegistreIDE(
				RCEntHelper.convertAndMap(uid.getPostOfficeBoxAddress(), new Function<Address, Adresse>() {
					@Override
					public Adresse apply(Address address) {
						return RCEntAddressHelper.fromRCEntAddress(address);
					}
				}),
				RCEntHelper.convertAndMap(uid.getStatus(), new Function<UidRegisterStatus, StatusRegistreIDE>() {
					@Override
					public StatusRegistreIDE apply(UidRegisterStatus uidRegisterStatus) {
						return StatusRegistreIDE.valueOf(uidRegisterStatus.toString());
					}
				}),
				RCEntHelper.convertAndMap(uid.getTypeOfOrganisation(), new Function<UidRegisterTypeOfOrganisation, TypeOrganisationRegistreIDE>() {
					@Override
					public TypeOrganisationRegistreIDE apply(UidRegisterTypeOfOrganisation uidRegisterTypeOfOrganisation) {
						return TypeOrganisationRegistreIDE.valueOf(uidRegisterTypeOfOrganisation.toString());
					}
				}),
				RCEntHelper.convertAndMap(uid.getEffectiveAddress(), new Function<Address, Adresse>() {
					@Override
					public Adresse apply(Address address) {
						return RCEntAddressHelper.fromRCEntAddress(address);
					}
				}),
				RCEntHelper.convertAndMap(uid.getLiquidationReason(), new Function<UidRegisterLiquidationReason, RaisonLiquidationRegistreIDE>() {
					@Override
					public RaisonLiquidationRegistreIDE apply(UidRegisterLiquidationReason uidRegisterLiquidationReason) {
						return RaisonLiquidationRegistreIDE.valueOf(uidRegisterLiquidationReason.toString());
					}
				})
		);
	}

}
