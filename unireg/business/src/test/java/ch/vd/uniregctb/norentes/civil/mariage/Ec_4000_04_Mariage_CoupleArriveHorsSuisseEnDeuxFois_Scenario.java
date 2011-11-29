package ch.vd.uniregctb.norentes.civil.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Un homme arrive HS célibataire ; il se marie avec une étrangère non-établie en Suisse qui le rejoint
 * à une date postérieure au mariage, mais l'événement de mariage ne nous parvient que plus tard encore
 */
public class Ec_4000_04_Mariage_CoupleArriveHorsSuisseEnDeuxFois_Scenario extends MariageApresArriveeScenarios {

	public static final String NAME = "Ec_4000_04_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage en Suisse de deux personnes venant de l'étranger, dont une arrive après la date de mariage (mais son arrivée est annoncée avant)";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndRafa = 3913648; // rafa
	private static final long noIndMaria = 3913649; // maria

	private MockIndividu indRafa;
	private MockIndividu indMaria;

	private long noHabRafa;
	private long noHabMaria;

	private final RegDate dateArriveeRafa = RegDate.get(2005, 11, 1);
	private final RegDate dateArriveeMaria = RegDate.get(2006, 2, 14);
	private final RegDate dateMariage = RegDate.get(2006, 1, 10);		// mariage annoncé après l'arrivée des deux, mais temporellement avant l'arrivée d'un seul
	private final MockCommune communeArrivee = MockCommune.Lausanne;

	private class InternalServiceCivil extends MockServiceCivil {
		@Override
		protected void init() {
			final RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
			indRafa = addIndividu(noIndRafa, dateNaissanceRafa, "Nadalino", "Rafa", true);

			final RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
			indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

			addOrigine(indRafa, MockPays.Espagne.getNomMinuscule());
			addNationalite(indRafa, MockPays.Espagne, dateNaissanceRafa, null);
			addPermis(indRafa, TypePermis.ETABLISSEMENT, RegDate.get(2005, 10, 1), null, false);
			addAdresse(indRafa, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeRafa, null);

			addOrigine(indMaria, MockPays.Espagne.getNomMinuscule());
			addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null);
			addPermis(indMaria, TypePermis.ETABLISSEMENT, RegDate.get(2005, 12, 1), null, false);
			addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeMaria, null);
		}

		public void prepareMariage() {
			marieIndividus(indRafa, indMaria, dateMariage);
		}
	}

	private InternalServiceCivil internalServiceCivil;

	@Override
	protected void initServiceCivil() {
		internalServiceCivil = new InternalServiceCivil();
		serviceCivilService.setUp(internalServiceCivil);
	}

	@Etape(id=1, descr="Envoi de l'événement d'arrivée du mari")
	public void step1() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndRafa, dateArriveeRafa, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=1, descr="Vérifie qu'un habitant correspondant au mari a bien été créé et qu'il possède un for ouvert")
	public void check1() throws Exception {
		// rafa
		noHabRafa = checkArriveeHabitant(noIndRafa, dateArriveeRafa);
	}

	@Etape(id=2, descr="Envoi de l'événement d'arrivée de la femme")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndMaria, dateArriveeMaria, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie qu'un habitant correspondant à l'épouse a bien été créé et qu'il possède un for ouvert")
	public void check2() throws Exception {
		// maria
		noHabMaria = checkArriveeHabitant(noIndMaria, dateArriveeMaria);
	}

	@Etape(id=3, descr="Mariage des individus dans le civil et envoi de l'événement de Mariage")
	public void step3() throws Exception {
		// marie les individus dans le civil
		internalServiceCivil.prepareMariage();
		// envoi de l'événement de mariage
		long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndMaria, dateMariage, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que les fors des habitants précédemment créés ont été annulés, le ménage commun créé et qu'il a un for ouvert")
	public void check3() throws Exception {

		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabMaria);

		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement de mariage devrait être traité");

		// rafa
		final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noHabRafa);
		{
			assertNotNull(rafa, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(rafa.getNumero()) + " n'a pas été trouvé");
			final ForFiscalPrincipal ffp = rafa.getForFiscalPrincipalAt(null);
			assertNull(ffp, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(rafa.getNumero()) + " devrait pas avoir de for fiscal principal ouvert");
			for (ForFiscalPrincipal ff : rafa.getForsParType(false).principaux) {
				if (dateArriveeRafa.equals(ff.getDateDebut())) {
					assertEquals(dateMariage.getOneDayBefore(), ff.getDateFin(), "Le for créé à l'arrivée devrait être fermé à la veille du mariage");
					assertFalse(ff.isAnnule(), "Le for créé lors de l'arrivée ne devrait pas être annulé");
				}
			}
		}

		// maria
		final PersonnePhysique maria = (PersonnePhysique) tiersDAO.get(noHabMaria);
		checkHabitantApresMariage(maria, dateArriveeMaria);

		// ménage
		{
			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(rafa, dateMariage);
			assertNotNull(couple, "Le ménage n'a pas été créé");
			checkMenageApresMariage(couple.getMenage(), dateMariage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}
	}
}
