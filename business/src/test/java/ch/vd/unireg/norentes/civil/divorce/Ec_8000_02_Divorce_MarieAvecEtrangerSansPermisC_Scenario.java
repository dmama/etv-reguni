package ch.vd.unireg.norentes.civil.divorce;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
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
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scenario d'un événement divorce.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_8000_02_Divorce_MarieAvecEtrangerSansPermisC_Scenario extends EvenementCivilScenario {

	public static final String NAME = "8000_02_Divorce";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DIVORCE;
	}

	@Override
	public String getDescription() {
		return "Divorce d'un habitant suisse marié avec un étranger sans permis C";
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

	private final RegDate dateNaissanceBea = RegDate.get(1963, 8, 20);
	private final RegDate dateMajorite = dateNaissanceBea.addYears(18);
	private final RegDate dateArriveeVillars = RegDate.get(1974, 3, 3);
	private final RegDate avantDateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateMariage = avantDateMariage.addDays(1);
	private final RegDate dateDivorce = RegDate.get(2008, 10, 10);
	private final MockCommune communeMariage = MockCommune.Lausanne;
	private final MockCommune communeDivorce = MockCommune.VillarsSousYens;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, dateNaissanceBea, "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);
				divorceIndividus(indMomo, indBea, dateDivorce);

				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null);
				addPermis(indMomo, TypePermis.COURTE_DUREE, RegDate.get(1963, 8, 20), null, false);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, dateMariage, null);

				addOrigine(indBea, MockCommune.Lausanne);
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1961, 3, 12), null);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, dateMariage, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement d'un habitant marié, de son conjoint et du ménage commun")
	public void etape1() {

		// momo
		final PersonnePhysique momo = addHabitant(noIndMomo);
		{
			noHabMomo = momo.getNumero();
			addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateArriveeVillars, avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                      ModeImposition.SOURCE);
		}

		// bea
		final PersonnePhysique bea = addHabitant(noIndBea);
		{
			noHabBea = bea.getNumero();
			addForFiscalPrincipal(bea, MockCommune.Lausanne, dateMajorite, avantDateMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);
			addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);

			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice est marié avec Béatrice et le For du menage existe")
	public void check1() throws Exception {

		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique)tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur Villars-sous-Yens");
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Envoi de l'événement de Divorce")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DIVORCE, noIndMomo, dateDivorce, communeDivorce.getNoOFS());
		commitAndStartTransaction();

		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et les Fors principaux des individus créés")
	public void check2() {

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Le for sur Lausanne n'est pas ouvert à la bonne date");
			assertNotNull(ffp.getDateFin(), "Le for sur Lausanne est ouvert");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipalPP ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + momo.getNumero() + " est fermé");
			// momo doit passer au mode mixte 137 al.1
			ModeImposition expected = ModeImposition.MIXTE_137_1;
			assertEquals(expected, ffp.getModeImposition(), "Le mode d'imposition n'est pas " + expected);
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipalPP ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + bea.getNumero() + " est fermé");
			// bea doit passer au mode ordinaire
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		assertBlocageRemboursementAutomatique(false, false, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMomo, boolean blocageAttenduBea, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduMomo, tiersDAO.get(noHabMomo));
		assertBlocageRemboursementAutomatique(blocageAttenduBea, tiersDAO.get(noHabBea));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
