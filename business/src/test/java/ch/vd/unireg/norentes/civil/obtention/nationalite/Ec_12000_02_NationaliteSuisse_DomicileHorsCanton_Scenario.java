package ch.vd.unireg.norentes.civil.obtention.nationalite;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.fiscal.CollectingEvenementFiscalSender;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

/**
 * Scénario d'un événement obtention de nationalité suisse d'un individu non-habitant dont l'adresse de domicile est hors-canton
 */
public class Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Scenario extends EvenementCivilScenario {

	public static final String NAME = "12000_02_NationaliteSuisse";

	private CollectingEvenementFiscalSender evenementFiscalSender;
	private AdresseService adresseService;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Obtention de nationalité suisse d'un individu domicilié hors canton";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.NATIONALITE_SUISSE;
	}

	public void setEvenementFiscalSender(CollectingEvenementFiscalSender evenementFiscalSender) {
		this.evenementFiscalSender = evenementFiscalSender;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	private static final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateObtentionPermis = RegDate.get(2003, 5, 9);
	private final RegDate dateObtentionNationalite = RegDate.get(2008, 11, 19);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				super.init();

				indJulie = getIndividu(noIndJulie);
				addNationalite(indJulie, MockPays.France, dateNaissance, null);
				addNationalite(indJulie, MockPays.Suisse, dateObtentionNationalite, null);
				addPermis(indJulie, TypePermis.SEJOUR, dateObtentionPermis, dateObtentionNationalite.getOneDayBefore(), false);
				addEtatCivil(indJulie, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addAdresse(indJulie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateObtentionPermis, null);
				addAdresse(indJulie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, new CasePostale(TexteCasePostale.CASE_POSTALE, 2133431), dateObtentionPermis, null);
			}
		});
		evenementFiscalSender.reset();
	}

	@Etape(id=1, descr="Chargement du non-habitant connu au civil")
	public void etape1() {
		final PersonnePhysique julie = tiersService.createNonHabitantFromIndividu(noIndJulie);
		noHabJulie = julie.getNumero();
	}

	@Check(id=1, descr="Vérifie que Julie est bien non-habitante sans for avec adresse de domicile hors-canton")
	public void check1() throws Exception {
		final PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		final Set<ForFiscal> ff = julie.getForsFiscaux();
		assertNotNull(ff, "Même sans for fiscaux, la collection devrait exister, non?");
		assertEquals(0, ff.size(), "Il ne devrait pas y avoir de for fiscal");

		final AdresseGenerique adresseDomicile = adresseService.getAdresseFiscale(julie, TypeAdresseFiscale.DOMICILE, null, false);
		assertEquals(MockCommune.Neuchatel.getNoOFS(), adresseDomicile.getNoOfsCommuneAdresse(), "L'adresse de domicile devrait être à Neuchâtel");
	}

	@Etape(id=2, descr="Envoi de l'événement Obtention de Nationalité Suisse")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.NATIONALITE_SUISSE, noIndJulie, dateObtentionNationalite, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux & que l'événement civil est traité")
	public void check2() {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabJulie);

		assertEquals(0, evenementFiscalSender.getCount(), "Aucun événement fiscal ne doit avoir été envoyé");

		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement devrait être traité");
		assertEquals(0, evt.getErreurs().size(), "Il ne devrait y avoir aucune erreur");

		final PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		final Set<ForFiscal> ff = julie.getForsFiscaux();
		assertNotNull(ff, "Même sans for fiscaux, la collection devrait exister, non?");
		assertEquals(0, ff.size(), "Il ne devrait pas y avoir de for fiscal (domicile hors-canton)");
	}
}
