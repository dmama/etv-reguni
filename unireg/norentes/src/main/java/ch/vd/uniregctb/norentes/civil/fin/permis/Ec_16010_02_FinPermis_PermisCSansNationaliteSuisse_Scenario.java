package ch.vd.uniregctb.norentes.civil.fin.permis;

import java.util.List;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
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
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scenario d'un événement fin de permis C.
 *
 * @author Pavel BLANCO
 */
public class Ec_16010_02_FinPermis_PermisCSansNationaliteSuisse_Scenario extends EvenementCivilScenario {

	public static final String NAME = "16010_02_FinPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER;
	}

	@Override
	public String getDescription() {
		return "Fin de permis C pour une personne n'ayant pas obtenu la nationalité suisse";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndRosa = 238947;  // rosa

	private long noHabRosa;

	private final RegDate dateArrivee = RegDate.get(2007, 1, 1);
	private final RegDate dateObtentionPermisC = RegDate.get(2007, 6, 1);
	private final RegDate dateFinPermisC = RegDate.get(2008, 10, 10);
	private final MockCommune communePermis = MockCommune.VillarsSousYens;

	private final class MyMockServiceCivil extends DefaultMockServiceCivil {

		@Override
		protected void init() {
			RegDate dateNaissanceRosa = RegDate.get(1980, 5, 30);
			MockIndividu rosa = addIndividu(noIndRosa, dateNaissanceRosa, "Rosa", "Martinez", false);
			addDefaultAdressesTo(rosa);
			addOrigine(rosa, MockPays.Espagne.getNomMinuscule());
			addNationalite(rosa, MockPays.Espagne, dateNaissanceRosa, null);

			addPermis(rosa, TypePermis.COURTE_DUREE, dateArrivee, dateObtentionPermisC.getOneDayBefore(), false);
			addPermis(rosa, TypePermis.ETABLISSEMENT, dateObtentionPermisC, null, false);
		}

		public void setupForTest() {
			((MockPermis) getPermisActif(noIndRosa, dateFinPermisC)).setDateFinValidite(dateFinPermisC);
		}
	}

	private MyMockServiceCivil mockServiceCivil;

	@Override
	protected void initServiceCivil() {
		mockServiceCivil = new MyMockServiceCivil();
		serviceCivilService.setUp(mockServiceCivil);
	}

	@Etape(id=1, descr="Chargement de l'habitant avec son for principal")
	public void step1() {
		PersonnePhysique rosa = addHabitant(noIndRosa);
		noHabRosa = rosa.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(rosa, communePermis, dateArrivee, dateObtentionPermisC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MotifFor.PERMIS_C_SUISSE);
		f.setModeImposition(ModeImposition.SOURCE);

		f = addForFiscalPrincipal(rosa, communePermis, dateObtentionPermisC, null, MotifFor.PERMIS_C_SUISSE, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie l'habitant Roberto")
	public void check1() {
		{
			PersonnePhysique roberto = (PersonnePhysique) tiersDAO.get(noHabRosa);
			ForFiscalPrincipal ffp = roberto.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + roberto.getNumero() + " null");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement de Fin de Permis")
	public void step2() throws Exception {
		mockServiceCivil.setupForTest();

		long id = addEvenementCivil(TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER, noIndRosa, dateFinPermisC, communePermis.getNoOFSEtendu());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement est bien en erreur (un traitement manuel est requis)")
	public void check2() {
		{
			EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabRosa);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement devrait être passé en mode manuel");

			final PersonnePhysique roberto = (PersonnePhysique) tiersDAO.get(noHabRosa);
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
