package ch.vd.unireg.norentes.civil.annulation.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;

public abstract class AbstractAnnulationSeparationScenario extends EvenementCivilScenario {

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_SEPARATION;
	}

	protected void checkHabitantApresAnnulation(PersonnePhysique habitant, RegDate dateSeparation) {
		ForFiscalPrincipal ffp = habitant.getForFiscalPrincipalAt(null);
		assertNull(ffp, "L'habitant " + habitant.getNumero() + " ne doit pas avoir de for principal actif après l'annulation de la séparation");
		// Vérification de l'annulation des anciens fors fiscaux, créés lors de la séparation
		for (ForFiscal forFiscal : habitant.getForsFiscaux()) {
			// recherche des fors ouverts avec date de début égal à celle de la séparation
			// ces fors doivent être annulés
			if (forFiscal.getDateFin() == null && dateSeparation.equals(forFiscal.getDateDebut()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT == ((ForFiscalRevenuFortune) forFiscal).getMotifOuverture())) {
				assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux ouverts lors de la séparation doivent être annulés");
			}
		}
	}
}