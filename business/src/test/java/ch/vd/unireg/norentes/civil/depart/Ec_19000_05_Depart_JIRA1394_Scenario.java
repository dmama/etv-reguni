package ch.vd.unireg.norentes.civil.depart;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_19000_05_Depart_JIRA1394_Scenario extends DepartScenario {

	public static final String NAME = "19000_05_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {
		
		return "Départ hors Suisse d'un couple propriétaire d'immeuble";
	}

	@Override
	public String getName() {
		return NAME;
	}
	
	private static final long noIndSven = 459371;
	private static final long noIndElly = 459372;

	private MockIndividu indSven;
	private MockIndividu indElly;

	private long noHabSven;
	private long noHabElly;
	private long noMenage;

	private final RegDate dateMariage = RegDate.get(1986, 10, 27);
	private final RegDate dateAchatImmeuble = RegDate.get(1986, 10, 31);
	private final RegDate dateDepart = RegDate.get(2008, 6, 30);
	private final MockCommune commune = MockCommune.Vallorbe;
	private final MockCommune nouvelleCommune = MockCommune.Zurich;
	private final MockPays paysDepart = MockPays.Danemark;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {

			@Override
			protected void init() {
				
				final RegDate dateNaissanceSven = RegDate.get(1925, 4, 14);
				final RegDate dateAmenagement = RegDate.get(2001, 3, 12);
				
				indSven = addIndividu(noIndSven, dateNaissanceSven, "Brise", "Sven", true);
				addNationalite(indSven, MockPays.Danemark, dateNaissanceSven, null);
				addAdresse(indSven, TypeAdresseCivil.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, dateAmenagement, null);
				addAdresse(indSven, TypeAdresseCivil.COURRIER, MockRue.Vallorbe.GrandRue, null, dateAmenagement, null);
				
				final RegDate dateNaissanceElly = RegDate.get(1973, 3, 7);
				indElly = addIndividu(noIndElly, dateNaissanceElly, "Brise", "Elly", false);
				addNationalite(indElly, MockPays.Danemark, dateNaissanceElly, null);
				addAdresse(indElly, TypeAdresseCivil.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, dateAmenagement, null);
				addAdresse(indElly, TypeAdresseCivil.COURRIER, MockRue.Vallorbe.GrandRue, null, dateAmenagement, null);
				
				marieIndividus(indSven, indElly, dateMariage);
			}
			
		});
	}
	
	@Etape(id=1, descr="Chargement du couple et ses fors")
	public void step1() {
		
		final PersonnePhysique sven = addHabitant(noIndSven);
		{
			noHabSven = sven.getNumero();
		}
		
		final PersonnePhysique elly = addHabitant(noIndElly);
		{
			noHabElly = elly.getNumero();
		}
		
		final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		{
			noMenage = menage.getNumero();

			tiersService.addTiersToCouple(menage, sven, dateMariage, null);
			tiersService.addTiersToCouple(menage, elly, dateMariage, null);
			
			addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			addForFiscalSecondaire(menage, commune.getNoOFS(), dateAchatImmeuble, null);
		}
	}
	
	@Check(id=1, descr="Vérifie que le couple a son adresse et ses Fors à Vallorbe")
	public void check1() throws Exception {

		{
			final PersonnePhysique sven = (PersonnePhysique) tiersDAO.get(noHabSven);
			assertNotNull(sven, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSven) + " non existant");
			final ForFiscalPrincipal ffp = sven.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + sven.getNumero() + " non null");

			// vérification que les adresses civiles sont à Vallorbe
			assertEquals(commune.getNomOfficiel(),
					serviceCivilService.getAdresses(noIndSven, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + commune.getNomOfficiel());
		}
		
		{
			final PersonnePhysique elly = (PersonnePhysique) tiersDAO.get(noHabElly);
			assertNotNull(elly, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabElly) + " non existant");
			final ForFiscalPrincipal ffp = elly.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + elly.getNumero() + " non null");

			// vérification que les adresses civiles sont à Vallorbe
			assertEquals(commune.getNomOfficiel(),
					serviceCivilService.getAdresses(noIndElly, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + commune.getNomOfficiel());
		}
		
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			assertNotNull(menage, "Ménage commun " + FormatNumeroHelper.numeroCTBToDisplay(noHabElly) + " non existant");
			assertEquals(2, menage.getForsFiscauxNonAnnules(false).size(), "Nombre de fors incorrect");
			
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage est null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for du ménage fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");
		}
		
	}
	
	@Etape(id=2, descr="Départ du couple hors Suisse")
	public void etape2() throws Exception {
		fermerAdresses(indSven, dateDepart);
		fermerAdresses(indElly, dateDepart);
		
		ouvrirAdresseEtranger(indSven);
		ouvrirAdresseEtranger(indElly);
	}

	private void ouvrirAdresseEtranger(MockIndividu individu) {
		ouvrirAdresseEtranger(individu, dateDepart.getOneDayAfter(), paysDepart);
	}

	@Check(id=2, descr="Vérifie que le couple a toujours son for à Vallorbe mais l'adresse hors canton")
	public void check2() throws Exception {

		{
			// vérification que les adresses civiles sont HS
			assertEquals(paysDepart.getNoOFS(),
					serviceCivilService.getAdresses(noIndSven, dateDepart.addDays(1), false).principale.getNoOfsPays(),
					"L'adresse principale n'est pas à " + nouvelleCommune.getNomOfficiel());
		}
		
		{
			// vérification que les adresses civiles sont HS
			assertEquals(paysDepart.getNoOFS(),
					serviceCivilService.getAdresses(noIndElly, dateDepart.addDays(1), false).principale.getNoOfsPays(),
					"L'adresse principale n'est pas à " + nouvelleCommune.getNomOfficiel());
		}
		
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage est null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for du ménage fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");
		}
	}
	
	@Etape(id=3, descr="Envoi de l'événement de départ HS")
	public void step3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndSven, dateDepart, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}
	
	@Check(id=3, descr="Vérifie qu'aucune tâche n'a été générée")
	public void check3() {

		final PersonnePhysique sven = tiersDAO.getPPByNumeroIndividu(noIndSven);
		
		// vérification qu'aucune tâche n'est actuellement en instance
		final TacheCriteria tacheCriteria = new TacheCriteria();
		tacheCriteria.setContribuable(sven);
		tacheCriteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		final List<Tache> taches = tacheDAO.find(tacheCriteria);
		assertNotNull(taches, "Liste des tâches en instance nulle");
		assertEquals(0, taches.size(), "Il y a déjà des tâches en instance");
	}
}
