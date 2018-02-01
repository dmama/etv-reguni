package ch.vd.unireg.norentes.civil.veuvage;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario de veuvage et décès d'un individu suisse marié seul.
 * Ce scénario teste les événements de veuvage et décès dans l'ordre suivant:
 *   - dècés
 *   - veuvage
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_10000_04_Veuvage_DecesPuisVeuvageMemeJour_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10000_04_Veuvage";

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
	private final RegDate dateDeces = dateVeuvage;
	private final RegDate lendemainDeces = dateDeces.getOneDayAfter();
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indPierre = addIndividu(12345, dateNaissance, "Dupont", "Pierre", true);

				marieIndividu(indPierre, dateMariage);

				addOrigine(indPierre, MockCommune.Croy);
				addNationalite(indPierre, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(indPierre, dateVeuvage, TypeEtatCivil.VEUF);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant Pierre, Suisse marié seul, décédé et veuf le meme jour")
	public void step1() {

		// Pierre
		PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();
		addForFiscalPrincipal(pierre, MockCommune.VillarsSousYens, RegDate.get(1974, 3, 3),avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ModeImposition.SOURCE);
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

	@Check(id=1, descr="Vérifie que l'habitant Pierre est marié seul et le For du menage existe")
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

	@Etape(id=2, descr="Envoi de l'événement Décès")
	public void step2() throws Exception {

		indPierre.setDateDeces(dateDeces);

		final long id = addEvenementCivil(TypeEvenementCivil.DECES, noIndPierre, dateDeces, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le menage commun a été fermé et le For principal de l'individu créé")
	public void check2() {

		final List<EvenementCivilRegPP> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.DECES);
		assertNotNull(evts, "Pas du tout d'événement de décès?");
		assertEquals(1, evts.size(), "Il devrait y avoir un événement de DECES pour Pierre");

		final EvenementCivilRegPP evt = evts.get(0);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement n'a pas été traité correctement");

		{
			PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			assertNull(pierre.getForFiscalPrincipalAt(lendemainDeces), "Pierre ne devrait pas avoir de for après son décès");
		}

		{
			MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " ouvert");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}

		// le survivant ne doit pas voir ses remboursements automatiques bloqués (mais on ne le connait pas ici)
		assertBlocageRemboursementAutomatique(true, true);

	}

	@Etape(id=3, descr="Envoi de l'événenent Veuvage")
	public void step3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que le défunt n'a aucun for ni aucune situation de famille active")
	public void check3() {

		final List<EvenementCivilRegPP> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.VEUVAGE);
		assertNotNull(evts, "Pas du tout d'événement de veuvage?");
		assertEquals(1, evts.size(), "Il devrait y avoir un événement de VEUVAGE pour Pierre");

		final EvenementCivilRegPP evt = evts.get(0);
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

		// le survivant ne doit pas voir ses remboursements automatiques bloqués (mais on ne le connait pas ici)
		assertBlocageRemboursementAutomatique(true, true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabPierre));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}

}
