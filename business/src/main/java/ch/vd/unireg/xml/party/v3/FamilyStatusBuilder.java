package ch.vd.unireg.xml.party.v3;

import ch.vd.unireg.xml.party.taxpayer.v3.FamilyStatus;
import ch.vd.unireg.situationfamille.VueSituationFamille;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public class FamilyStatusBuilder {
	public static FamilyStatus newFamilyStatus(VueSituationFamille situation) {
		final FamilyStatus s = new FamilyStatus();
		s.setDateFrom(DataHelper.coreToXMLv2(situation.getDateDebut()));
		s.setDateTo(DataHelper.coreToXMLv2(situation.getDateFin()));
		s.setCancellationDate(DataHelper.coreToXMLv2(situation.getAnnulationDate()));
		s.setNumberOfChildren(situation.getNombreEnfants());

		if (situation instanceof ch.vd.unireg.situationfamille.VueSituationFamilleMenageCommun) {
			final ch.vd.unireg.situationfamille.VueSituationFamilleMenageCommun situtationMenage = (ch.vd.unireg.situationfamille.VueSituationFamilleMenageCommun) situation;

			s.setApplicableTariff(EnumHelper.coreToXMLv3(situtationMenage.getTarifApplicable()));
			final Long numeroContribuablePrincipal = situtationMenage.getNumeroContribuablePrincipal();
			s.setMainTaxpayerNumber(numeroContribuablePrincipal == null ? null : numeroContribuablePrincipal.intValue());
		}

		s.setMaritalStatus(EnumHelper.coreToXMLv3(situation.getEtatCivil()));
		return s;
	}
}
