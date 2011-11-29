package ch.vd.uniregctb.norentes.civil.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario de mariage d'un couple déjà marié.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_05_Mariage_EvtCoupleMarie_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4000_05_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage d'un couple déjà marié.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndJeanBruno = 737533; // Jean-Bruno
	private static final long noIndChristelle = 736876; // Christelle

	private MockIndividu indJeanBruno;
	private MockIndividu indChristelle;

	private long noHabJeanBruno;
	private long noHabChristelle;
	private long noMenage;

	private final RegDate dateDebutJeanBruno = RegDate.get(2004, 11, 11);
	private final RegDate dateDemenagementChristelle = RegDate.get(2004, 9, 1);
	private final RegDate dateMariage = RegDate.get(2006, 8, 25);
	private final RegDate dateMariageDoublon = RegDate.get(2008, 12, 1);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				final RegDate dateNaissanceJeanBruno = RegDate.get(1968, 10, 20);
				indJeanBruno = addIndividu(noIndJeanBruno, dateNaissanceJeanBruno , "Lekeufack", "Jean-Bruno", true);

				final RegDate dateNaissanceChristelle = RegDate.get(1978, 1, 12);
				indChristelle = addIndividu(noIndChristelle, dateNaissanceChristelle, "Berdoz", "Christelle", false);

				marieIndividus(indJeanBruno, indChristelle, dateMariage);

				final RegDate dateEtablissement = RegDate.get(2008, 1, 14);

				addOrigine(indJeanBruno, MockPays.France.getNomMinuscule());
				addNationalite(indJeanBruno, MockPays.France, dateNaissanceJeanBruno, null);
				addPermis(indJeanBruno, TypePermis.ANNUEL, RegDate.get(2004, 11, 10), null, false);
				addPermis(indJeanBruno, TypePermis.ETABLISSEMENT, dateEtablissement, null, false);
				addAdresse(indJeanBruno, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateEtablissement, null);
				addAdresse(indJeanBruno, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateEtablissement, null);

				addOrigine(indChristelle, MockPays.Suisse.getNomMinuscule());
				addNationalite(indChristelle, MockPays.Suisse, dateNaissanceChristelle, null);
				addAdresse(indChristelle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateEtablissement, null);
				addAdresse(indChristelle, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateEtablissement, null);
			}

		});
	}

	@Etape(id=1, descr="Chargement d'un habitant marié, de son conjoint et du ménage commun")
	public void step1() {
		// pierre
		final PersonnePhysique jeanBruno = addHabitant(noIndJeanBruno);
		{
			noHabJeanBruno = jeanBruno.getNumero();
			addForFiscalPrincipal(jeanBruno, commune, dateDebutJeanBruno, dateMariage.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.DEMENAGEMENT_VD);
		}

		final PersonnePhysique christelle = addHabitant(noIndChristelle);
		{
			noHabChristelle = christelle.getNumero();
			addForFiscalPrincipal(christelle, commune, dateDemenagementChristelle, dateMariage.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MotifFor.DEMENAGEMENT_VD);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, jeanBruno, dateMariage, null);
			tiersService.addTiersToCouple(menage, christelle, dateMariage, null);

			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.DEMENAGEMENT_VD, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);

			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que l'habitant Jean-Bruno est marié avec Christelle et le For du ménage existe")
	public void check1() {

		{
			final PersonnePhysique jeanBruno = (PersonnePhysique) tiersDAO.get(noHabJeanBruno);
			final ForFiscalPrincipal ffp = jeanBruno.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + jeanBruno.getNumero() + " non null");
		}

		{
			final PersonnePhysique christelle = (PersonnePhysique)tiersDAO.get(noHabChristelle);
			final ForFiscalPrincipal ffp = christelle.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + christelle.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Envoi de l'événement de mariage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndChristelle, dateMariageDoublon, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'événement est bien en erreur")
	public void check2() {

		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabChristelle);

		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement de mariage devrait être en erreur car le couple existe déjà");

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduKarina, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabJeanBruno));
		assertBlocageRemboursementAutomatique(blocageAttenduKarina, tiersDAO.get(noHabChristelle));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}

}
