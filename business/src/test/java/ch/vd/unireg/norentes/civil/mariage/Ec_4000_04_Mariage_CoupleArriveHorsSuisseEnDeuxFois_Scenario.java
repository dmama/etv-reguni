package ch.vd.unireg.norentes.civil.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

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

			addNationalite(indRafa, MockPays.Espagne, dateNaissanceRafa, null);
			addPermis(indRafa, TypePermis.ETABLISSEMENT, RegDate.get(2005, 10, 1), null, false);
			addAdresse(indRafa, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArriveeRafa, null);

			addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null);
			addPermis(indMaria, TypePermis.ETABLISSEMENT, RegDate.get(2005, 12, 1), null, false);
			addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArriveeMaria, null);
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

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabMaria);

		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement de mariage devrait être traité");

		// rafa
		final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noHabRafa);
		{
			assertNotNull(rafa, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(rafa.getNumero()) + " n'a pas été trouvé");
			final ForFiscalPrincipal ffp = rafa.getForFiscalPrincipalAt(null);
			assertNull(ffp, "L'habitant n°" + FormatNumeroHelper.numeroCTBToDisplay(rafa.getNumero()) + " devrait pas avoir de for fiscal principal ouvert");
			for (ForFiscalPrincipal ff : rafa.getForsParType(false).principauxPP) {
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
