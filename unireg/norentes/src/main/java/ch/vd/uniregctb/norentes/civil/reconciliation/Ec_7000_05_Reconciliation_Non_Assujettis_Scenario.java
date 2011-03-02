package ch.vd.uniregctb.norentes.civil.reconciliation;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de réconciliation avec des habitants non assujettis
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_7000_05_Reconciliation_Non_Assujettis_Scenario extends EvenementCivilScenario {

	public static final String NAME = "7000_05_Reconciliation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.RECONCILIATION;
	}

	@Override
	public String getDescription() {
		return "Scénario de réconciliation avec habitants non assujettis fiscalement";
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

	private final RegDate dateMariage = RegDate.get(1990, 4, 8);
	private final RegDate dateSeparation = RegDate.get(2000, 5, 1);
	private final RegDate dateDepartHC = RegDate.get(2000, 7, 30);
	private final RegDate dateReconciliation = RegDate.get(2004, 7, 1);

	private final Commune lausanne = MockCommune.Lausanne;
	private final Commune zurich = MockCommune.Zurich;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
				addOrigine(indMomo, MockPays.Suisse, null, RegDate.get(1961, 3, 12));
				addNationalite(indMomo, MockPays.Suisse, RegDate.get(1961, 3, 12), null, 0);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate
						.get(1961, 3, 12), dateDepartHC.getOneDayBefore());

				indBea = addIndividu(noIndBea, RegDate.get(1963, 8, 20), "Duval", "Béatrice", false);
				addOrigine(indBea, MockPays.Suisse, lausanne, RegDate.get(1963, 8, 20));
				addNationalite(indBea, MockPays.Suisse, RegDate.get(1963, 8, 20), null, 0);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, RegDate.get(
						1963, 8, 20), dateDepartHC.getOneDayBefore());

				// mariage : dans le canton
				marieIndividus(indMomo, indBea, dateMariage);

				// séparation : les deux partent hors-canton
				separeIndividus(indMomo, indBea, dateSeparation);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateDepartHC, null);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null,
						dateDepartHC, dateReconciliation.getOneDayBefore());

				// réconciliation : toujours hors-canton
				marieIndividus(indMomo, indBea, dateReconciliation);
				addAdresse(indBea, TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateReconciliation,
						null);
			}
		});
	}

	@Etape(id = 1, descr = "Chargement des contribuables Maurice, Béatrice et du ménage commun, dans l'état séparés et partis hors-Canton.")
	public void step1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		addForFiscalPrincipal(momo, lausanne, RegDate.get(1981, 3, 12), dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		addForFiscalPrincipal(momo, lausanne, dateSeparation, dateDepartHC.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.DEPART_HC);

		// bea
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, lausanne, RegDate.get(1983, 8, 20), dateMariage.getOneDayBefore(), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		addForFiscalPrincipal(bea, lausanne, dateSeparation, dateDepartHC.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MotifFor.DEPART_HC);

		// ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, dateSeparation.getOneDayBefore());
		tiersService.addTiersToCouple(menage, bea, dateMariage, dateSeparation.getOneDayBefore());
		addForFiscalPrincipal(menage, lausanne, dateMariage, dateSeparation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
	}

	@Check(id = 1, descr = "Vérifie que les habitants ne sont pas assujettis (Maurice et Béatrice possèdent chacun un for fiscal principal fermé et le for principal du ménage est aussi fermé)")
	public void check1() {
		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Maurice ne possède pas de for fiscal principal");
			assertEquals(dateDepartHC.getOneDayBefore(), ffp.getDateFin(), "Le for principal de Maurice possède une date de fin fausse");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Béatrice ne possède pas de for fiscal principal");
			assertEquals(dateDepartHC.getOneDayBefore(), ffp.getDateFin(), "Le for principal de Béatrice possède une date de fin fausse");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le ménage commune ne possède pas de for fiscal principal");
			assertEquals(dateMariage, ffp.getDateDebut(),
					"Le for fiscal principal du ménage commun possède une date de début qui est fausse");
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin(),
					"Le for principal du ménage commun possède une date de fin fausse");
			assertEquals(lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for fiscal principal du ménage commun n'est pas sur la commune de " + lausanne.getNomMajuscule());
		}
	}

	@Etape(id = 2, descr = "Envoi d'un événement de réconciliation (commune de Zurich)")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.RECONCILIATION, noIndMomo, dateReconciliation, zurich.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id = 2, descr = "Vérifie que l'événement civil est traité mais qu'aucun for n'a bougé")
	public void check2() {
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(),
				"L'événement de réconciliation devrait être traité car le couple est bien séparé");
		check1();
	}
}
