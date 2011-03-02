package ch.vd.uniregctb.norentes.civil.reconciliation;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
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
 * Scénario de réconciliation avec des habitants non séparés
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_7000_04_Reconciliation_Non_Separes_Scenario extends EvenementCivilScenario {

	public static final String NAME = "7000_04_Reconciliation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.RECONCILIATION;
	}

	@Override
	public String getDescription() {
		return "Scénario de réconciliation avec habitants non séparés fiscal (cas d'erreur)";
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

	private final RegDate dateMariage = RegDate.get(2004, 4, 8);
	private final RegDate dateReconciliation = RegDate.get(2006, 7, 1);

	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				indBea = addIndividu(noIndBea, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);

				marieIndividus(indMomo, indBea, dateMariage);

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
		addForFiscalPrincipal(momo, commune, RegDate.get(1981, 3, 12), dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		// bea
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, commune, RegDate.get(1983, 8, 20), dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		// ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, null);
		tiersService.addTiersToCouple(menage, bea, dateMariage, null);
		addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
	}

	@Check(id = 1, descr = "Vérifie que les habitants sont bien toujours mariés (Maurice et Béatrice possèdent chacun un for fiscal principal fermé et le for principal du ménage est ouvert)")
	public void check1() {
		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Maurice ne possède pas de for fiscal principal");
			assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin(), "Le for principal de Maurice possède une date de fin fausse");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Béatrice ne possède pas de for fiscal principal");
			assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin(), "Le for principal de Béatrice possède une date de fin fausse");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le ménage commune ne possède pas de for fiscal principal");
			assertEquals(dateMariage, ffp.getDateDebut(),
					"Le for fiscal principal du ménage commun possède une date de début qui est fausse");
			assertNull(ffp.getDateFin(), "Le for fiscal principal du ménage commun est fermé");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for fiscal principal du ménage commun n'est pas sur la commune de " + commune.getNomMajuscule());
		}
	}

	@Etape(id = 2, descr = "Envoi d'un événement de réconciliation")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.RECONCILIATION, noIndBea, dateReconciliation, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'événement civil est en erreur")
	public void check2() {

		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabBea);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(),
				"L'événement de réconciliation devrait être en erreur car le couple n'était pas séparé");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir exactement une erreur");

		final EvenementCivilExterneErreur erreur = evt.getErreurs().iterator().next();
		assertEquals("Le couple n'est pas séparé", erreur.getMessage(), "L'erreur n'est pas la bonne");
	}
}
