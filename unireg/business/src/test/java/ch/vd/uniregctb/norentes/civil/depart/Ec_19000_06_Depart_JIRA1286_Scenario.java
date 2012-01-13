package ch.vd.uniregctb.norentes.civil.depart;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class Ec_19000_06_Depart_JIRA1286_Scenario extends DepartScenario {

	public static final String NAME = "19000_06_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {
		return "Départ à l'étranger d'un habitant mineur (UNIREG-1286)";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndIan = 874791;

	private MockIndividu indIan;
	
	private long noTiersIan;
	
	private final RegDate dateNaissance = RegDate.get(2004, 4, 21);
	private final RegDate dateDepart = RegDate.get(2009, 6, 15);
	private final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.Lausanne;
	private final MockPays paysArrivee = MockPays.France;
	
	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			
			@Override
			protected void init() {
				
				indIan = addIndividu(noIndIan, dateNaissance, "Schuster", "Ian", true);
				addOrigine(indIan, MockCommune.Neuchatel);
				addNationalite(indIan, MockPays.Espagne, dateNaissance, null);
				setPermis(indIan, TypePermis.ANNUEL, RegDate.get(2006, 7, 9), null, false);
				
				RegDate dateAmenagement = RegDate.get(2006, 7, 9);
				addAdresse(indIan, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateAmenagement, null);
				addAdresse(indIan, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null, dateAmenagement, null);
				
			}
		});
	}
	
	@Etape(id=1, descr="Chargement de l'habitant")
	public void step1() {
		
		final PersonnePhysique ian = addHabitant(noIndIan);
		noTiersIan = ian.getNumero();
		
	}
	
	@Check(id=1, descr="Vérifie que l'habitant a son adresse à Lausanne et ne possède aucun for fiscal")
	public void check1() throws Exception {

		final PersonnePhysique ian = (PersonnePhysique) tiersDAO.get(noTiersIan);
		assertNotNull(ian, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noTiersIan) + " non existant");
		final ForFiscalPrincipal ffp = ian.getDernierForFiscalPrincipal();
		assertNull(ffp, "For principal de l'Habitant " + noTiersIan + " non null");
		
		// vérification que les adresses civiles sont à Lausanne
		assertEquals(communeDepart.getNomMinuscule(), 
				serviceCivilService.getAdresses(noIndIan, RegDate.get(), false).principale.getLocalite(),
				"L'adresse principale n'est pas à " + communeDepart.getNomMinuscule());
		
	}
	
	@Etape(id=2, descr="Départ de l'habitant à l'étranger")
	public void step2() {
		fermerAdresses(indIan, dateDepart);
		ouvrirAdresseEtranger(indIan, dateArrivee, paysArrivee);
	}

	@Check(id=2, descr="Vérifie que l'habitant a son adresse à l'étranger")
	public void check2() throws Exception {

		final PersonnePhysique ian = (PersonnePhysique) tiersDAO.get(noTiersIan);
		assertNotNull(ian, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noTiersIan) + " non existant");
		final ForFiscalPrincipal ffp = ian.getDernierForFiscalPrincipal();
		assertNull(ffp, "For principal de l'Habitant " + noTiersIan + " non null");

		// vérification que les adresses civiles sont à Zurich
		assertEquals(paysArrivee.getNoOFS(), 
				serviceCivilService.getAdresses(noIndIan, dateDepart.addDays(1), false).principale.getNoOfsPays(),
				"L'adresse principale n'est pas à " + paysArrivee.getNomMinuscule());
	}
	
	@Etape(id=3, descr="Envoi de l'événement de départ")
	public void etape3() throws Exception {
		
		long id = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndIan, dateDepart, communeDepart.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
		
	}

	@Check(id=3, descr="Vérifie que l'habitant n'a pas de for ouvert car il est mineur")
	public void check3() throws Exception {
		
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noTiersIan);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement civil devrait être traité");
		
		final PersonnePhysique ian = (PersonnePhysique) tiersDAO.get(noTiersIan);
		final ForFiscalPrincipal ffp = ian.getDernierForFiscalPrincipal();
		assertNull(ffp, "For principal de l'Habitant " + noTiersIan + " aurait pas dû exister car l'habitant est mineur");
		
	}
	
}
