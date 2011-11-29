package ch.vd.uniregctb.norentes.civil.annulation.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
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
 * Scénario d'un événement d'annulation de décès d'une personne pacsée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_2001_04_AnnulationDeces_Pacse_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2001_04_AnnulationDeces";

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

	private static final long noIndDavid = 45678;
	private static final long noIndJulien = 56789;

	private long noHabDavid;
	private long noHabJulien;
	private long noMenage;

	private final RegDate dateMajoriteJulien = RegDate.get(1981, 8, 20);
	private final RegDate dateArriveeDavid = RegDate.get(1974, 3, 3);
	private final RegDate datePacs = RegDate.get(1986, 4, 27);
	private final RegDate veillePacs = datePacs.getOneDayBefore();
	private final RegDate dateDeces = RegDate.get(2008, 6, 12);
	private final RegDate lendemainDeces = dateDeces.getOneDayAfter();
	private final Commune commune = MockCommune.Vevey;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil());
	}

	@Etape(id=1, descr="Chargement du couple")
	public void step1() {
		// david
		PersonnePhysique david = addHabitant(noIndDavid);
		noHabDavid = david.getNumero();
		addForFiscalPrincipal(david, commune, dateArriveeDavid, veillePacs, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		addSituationFamille(david, dateArriveeDavid, veillePacs, EtatCivil.CELIBATAIRE, 0);

		// julien
		PersonnePhysique julien = addHabitant(noIndJulien);
		noHabJulien = julien.getNumero();
		addForFiscalPrincipal(julien, MockCommune.Lausanne, dateMajoriteJulien, veillePacs, MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		addSituationFamille(julien, dateMajoriteJulien, veillePacs, EtatCivil.CELIBATAIRE, 0);
		addSituationFamille(julien, lendemainDeces, null, EtatCivil.VEUF, 0);

		// ménage
		MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, david, datePacs, dateDeces);
		tiersService.addTiersToCouple(menage, julien, datePacs, dateDeces);
		addForFiscalPrincipal(menage, commune, datePacs, dateDeces, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.VEUVAGE_DECES);

		addSituationFamille(menage, datePacs, dateDeces, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, null, david);
	}

	@Check(id=1, descr="Vérifie que les fors et la situation de famille du ménage sont fermés car David est décédé")
	public void check1() {
		{
			PersonnePhysique david = (PersonnePhysique) tiersDAO.get(noHabDavid);
			ForFiscalPrincipal ffp = david.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + david.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal l'habitant " + david.getNumero() + " est ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertNull(david.getSituationFamilleActive(), "La situation de famille de David devrait être fermée");
		}
		{
			PersonnePhysique julien = (PersonnePhysique) tiersDAO.get(noHabJulien);
			ForFiscalPrincipal ffp = julien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + julien.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal l'habitant " + julien.getNumero() + " est ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertSituationFamille(lendemainDeces, null, EtatCivil.VEUF, 0, julien.getSituationFamilleActive(), "Situation de famille de Julien:");
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

	@Etape(id=2, descr="Envoi de l'événement d'annulation de décès de David")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_DECES, noIndDavid, dateDeces, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux et de la situation de famille")
	public void check2() {

		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabDavid);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil devrait être traité");

		{
			PersonnePhysique david = (PersonnePhysique) tiersDAO.get(noHabDavid);
			ForFiscalPrincipal ffp = david.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le dernier for principal de l'habitant " + david.getNumero() + " est nul");
			assertEquals(veillePacs, ffp.getDateFin(), "Le for principal l'habitant " + david.getNumero() + " n'est pas fermé à la date correcte");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertSituationFamille(dateArriveeDavid, veillePacs, EtatCivil.CELIBATAIRE, 0, david.getSituationFamilleAt(veillePacs), "Situation de famille de David:");
			assertNull(david.getSituationFamilleActive(), "Situation de famille de David ne devrait pas exister");
		}
		{
			PersonnePhysique julien = (PersonnePhysique) tiersDAO.get(noHabJulien);
			ForFiscalPrincipal ffp = julien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le dernier for principal de l'habitant " + julien.getNumero() + " est nul");
			assertEquals(veillePacs, ffp.getDateFin(), "Le for principal l'habitant " + julien.getNumero() + " n'est pas fermé à la date correcte");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");

			// [UNIREG-823] situation de famille
			assertSituationFamille(dateMajoriteJulien, veillePacs, EtatCivil.CELIBATAIRE, 0, julien.getSituationFamilleAt(veillePacs), "Situation de famille de Julien:");
			assertNull(julien.getSituationFamilleAt(datePacs), "Situation de famille de Julien ne devrait pas exister");
		}
		{
			MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "Le for principal du ménage " + menage.getNumero() + " est nul");
			assertNull(ffp.getDateFin(), "Le for principal du ménage " + menage.getNumero() + " devrait être ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture(), "Le motif d'ouverture devrait être MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
			assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être vide");

			// [UNIREG-823] situation de famille
			assertSituationFamille(datePacs, null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, menage.getSituationFamilleActive(), "Situation de famille du ménage commun:");
		}
	}
}
