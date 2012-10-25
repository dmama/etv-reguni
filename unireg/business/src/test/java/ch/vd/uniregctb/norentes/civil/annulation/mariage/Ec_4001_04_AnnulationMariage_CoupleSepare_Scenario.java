package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de mariage d'un couple séparé.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_04_AnnulationMariage_CoupleSepare_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4001_04_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de mariage d'un couple séparé (cas d'erreur).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				super.init();

				MockIndividu momo = getIndividu(noIndMomo);
				MockIndividu bea = getIndividu(noIndBea);
				separeIndividus(momo, bea, dateMariage);
			}
		});
	}

	private static final long noIndMomo = 54321; // Maurice
	private static final long noIndBea = 23456; // Béatrice

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateDebutMomo = RegDate.get(1997, 1, 4);
	private final RegDate datedebutBea = RegDate.get(1991, 8, 20);
	private final RegDate dateMariage = RegDate.get(2005, 4, 8);
	private final RegDate dateSeparation = RegDate.get(2007, 12, 3);
	private final MockCommune commune = MockCommune.Lausanne;

	@Etape(id=1, descr="Chargement des habitants et leur ménage")
	public void step1() {
		// Maurice
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		ForFiscalPrincipal ffpMomo = addForFiscalPrincipal(momo, commune, dateDebutMomo, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffpMomo.setModeImposition(ModeImposition.ORDINAIRE);

		ffpMomo = addForFiscalPrincipal(momo, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
		ffpMomo.setModeImposition(ModeImposition.ORDINAIRE);

		// Béatrice
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		ForFiscalPrincipal ffpBea = addForFiscalPrincipal(bea, commune, datedebutBea, dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffpBea.setModeImposition(ModeImposition.ORDINAIRE);

		ffpBea = addForFiscalPrincipal(bea, commune, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
		ffpBea.setModeImposition(ModeImposition.ORDINAIRE);

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, bea, dateMariage, dateSeparation.getOneDayBefore());

		ForFiscalPrincipal ffpMennage = addForFiscalPrincipal(menage, commune, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		ffpMennage.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que les habitants ont chacun un For ouvert et le For du ménage est fermé")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Mariage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_MARIAGE, noIndBea, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement civil est en erreur")
	public void check2() {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabBea);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement d'annulation de mariage devrait être en erreur car le couple est séparé");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir exactement une erreur");

		final EvenementCivilRegPPErreur erreur = evt.getErreurs().iterator().next();
		assertNotNull(erreur, "Il me manque une erreur...");
		assertTrue(erreur.getMessage().contains("Il y a eu d'autres opérations après le mariage/réconciliation"), "Mauvaise erreur");
	}
}
