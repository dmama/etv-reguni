package ch.vd.uniregctb.norentes.civil.obtention.nationalite;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalSender;
import ch.vd.uniregctb.evenement.fiscal.MockEvenementFiscalSender;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Scénario d'un événement obtention de nationalité suisse d'un individu non-habitant dont l'adresse de domicile est hors-canton
 */
public class Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Scenario extends EvenementCivilScenario {

	public static final String NAME = "12000_02_NationaliteSuisse";

	private EvenementFiscalSender evenementFiscalSender;
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

	public void setEvenementFiscalSender(EvenementFiscalSender evenementFiscalSender) {
		this.evenementFiscalSender = evenementFiscalSender;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	private final long noIndJulie = 6789; // julie

	private MockIndividu indJulie;

	private long noHabJulie;

	private final RegDate dateNaissance = RegDate.get(1977, 4, 19);
	private final RegDate dateObtentionPermis = RegDate.get(2003, 5, 9);
	private final RegDate dateObtentionNationalite = RegDate.get(2008, 11, 19);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				super.init();

				indJulie = getIndividu(noIndJulie);
				addOrigine(indJulie, MockPays.France.getNomMinuscule());
				addNationalite(indJulie, MockPays.France, dateNaissance, null);
				addNationalite(indJulie, MockPays.Suisse, dateObtentionNationalite, null);
				addPermis(indJulie, TypePermis.ANNUEL, dateObtentionPermis, dateObtentionNationalite.getOneDayBefore(), false);
				addEtatCivil(indJulie, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addAdresse(indJulie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateObtentionPermis, null);
				addAdresse(indJulie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.PlaceSaintFrancois, new CasePostale(TexteCasePostale.CASE_POSTALE, 2133431), dateObtentionPermis, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement du non-habitant connu au civil")
	public void etape1() {
		final PersonnePhysique julie = addHabitant(noIndJulie);
		tiersService.changeHabitantenNH(julie);
		noHabJulie = julie.getNumero();
	}

	@Check(id=1, descr="Vérifie que Julie est bien non-habitante sans for avec adresse de domicile hors-canton")
	public void check1() throws Exception {
		final PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		final Set<ForFiscal> ff = julie.getForsFiscaux();
		assertNotNull(ff, "Même sans for fiscaux, la collection devrait exister, non?");
		assertEquals(0, ff.size(), "Il ne devrait pas y avoir de for fiscal");

		final AdresseGenerique adresseDomicile = adresseService.getAdresseFiscale(julie, TypeAdresseFiscale.DOMICILE, null, false);
		assertEquals(MockCommune.Neuchatel.getNoOFSEtendu(), adresseDomicile.getCommuneAdresse().getNoOFSEtendu(), "L'adresse de domicile devrait être à Neuchâtel");
	}

	@Etape(id=2, descr="Envoi de l'événement Obtention de Nationalité Suisse")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.NATIONALITE_SUISSE, noIndJulie, dateObtentionNationalite, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux & que l'événement civil est traité")
	public void check2() {
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabJulie);

		assertEquals(0, ((MockEvenementFiscalSender) evenementFiscalSender).count, "Aucun événement fiscal ne doit avoir été envoyé");

		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement devrait être traité");
		assertEquals(0, evt.getErreurs().size(), "Il ne devrait y avoir aucune erreur");

		final PersonnePhysique julie = (PersonnePhysique) tiersDAO.get(noHabJulie);
		final Set<ForFiscal> ff = julie.getForsFiscaux();
		assertNotNull(ff, "Même sans for fiscaux, la collection devrait exister, non?");
		assertEquals(0, ff.size(), "Il ne devrait pas y avoir de for fiscal (domicile hors-canton)");
	}
}
