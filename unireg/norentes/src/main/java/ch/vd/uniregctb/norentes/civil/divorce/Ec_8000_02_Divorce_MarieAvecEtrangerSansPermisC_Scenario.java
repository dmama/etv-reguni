package ch.vd.uniregctb.norentes.civil.divorce;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
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

	private final long noIndMomo = 54321; // momo
	private final long noIndBea = 23456; // bea

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
	private final int communeMariage = MockCommune.Lausanne.getNoOFS();
	private final int communeDivorce = MockCommune.VillarsSousYens.getNoOFS();

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, dateNaissanceBea, "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);
				divorceIndividus(indMomo, indBea, dateDivorce);

				addOrigine(indMomo, MockPays.France, null, RegDate.get(1963, 8, 20));
				addNationalite(indMomo, MockPays.France, RegDate.get(1963, 8, 20), null, 0);
				addPermis(indMomo, EnumTypePermis.COURTE_DUREE, RegDate.get(1963, 8, 20), null, 0, false);

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
			final ForFiscalPrincipal f = addForFiscalPrincipal(momo, MockCommune.VillarsSousYens.getNoOFS(), dateArriveeVillars,
					avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f.setModeImposition(ModeImposition.SOURCE);
		}

		// bea
		final PersonnePhysique bea = addHabitant(noIndBea);
		{
			noHabBea = bea.getNumero();
			final ForFiscalPrincipal f = addForFiscalPrincipal(bea, MockCommune.Lausanne.getNoOFS(), dateMajorite, avantDateMariage,
					MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f.setModeImposition(ModeImposition.ORDINAIRE);
		}

		// ménage
		{
			MenageCommun menage = new MenageCommun();
			menage = (MenageCommun)tiersDAO.save(menage);
			noMenage = menage.getNumero();
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);
			final ForFiscalPrincipal f = addForFiscalPrincipal(menage, communeMariage, dateMariage, null,
					MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
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
			assertEquals(communeMariage, ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur Villars-sous-Yens");
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, true);
	}

	@Etape(id=2, descr="Envoi de l'événement de Divorce")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DIVORCE, noIndMomo, dateDivorce, communeDivorce);
		commitAndStartTransaction();

		regroupeEtTraiteEvenements(id);
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
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
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
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for de l'habitant " + bea.getNumero() + " est fermé");
			// bea doit passer au mode ordinaire
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture(),
					"Le motif de fermeture n'est pas SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT");
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduMomo, boolean blocageAttenduBea, boolean blocageAttenduMenage) {

		assertBlocageRemboursementAutomatique(blocageAttenduMomo, tiersDAO.get(noHabMomo));
		assertBlocageRemboursementAutomatique(blocageAttenduBea, tiersDAO.get(noHabBea));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}