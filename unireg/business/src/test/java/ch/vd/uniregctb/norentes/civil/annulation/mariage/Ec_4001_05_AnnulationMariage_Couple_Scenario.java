package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de mariage du cas JIRA UNIREG-1086.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_05_AnnulationMariage_Couple_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4001_05_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de mariage d'un couple (UNIREG-1157).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final class DefaultMockServiceCivil extends MockServiceCivil {

		@Override
		protected void init() {

			MockIndividu alexandre = addIndividu(noIndAlexandre, dateNaissanceAlexandre, "Getaz", "Alexandre", true);
			addOrigine(alexandre, MockPays.Suisse.getNomMinuscule());
			addNationalite(alexandre, MockPays.Suisse, dateNaissanceAlexandre, null);

			MockIndividu sylvie = addIndividu(noIndSylvie, dateNaissanceSylvie, "Grandchamp", "Sylvie", false);
			addOrigine(sylvie, MockPays.Suisse.getNomMinuscule());
			addNationalite(sylvie, MockPays.Suisse, dateNaissanceSylvie, null);

			marieIndividus(alexandre, sylvie , dateMariage);
		}

		public void annuleMariage() {
			MockIndividu alexandre = getIndividu(noIndAlexandre);
			MockIndividu sylvie = getIndividu(noIndSylvie);
			annuleMariage(alexandre, sylvie);
		}

	}

	private DefaultMockServiceCivil serviceCivil;

	@Override
	protected void initServiceCivil() {
		serviceCivil = new DefaultMockServiceCivil();
		serviceCivilService.setUp(serviceCivil);
	}

	private static final long noIndAlexandre = 2000465; // Alexandre
	private static final long noIndSylvie = 2000457; // Sylvie

	private long noHabAlexandre;
	private long noHabSylvie;
	private long noMenage;

	private final RegDate dateNaissanceSylvie = RegDate.get(1971, 11, 6);
	private final RegDate dateNaissanceAlexandre = RegDate.get(1970, 3, 3);
	private final RegDate dateDebutAlexandre = RegDate.get(2009, 1, 1);
	private final RegDate dateDernierForSilvie = RegDate.get(2009, 7, 6);
	private final RegDate dateMariage = RegDate.get(2009, 7, 8);
	private final MockCommune commune = MockCommune.Lausanne;

	@Etape(id=1, descr="Chargement des habitants et leur ménage")
	public void step1() {
		// Alexandre
		PersonnePhysique alexandre = addHabitant(noIndAlexandre);
		noHabAlexandre = alexandre.getNumero();
		ForFiscalPrincipal ffpAlexandre = addForFiscalPrincipal(alexandre, commune, dateDebutAlexandre, RegDate.get(2009, 7, 7), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffpAlexandre.setModeImposition(ModeImposition.ORDINAIRE);

		// Sylvie
		PersonnePhysique sylvie = addHabitant(noIndSylvie);
		noHabSylvie = sylvie.getNumero();

		/*
		 * Fors de Sylvie
		 * ==============
		 * Revenu fortune  Domicile  Ordinaire  Villeneuve (VD)	06.07.2009  Séparation / Divorce / Dissol. Part.	07.07.2009  Mariage / Partenariat / Réconcil.
		 * Revenu fortune  Domicile  Ordinaire  Morges  		01.07.2009  Séparation / Divorce / Dissol. Part.	01.07.2009  Mariage / Partenariat / Réconcil.
		 * Revenu fortune  Domicile  Ordinaire  Morges  		01.01.2009  Arrivée hors canton  					31.03.2009  Mariage / Partenariat / Réconcil
		 */
		// premier for de Sylvie
		{
			ForFiscalPrincipal ffpSylvie = addForFiscalPrincipal(sylvie, MockCommune.Vevey, dateDebutAlexandre, RegDate.get(2009, 3, 31), MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffpSylvie.setModeImposition(ModeImposition.ORDINAIRE);
		}
		// second for de Sylvie
		{
			ForFiscalPrincipal ffpSylvie = addForFiscalPrincipal(sylvie, MockCommune.Vevey, RegDate.get(2009, 7, 1), RegDate.get(2009, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffpSylvie.setModeImposition(ModeImposition.ORDINAIRE);
		}
		// troisieme for de Sylvie
		{
			ForFiscalPrincipal ffpSylvie = addForFiscalPrincipal(sylvie, commune, dateDernierForSilvie, RegDate.get(2009, 7, 7), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffpSylvie.setModeImposition(ModeImposition.ORDINAIRE);
		}

		/*
		 * Situations famille Sylvie
		 * =========================
		 *	Divorcé					07.07.2009
		 *	Séparé					06.07.2009	06.07.2009
		 *	Marié-e					02.07.2009	05.07.2009
		 *	Parten. Dissous judic.	01.07.2009	01.07.2009
		 *	Partenariat enregistré	01.04.2009	30.06.2009
		 *	Célibataire				28.06.1970	31.03.2009
		 */
		addSituationFamille(sylvie, RegDate.get(1970, 6, 28),	RegDate.get(2009, 3, 31), 	EtatCivil.CELIBATAIRE, 0);
		addSituationFamille(sylvie, RegDate.get(2009, 4,  1),	RegDate.get(2009, 6, 30), 	EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0);
		addSituationFamille(sylvie, RegDate.get(2009, 7,  1),	RegDate.get(2009, 7,  1), 	EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT, 0);
		addSituationFamille(sylvie, RegDate.get(2009, 7,  2),	RegDate.get(2009, 7,  5), 	EtatCivil.MARIE, 0);
		addSituationFamille(sylvie, dateDernierForSilvie,		dateDernierForSilvie, 		EtatCivil.SEPARE, 0);
		addSituationFamille(sylvie, RegDate.get(2009, 7,  7), 	null, 						EtatCivil.DIVORCE, 0);

		// Ménage commun
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		/*
		 * Rapports ménage
		 * ===============
		 * Appartenance ménage	08.07.2009	101.066.03	Alexandre Getaz
		 * Appartenance ménage	08.07.2009	101.046.03	Sylvie Grandchamp
		 */
		tiersService.addTiersToCouple(menage, alexandre, dateMariage, null);
		tiersService.addTiersToCouple(menage, sylvie, dateMariage, null);

		ForFiscalPrincipal ffpMenage = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		ffpMenage.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que les habitants ont chacun un For ouvert et le For du ménage est fermé")
	public void check1() {
		{
			PersonnePhysique alexandre = (PersonnePhysique) tiersDAO.get(noHabAlexandre);
			ForFiscalPrincipal ffp = alexandre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + alexandre.getNumero() + " null");
			assertNotNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
		{
			PersonnePhysique sylvie = (PersonnePhysique) tiersDAO.get(noHabSylvie);
			ForFiscalPrincipal ffp = sylvie.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + sylvie.getNumero() + " null");
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
		// annulation du mariage dans le civil
		serviceCivil.annuleMariage();
		// envoi de l'événement
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_MARIAGE, noIndAlexandre, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement civil est en erreur")
	public void check2() {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabAlexandre);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement d'annulation de mariage devrait être traité");

		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabAlexandre), dateDebutAlexandre, MotifFor.ARRIVEE_HC, null);
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabSylvie), dateDernierForSilvie, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, EtatCivil.DIVORCE);
	}

	private void checkHabitantApresAnnulation(PersonnePhysique habitant, RegDate dateFor, MotifFor motifFor, EtatCivil situationFamille) {
		ForFiscalPrincipal ffp = habitant.getForFiscalPrincipalAt(null);

		final String numeroHabitant = FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero());
		assertNotNull(ffp, "L'habitant " + numeroHabitant + " doit avoir un for principal actif après l'annulation de mariage");
		assertEquals(dateFor, ffp.getDateDebut(), "Le for de l'habitant " + numeroHabitant + " devrait commencer le " + dateFor);
		assertEquals(motifFor, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas " + motifFor.name());
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + numeroHabitant + " est fermé");
		assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être null");
		// Vérification des fors fiscaux
		for (ForFiscal forFiscal : habitant.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux fermés lors du mariage doivent être annulés");
			}
		}
		SituationFamille sf = habitant.getSituationFamilleActive();
		if (situationFamille == null) {
			assertNull(sf, "La situation de famille devrait être null");
		}
		else {
			assertNotNull(sf, "La situation de famille null");
			assertEquals(situationFamille, sf.getEtatCivil(), "La situation de famille devrait être " + situationFamille.name());
		}
	}
}
