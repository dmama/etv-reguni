package ch.vd.unireg.norentes.civil.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario de veuvage et dècés d'un individu suisse marié seul.
 * Ce scénario teste les événements de veuvage et décès dans l'ordre suivant:
 *   - veuvage
 *   - dècés
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_10000_03_Veuvage_VeuvagePuisDecesMemeJour_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10000_03_Veuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario de veuvage et décès d'un marié seul";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndPierre = 12345; // Pierre

	private MockIndividu indPierre;

	private long noHabPierre;
	private long noMenage;

	private final RegDate dateNaissance = RegDate.get(1953, 11, 2);
	private final RegDate avantDateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateMariage = avantDateMariage.getOneDayAfter();
	private final RegDate dateVeuvage = RegDate.get(2008, 1, 1);
	private final RegDate lendemainVeuvage = dateVeuvage.getOneDayAfter();
	private final RegDate dateDeces = dateVeuvage;
	private final RegDate lendemainDeces = dateDeces.getOneDayAfter();
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {

				indPierre = addIndividu(12345, dateNaissance, "Dupont", "Pierre", true);

				marieIndividu(indPierre, dateMariage);

				addOrigine(indPierre, MockCommune.Lausanne);
				addNationalite(indPierre, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(indPierre, dateVeuvage, TypeEtatCivil.VEUF);
				addAdresse(indPierre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, dateMariage, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant Pierre, Suisse marié seul, veuf et décédé le meme jour")
	public void step1() {

		// Pierre
		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();
		addForFiscalPrincipal(pierre, MockCommune.VillarsSousYens, RegDate.get(1974, 3, 3), avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ModeImposition.SOURCE);
		addSituationFamille(pierre, dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, 0);

		// ménage
		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun)tiersDAO.save(menage);
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, null);
		addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		addSituationFamille(menage, dateMariage, null, EtatCivil.MARIE, 0, null, pierre);

		menage.setBlocageRemboursementAutomatique(false);
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre est marié seul et le For du ménage existe")
	public void check1() throws Exception {

		PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
		{
			ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, dateMariage.getOneDayAfter());
			assertNull(etc.getConjoint(pierre), "Pierre n'est pas marié seul");
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}

		assertBlocageRemboursementAutomatique(true, false);
	}

	@Etape(id=2, descr="Envoi de l'événenent Veuvage")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le ménage commun a été fermé et le For principal de l'individu créé")
	public void check2() {

		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			ForFiscalPrincipalPP ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Le for principal de l'habitant est fermé");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "L'habitant devrait être en mode ordinaire");
			assertNull(pierre.getSituationFamilleAt(dateVeuvage), "Pierre ne devrait pas avoir de situation de famille le jour du veuvage.");
			assertNull(pierre.getSituationFamilleAt(lendemainVeuvage), "Pierre ne devrait pas avoir de nouvelle situation de famille au lendemain du veuvage car il est déjà marqué comme VEUF au civil");
		}

		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " ouvert");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
			assertSituationFamille(dateMariage, dateVeuvage, EtatCivil.MARIE, 0, mc.getSituationFamilleAt(dateVeuvage), "Situation de famille du ménage le jour du veuvage:");
			assertNull(mc.getSituationFamilleAt(lendemainDeces), "Le ménage ne devrait plus avoir de situation de famille le lendemain du veuvage.");
		}

		// le survivant ne doit pas voir ses remboursements automatiques bloqués
		assertBlocageRemboursementAutomatique(false, true);
	}

	@Etape(id=3, descr="Envi de l'événement Décès")
	public void step3() throws Exception {

		doModificationIndividu(noIndPierre, individu -> individu.setDateDeces(dateDeces));

		long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndPierre, dateDeces, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que le défunt n'a aucun for ni aucune situation de famille active")
	public void check3() {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabPierre);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement n'a pas été traité correctement");

		{
			final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			assertNull(pierre.getForFiscalPrincipalAt(dateVeuvage), "Pierre ne devrait pas avoir de for à la date de veuvage (aussi son décès)");
			assertNull(pierre.getForFiscalPrincipalAt(lendemainDeces), "Pierre ne devrait pas avoir de for le lendemain de son décès");
			assertNull(pierre.getSituationFamilleAt(dateDeces), "La situation de famille ne devrait pas exister à la date de veuvage (aussi son décès)");
			assertNull(pierre.getSituationFamilleAt(lendemainDeces), "La situation de famille ne devrait pas exister le lendemain de son décès");
		}

		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			assertSituationFamille(dateMariage, dateDeces, EtatCivil.MARIE, 0, mc.getSituationFamilleAt(dateDeces), "Situation de famille du ménage le jour du décès:");
			assertNull(mc.getSituationFamilleAt(lendemainDeces), "Le ménage ne devrait plus avoir de situation de famille le lendemain du décès/veuvage.");
		}

		assertBlocageRemboursementAutomatique(true, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabPierre));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
