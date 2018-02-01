package ch.vd.unireg.xml.party.v5;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.landregistry.v1.AcquisitionReason;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.xml.DataHelper;

public class AcquisitionReasonBuilder {
	@Nullable
	public static AcquisitionReason get(@Nullable RaisonAcquisitionRF raisonAcquisition) {
		if (raisonAcquisition == null) {
			return null;
		}
		return newAcquisitionReason(raisonAcquisition);
	}

	@NotNull
	private static AcquisitionReason newAcquisitionReason(@NotNull RaisonAcquisitionRF raisonAcquisition) {
		final AcquisitionReason reason = new AcquisitionReason();
		reason.setDate(DataHelper.coreToXMLv2(raisonAcquisition.getDateAcquisition()));
		reason.setReason(raisonAcquisition.getMotifAcquisition());
		reason.setCaseIdentifier(LandRightBuilder.getCaseIdentifier(raisonAcquisition.getNumeroAffaire()));
		return reason;
	}
}
