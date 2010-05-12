package ch.vd.uniregctb.norentes.civil.annulation.deces;

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
 * Scénario d'un événement d'annulation de décès d'un marié seul.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_2001_02_AnnulationDeces_MarieSeul_Scenario extends EvenementCivilScenario {

	public static final String NAME = "2001_02_AnnulationDeces";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_DECES;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement d'annulation de décès d'un marié seul.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndAndre = 92647;

	private MockIndividu indAndre;
	private long noHabAndre;
	private long noMenage;

	private final RegDate dateDebutSuisse = RegDate.get(1980, 3, 1);
	private final RegDate dateMariage = RegDate.get(1982, 12, 4);
	private final RegDate dateDeces = RegDate.get(2008, 1, 1);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				indAndre = addIndividu(noIndAndre, RegDate.get(1956, 2, 25), "Girard", "André", true);
				marieIndividu(indAndre, dateMariage);
				addOrigine(indAndre, MockPays.France, null, indAndre.getDateNaissance());
				addNationalite(indAndre, MockPays.France, indAndre.getDateNaissance(), null, 0);
				addPermis(indAndre, EnumTypePermis.FRONTALIER, RegDate.get(2008, 9, 8), null, 0, false);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant marié seul")
	public void step1() {
		PersonnePhysique andre = addHabitant(noIndAndre);
		noHabAndre = andre.getNumero();
		ForFiscalPrincipal f = addForFiscalPrincipal(andre, commune, dateDebutSuisse, dateMariage.getOneDayBefore(),
				MotifFor.ARRIVEE_HC, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		f.setModeImposition(ModeImposition.SOURCE);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun) tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, andre, dateMariage, dateDeces);
		f = addForFiscalPrincipal(menage, commune, dateMariage, dateDeces,
				MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.VEUVAGE_DECES);
		f.setModeImposition(ModeImposition.SOURCE);
	}

	@Check(id=1, descr="Vérifie que les fors du ménage sont fermés car l'habitant est sensé être décédé")
	public void check1() {
		{
			PersonnePhysique andre = (PersonnePhysique) tiersDAO.get(noHabAndre);
			ForFiscalPrincipal ffp = andre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + andre.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal l'habitant " + andre.getNumero() + " est ouvert");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
		}
		{
			MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage " + menage.getNumero() + " est nul");
			assertNotNull(ffp.getDateFin(), "Le for principal du ménage " + menage.getNumero() + " est ouvert");
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas VEUVAGE_DECES");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement Annulation Décès")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ANNUL_DECES, noIndAndre, dateDeces, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux")
	public void check2() {
		{
			PersonnePhysique andre = (PersonnePhysique) tiersDAO.get(noHabAndre);
			ForFiscalPrincipal ffp = andre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'habitant " + andre.getNumero() + " null");
			assertEquals(dateDebutSuisse, ffp.getDateDebut(), "Date de début for fausse");
			assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin(), "Date de fin for fausse");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Le motif de fermeture n'est pas MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION");
			assertEquals(ffp.getModeImposition(), ModeImposition.SOURCE, "Le mode d'imposition n'est pas SOURCE");
		}
		{
			MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début for fausse");
			assertNull(ffp.getDateFin(), "Le for principal du ménage " + menage.getNumero() + " est fermé");
			assertNull(ffp.getMotifFermeture(), "Le for principal du ménage " + menage.getNumero() + " est fermé");
		}
	}

}
