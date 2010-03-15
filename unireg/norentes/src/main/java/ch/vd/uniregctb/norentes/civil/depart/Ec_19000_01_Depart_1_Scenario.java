package ch.vd.uniregctb.norentes.civil.depart;

import java.util.Collection;
import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_19000_01_Depart_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "19000_01_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {

		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {

		return "Départ d'un individu seul Pour zurich avec residence principal à Bex";
	}

	@Override
	public String getName() {

		return NAME;
	}

	private final long noIndAlain = 122456L;

	private MockIndividu indAlain;

	private long noHabAlain;




	private final int communeDepartBex = MockCommune.Bex.getNoOFS();
	private final RegDate dateArriveeBex = RegDate.get(1982, 7, 5);
	private final RegDate dateDepartBex = RegDate.get(2006, 4, 11);

	private final int NouvelleCommuneZurich = MockCommune.Zurich.getNoOFS();
	private final RegDate dateArriveeZurich = dateDepartBex.addDays(1);




	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Gregoire", "Alain", true);

				addAdresse(indAlain, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateArriveeBex, null);


			}
		});
	}


	@Etape(id=1, descr="Chargement d'un habitant à Bex")
	public void etape1() throws Exception {

		final PersonnePhysique alain = addHabitant(noIndAlain);
		noHabAlain = alain.getNumero();
		addForFiscalPrincipal(alain, MockCommune.Bex.getNoOFS(), dateArriveeBex, null, MotifFor.ARRIVEE_HC, null);
	}

	@Check(id=1, descr="Vérifie que l'habitant Alain a son adresse à Bex et son For à Bex")
	public void check1() throws Exception {

		{
			final PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			final ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertNull( ffp.getDateFin(), "Date de fin du dernier for d'Alain fausse");
		}

		// vérification que les adresses civiles sont a Bex
		assertEquals(MockCommune.Bex.getNomMinuscule(), serviceCivilService.getAdresses(noIndAlain, dateArriveeBex, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Bex");

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true);
	}

	@Etape(id=2, descr="Départ de Alain vers Zurich")
	public void etape2() throws Exception {
		addAdresseZurich(indAlain);
	}



	@Check(id=2, descr="Vérifie que l'habitant Alain a toujours son For à Bex mais son adresse a zurich")
	public void check2() throws Exception {

		{
			final PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
			final ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
			assertNull( ffp.getDateFin(), "Date de fin du dernier for fausse");
		}

		// vérification que les adresses civiles sont a Zurich
		assertEquals(MockCommune.Zurich.getNomMinuscule(), serviceCivilService.getAdresses(noIndAlain, dateArriveeZurich, false).principale.getLocalite(),
			"l'adresse principale n'est pas à Zurich");

		// PBM 29.07.2009: UNIREG-1266 -> Blocage des remboursements automatiques sur tous les nouveaux tiers
		assertBlocageRemboursementAutomatique(true);
	}


	@Etape(id=3, descr="Envoi de l'événement de départ de l'individu Alain")
	public void etape3() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndAlain, dateDepartBex, communeDepartBex);

		commitAndStartTransaction();

		// On traite les evenements
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que l'habitant Alain n'a plus son for sur Bex mais sur Zurich")
	public void check3() throws Exception {


		// On check que Alain  est parti
		{
			final EvenementCivilRegroupe evt = getEvenementCivilRegoupeForHabitant(noHabAlain);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

			final PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabAlain);
			final List<ForFiscal> list = hab.getForsFiscauxSorted();

			// For fermé sur Bex
			final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal)list.get(list.size()-2);
			assertEquals(dateDepartBex, ffpFerme.getDateFin(), "Le for sur Bex n'est pas fermé à la bonne date");

			// For ouvert sur Zurich
			final ForFiscalPrincipal ffpOuvert = (ForFiscalPrincipal)list.get(list.size()-1);
			assertEquals(dateArriveeZurich, ffpOuvert.getDateDebut(), "Le for sur zurich n'est pas ouvert à la bonne date");
			assertEquals(new Integer(NouvelleCommuneZurich), ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur Zurich");
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le ModeImposition du for est faux");
		}

		assertBlocageRemboursementAutomatique(true);
	}

	private void addAdresseZurich(MockIndividu ind) {
		Collection<Adresse> adrs = ind.getAdresses();
		MockAdresse last = null;
		for (Adresse a : adrs) {
			last = (MockAdresse)a;
		}
		last.setDateFinValidite(dateDepartBex);
		Adresse aa = MockServiceCivil.newAdresse(EnumTypeAdresse.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateArriveeZurich, null);
		adrs.add(aa);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabAlain));
	}
}
