package ch.vd.unireg.norentes.civil.correction.conjoint;

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
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario de correction de conjoint d'un individu marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_41040_01_CorrectionConjoint_MarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "41040_01_CorrectionConjoint";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CORREC_CONJOINT;
	}

	@Override
	public String getDescription() {
		return "Correction de conjoint d'un individu marié seul.";
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
	private long noMenage;

	private final RegDate dateArrivee = RegDate.get(2008, 11, 1);
	private final RegDate dateMariage = RegDate.get(2009, 6, 21);
	private final RegDate avantDateMariage = dateMariage.getOneDayBefore();

	private final MockCommune commune = MockCommune.Lausanne;

	private class InternalIndividuConnector extends MockIndividuConnector {

		@Override
		protected void init() {

			RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
			indRafa = addIndividu(noIndRafa, dateNaissanceRafa, "Nadalino", "Rafa", true);

			marieIndividu(indRafa, dateMariage);

			RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
			indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

			addNationalite(indRafa, MockPays.Espagne, dateNaissanceRafa, null);
			addPermis(indRafa, TypePermis.ETABLISSEMENT, RegDate.get(2008, 10, 1), null, false);
			addAdresse(indRafa, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArrivee, null);

			addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null);
			addPermis(indMaria, TypePermis.ETABLISSEMENT, RegDate.get(2008, 10, 1), null, false);
			addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArrivee, null);

		}

		void prepareMariage() {
			marieIndividus(indRafa, indMaria, dateMariage);
		}
	}

	private InternalIndividuConnector internalServiceCivil;

	@Override
	protected void initServiceCivil() {
		internalServiceCivil = new InternalIndividuConnector();
		serviceCivilService.setUp(internalServiceCivil);
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul et de son conjoint")
	public void step1() throws Exception {
		// rafa
		PersonnePhysique rafa = addHabitant(noIndRafa);
		{
			noHabRafa = rafa.getNumero();
			addForFiscalPrincipal(rafa, commune, dateArrivee, avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// maria
		PersonnePhysique maria = addHabitant(noIndMaria);
		{
			noHabMaria = maria.getNumero();
			addForFiscalPrincipal(maria, commune, dateArrivee, null, MotifFor.DEMENAGEMENT_VD, null);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, rafa, dateMariage, null);
			addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		}
	}

	@Check(id=1, descr="Vérifie que l'habitant Rafa est marié seul et le For du ménage existe")
	public void check1() throws Exception {

		{
			final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noHabRafa);
			final ForFiscalPrincipal ffp = rafa.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + rafa.getNumero() + " non null");
		}

		{
			final PersonnePhysique maria = (PersonnePhysique)tiersDAO.get(noHabMaria);
			final ForFiscalPrincipal ffp = maria.getForFiscalPrincipalAt(null);
			assertNotNull(ffp, "For principal de l'Habitant " + maria.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomOfficiel());
			assertNotNull(mc.getRapportsObjet(), "Aucun rapport appartenance ménage trouvé");
			assertEquals(1, mc.getRapportsObjet().size(), "Plus d'un rapport appartenance ménage trouvé, l'habitant devrait être marié seul");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Correction de coinjoint")
	public void step2() throws Exception {
		internalServiceCivil.prepareMariage();
		long id = addEvenementCivil(TypeEvenementCivil.CORREC_CONJOINT, noIndRafa, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le ménage est complet et l'habitant ajouté n'a plus de for ouvert")
	public void check2() {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabRafa);
		assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat(), "L'événement civil devrait être à vérifier");
		assertEquals(2, evt.getWarnings().size(), "Il devrait y avoir 2 warnings");

		{
			final PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noHabRafa);
			final ForFiscalPrincipal ffp = rafa.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + rafa.getNumero() + " non null");
		}

		{
			final PersonnePhysique maria = (PersonnePhysique)tiersDAO.get(noHabMaria);
			final ForFiscalPrincipal ffp = maria.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + maria.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomOfficiel());
			assertNotNull(mc.getRapportsObjet(), "Aucun rapport appartenance ménage trouvé");
			assertEquals(2, mc.getRapportsObjet().size(), "Les deux conjoints devraient appartenir au ménage commun");
		}
	}

}
