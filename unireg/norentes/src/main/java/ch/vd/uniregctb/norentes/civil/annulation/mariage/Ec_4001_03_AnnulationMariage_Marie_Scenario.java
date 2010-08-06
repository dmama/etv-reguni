package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de mariage d'un couple de mariés.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_03_AnnulationMariage_Marie_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4001_03_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de mariage d'un couple de mariés.";
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

	private final long noIndMomo = 54321; // Maurice
	private final long noIndBea = 23456; // Béatrice

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateDebutMomo = RegDate.get(1997, 1, 4);
	private final RegDate datedebutBea = RegDate.get(1991, 8, 20);
	private final RegDate dateMariage = RegDate.get(2005, 4, 8);
	private final MockCommune commune = MockCommune.Lausanne;

	@Etape(id=1, descr="Chargement des habitants et leur ménage")
	public void step1() {
		// Maurice
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		ForFiscalPrincipal ffpMomo = addForFiscalPrincipal(momo, commune, dateDebutMomo, dateMariage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffpMomo.setModeImposition(ModeImposition.ORDINAIRE);

		// Béatrice
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		ForFiscalPrincipal ffpBea = addForFiscalPrincipal(bea, commune, datedebutBea, dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffpBea.setModeImposition(ModeImposition.ORDINAIRE);

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, null);
		tiersService.addTiersToCouple(menage, bea, dateMariage, null);

		ForFiscalPrincipal ffpMennage = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		ffpMennage.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que les habitants ont chacun un For fermé et le For du ménage est ouvert")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMajuscule());
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation de Mariage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_MARIAGE, noIndMomo, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le For principal du ménage a été fermé et celui des habitants rouverts")
	public void check2() {
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabMomo), dateDebutMomo, MotifFor.ARRIVEE_HC);
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabBea), datedebutBea, MotifFor.MAJORITE);
	}

	private void checkHabitantApresAnnulation(PersonnePhysique habitant, RegDate dateFor, MotifFor motifFor) {
		ForFiscalPrincipal ffp = habitant.getForFiscalPrincipalAt(null);
		assertNotNull(ffp, "Pierre doit avoir un for principal actif après l'annulation de mariage");
		assertEquals(dateFor, ffp.getDateDebut(), "Le for de l'habitant " + habitant.getNumero() + " devrait commencer le " + dateFor);
		assertEquals(motifFor, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas " + motifFor.name());
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + habitant.getNumero() + " est fermé");
		assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être null");
		// Vérification des fors fiscaux
		for (ForFiscal forFiscal : habitant.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(((ForFiscalRevenuFortune) forFiscal).getMotifFermeture()))) {
				assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux fermés lors du mariage doivent être annulés");
			}
		}
	}
}
