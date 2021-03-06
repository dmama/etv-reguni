package ch.vd.unireg.norentes.civil.deces;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_2000_06_Deces_Membre_Pacse_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2000_06_Deces_Membre_Pacse";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DECES;
	}

	@Override
	public String getDescription() {
		return "Décès d'un habitant pacsé";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndJuliette = 43252;

	private static final long noIndHeidi = 43134;

	private MockIndividu indJuliette;

	private MockIndividu indHeidi;

	private long noHabJuliette;

	private long noHabHeidi;

	private long noMenage;

	private final RegDate dateNaissanceJuliette = RegDate.get(1952, 2, 21);

	private final RegDate dateNaissanceHeidi = RegDate.get(1952, 9, 5);

	private final RegDate datePacs = RegDate.get(1975, 6, 12);

	private final MockCommune communePacs = MockCommune.Lausanne;

	private final RegDate veillePacs = datePacs.getOneDayBefore();

	private final RegDate dateArriveeVD = RegDate.get(1970, 9, 11);

	private final RegDate dateDeces = RegDate.get(2006, 8, 1);

	private final RegDate lendemainDeces = dateDeces.getOneDayAfter();

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {
				indJuliette = addIndividu(noIndJuliette, dateNaissanceJuliette, "Tell", "Juliette", false);
				addOrigine(indJuliette, MockCommune.Neuchatel);
				addNationalite(indJuliette, MockPays.Suisse, dateNaissanceJuliette, null);

				indHeidi = addIndividu(noIndHeidi, dateNaissanceHeidi, "Von der Heide", "Heidi", false);
				addOrigine(indHeidi, MockCommune.Bern);
				addNationalite(indHeidi, MockPays.Suisse, dateNaissanceHeidi, null);
				addAdresse(indHeidi, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArriveeVD, null);

				marieIndividus(indJuliette, indHeidi, datePacs);
			}
		});
	}

	@Etape(id = 1, descr = "Chargement du couple marié")
	public void etape1() {

		final PersonnePhysique juliette = addHabitant(noIndJuliette);
		{
			noHabJuliette = juliette.getNumero();
			addForFiscalPrincipal(juliette, MockCommune.Lausanne, dateArriveeVD, veillePacs, MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			addSituationFamille(juliette, dateArriveeVD, veillePacs, EtatCivil.CELIBATAIRE, 0);
			addSituationFamille(juliette, datePacs, null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0);
		}

		final PersonnePhysique heidi = addHabitant(noIndHeidi);
		{
			noHabHeidi = heidi.getNumero();
			addForFiscalPrincipal(heidi, MockCommune.Lausanne, dateArriveeVD, veillePacs, MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			addSituationFamille(heidi, dateArriveeVD, veillePacs, EtatCivil.CELIBATAIRE, 0);
			addSituationFamille(heidi, datePacs, null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0);
		}

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, juliette, datePacs, null);
			tiersService.addTiersToCouple(menage, heidi, datePacs, null);
			addForFiscalPrincipal(menage, communePacs, datePacs, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			menage.setBlocageRemboursementAutomatique(false);

			addSituationFamille(menage, datePacs, null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, null, null);
		}

	}

	@Check(id = 1, descr = "Vérification que Juliette et Heidi sont mariées, que le for fiscal du ménage existe et que les situations de famille des tiers sont correctes")
	public void check1() {

		{
			final PersonnePhysique juliette = (PersonnePhysique) tiersDAO.get(noHabJuliette);
			final ForFiscalPrincipal ffp = juliette.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + juliette.getNumero() + " null");
			assertEquals(veillePacs, ffp.getDateFin(), "Date de fin du dernier for de Juliette fausse");

			// [UNIREG-823] situation de famille
			assertEquals(EtatCivil.CELIBATAIRE, juliette.getSituationFamilleAt(veillePacs).getEtatCivil(),
					"La situation de famille de Juliette n'est pas 'célibataire' la veille de son mariage");
			assertEquals(EtatCivil.LIE_PARTENARIAT_ENREGISTRE, juliette.getSituationFamilleAt(datePacs).getEtatCivil(),
					"La situation de famille de Juliette n'est pas 'marié' le jour de son mariage");
		}

		{
			final PersonnePhysique heidi = (PersonnePhysique) tiersDAO.get(noHabHeidi);
			final ForFiscalPrincipal ffp = heidi.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + heidi.getNumero() + " null");
			assertEquals(veillePacs, ffp.getDateFin(), "Date de fin du dernier for de Heidi fausse");

			// [UNIREG-823] situation de famille
			assertEquals(EtatCivil.CELIBATAIRE, heidi.getSituationFamilleAt(veillePacs).getEtatCivil(),
					"La situation de famille de Heidi n'est pas 'célibataire' la veille de son mariage");
			assertEquals(EtatCivil.LIE_PARTENARIAT_ENREGISTRE, heidi.getSituationFamilleAt(datePacs).getEtatCivil(),
					"La situation de famille de Heidi n'est pas 'marié' le jour de son mariage");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(datePacs, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communePacs.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur Villars-sous-Yens");

			// [UNIREG-823] situation de famille
			assertNull(mc.getSituationFamilleAt(veillePacs),
					"Le ménage commun ne devrait pas posséder de situation de famille la veille du mariage");
			assertNotNull(mc.getSituationFamilleAt(datePacs),
					"Le ménage commun devrait posséder une situation de famille le jour du mariage");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id = 2, descr = "Déclaration de décès de Juliette")
	public void etape2() throws Exception {

		doModificationIndividu(noIndJuliette, individu -> individu.setDateDeces(dateDeces));

		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndJuliette, dateDeces, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification que le for est bien fermé sur Lausanne après le décès, et que les remboursements automatiques sont bien bloqués")
	public void check2() {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabJuliette);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		final List<ForFiscal> list = menage.getForsFiscauxSorted();
		assertEquals(1, list.size(), "Plusieurs for?: " + list.size());

		// For fermé sur Lausanne pour cause de décès
		final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(0);
		assertEquals(dateDeces, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
		assertEquals(MotifFor.VEUVAGE_DECES, ffpFerme.getMotifFermeture(), "Le for sur Lausanne n'est pas fermé pour cause de décès");

		// for ouvert sur le survivant
		final PersonnePhysique survivant = (PersonnePhysique) tiersDAO.get(noHabHeidi);
		final ForFiscalPrincipalPP ffpSurvivant = survivant.getDernierForFiscalPrincipal();
		assertNotNull(ffpSurvivant, "Le survivant n'a pas de for principal");
		assertEquals(dateDeces.getOneDayAfter(), ffpSurvivant.getDateDebut(), "Le for principal du survivant devrait être ouvert au lendemain du décès");
		assertEquals(MotifFor.VEUVAGE_DECES, ffpSurvivant.getMotifOuverture(), "Le for principal du survivant devrait être ouvert pour cause de veuvage/décès");
		assertEquals(ModeImposition.ORDINAIRE, ffpSurvivant.getModeImposition(), "Le mode d'imposition du survivant devrait être ordinaire");

		// [UNIREG-823] situation de famille
		{
			final PersonnePhysique juliette = (PersonnePhysique) tiersDAO.get(noHabJuliette);
			assertSituationFamille(datePacs, dateDeces, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, juliette.getSituationFamilleAt(dateDeces),
					"Situation de famille de Juliette le jour du décès:");
			assertNull(juliette.getSituationFamilleAt(lendemainDeces),
					"Juliette ne devrait plus avoir de situation de famille le lendemain de son décès.");

			final PersonnePhysique heidi = (PersonnePhysique) tiersDAO.get(noHabHeidi);
			assertSituationFamille(datePacs, dateDeces, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, heidi.getSituationFamilleAt(dateDeces),
					"Situation de famille de Heidi le jour du décès:");
			assertSituationFamille(lendemainDeces, null, EtatCivil.VEUF, 0, heidi.getSituationFamilleAt(lendemainDeces),
					"Situation de famille de Heidi le lendemain du décès:");

			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertSituationFamille(datePacs, dateDeces, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, mc.getSituationFamilleAt(dateDeces),
					"Situation de famille de ménage commun le jour du décès:");
			assertNull(mc.getSituationFamilleAt(lendemainDeces),
					"Le ménage commun ne devrait plus avoir de situation de famille le lendemain du décès.");
		}

		assertBlocageRemboursementAutomatique(true, false, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduJuliette, boolean blocageAttenduHeidi, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduJuliette, tiersDAO.get(noHabJuliette));
		assertBlocageRemboursementAutomatique(blocageAttenduHeidi, tiersDAO.get(noHabHeidi));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
