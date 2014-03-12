package ch.vd.uniregctb.norentes.civil.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.MotifFor;

public abstract class MariageApresArriveeScenarios extends EvenementCivilScenario {

	protected long checkArriveeHabitant(long numeroIndividu, RegDate dateArrivee) {
		PersonnePhysique habitant = tiersDAO.getHabitantByNumeroIndividu(numeroIndividu);
		assertNotNull(habitant, "L'habitant correspondant à l'invidu n° " + numeroIndividu + " n'a pas été créé");
		{
			ForFiscalPrincipal ffp = habitant.getForFiscalPrincipalAt(dateArrivee);
			assertNotNull(ffp, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()) + " devrait avoir un for principal");
			assertEquals(dateArrivee, ffp.getDateDebut(), "La date d'ouverture du for est pas valide");
			assertNull(ffp.getDateFin(), "Le for est fermé");
		}

		return habitant.getNumero();
	}

	protected void checkHabitantApresMariage(PersonnePhysique pp, RegDate dateArrivee) {
		assertNotNull(pp, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) + " n'a pas été trouvé");
		ForFiscalPrincipal ffp = pp.getForFiscalPrincipalAt(null);
		assertNull(ffp, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()) + " devrait pas avoir de for fiscal principal ouvert");
		for (ForFiscalPrincipal ff : pp.getForsParType(false).principaux) {
			if (dateArrivee.equals(ff.getDateDebut())) {
				assertEquals(true, ff.isAnnule(), "Le for créé lors de l'arrivée devrait être annulé");
			}
		}
	}

	protected void checkMenageApresMariage(MenageCommun menage, RegDate dateDebutRapportEntreTiers, RegDate dateDebutFor, MotifFor motifOuvertureFor) {
		assertNotNull(menage, "Le ménage n'a pas été créé");
		for (RapportEntreTiers rapport : menage.getRapportsObjet()) {
			assertEquals(dateDebutRapportEntreTiers, rapport.getDateDebut(), "Le rapport n'a pas été créé à la bonne date");
			assertNull(rapport.getDateFin(), "Le rapport est fermé");
		}
		ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Le for principal du ménage n'a pas été trouvé");
		assertEquals(dateDebutFor, ffp.getDateDebut(), "La date de début du for principal du ménage n'est pas correcte");
		assertNull(ffp.getDateFin(), "Le for principal du ménage est fermé");
		assertEquals(motifOuvertureFor, ffp.getMotifOuverture(), "Le motif d'ouverture du for principal du ménage n'est pas correct");
	}

}
