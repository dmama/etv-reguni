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

public class Ec_2000_03_Deces_Membre_Marie_Depense_Surv_Suisse_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2000_03_Décès";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DECES;
	}

	@Override
	public String getDescription() {
		return "Vérification du mode d'imposition du survivant suisse d'un couple à la dépense";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndHamlet = 43252;

	private static final long noIndHeidi = 43134;

	private MockIndividu indHamlet;

	private MockIndividu indHeidi;

	private long noHabHamlet;

	private long noHabHeidi;

	private long noMenage;

	private final RegDate dateNaissanceHamlet = RegDate.get(1952, 2, 21);

	private final RegDate dateNaissanceHeidi = RegDate.get(1952, 9, 5);

	private final RegDate dateMariage = RegDate.get(1975, 6, 12);

	private final RegDate veilleMariage = dateMariage.getOneDayBefore();

	private final RegDate dateArriveeVD = RegDate.get(1980, 9, 11);

	private final RegDate dateDeces = RegDate.get(2006, 8, 1);

	private final RegDate lendemainDeces = dateDeces.getOneDayAfter();

	private final MockCommune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {
				indHamlet = addIndividu(noIndHamlet, dateNaissanceHamlet, "du Danemark", "Hamlet", true);
				addNationalite(indHamlet, MockPays.Danemark, dateNaissanceHamlet, null);

				indHeidi = addIndividu(noIndHeidi, dateNaissanceHeidi, "Von der Heide", "Heidi", false);
				addOrigine(indHeidi, MockCommune.Bern);
				addNationalite(indHeidi, MockPays.Suisse, dateNaissanceHeidi, null);
				addAdresse(indHeidi, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateMariage, null);

				marieIndividus(indHamlet, indHeidi, dateMariage);
			}
		});
	}

	@Etape(id = 1, descr = "Chargement du couple marié")
	public void etape1() {

		final PersonnePhysique hamlet = addHabitant(noIndHamlet);
		noHabHamlet = hamlet.getNumero();
		addSituationFamille(hamlet, dateNaissanceHamlet, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, 0);

		final PersonnePhysique heidi = addHabitant(noIndHeidi);
		noHabHeidi = heidi.getNumero();
		addSituationFamille(heidi, dateNaissanceHamlet, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, 0);

		{
			final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, hamlet, dateMariage, null);
			tiersService.addTiersToCouple(menage, heidi, dateMariage, null);
			addForFiscalPrincipal(menage, commune, dateArriveeVD, null, MotifFor.ARRIVEE_HC, null, ModeImposition.DEPENSE);

			addSituationFamille(menage, dateMariage, null, EtatCivil.MARIE, 0, null, null);
			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id = 1, descr = "Vérification que les deux sont mariés, que le for fiscal du ménage existe et que les situations de famille des tiers sont correctes")
	public void check1() {

		{
			final PersonnePhysique hamlet = (PersonnePhysique) tiersDAO.get(noHabHamlet);
			final ForFiscalPrincipal ffp = hamlet.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + hamlet.getNumero() + " non null");

			// [UNIREG-823] situation de famille
			assertEquals(EtatCivil.CELIBATAIRE, hamlet.getSituationFamilleAt(veilleMariage).getEtatCivil(),
					"La situation de famille de Hamlet n'est pas 'célibataire' la veille de son mariage");
			assertNull(hamlet.getSituationFamilleAt(dateMariage), "La situation de famille de Hamlet existe le jour de son mariage");
		}

		{
			final PersonnePhysique heidi = (PersonnePhysique) tiersDAO.get(noHabHeidi);
			final ForFiscalPrincipal ffp = heidi.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + heidi.getNumero() + " non null");

			// [UNIREG-823] situation de famille
			assertEquals(EtatCivil.CELIBATAIRE, heidi.getSituationFamilleAt(veilleMariage).getEtatCivil(),
					"La situation de famille de Heidi n'est pas 'célibataire' la veille de son mariage");
			assertNull(heidi.getSituationFamilleAt(dateMariage), "La situation de famille de Heidi existe le jour de son mariage");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateArriveeVD, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur Lausanne");
			assertEquals(ModeImposition.DEPENSE, ffp.getModeImposition(), "Le mode d'imposition n'est pas la dépense");

			// [UNIREG-823] situation de famille
			assertNull(mc.getSituationFamilleAt(veilleMariage),
					"Le ménage commun ne devrait pas posséder de situation de famille la veille du mariage");
			assertNotNull(mc.getSituationFamilleAt(dateMariage),
					"Le ménage commun devrait posséder une situation de famille le jour du mariage");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id = 2, descr = "Déclaration de décès de Hamlet")
	public void etape2() throws Exception {

		doModificationIndividu(noIndHamlet, individu -> individu.setDateDeces(dateDeces));

		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndHamlet, dateDeces, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérification que le for est bien fermé sur Lausanne après le décès, et que les remboursements automatiques sont bien bloqués")
	public void check2() {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabHamlet);
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
		assertEquals(MotifFor.VEUVAGE_DECES, ffpSurvivant.getMotifOuverture(), "Le for principal du survivant devrait être ouvert pour cause de veuvage/décès");
		assertEquals(dateDeces.getOneDayAfter(), ffpSurvivant.getDateDebut(), "Le for principal du survivant devrait être ouvert au lendemain du décès");
		assertEquals(ModeImposition.DEPENSE, ffpSurvivant.getModeImposition(), "Le mode d'imposition du survivant devrait être dépense");

		// [UNIREG-823] situation de famille
		{
			final PersonnePhysique hamlet = (PersonnePhysique) tiersDAO.get(noHabHamlet);
			assertNull(hamlet.getSituationFamilleAt(dateDeces), "Situation de famille de Hamlet le jour du décès");
			assertNull(hamlet.getSituationFamilleAt(lendemainDeces),
					"Hamlet ne devrait plus avoir de situation de famille le lendemain de son décès.");

			final PersonnePhysique heidi = (PersonnePhysique) tiersDAO.get(noHabHeidi);
			assertNull(heidi.getSituationFamilleAt(dateDeces), "Situation de famille de Heidi le jour du décès");
			assertSituationFamille(lendemainDeces, null, EtatCivil.VEUF, 0, heidi.getSituationFamilleAt(lendemainDeces),
					"Situation de famille de Heidi le lendemain du décès:");

			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertSituationFamille(dateMariage, dateDeces, EtatCivil.MARIE, 0, mc.getSituationFamilleAt(dateDeces),
					"Situation de famille de ménage commun le jour du décès:");
			assertNull(mc.getSituationFamilleAt(lendemainDeces),
					"Le ménage commun ne devrait plus avoir de situation de famille le lendemain du décès.");
		}

		assertBlocageRemboursementAutomatique(true, false, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduHamlet, boolean blocageAttenduHeidi, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduHamlet, tiersDAO.get(noHabHamlet));
		assertBlocageRemboursementAutomatique(blocageAttenduHeidi, tiersDAO.get(noHabHeidi));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
