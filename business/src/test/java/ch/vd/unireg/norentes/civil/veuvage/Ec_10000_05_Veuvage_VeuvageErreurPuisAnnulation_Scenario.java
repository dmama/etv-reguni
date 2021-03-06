package ch.vd.unireg.norentes.civil.veuvage;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockEtatCivilList;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Scénario de veuvage en erreur initialement suivi d'une annulation
 * Ce scénario teste les événements de veuvage et décès dans l'ordre suivant:
 *   - veuvage
 *   - annulation veuvage
 */
public class Ec_10000_05_Veuvage_VeuvageErreurPuisAnnulation_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10000_05_Veuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario de veuvage en erreur initialement suivi d'une annulation";
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
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant Pierre, Suisse marié seul, veuf ")
	public void step1() {

		// Pierre
		final PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();
		addForFiscalPrincipal(pierre, MockCommune.VillarsSousYens, RegDate.get(1974, 3, 3), avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		// ménage
		final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, dateVeuvage);
		addForFiscalPrincipal(menage, communeMariage, dateMariage, dateVeuvage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.VEUVAGE_DECES);

		// veuvage
		addForFiscalPrincipal(pierre, MockCommune.VillarsSousYens, dateVeuvage.addDays(1), null,  MotifFor.VEUVAGE_DECES, null);
		pierre.setBlocageRemboursementAutomatique(false);
	}

	@Check(id=1, descr="Vérifie que l'habitant Pierre est veuf fiscal")
	public void check1() throws Exception {

		final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
		{
			final ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertEquals(dateVeuvage.addDays(1), ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture(), "Motif d'ouverture du dernier for faux");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, dateMariage);
			assertNull(etc.getConjoint(pierre), "Pierre n'est pas marié seul");
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomOfficiel());
		}

		assertBlocageRemboursementAutomatique(false, true);
	}

	@Etape(id=2, descr="Envoi de l'événement Veuvage")
	public void step2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="L'événement de veuvage doit être en erreur car Pierre est déjà veuf")
	public void check2() {

		final List<EvenementCivilRegPP> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.VEUVAGE);
		assertNotNull(evts, "Pas d'événement trouvé!");
		assertEquals(1, evts.size(), "Il devrait y avoir un et un seul événement civil de veuvage sur Pierre");

		final EvenementCivilRegPP evt = evts.get(0);
		assertNotNull(evt, "Evenement null?");
		assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat(), "L'événement civil a été traité!");

		// rien n'a changé (événement redondant)
		assertBlocageRemboursementAutomatique(false, true);
	}

	@Etape(id=3, descr="Envoi de l'événenent d'annulation de veuvage")
	public void step3() throws Exception {

		final MockEtatCivilList ecs = indPierre.getEtatsCivils();
		assertNotNull(ecs, "Pas d'états civils du tout?");

		final EtatCivil ecVeuf = ecs.getEtatCivilAt(dateVeuvage);
		assertNotNull(ecVeuf, "Pas d'état civil à la date de veuvage?");
		assertEquals(TypeEtatCivil.VEUF, ecVeuf.getTypeEtatCivil(), "Devrait être veuf");
		ecs.remove(ecVeuf);

		final EtatCivil ecMarie = ecs.getEtatCivilAt(dateVeuvage);
		assertNotNull(ecMarie, "Pas d'état civil à la date de veuvage?");
		assertEquals(TypeEtatCivil.MARIE, ecMarie.getTypeEtatCivil(), "Devrait être marié, puisque le veuvage est annulé");

		final long id = addEvenementCivil(TypeEvenementCivil.ANNUL_VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que Pierre n'est plus veuf")
	public void check3() {

		{
			final List<EvenementCivilRegPP> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.ANNUL_VEUVAGE);
			assertNotNull(evts, "Pas d'événement trouvé!");
			assertEquals(1, evts.size(), "Il devrait y avoir un et un seul événement civil d'annulation de veuvage sur Pierre");

			final EvenementCivilRegPP evt = evts.get(0);
			assertNotNull(evt, "Evenement null?");
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil n'a pas été traité!");
		}

		{
			final List<EvenementCivilRegPP> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.VEUVAGE);
			assertNotNull(evts, "Pas d'événement trouvé!");
			assertEquals(1, evts.size(), "Il devrait y avoir un et un seul événement civil de veuvage sur Pierre");

			final EvenementCivilRegPP evt = evts.get(0);
			assertNotNull(evt, "Evenement null?");
			assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat(), "L'événement civil a été traité!");

			final Set<EvenementCivilRegPPErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs, "Evénement en erreur mais collection d'erreurs nulle?");
			assertEquals(0, erreurs.size(), "L'evenement ne devrait pas être en erreur car redondant");

		}

		{
			final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
			assertNull(pierre.getForFiscalPrincipalAt(dateVeuvage), "Pierre ne devrait pas avoir de for à la date de veuvage");
			assertNull(pierre.getForFiscalPrincipalAt(lendemainVeuvage), "Pierre ne devrait pas avoir de for le lendemain de son veuvage");

			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(dateMariage);
			assertNotNull(ffp, "Pas de for sur le ménage?");
			assertNull(ffp.getDateFin(), "Le for sur le ménage devrait avoir été ré-ouvert!");
		}

		assertBlocageRemboursementAutomatique(true, false);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabPierre));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}

}
