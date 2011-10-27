package ch.vd.uniregctb.norentes.civil.depart;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_19000_11_Depart_JIRA771_Scenario extends DepartScenario {

	private AdresseService adresseService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}
	
	public static final String NAME = "19000_11_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {

		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {

		return "Départ hors canton du contribuable principal d'un couple";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndSebastien = 844770;
	private final long noIndGloria = 646404;

	private MockIndividu indSebastien;
	private MockIndividu indGloria;

	private long noHabSebastien;
	private long noHabGloria;
	private long noMenage;

	private final RegDate dateMariage = RegDate.get(2005, 6, 29);
	private final RegDate dateDepart = RegDate.get(2008, 6, 30);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.VillarsSousYens;
	private final MockCommune communeArrivee = MockCommune.Zurich;
	
	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			
			@Override
			protected void init() {

				final RegDate dateAmenagement = RegDate.get(1971, 6, 27);
				
				final RegDate dateNaissanceSebastien = RegDate.get(1971, 6, 27);
				indSebastien = addIndividu(noIndSebastien, dateNaissanceSebastien, "Fournier", "Sebastien", true);
				addOrigine(indSebastien, MockCommune.Neuchatel);
				addNationalite(indSebastien, MockPays.Suisse, dateNaissanceSebastien, null);
				addAdresse(indSebastien, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null, dateAmenagement, null);
				addAdresse(indSebastien, TypeAdresseCivil.COURRIER, MockRue.VillarsSousYens.CheminDuCollege, null, dateAmenagement, null);

				final RegDate dateNaissanceGloria = RegDate.get(1973, 3, 7);
				indGloria = addIndividu(noIndGloria, dateNaissanceGloria, "Fournier", "Gloria", false);
				addOrigine(indGloria, MockCommune.Neuchatel);
				addNationalite(indGloria, MockPays.Suisse, dateNaissanceGloria, null);
				addAdresse(indGloria, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null, dateAmenagement, null);
				addAdresse(indGloria, TypeAdresseCivil.COURRIER, MockRue.VillarsSousYens.CheminDuCollege, null, dateAmenagement, null);
				
				marieIndividus(indSebastien, indGloria, dateMariage);
			}
			
		});
	}

	@Etape(id=1, descr="Chargement du couple et son for")
	public void etape1() throws Exception {

		final PersonnePhysique sebastien = addHabitant(noIndSebastien);
		{
			noHabSebastien = sebastien.getNumero();
		}
		
		final PersonnePhysique gloria = addHabitant(noIndGloria);
		{
			noHabGloria = gloria.getNumero();
		}
		
		final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
		{
			noMenage = menage.getNumero();

			tiersService.addTiersToCouple(menage, sebastien, dateMariage, null);
			tiersService.addTiersToCouple(menage, gloria, dateMariage, null);
			
			addForFiscalPrincipal(menage, communeDepart, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			menage.setBlocageRemboursementAutomatique(false);
		}
	}

	@Check(id=1, descr="Vérifie que le couple a son adresse et son For à Villars-sous-Yens")
	public void check1() throws Exception {

		{
			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
			assertNotNull(sebastien, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " non existant");
			final ForFiscalPrincipal ffp = sebastien.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + sebastien.getNumero() + " non null");
		}
		
		{
			final PersonnePhysique gloria = (PersonnePhysique) tiersDAO.get(noHabGloria);
			assertNotNull(gloria, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabGloria) + " non existant");
			final ForFiscalPrincipal ffp = gloria.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + gloria.getNumero() + " non null");
		}
		
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du ménage est null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for du ménage fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont à Villars-sous-Yens
			assertEquals(communeDepart.getNomMinuscule(), 
					serviceCivilService.getAdresses(noIndSebastien, RegDate.get(), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + communeDepart.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}

	@Etape(id=2, descr="Départ hors canton du contribuable principal du couple")
	public void etape2() throws Exception {
		fermerAdresses(indSebastien, dateDepart);
		ouvrirAdresseZurich(indSebastien, dateArrivee);
	}

	@Check(id=2, descr="Vérifie que l'habitant a toujours son For à Villars-sous-Yens mais l'adresse hors canton")
	public void check2() throws Exception {

		{
			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
			assertNotNull(sebastien, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " non existant");
			final ForFiscalPrincipal ffp = sebastien.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + sebastien.getNumero() + " non null");

		}
		
		{
			final PersonnePhysique gloria = (PersonnePhysique) tiersDAO.get(noHabGloria);
			assertNotNull(gloria, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabGloria) + " non existant");
			final ForFiscalPrincipal ffp = gloria.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + gloria.getNumero() + " non null");
		}
		
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
			final ForFiscalPrincipal ffp = menage.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for de Sebastien fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont à Zurich
			assertEquals(communeArrivee.getNomMinuscule(), 
					serviceCivilService.getAdresses(noIndSebastien, dateDepart.addDays(1), false).principale.getLocalite(),
					"L'adresse principale n'est pas à " + communeArrivee.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(true, true, false);
	}


	@Etape(id=3, descr="Envoi de l'événement de départ")
	public void etape3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndSebastien, dateDepart, communeDepart.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que le couple a toujours son for sur Villars-sous-Yens")
	public void check3() throws Exception {
		
		final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
		assertFalse(sebastien.isHabitantVD(), "Le contribuable" + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " aurait dû devenir non-habitant");
		assertAdressesFiscales(sebastien, communeArrivee);
		
		final PersonnePhysique gloria = (PersonnePhysique) tiersDAO.get(noHabGloria);
		assertTrue(gloria.isHabitantVD(), "Le contribuable " + FormatNumeroHelper.numeroCTBToDisplay(noHabGloria) + " devrait toujours être habitant");
		assertAdressesFiscales(gloria, communeDepart);
		
		final MenageCommun menageCommun = (MenageCommun) tiersDAO.get(noMenage);
		final ForFiscalPrincipal ffp = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull(ffp, "Le couple n'est plus asujetti");
		assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for n'est pas sur la bonne commune");
		
		assertBlocageRemboursementAutomatique(true, true, false);
	}

	private void assertAdressesFiscales(Tiers tiers, MockCommune commune) throws AdresseException {
		final AdressesFiscales adresses = adresseService.getAdressesFiscales(tiers, null, false);
		
		String messageHeader = "CTB [" + FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()) + "] : ";
		assertNotNull(adresses.domicile, "Aucune adresse domicile trouvée");
		assertEquals(adresses.domicile.getLocalite(), commune.getNomMinuscule(), messageHeader + "l'adresse domicile n'est pas à la bonne commune");
		
		assertNotNull(adresses.courrier, "Aucune adresse courier trouvée");
		assertEquals(adresses.courrier.getLocalite(), commune.getNomMinuscule(), messageHeader + "l'adresse courier n'est pas à la bonne commune");
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttenduSebastien, boolean blocageAttenduGloria, boolean blocageAttenduMenage) {
		assertBlocageRemboursementAutomatique(blocageAttenduSebastien, tiersDAO.get(noHabSebastien));
		assertBlocageRemboursementAutomatique(blocageAttenduGloria, tiersDAO.get(noHabGloria));
		assertBlocageRemboursementAutomatique(blocageAttenduMenage, tiersDAO.get(noMenage));
	}
}
