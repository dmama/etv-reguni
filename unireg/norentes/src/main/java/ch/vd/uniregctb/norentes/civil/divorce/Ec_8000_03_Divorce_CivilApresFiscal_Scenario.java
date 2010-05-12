package ch.vd.uniregctb.norentes.civil.divorce;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scenario d'un événement divorce.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_8000_03_Divorce_CivilApresFiscal_Scenario extends EvenementCivilScenario {

	private MetierService metierService;

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public static final String NAME = "8000_03_Divorce";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DIVORCE;
	}

	@Override
	public String getDescription() {
		return "Divorce d'un couple en 2 phases: d'abord fiscalement, ensuite dans le civil";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndPierre = 569550; // Pierre
	private final long noIndKarina = 569531; // Karina

	private MockIndividu indPierre;
	private MockIndividu indKarina;

	private long noHabPierre;
	private long noHabKarina;
	private long noMenage;

	private final RegDate dateNaissancePierre = RegDate.get(1970, 3, 2);
	private final RegDate dateNaissanceKarina = RegDate.get(1975, 6, 3);
	private final RegDate dateDemenagement = RegDate.get(2007, 4, 26);
	private final RegDate dateDivorce = RegDate.get(2009, 2, 2);

	private final Commune commune = MockCommune.Vevey;

	private class InternalServiceCivil extends MockServiceCivil {

		@Override
		protected void init() {

			indPierre = addIndividu(noIndPierre, dateNaissancePierre, "Mettraux", "Pierre", true);
			indKarina = addIndividu(noIndKarina, dateNaissanceKarina, "Mettraux-Pousaz", "Karina", false);

			marieIndividus(indPierre, indKarina, dateDemenagement);

			addOrigine(indPierre, MockPays.Suisse, MockCommune.Renens, dateNaissancePierre);
			addNationalite(indPierre, MockPays.Suisse, dateNaissancePierre, null, 0);

			addOrigine(indKarina, MockPays.Suisse, MockCommune.Lausanne, dateNaissanceKarina);
			addNationalite(indKarina, MockPays.Suisse, dateNaissanceKarina, null, 0);
		}

		public void divorceCivil() {
			divorceIndividus(indPierre, indKarina, dateDivorce);
		}
	}

	private InternalServiceCivil serviceCivil;

	@Override
	protected void initServiceCivil() {
		serviceCivil = new InternalServiceCivil();
		serviceCivilService.setUp(serviceCivil);
	}

	@Etape(id=1, descr="Chargement d'un habitant marié, de son conjoint et du ménage commun")
	public void step1() {

		// pierre
		final PersonnePhysique pierre = addHabitant(noIndPierre);
		{
			noHabPierre = pierre.getNumero();
		}

		// karina
		final PersonnePhysique karina = addHabitant(noIndKarina);
		{
			noHabKarina = karina.getNumero();
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, pierre, dateDemenagement, null);
			tiersService.addTiersToCouple(menage, karina, dateDemenagement, null);

			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, commune, dateDemenagement, null, MotifFor.DEMENAGEMENT_VD, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
		}
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre est marié avec Karina et le For du ménage existe")
	public void check1() throws Exception {

		{
			final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			final ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " non null");
		}

		{
			final PersonnePhysique karina = (PersonnePhysique)tiersDAO.get(noHabKarina);
			final ForFiscalPrincipal ffp = karina.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + karina.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateDemenagement, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + commune.getNomMinuscule());
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, true);
	}

	@Etape(id=2, descr="Divorce fiscal")
	public void step2() throws Exception {
		metierService.separe((MenageCommun) tiersDAO.get(noMenage), dateDivorce, null, EtatCivil.DIVORCE, true, null);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et les Fors principaux des individus créés")
	public void check2() {
		checkForsApresDivorceFiscal();
		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, true);
	}

	@Etape(id=3, descr="Envoi de l'événement de Divorce")
	public void step3() throws Exception {

		serviceCivil.divorceCivil();

		// on ré-autorise les remboursements automatiques pour vérifier qu'ils ne sont pas
		// bloqués à nouveau lors du divorce civil
		final MenageCommun menageCommun = (MenageCommun) tiersDAO.get(noMenage);
		menageCommun.setBlocageRemboursementAutomatique(false);

		long id = addEvenementCivil(TypeEvenementCivil.DIVORCE, noIndKarina, dateDivorce, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Tout doit être comme après l'étape 2, sauf le blocage des remboursements automatiques qui ne doit pas être touché")
	public void check3() {

		checkForsApresDivorceFiscal();

		// les remboursements automatiques ne doivent pas être bloqués par l'événement de divorce
		// civil suivant un divorce fiscal
		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, false);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduKarina, boolean blocageAttenduMenage) {

		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabPierre));
		assertBlocageRemboursementAutomatique(blocageAttenduKarina, tiersDAO.get(noHabKarina));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}

	private void checkForsApresDivorceFiscal() {

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateDemenagement, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			final ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + pierre.getNumero() + " est fermé");
			// pierre doit passer au mode dépense
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique karina = (PersonnePhysique) tiersDAO.get(noHabKarina);
			final ForFiscalPrincipal ffp = karina.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + karina.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + karina.getNumero() + " est fermé");
			// karina doit passer au mode ordinaire
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}
	}
}