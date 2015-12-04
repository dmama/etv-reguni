package ch.vd.uniregctb.xml.party.v4;

import ch.vd.unireg.xml.party.taxpayer.v4.FamilyStatus;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class FamilyStatusBuilder {

	public static FamilyStatus newFamilyStatus(VueSituationFamille situation) {
		final FamilyStatus s = new FamilyStatus();
		s.setDateFrom(DataHelper.coreToXMLv2(situation.getDateDebut()));
		s.setDateTo(DataHelper.coreToXMLv2(situation.getDateFin()));
		s.setCancellationDate(DataHelper.coreToXMLv2(situation.getAnnulationDate()));
		s.setNumberOfChildren(situation.getNombreEnfants());

		if (situation instanceof ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) {
			final ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun situtationMenage = (ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) situation;

			s.setApplicableTariff(EnumHelper.coreToXMLv4(situtationMenage.getTarifApplicable()));
			final Long numeroContribuablePrincipal = situtationMenage.getNumeroContribuablePrincipal();
			s.setMainTaxpayerNumber(numeroContribuablePrincipal == null ? null : numeroContribuablePrincipal.intValue());
		}

		s.setMaritalStatus(EnumHelper.coreToXMLv4(situation.getEtatCivil()));
		return s;
	}
}
