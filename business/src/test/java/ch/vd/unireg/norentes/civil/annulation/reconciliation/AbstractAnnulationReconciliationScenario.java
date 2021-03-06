package ch.vd.unireg.norentes.civil.annulation.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;

public abstract class AbstractAnnulationReconciliationScenario extends EvenementCivilScenario {

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_RECONCILIATION;
	}
	
	protected void checkHabitantApresAnnulation(PersonnePhysique habitant, RegDate dateDernierFor, MotifFor motifDernierFor, RegDate dateReconciliation) {
		ForFiscalPrincipal ffp = habitant.getForFiscalPrincipalAt(null);
		assertNotNull(ffp, "L'habitant " + habitant.getNumero() + " doit avoir un for principal actif après l'annulation de réconciliation");
		assertEquals(dateDernierFor, ffp.getDateDebut(), "Le for de l'habitant " + habitant.getNumero() + " devrait commencer le " + dateDernierFor);
		assertEquals(motifDernierFor, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas " + motifDernierFor.name());
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + habitant.getNumero() + " est fermé");
		assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être null");
		// Vérification des fors fiscaux
		for (ForFiscal forFiscal : habitant.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateReconciliation.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux fermés lors de la réconciliation doivent être annulés");
			}
		}
	}

}
