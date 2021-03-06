package ch.vd.unireg.norentes.civil.mariage;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario d'un mariage entre Suisse et Etranger Permis B
 *
 */
public class Ec_4000_10_Mariage_SuisseAvecPermisB_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4000_10_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage d'une Suissesse avec un titulaire du permis B sourcier";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMaria = 3913649; // maria
	private static final long noIndRafa = 1215487; // raph'

	private MockIndividu indMaria;
	private MockIndividu indRafa;

	private long noCtbRafa;
	private long noCtbMaria;

	private final RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
	private final RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
	private final RegDate dateMajoriteRafa = dateNaissanceRafa.addYears(18);
	private final RegDate dateMariage = RegDate.get(2008, 6, 21);

	private final MockCommune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);
				indRafa = addIndividu(noIndRafa, dateNaissanceRafa, "Marbo", "Raphaël", true);

				addNationalite(indMaria, MockPays.Suisse, dateNaissanceMaria, null);
				addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateNaissanceMaria, null);

				addNationalite(indRafa, MockPays.France, dateNaissanceRafa, null);
				addPermis(indRafa, TypePermis.SEJOUR, RegDate.get(2006, 5, 3), null, false);
				addAdresse(indRafa, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateNaissanceRafa, null);

				marieIndividus(indRafa, indMaria, dateMariage);
			}
		});
	}

	@Etape(id=1, descr="Création des tiers PP")
	public void etape1() throws Exception {

		// rafa & maria
		final PersonnePhysique rafa = addHabitant(noIndRafa);
		final PersonnePhysique maria = addHabitant(noIndMaria);

		// rafa est sourcier
		noCtbRafa = rafa.getNumero();
		addForFiscalPrincipal(rafa, MockCommune.Lausanne, dateMajoriteRafa, null, MotifFor.MAJORITE, null, ModeImposition.SOURCE);

		// maria n'a pas de for
		noCtbMaria = maria.getNumero();
	}

	@Check(id=1, descr="Vérifie les tiers sont bien là")
	public void check1() throws Exception {
		// rafa
		final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noCtbRafa);
		assertNotNull(rafa, "Le contribuable Rafa n'a pas été créé");
		assertExistenceForPrincipal(rafa);

		// maria
		final PersonnePhysique maria = (PersonnePhysique) tiersDAO.get(noCtbMaria);
		assertNotNull(maria, "Le contribuable Maria n'a pas été créé");
		assertNull(maria.getDernierForFiscalPrincipal(), "Maria ne devrait pas avoir de for principal");
	}

	private void assertExistenceForPrincipal(Contribuable contribuable) {
		final ForFiscalPrincipal ffp = contribuable.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()) + " devrait avoir un for principal");
	}

	@Etape(id=2, descr="Envoi de l'événement de mariage")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndRafa, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement de mariage est passé et que le for du couple est bon")
	public void check2() {

		final List<EvenementCivilRegPP> list = evtExterneDAO.getAll();
		for (EvenementCivilRegPP evt : list) {
			if (noIndRafa == evt.getNumeroIndividuPrincipal()) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), String.valueOf(evt.getId()));
			}
		}

		final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noCtbRafa);
		final EnsembleTiersCouple ensembleCouple = tiersService.getEnsembleTiersCouple(rafa, dateMariage);
		final MenageCommun menage = ensembleCouple.getMenage();
		final ForFiscalPrincipalPP forMenage = menage.getDernierForFiscalPrincipal();
		assertNotNull(forMenage, "Le ménage n'a pas de for fiscal principal");
		assertEquals(dateMariage, forMenage.getDateDebut(), "Le for du ménage de commence pas à la bonne date");
		assertNull(forMenage.getDateFin(), "Le for du ménage ne devrait pas être fermé");
		assertEquals(ModeImposition.ORDINAIRE, forMenage.getModeImposition(), "Le mode d'imposition du for ménage est erroné (Madame est Suisse !)");
	}
}
