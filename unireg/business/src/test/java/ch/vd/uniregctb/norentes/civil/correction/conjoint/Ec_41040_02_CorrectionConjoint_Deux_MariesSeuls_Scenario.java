package ch.vd.uniregctb.norentes.civil.correction.conjoint;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
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
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario de correction de conjoint d'un individu marié seul avec un autre marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_41040_02_CorrectionConjoint_Deux_MariesSeuls_Scenario extends EvenementCivilScenario {

	public static final String NAME = "41040_02_CorrectionConjoint";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CORREC_CONJOINT;
	}

	@Override
	public String getDescription() {
		return "Correction de conjoint d'un individu marié seul avec un autre marié seul.";
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
	private long noMenageRafa;
	private long noMenageMaria;

	private final RegDate dateArrivee = RegDate.get(2008, 11, 1);
	private final RegDate dateMariage = RegDate.get(2009, 6, 21);
	private final RegDate dateAvantMariage = dateMariage.getOneDayBefore();

	private final MockCommune commune = MockCommune.Lausanne;

	private class InternalServiceCivil extends MockServiceCivil {

		@Override
		protected void init() {

			RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
			indRafa = addIndividu(noIndRafa, dateNaissanceRafa, "Nadalino", "Rafa", true);

			marieIndividu(indRafa, dateMariage);

			addOrigine(indRafa, MockPays.Espagne.getNomMinuscule());
			addNationalite(indRafa, MockPays.Espagne, dateNaissanceRafa, null);
			setPermis(indRafa, TypePermis.ETABLISSEMENT, RegDate.get(2008, 10, 1), null, false);
			addAdresse(indRafa, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArrivee, null);

			RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
			indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

			marieIndividu(indMaria, dateMariage);

			addOrigine(indMaria, MockPays.Espagne.getNomMinuscule());
			addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null);
			setPermis(indMaria, TypePermis.ETABLISSEMENT, RegDate.get(2008, 10, 1), null, false);
			addAdresse(indMaria, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArrivee, null);

		}

		void prepareMariage() {
			marieIndividus(indRafa, indMaria, dateMariage);
		}
	}

	private InternalServiceCivil internalServiceCivil;

	@Override
	protected void initServiceCivil() {
		internalServiceCivil = new InternalServiceCivil();
		serviceCivilService.setUp(internalServiceCivil);
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul et de son conjoint")
	public void step1() throws Exception {
		// rafa
		PersonnePhysique rafa = addHabitant(noIndRafa);
		{
			noHabRafa = rafa.getNumero();
			final ForFiscalPrincipal f = addForFiscalPrincipal(rafa, commune, dateArrivee, dateAvantMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f.setModeImposition(ModeImposition.ORDINAIRE);
		}

		// ménage rafa
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenageRafa = menage.getNumero();
			tiersService.addTiersToCouple(menage, rafa, dateMariage, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
		}

		// maria
		PersonnePhysique maria = addHabitant(noIndMaria);
		{
			noHabMaria = maria.getNumero();
			final ForFiscalPrincipal f = addForFiscalPrincipal(maria, commune, dateArrivee, dateAvantMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f.setModeImposition(ModeImposition.ORDINAIRE);
		}

		// ménage maria
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenageMaria = menage.getNumero();
			tiersService.addTiersToCouple(menage, maria, dateMariage, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			f.setAnnule(true);
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
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenageRafa);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage de Rafa a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage de Rafa " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMinuscule());
			assertNotNull(mc.getRapportsObjet(), "Aucun rapport appartenance ménage trouvé");
			assertEquals(1, mc.getRapportsObjet().size(), "Plus d'un rapport appartenance ménage trouvé, l'habitant devrait être marié seul");
		}

		{
			final PersonnePhysique maria = (PersonnePhysique)tiersDAO.get(noHabMaria);
			final ForFiscalPrincipal ffp = maria.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + maria.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenageMaria);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage de Maria a plus d'un for principal");
			//UNIREG-2771 le menage commun de maria ne doit pas posseder de for
			//sinon pas de fusion.

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

	@Check(id=2, descr="Vérifie qu'un ménage est complet, l'autre annulé et les habitants n'ont aucun for ouvert")
	public void check2() {

		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabRafa);
		assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat(), "L'événement civil devrait être à vérifier");

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
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenageRafa);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMinuscule());
			assertNotNull(mc.getRapportsObjet(), "Aucun rapport appartenance ménage trouvé");
			assertEquals(2, mc.getRapportsObjet().size(), "Les deux conjoints devraient appartenir au ménage commun");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenageMaria);
			assertTrue(mc.isAnnule(), "L'ancien ménage de Maria aurait dû être annulé");
			for (RapportEntreTiers rapport : mc.getRapportsObjet()) {
				assertTrue(rapport.isAnnule(), "Tous les rapport entre tiers de l'ancien ménage de Maria doivent être annulés");
			}
		}
	}

}
