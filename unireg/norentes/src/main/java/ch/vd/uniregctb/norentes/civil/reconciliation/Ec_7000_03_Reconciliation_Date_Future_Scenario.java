package ch.vd.uniregctb.norentes.civil.reconciliation;

import annotation.Check;
import annotation.Etape;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de réconciliation avec une date dans le futur
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_7000_03_Reconciliation_Date_Future_Scenario extends EvenementCivilScenario {

	public static final String NAME = "7000_03_Reconciliation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.RECONCILIATION;
	}

	@Override
	public String getDescription() {
		return "Scénario de réconciliation dans le futur (cas d'erreur)";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMomo = 54321; // Momo
	private final long noIndBea = 23456; // Béa

	protected MockIndividu indMomo;
	protected MockIndividu indBea;

	private Long noHabMomo;
	private Long noHabBea;
	private Long noMenage;

	private final RegDate dateMariage = RegDate.get(1986, 4, 8);
	private final RegDate dateSeparation = RegDate.get(2007, 3, 24);
	private final RegDate dateReconciliation = RegDate.get(2080, 1, 1);

	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);
				separeIndividus(indMomo, indBea, dateSeparation);
				marieIndividus(indMomo, indBea, dateReconciliation);

				addOrigine(indBea, MockPays.Suisse, commune, RegDate.get(1963, 8, 20));
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);

				addOrigine(indMomo, MockPays.Suisse, null, RegDate.get(1961, 3, 12));
				addNationalite(indMomo, MockPays.Suisse, RegDate.get(1961, 3, 12), null, 0);
			}
		});
	}

	@Etape(id = 1, descr = "Chargement de l'habitant marié, de son conjoint et du ménage commun")
	public void step1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		addForFiscalPrincipal(momo, commune.getNoOFS(), dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);

		// bea
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, commune.getNoOFS(), dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun) tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, bea, dateMariage, dateSeparation.getOneDayBefore());
		addForFiscalPrincipal(menage, commune.getNoOFS(), dateMariage, dateSeparation.getOneDayBefore(),
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
	}

	@Check(id = 1, descr = "Vérifie que les habitants sont bien séparés (Maurice et Béatrice possèdent chacun un for fiscal principal ouvert et le for principal du ménage est fermé)")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Maurice ne possède pas de for fiscal principal");
			assertNull(ffp.getDateFin(), "Le for principal de Maurice est fermé");
		}

		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Béatrice ne possède pas de for fiscal principal");
			assertNull(ffp.getDateFin(), "Le for principal de Béatrice est fermé");
		}

		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le ménage commune ne possède pas de for fiscal principal");
			assertEquals(dateMariage, ffp.getDateDebut(),
					"Le dernier for fiscal principal du ménage commun possède une date de début qui est fausse");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(),
					"Le dernier for fiscal principal du ménage commun possède une date de fin qui est fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for fiscal principal du ménage commun n'est pas sur la commune de " + commune.getNomMajuscule());
		}
	}

	@Etape(id = 2, descr = "Envoi d'un événement de réconciliation au 1er janvier 2080")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.RECONCILIATION, noIndMomo, dateReconciliation, commune.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'événement civil est en erreur")
	public void check2() {

		final EvenementCivilRegroupe evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(),
				"L'événement de réconciliation devrait être en erreur car la date est dans le futur");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir exactement une erreur");

		final EvenementCivilErreur erreur = evt.getErreurs().iterator().next();
		assertEquals("La date de l'événement est dans le futur", erreur.getMessage(), "L'erreur n'est pas la bonne");
	}
}
