package ch.vd.uniregctb.norentes.civil.annulation.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement d'annulation de décès d'une personne mariée.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_2001_03_AnnulationDeces_Marie_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2001_03_AnnulationDeces";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_DECES;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement d'annulation de décès d'une personne mariée.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMomo = 54321;
	private static final long noIndBea = 23456;

	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final RegDate dateMajoriteBea = RegDate.get(1981, 8, 20);
	private final RegDate dateArriveeMomo = RegDate.get(1974, 3, 3);
	private final RegDate dateMariage = RegDate.get(1986, 4, 27);
	private final RegDate veilleMariage = dateMariage.getOneDayBefore();
	private final RegDate dateDeces = RegDate.get(2008, 6, 12);
	private final RegDate lendemainDeces = dateDeces.getOneDayAfter();
	private final Commune commune = MockCommune.Vevey;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil());
	}

	@Etape(id=1, descr="Chargement du couple")
	public void step1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();
		addForFiscalPrincipal(momo, commune, dateArriveeMomo, veilleMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		addSituationFamille(momo, dateArriveeMomo, veilleMariage, EtatCivil.CELIBATAIRE, 0);

		// bea
		PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();
		addForFiscalPrincipal(bea, MockCommune.Lausanne, dateMajoriteBea, veilleMariage, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		addSituationFamille(bea, dateMajoriteBea, veilleMariage, EtatCivil.CELIBATAIRE, 0);
		addSituationFamille(bea, lendemainDeces, null, EtatCivil.VEUF, 0);

		// ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, momo, dateMariage, dateDeces);
		tiersService.addTiersToCouple(menage, bea, dateMariage, dateDeces);
		addForFiscalPrincipal(menage, commune, dateMariage, dateDeces, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.VEUVAGE_DECES);

		addSituationFamille(menage, dateMariage, dateDeces, EtatCivil.MARIE, 0, null, momo);
	}

	@Check(id=1, descr="Vérifie que les fors et la situation de famille du ménage sont fermés car un Maurice est décédé")
	public void check1() {
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + momo.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal l'habitant " + momo.getNumero() + " est ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertNull(momo.getSituationFamilleActive(), "La situation de famille de Maurice devrait être fermée");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + bea.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal l'habitant " + bea.getNumero() + " est ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertSituationFamille(lendemainDeces, null, EtatCivil.VEUF, 0, bea.getSituationFamilleActive(), "Situation de famille de Béatrice:");
		}
		{
			MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage " + menage.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal du ménage " + menage.getNumero() + " est ouvert");
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas VEUVAGE_DECES");

			// [UNIREG-823] situation de famille
			assertNull(menage.getSituationFamilleActive(), "La situation de famille du ménage devrait être fermée");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement d'annulation de décès de Maurice")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_DECES, noIndMomo, dateDeces, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux et de la situation de famille")
	public void check2() {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabMomo);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil devrait être traité");

		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le dernier for principal de l'habitant " + momo.getNumero() + " est nul");
			assertEquals(veilleMariage, ffp.getDateFin(), "Le for principal l'habitant " + momo.getNumero() + " n'est pas fermé à la date correcte");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertSituationFamille(dateArriveeMomo, veilleMariage, EtatCivil.CELIBATAIRE, 0, momo.getSituationFamilleAt(veilleMariage), "Situation de famille de Maurice:");
			assertNull(momo.getSituationFamilleActive(), "Situation de famille de Maurice ne devrait pas exister");
		}
		{
			PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le dernier for principal de l'habitant " + bea.getNumero() + " est nul");
			assertEquals(veilleMariage, ffp.getDateFin(), "Le for principal l'habitant " + bea.getNumero() + " n'est pas fermé à la date correcte");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertSituationFamille(dateMajoriteBea, veilleMariage, EtatCivil.CELIBATAIRE, 0, bea.getSituationFamilleAt(veilleMariage), "Situation de famille de Béatrice:");
			assertNull(bea.getSituationFamilleAt(dateMariage), "Situation de famille de Béatrice ne devrait pas exister");
		}
		{
			MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for principal du ménage " + menage.getNumero() + " est nul");
			assertNull(ffp.getDateFin(), "Le for principal du ménage " + menage.getNumero() + " devrait être ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture(), "Le motif d'ouverture devrait être MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
			assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être vide");

			// [UNIREG-823] situation de famille
			assertSituationFamille(dateMariage, null, EtatCivil.MARIE, 0, menage.getSituationFamilleActive(), "Situation de famille du ménage commun:");
		}
	}
}
