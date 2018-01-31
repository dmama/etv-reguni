package ch.vd.uniregctb.xml.party.v5;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.landtaxlightening.v1.HousingActData;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.landtaxlightening.v1.UseData;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.xml.DataHelper;

public class LandTaxLighteningBuilder {

	@NotNull
	public static IfoncExemption buildIfoncExemption(@NotNull ExonerationIFONC exo) {
		final IfoncExemption exemption = new IfoncExemption();
		exemption.setDateFrom(DataHelper.coreToXMLv2(exo.getDateDebut()));
		exemption.setDateTo(DataHelper.coreToXMLv2(exo.getDateFin()));
		exemption.setExemptionPercent(exo.getPourcentageExoneration());
		exemption.setImmovablePropertyId(exo.getImmeuble().getId());
		return exemption;
	}

	@NotNull
	public static IciAbatement buildIciAbatement(@NotNull DegrevementICI degrev) {
		final IciAbatement abatement = new IciAbatement();
		abatement.setDateFrom(DataHelper.coreToXMLv2(degrev.getDateDebut()));
		abatement.setDateTo(DataHelper.coreToXMLv2(degrev.getDateFin()));
		abatement.setOwnUse(buildUseData(degrev.getPropreUsage()));
		abatement.setRentalUse(buildUseData(degrev.getLocation()));
		abatement.setHousingAct(buildHousingAct(degrev.getLoiLogement()));
		abatement.setImmovablePropertyId(degrev.getImmeuble().getId());
		abatement.setAbatementPercent(degrev.getPourcentageDegrevement().stripTrailingZeros());
		return abatement;
	}

	@NotNull
	public static IciAbatementRequest buildIciAbatementRequest(@NotNull DemandeDegrevementICI demande) {
		final IciAbatementRequest request = new IciAbatementRequest();
		request.setSendDate(DataHelper.coreToXMLv2(demande.getDateEnvoi()));
		request.setDeadline(DataHelper.coreToXMLv2(demande.getDelaiRetour()));
		request.setReminderDate(DataHelper.coreToXMLv2(demande.getDateRappel()));
		request.setReturnDate(DataHelper.coreToXMLv2(demande.getDateRetour()));
		request.setSequenceNumber(demande.getNumeroSequence());
		request.setTaxPeriod(demande.getPeriodeFiscale());
		request.setImmovablePropertyId(demande.getImmeuble().getId());
		return request;
	}

	@Nullable
	private static UseData buildUseData(@Nullable DonneesUtilisation utilisation) {
		if (utilisation == null) {
			return null;
		}
		final UseData data = new UseData();
		data.setArea(utilisation.getSurface());
		data.setVolume(utilisation.getVolume());
		data.setIncome(utilisation.getRevenu());
		data.setDeclaredPercent(utilisation.getPourcentage());
		data.setApprovedPercent(utilisation.getPourcentageArrete());
		return data;
	}

	@Nullable
	private static HousingActData buildHousingAct(@Nullable DonneesLoiLogement loi) {
		if (loi == null) {
			return null;
		}
		final HousingActData data = new HousingActData();
		data.setGrantDate(DataHelper.coreToXMLv2(loi.getDateOctroi()));
		data.setExpiryDate(DataHelper.coreToXMLv2(loi.getDateEcheance()));
		data.setSocialNaturePercent(loi.getPourcentageCaractereSocial());
		data.setUnderControlOfHousingOffice(loi.getControleOfficeLogement() != null && loi.getControleOfficeLogement());
		return data;
	}
}
