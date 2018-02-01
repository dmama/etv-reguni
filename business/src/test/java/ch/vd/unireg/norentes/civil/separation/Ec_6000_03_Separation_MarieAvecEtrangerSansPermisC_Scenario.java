package ch.vd.unireg.norentes.civil.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scenario d'un événement divorce d'une personne mariée avec un étranger sans permis C.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_6000_03_Separation_MarieAvecEtrangerSansPermisC_Scenario extends EvenementCivilScenario {

	public static final String NAME = "6000_03_Separation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.SEPARATION;
	}

	@Override
	public String getDescription() {
		return "Séparation d'un habitant suisse marié avec un étranger n'ayant pas de permis C";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMomo = 54321; // momo
	private static final long noIndBea = 23456; // bea

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
	private final RegDate dateAvantMariage = dateMariage.addDays(-1);
	private final RegDate dateSeparation = RegDate.get(2008, 1, 22);
	private final RegDate dateDivorce = dateSeparation.addMonths(8).addDays(7);

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

				addNationalite(indMomo, MockPays.France, dateNaissanceMomo, null);
				addPermis(indMomo, TypePermis.COURTE_DUREE, dateNaissanceMomo, null, false);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateMariage, dateSeparation.getOneDayBefore());
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Chamblon.RueDesUttins, null, dateSeparation, null);

				addOrigine(indBea, MockCommune.Lausanne);
				addNationalite(indBea, MockPays.Suisse, dateNaissanceBea, null);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateNaissanceBea, null);
			}

		});
	}

	@Etape(id=1, descr="Chargement de l'habitant suisse, de son conjoint et du ménage commun")
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
			menage = (MenageCommun) tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);
			addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, ModeImposition.DEPENSE);
			menage.setBlocageRemboursementAutomatique(false);
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
					"Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Envoi des événements de séparation")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndBea, dateSeparation, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des for commun et principaux")
	public void check2() {
		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture(),  "Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipalPP ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + momo.getNumero() + " est fermé");
			// momo doit passer au mode dépense
			final ModeImposition expected = ModeImposition.DEPENSE;
			assertEquals(expected, ffp.getModeImposition(), "Le mode d'imposition n'est pas " + expected.texte());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(MockCommune.Chamblon.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Mauvaise commune de for");
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipalPP ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + bea.getNumero() + " est fermé");
			// bea doit passer au mode dépense
			final ModeImposition expected = ModeImposition.DEPENSE;
			assertEquals(expected, ffp.getModeImposition(), "Le mode d'imposition n'est pas " + expected.texte());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
			assertEquals(MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Mauvaise commune de for");
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		}

		assertBlocageRemboursementAutomatique(false, false, true);
	}

	@Etape(id=3, descr="Envoi des événements de Divorce")
	public void etape3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DIVORCE, noIndMomo, dateDivorce, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que rien n'a changé depuis l'étape précédente")
	public void check3() {
		check2();
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMomo, boolean blocageAttenduBea, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduMomo, tiersDAO.get(noHabMomo));
		assertBlocageRemboursementAutomatique(blocageAttenduBea, tiersDAO.get(noHabBea));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
