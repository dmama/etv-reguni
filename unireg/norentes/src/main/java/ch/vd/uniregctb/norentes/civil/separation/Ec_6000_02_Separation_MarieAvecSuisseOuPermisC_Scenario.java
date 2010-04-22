package ch.vd.uniregctb.norentes.civil.separation;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scenario d'un événement divorce d'une personne de nationalité suisse avec un étranger avec permis C.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6000_02_Separation_MarieAvecSuisseOuPermisC_Scenario extends EvenementCivilScenario {

	public static final String NAME = "6000_02_Separation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.SEPARATION;
	}

	@Override
	public String getDescription() {
		return "Séparation d'un habitant suisse marié avec un étranger ayant un permis C";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMomo = 54321; // momo
	private final long noIndBea = 23456; // bea

	private MockIndividu indMomo;
	private MockIndividu indBea;

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateNaissanceMomo = RegDate.get(1961, 3, 12);
	private final RegDate dateArriveeMomoVillars = RegDate.get(1974, 3, 3);
	private final RegDate dateNaissanceBea = RegDate.get(1963, 8, 20);
	private final RegDate dateMajoriteBea = dateNaissanceBea.addYears(18);
	private final RegDate dateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateSeparation = RegDate.get(2008, 1, 22);
	private final RegDate dateDivorce = dateSeparation.addMonths(4);
	private final RegDate dateAvantMariage = dateMariage.addDays(-1);

	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, dateNaissanceMomo, "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, dateNaissanceBea, "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);
				separeIndividus(indMomo, indBea, dateSeparation);
				divorceIndividus(indMomo, indBea, dateDivorce);

				addOrigine(indMomo, MockPays.France, null, RegDate.get(1963, 8, 20));
				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(indMomo, EnumTypePermis.ETABLLISSEMENT, RegDate.get(1963, 8, 20), null, 0, false);

				addOrigine(indBea, MockPays.Suisse, MockCommune.Lausanne, RegDate.get(1961, 3, 12));
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1961, 3, 12), null, 0);
			}
		});
	}

	@Etape(id=1, descr="Chargement d'un habitant marié, de son conjoint et du ménage commun")
	public void etape1() {
		// momo
		final PersonnePhysique momo = addHabitant(noIndMomo);
		{
			noHabMomo = momo.getNumero();
			addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateArriveeMomoVillars, dateAvantMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// bea
		final PersonnePhysique bea = addHabitant(noIndBea);
		{
			noHabBea = bea.getNumero();
			addForFiscalPrincipal(bea, MockCommune.Lausanne, dateMajoriteBea, dateAvantMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			f.setModeImposition(ModeImposition.DEPENSE);
		}
	}

	@Check(id=1, descr="Vérifie que les habitants n'ont pas de For ouvert et le For du ménage existe")
	public void check1() {
		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique)tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertEquals(dateAvantMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, true);
	}

	@Etape(id=2, descr="Envoi des évémenent de Séparation")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndBea, dateSeparation, communeMariage.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des for commun et principaux")
	public void check2() {
		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(ffp.getMotifFermeture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + momo.getNumero() + " est fermé");
			// momo doit passer au mode dépense
			final ModeImposition expected = ModeImposition.DEPENSE;
			assertEquals(ffp.getModeImposition(), expected, "Le mode d'imposition n'est pas " + expected.texte());
			assertEquals(ffp.getMotifOuverture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + bea.getNumero() + " est fermé");
			// bea doit passer au mode dépense
			final ModeImposition expected = ModeImposition.DEPENSE;
			assertEquals(ffp.getModeImposition(), expected, "Le mode d'imposition n'est pas " + expected.texte());
			assertEquals(ffp.getMotifOuverture(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, true);
	}

	@Etape(id=3, descr="Envoi des événements de Divorce")
	public void etape3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DIVORCE, noIndMomo, dateDivorce, communeMariage.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=3, descr="Tout doit être comme aprés l'étape 2")
	public void check3() {
		check2();
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMomo, boolean blocageAttenduBea, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduMomo, tiersDAO.get(noHabMomo));
		assertBlocageRemboursementAutomatique(blocageAttenduBea, tiersDAO.get(noHabBea));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}