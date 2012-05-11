package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_07_Arrivee_Commune_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_07_Arrivee_Commune";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_DANS_COMMUNE;
	}

	@Override
	public String getDescription() {
		return "Arrivée d'un individu provenant d'une autre commune vaudoise.";
	}

	private static final long noIndAdrienne = 203674;

	private MockIndividu indAdrienne;

	private long noHabAdrienne;

	private final MockCommune communeDepart = MockCommune.Orbe;
	private final MockCommune communeArrivee = MockCommune.Lausanne;
	protected static final RegDate dateNaissance = RegDate.get(1935, 3, 23);
	protected static final RegDate dateDemenagement = RegDate.get(2009, 2, 28);
	protected static final RegDate dateArriveeLausanne = dateDemenagement.getOneDayAfter();

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAdrienne = addIndividu(noIndAdrienne, dateNaissance , "Pittet", "Adrienne", false);
				setNationalite(indAdrienne, dateNaissance, null, MockPays.Suisse);
				addAdresse(indAdrienne, TypeAdresseCivil.PRINCIPALE, MockRue.Orbe.RueDuMoulinet, null,
						null, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant à Orbe")
	public void step1() throws Exception {
		PersonnePhysique adrienne = addHabitant(noIndAdrienne);
		noHabAdrienne = adrienne.getNumero();
		addForFiscalPrincipal(adrienne, communeDepart, RegDate.get(2003, 1, 6), null, MotifFor.VEUVAGE_DECES, null);
	}

	@Check(id=1, descr="Vérifie qu'Adrienne a son adresse et son for à Orbe")
	public void check1() throws Exception {
		PersonnePhysique adrienne = tiersDAO.getHabitantByNumeroIndividu(noIndAdrienne);
		assertNotNull(adrienne, "Adrienne devrait être dans le registre");
		ForFiscalPrincipal ffp = adrienne.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + adrienne.getNumero() + " null");
		assertNull(ffp.getDateFin(), "Date de fin du dernier for d'Adrienne fausse");
		assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Le for principal n'est pas sur " + communeDepart.getNomMinuscule());
		// vérification que les adresses civiles sont sur Orbe
		assertEquals(communeDepart.getNomMinuscule(), serviceCivilService.getAdresses(noIndAdrienne, null, false).principale.getLocalite(),
			"L'adresse principale n'est pas sur " + communeDepart.getNomMinuscule());
	}

	private void addNouvelleAdresse(MockIndividu individu) {
		Collection<Adresse> adrs = individu.getAdresses();
		MockAdresse lastAdr = null;
		for (Adresse a : adrs) {
			lastAdr = (MockAdresse)a;
		}
		lastAdr.setDateFinValidite(dateDemenagement);

		Adresse adresse = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
				dateArriveeLausanne, null);
		adrs.add(adresse);
	}

	@Etape(id=2, descr="Déménagement de Adrienne")
	public void step2() throws Exception {
		addNouvelleAdresse(indAdrienne);
	}

	@Check(id=2, descr="Vérifie que l'habitant Adrienne a toujours son For à Orbe mais son adresse à Lausanne")
	public void check2() throws Exception {
		PersonnePhysique adrienne = (PersonnePhysique) tiersDAO.get(noHabAdrienne);
		ForFiscalPrincipal ffp = adrienne.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant " + adrienne.getNumero() + " null");
		assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
		assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "le for principal n'est pas sur " + communeDepart.getNomMinuscule());
		assertEquals(communeArrivee.getNomMinuscule(), serviceCivilService.getAdresses(noIndAdrienne, null, false).principale.getLocalite(),
				"L'adresse principale n'est pas sur " + communeArrivee.getNomMinuscule());
	}

	@Etape(id=3, descr="Envoi de l'événement d'arrivée d'Adrienne")
	public void step3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, noIndAdrienne, dateDemenagement.addDays(1), communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que l'evenement d'arrivée est au statut à verifier et qu'Antoine a un for ouvert sur ")
	public void check3() throws Exception {
		{
			EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabAdrienne);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");
		}
		{
			PersonnePhysique habitant = (PersonnePhysique) tiersDAO.get(noHabAdrienne);
			// For fermé sur Orbe
			ForFiscalPrincipal ffpFerme = habitant.getForFiscalPrincipalAt(dateDemenagement.getOneDayBefore());
			assertEquals(dateDemenagement, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
			assertEquals(communeDepart.getNoOFS(), ffpFerme.getNumeroOfsAutoriteFiscale(), "le for précédent n'est pas sur " + communeDepart.getNomMinuscule());

			// For ouvert sur Lausanne
			ForFiscalPrincipal ffpOuvert = habitant.getForFiscalPrincipalAt(dateArriveeLausanne);
			assertEquals(dateDemenagement.addDays(1) , ffpOuvert.getDateDebut(), "Le for sur " + communeArrivee.getNomMinuscule() + " n'est pas ouvert à la bonne date");
			assertEquals(communeArrivee.getNoOFS(), ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur " + communeArrivee.getNomMinuscule());
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le ModeImposition du for est faux");
			assertEquals(MotifFor.DEMENAGEMENT_VD, ffpOuvert.getMotifOuverture(), "Le motif d'ouverture du for n'est pas " + MotifFor.DEMENAGEMENT_VD.name());
		}
	}
}
