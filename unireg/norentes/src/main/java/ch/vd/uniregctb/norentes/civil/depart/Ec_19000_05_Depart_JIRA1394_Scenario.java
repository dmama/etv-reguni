package ch.vd.uniregctb.norentes.civil.depart;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeEvenementCivil;

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
	
	private final long noIndSven = 459371;
	private final long noIndElly = 459372;

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
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {
				
				final RegDate dateNaissanceSven = RegDate.get(1925, 4, 14);
				final RegDate dateAmenagement = RegDate.get(2001, 3, 12);
				
				indSven = addIndividu(noIndSven, dateNaissanceSven, "Brise", "Sven", true);
				addOrigine(indSven, MockPays.Danemark, null, dateNaissanceSven);
				addNationalite(indSven, MockPays.Danemark, dateNaissanceSven, null, 1);
				addAdresse(indSven, EnumTypeAdresse.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, MockLocalite.Vallorbe, dateAmenagement, null);
				addAdresse(indSven, EnumTypeAdresse.COURRIER, MockRue.Vallorbe.GrandRue, null, MockLocalite.Vallorbe, dateAmenagement, null);
				
				final RegDate dateNaissanceElly = RegDate.get(1973, 3, 7);
				indElly = addIndividu(noIndElly, dateNaissanceElly, "Brise", "Elly", false);
				addOrigine(indElly, MockPays.Danemark, null, dateNaissanceElly);
				addNationalite(indElly, MockPays.Danemark, dateNaissanceElly, null, 1);
				addAdresse(indElly, EnumTypeAdresse.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, MockLocalite.Vallorbe, dateAmenagement, null);
				addAdresse(indElly, EnumTypeAdresse.COURRIER, MockRue.Vallorbe.GrandRue, null, MockLocalite.Vallorbe, dateAmenagement, null);
				
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
			
			addForFiscalPrincipal(menage, commune.getNoOFS(), dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
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
			assertEquals(commune.getNomMinuscule(), 
					serviceCivilService.getAdresses(noIndSven, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + commune.getNomMinuscule());
		}
		
		{
			final PersonnePhysique elly = (PersonnePhysique) tiersDAO.get(noHabElly);
			assertNotNull(elly, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabElly) + " non existant");
			final ForFiscalPrincipal ffp = elly.getForFiscalPrincipalAt(null);
			assertNull(ffp, "For principal de l'Habitant " + elly.getNumero() + " non null");

			// vérification que les adresses civiles sont à Vallorbe
			assertEquals(commune.getNomMinuscule(), 
					serviceCivilService.getAdresses(noIndElly, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + commune.getNomMinuscule());
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
					"L'adresse principale n'est pas à " + nouvelleCommune.getNomMinuscule());
		}
		
		{
			// vérification que les adresses civiles sont HS
			assertEquals(paysDepart.getNoOFS(),
					serviceCivilService.getAdresses(noIndElly, dateDepart.addDays(1), false).principale.getNoOfsPays(),
					"L'adresse principale n'est pas à " + nouvelleCommune.getNomMinuscule());
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
		regroupeEtTraiteEvenements(id);
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
