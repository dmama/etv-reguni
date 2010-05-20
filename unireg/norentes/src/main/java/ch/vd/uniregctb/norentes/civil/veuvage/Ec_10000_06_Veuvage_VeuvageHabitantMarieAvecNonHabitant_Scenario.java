package ch.vd.uniregctb.norentes.civil.veuvage;

import java.util.List;

import annotation.Check;
import annotation.Etape;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de veuvage d'un habitant marié avec un non-habitant connu (effectivement, au niveau civil,
 * il est considéré comme marié seul)<br>
 * On va ensuite également annuler le veuvage pour vérifier que tout redevient comme avant
 */
public class Ec_10000_06_Veuvage_VeuvageHabitantMarieAvecNonHabitant_Scenario extends EvenementCivilScenario {

	public static final String NAME = "10000_06_Veuvage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.VEUVAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario de veuvage d'un habitant marié à un non-habitant inconnu au civil";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndPierre = 12345; // Pierre

	private MockIndividu indPierre;

	private long noHabPierre;
	private long noNonHabMarie;
	private long noMenage;

	private final RegDate dateNaissance = RegDate.get(1953, 11, 2);
	private final RegDate dateArriveePierre = RegDate.get(1974, 3, 3);
	private final RegDate avantDateMariage = RegDate.get(1986, 4, 27);
	private final RegDate dateMariage = avantDateMariage.getOneDayAfter();
	private final RegDate dateVeuvage = RegDate.get(2008, 1, 1);
	private final RegDate lendemainVeuvage = dateVeuvage.getOneDayAfter();
	private final Commune communeMariage = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				indPierre = addIndividu(12345, dateNaissance, "Dupont", "Pierre", true);

				marieIndividu(indPierre, dateMariage);

				addOrigine(indPierre, MockPays.Suisse, null, dateNaissance);
				addNationalite(indPierre, MockPays.Suisse, dateNaissance, null, 0);
				addEtatCivil(indPierre, dateVeuvage, EnumTypeEtatCivil.VEUF);
				addAdresse(indPierre, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.PlaceSaintFrancois, null, dateMariage, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant Pierre, suisse marié avec non-habitant")
	public void step1() {

		// Pierre
		final PersonnePhysique pierre = addHabitant(noIndPierre);
		noHabPierre = pierre.getNumero();
		addForFiscalPrincipal(pierre, MockCommune.VillarsSousYens, dateArriveePierre, avantDateMariage, MotifFor.DEMENAGEMENT_VD, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

		// Marie, non-habitante
		final PersonnePhysique marie = addNonHabitant("Dupont", "Marie", RegDate.get(1955, 3, 12), Sexe.FEMININ);
		noNonHabMarie = marie.getNumero();

		// ménage
		final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		noMenage = menage.getNumero();
		tiersService.addTiersToCouple(menage, pierre, dateMariage, null);
		tiersService.addTiersToCouple(menage, marie, dateMariage, null);
		addForFiscalPrincipal(menage, communeMariage, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
	}

	@Check(id=1, descr="Vérifie les tiers, rapports ménages et les fors")
	public void check1() throws Exception {

		final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
		{
			final ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertEquals(dateArriveePierre, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture(), "Motif d'ouverture du dernier for faux");
			assertEquals(avantDateMariage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifFermeture(), "Motif de fermeture du dernier for faux");
		}

		final PersonnePhysique marie = (PersonnePhysique) tiersDAO.get(noNonHabMarie);
		{
			final ForFiscalPrincipal ffp = marie.getDernierForFiscalPrincipal();
			assertNull(ffp, "Marie ne doit pas avoir de for à elle");
			assertNull(marie.getDateDeces(), "Marie n'est pas encore morte...");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, dateMariage);
			assertEquals(noNonHabMarie, etc.getConjoint(pierre).getNumero(), "Pierre n'est pas marié avec Marie");
			assertEquals(1, mc.getForsFiscauxNonAnnules(false).size(), "Le ménage a plus d'un for principal");

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true, true);
	}

	@Etape(id=2, descr="Envoi de l'événement Veuvage")
	public void step2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="L'événement de veuvage doit être traité correctement même si Pierre n'est pas marié seul au fiscal (il l'est au civil)")
	public void check2() {

		final List<EvenementCivilData> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.VEUVAGE);
		assertNotNull(evts, "Pas d'événement trouvé!");
		assertEquals(1, evts.size(), "Il devrait y avoir un et un seul événement civil de veuvage sur Pierre");

		final EvenementCivilData evt = evts.get(0);
		assertNotNull(evt, "Evenement null?");
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil n'a pas été traité!");

		// vérification des for après veuvage

		final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(noHabPierre);
		{
			final ForFiscalPrincipal ffp = pierre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + pierre.getNumero() + " null");
			assertEquals(dateVeuvage.addDays(1), ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture(), "Motif d'ouverture du dernier for faux");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		final PersonnePhysique marie = (PersonnePhysique) tiersDAO.get(noNonHabMarie);
		{
			final ForFiscalPrincipal ffp = marie.getDernierForFiscalPrincipal();
			assertNull(ffp, "Marie ne doit pas avoir de for à elle");
			assertEquals(dateVeuvage, marie.getDateDeces(), "La date de décès de Marie n'a pas été mise à jour correctement");
		}

		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(mc, dateVeuvage.addDays(1));
			assertNull(etc.getConjoint(pierre), "Pierre est toujours marié avec Marie après son décès?");
			assertEquals(1, mc.getForsFiscaux().size(), "Le ménage a plus d'un for principal");

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture(), "Motif d'ouverture du dernier for faux");
			assertEquals(dateVeuvage, ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture(), "Motif d'ouverture du dernier for faux");
			assertEquals(communeMariage.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le dernier for n'est pas sur " + communeMariage.getNomMinuscule());
		}
	}

	@Etape(id=3, descr="Envoi de l'événement d'annulation de veuvage")
	public void step3() throws Exception {

		final EtatCivilList ecs = indPierre.getEtatsCivils();
		assertNotNull(ecs, "Pas d'états civils du tout?");

		final EtatCivil ecVeuf = ecs.getEtatCivilAt(dateVeuvage);
		assertNotNull(ecVeuf, "Pas d'état civil à la date de veuvage?");
		assertEquals(EnumTypeEtatCivil.VEUF, ecVeuf.getTypeEtatCivil(), "Devrait être veuf");
		ecs.remove(ecVeuf);

		final EtatCivil ecMarie = ecs.getEtatCivilAt(dateVeuvage);
		assertNotNull(ecMarie, "Pas d'état civil à la date de veuvage?");
		assertEquals(EnumTypeEtatCivil.MARIE, ecMarie.getTypeEtatCivil(), "Devrait être marié, puisque le veuvage est annulé");

		final long id = addEvenementCivil(TypeEvenementCivil.ANNUL_VEUVAGE, noIndPierre, dateVeuvage, communeMariage.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérification après annulation de veuvage : le conjoint doit être rescussité et les fors rétablis")
	public void check3() throws Exception {

		final List<EvenementCivilData> evts = getEvenementsCivils(noIndPierre, TypeEvenementCivil.ANNUL_VEUVAGE);
		assertNotNull(evts, "Pas d'événement trouvé!");
		assertEquals(1, evts.size(), "Il devrait y avoir un et un seul événement civil d'annulation de veuvage sur Pierre");

		final EvenementCivilData evt = evts.get(0);
		assertNotNull(evt, "Evenement null?");
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil n'a pas été traité!");

		// vérification des for après annulation de veuvage
		check1();
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduPierre, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduPierre, tiersDAO.get(noHabPierre));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}

}