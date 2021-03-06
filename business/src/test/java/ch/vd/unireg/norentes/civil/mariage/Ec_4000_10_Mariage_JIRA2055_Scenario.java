package ch.vd.unireg.norentes.civil.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario de mariage d'un couple déjà marié.
 *
 * @author Baba NGOM
 *
 */
public class Ec_4000_10_Mariage_JIRA2055_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4000_10_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Re-Mariage d'une personne  déjà mariée et divorcée.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndJeanMarc = 132720;
	private static final long noIndJoseph = 226478;
	private static final long noIndAmelie = 845875;
	private static final long noIndStephanie = 387602;

	private MockIndividu indJeanMarc;
	private MockIndividu indAmelie;
	private MockIndividu indStephanie;
	private MockIndividu indJoseph;

	private long noHabJeanMarc;
	private long noHabAmelie;
	private long noHabStephanie;
	private long noHabJoseph;
	private long noMenage1;
	private long noMenage2;
	private long noMenage3;

	private final RegDate dateDebutJeanMarc = RegDate.get(2004, 11, 11);
	private final RegDate dateDemenagementAmelie = RegDate.get(2004, 9, 1);
	private final RegDate dateMariageAvecAmelie = RegDate.get(2007, 6, 23);
	private final RegDate dateSeparationAvecAmelie = RegDate.get(2008, 6, 21);
	private final RegDate dateDivorceAvecAmelie = RegDate.get(2009, 6, 9);
	private final RegDate dateMariageAvecStephanie = RegDate.get(2009, 8, 8);
	private final RegDate dateMariageAvecJoseph = RegDate.get(2009, 9, 11);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				final RegDate dateNaissanceJeanMarc = RegDate.get(1977,2, 23);
				final RegDate dateNaissanceAmelie = RegDate.get(1977,2, 27);
				final RegDate dateNaissanceStephanie = RegDate.get(1976,9, 27);
				final RegDate dateNaissanceJoseph = RegDate.get(1949,5, 7);
				indJeanMarc = addIndividu(noIndJeanMarc, dateNaissanceJeanMarc , "Baud", "Jean-Marc", true);
				indAmelie = addIndividu(noIndAmelie, dateNaissanceAmelie, "Lucas", "Amélie", false);
				indStephanie = addIndividu(noIndStephanie, dateNaissanceStephanie, "Baud", "Stéphanie", false);
				indJoseph = addIndividu(noIndJoseph, dateNaissanceJoseph, "Lucas", "Joseph", true);

				addNationalite(indJeanMarc, MockPays.Suisse, dateNaissanceJeanMarc, null);
				addAdresse(indJeanMarc, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateNaissanceJeanMarc, null);

				addNationalite(indAmelie, MockPays.Suisse, dateNaissanceAmelie, null);
				addAdresse(indAmelie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateNaissanceAmelie, null);

				addNationalite(indJoseph, MockPays.France, dateNaissanceJoseph, null);
				addAdresse(indJoseph, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateNaissanceJoseph, null);

				addNationalite(indStephanie, MockPays.Suisse, dateNaissanceStephanie, null);
				addAdresse(indStephanie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateNaissanceStephanie, null);


				marieIndividus(indJeanMarc, indAmelie, dateMariageAvecAmelie);
				separeIndividus(indJeanMarc, indAmelie, dateSeparationAvecAmelie);
				divorceIndividus(indJeanMarc, indAmelie, dateDivorceAvecAmelie);
				marieIndividus(indJoseph, indAmelie, dateMariageAvecJoseph);
				marieIndividu(indJeanMarc, dateMariageAvecStephanie);
				//marieIndividus(indJeanMarc, indStephanie, dateMariageAvecStephanie);



			}

		});
	}

	@Etape(id=1, descr="Mariage et Séparation entre Jean marc et Amélie, on créée Stéphanie")
	public void step1() {
		// pierre
		final PersonnePhysique jeanMarc = addHabitant(noIndJeanMarc);
		{
			noHabJeanMarc = jeanMarc.getNumero();
			addForFiscalPrincipal(jeanMarc, commune, dateDebutJeanMarc, dateMariageAvecAmelie.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		final PersonnePhysique amelie = addHabitant(noIndAmelie);
		{
			noHabAmelie = amelie.getNumero();
			addForFiscalPrincipal(amelie, commune, dateDemenagementAmelie, dateMariageAvecAmelie.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		final PersonnePhysique stephanie = addHabitant(noIndStephanie);
		{
			noHabStephanie = stephanie.getNumero();
			addForFiscalPrincipal(stephanie, commune, dateDemenagementAmelie, dateMariageAvecStephanie.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage1 = menage.getNumero();
			tiersService.addTiersToCouple(menage, jeanMarc, dateMariageAvecAmelie, dateSeparationAvecAmelie);
			tiersService.addTiersToCouple(menage, amelie, dateMariageAvecAmelie,dateSeparationAvecAmelie);

			addForFiscalPrincipal(menage, commune, dateMariageAvecAmelie, dateSeparationAvecAmelie, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
		}
	}

	@Check(id=1, descr="Vérifie que l'habitant Jean-Marc est marié puis divorcé avec Christelle et le For du ménage existe")
	public void check1() {

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage1);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariageAvecAmelie, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateSeparationAvecAmelie, ffp.getDateFin(), "Date de fin du dernier for fausse");

		}

		assertBlocageRemboursementAutomatique(false, false, false);
	}


	@Etape(id=2, descr="Mariage avec Stephanie")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndJeanMarc, dateMariageAvecStephanie, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie qu'un nouveau couple different du premier a été créé")
	public void check2() {
		final PersonnePhysique jeanMarc = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndJeanMarc);
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(jeanMarc,null);
		final PersonnePhysique stephanie = ensemble.getConjoint(jeanMarc);
		assertNull(stephanie, "stephanie n'est pas null");
		final MenageCommun menage = ensemble.getMenage();
		noMenage2  = menage.getNumero();
		//assertEquals(noHabStephanie, stephanie.getNumero(), "C'est pas stéphanie");
		assertFalse(noMenage1==noMenage2, "Le ménage n'est pas le bon");

	}




	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduKarina, boolean blocageAttenduMenage) {

		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabJeanMarc));
		assertBlocageRemboursementAutomatique(blocageAttenduKarina, tiersDAO.get(noHabAmelie));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage1));
	}

}
