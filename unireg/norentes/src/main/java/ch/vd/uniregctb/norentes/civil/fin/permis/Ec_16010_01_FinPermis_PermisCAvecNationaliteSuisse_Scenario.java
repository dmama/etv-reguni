package ch.vd.uniregctb.norentes.civil.fin.permis;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scenario d'un événement fin de permis C.
 *
 * @author Pavel BLANCO
 */
public class Ec_16010_01_FinPermis_PermisCAvecNationaliteSuisse_Scenario extends EvenementCivilScenario {

	public static final String NAME = "16010_01_FinPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER;
	}

	@Override
	public String getDescription() {
		return "Fin de permis C pour une personne ayant obtenu la nationalité suisse";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndRoberto = 97136; // roberto

	private long noHabRoberto;

	private final RegDate dateArrivee = RegDate.get(2007, 1, 1);
	private final RegDate dateObtentionPermisC = RegDate.get(2007, 6, 1);
	private final RegDate dateFinPermisC = RegDate.get(2008, 10, 10);
	private final RegDate dateObtentionNationalite = dateFinPermisC.getOneDayAfter();
	private final MockCommune communePermis = MockCommune.VillarsSousYens;

	private final class MyMockServiceCivil extends DefaultMockServiceCivil {

		@Override
		protected void init() {
			RegDate dateNaissanceRoberto = RegDate.get(1961, 3, 12);
			MockIndividu roberto = addIndividu(noIndRoberto, dateNaissanceRoberto, "Martin", "Roberto", true);
			addDefaultAdressesTo(roberto);
			addOrigine(roberto, MockPays.Espagne, null, dateNaissanceRoberto);
			addNationalite(roberto, MockPays.Espagne, dateNaissanceRoberto, null, 0);

			addPermis(roberto, EnumTypePermis.COURTE_DUREE, dateArrivee, dateObtentionPermisC.getOneDayBefore(), 0, false);
			addPermis(roberto, EnumTypePermis.ETABLLISSEMENT, dateObtentionPermisC, null, 1, false);
		}

		public void setupForTest() {
			((MockPermis) getPermisActif(noIndRoberto, dateFinPermisC)).setDateFinValidite(dateFinPermisC);
			MockIndividu roberto = (MockIndividu) getIndividu(noIndRoberto, 2008);
			addNationalite(roberto, MockPays.Suisse, dateObtentionNationalite, null, 1);
		}

	}

	private MyMockServiceCivil mockServiceCivil;

	@Override
	protected void initServiceCivil() {
		mockServiceCivil = new MyMockServiceCivil();
		serviceCivilService.setUp(mockServiceCivil);
	}

	@Etape(id = 1, descr = "Chargement de l'habitant avec son for principal")
	public void step1() {
		PersonnePhysique roberto = addHabitant(noIndRoberto);
		noHabRoberto = roberto.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(roberto, communePermis, dateArrivee, dateObtentionPermisC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.PERMIS_C_SUISSE);
		f.setModeImposition(ModeImposition.SOURCE);

		f = addForFiscalPrincipal(roberto, communePermis, dateObtentionPermisC, null, MotifFor.PERMIS_C_SUISSE, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id = 1, descr = "Vérifie l'habitant Roberto")
	public void check1() {
		{
			PersonnePhysique roberto = (PersonnePhysique) tiersDAO.get(noHabRoberto);
			ForFiscalPrincipal ffp = roberto.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for principal de l'habitant " + roberto.getNumero() + " est null");
		}
	}

	@Etape(id = 2, descr = "Envoi de l'événement de Fin de Permis")
	public void step2() throws Exception {
		mockServiceCivil.setupForTest();

		long id = addEvenementCivil(TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER, noIndRoberto, dateFinPermisC, communePermis.getNoOFSEtendu());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'événement est traité, mais qu'il n'y a aucun changement effectué (en attente de l'événement d'obtention de nationalité Suisse)")
	public void check2() {
		{
			final EvenementCivilData evt = getEvenementCivilRegoupeForHabitant(noHabRoberto);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement devrait être traité");

			final PersonnePhysique roberto = (PersonnePhysique) tiersDAO.get(noHabRoberto);
			final List<ForFiscal> fors = roberto.getForsFiscauxSorted();
			assertEquals(2, fors.size(), "Il devrait y avoir 2 fors fiscaux");

			final ForFiscalPrincipal premier = (ForFiscalPrincipal) fors.get(0);
			assertEquals(dateArrivee, premier.getDateDebut(), "La date d'ouverture du premier for fiscal n'est pas la bonne");
			assertEquals(MotifFor.ARRIVEE_HC, premier.getMotifOuverture(), "Le motif d'ouverture du premier for fiscal n'est pas le bon");
			assertEquals(dateObtentionPermisC.getOneDayBefore(), premier.getDateFin(),
					"La date de fermeture du premier for fiscal n'est pas la bonne");
			assertEquals(MotifFor.PERMIS_C_SUISSE, premier.getMotifFermeture(),
					"Le motif de fermeture du premier for fiscal n'est pas le bon");
			assertEquals(ModeImposition.SOURCE, premier.getModeImposition(), "Le mode d'imposition du premier for fiscal n'est pas le bon");

			final ForFiscalPrincipal second = (ForFiscalPrincipal) fors.get(1);
			assertEquals(dateObtentionPermisC, second.getDateDebut(), "La date d'ouverture du premier for fiscal n'est pas la bonne");
			assertEquals(MotifFor.PERMIS_C_SUISSE, second.getMotifOuverture(),
					"Le motif d'ouverture du premier for fiscal n'est pas le bon");
			assertNull(second.getDateFin(), "Le second for fiscal ne devrait pas être fermé");
			assertNull(second.getMotifFermeture(), "Le motif de fermeture dusecond for fiscal de devrait pas être renseigné");
			assertEquals(ModeImposition.ORDINAIRE, second.getModeImposition(), "Le mode d'imposition du second for fiscal n'est pas le bon");
		}
	}
}
