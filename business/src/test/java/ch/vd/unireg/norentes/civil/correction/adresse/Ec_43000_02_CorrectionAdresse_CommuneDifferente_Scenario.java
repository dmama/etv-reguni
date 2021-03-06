package ch.vd.unireg.norentes.civil.correction.adresse;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

public class Ec_43000_02_CorrectionAdresse_CommuneDifferente_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_43000_02_CorrectionAdresse";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CORREC_ADRESSE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Evénement de correction d'adresse avec changement de commune (invalide, donc!)";
	}

	private static final long noIndConceicao = 899126;
	private static final RegDate dateCorrection = RegDate.get(2008, 7, 1);

	private long noHabConceicao;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu indConceicao = addIndividu(noIndConceicao, RegDate.get(1976, 7, 24), "Conceiçao", "Aparecida Porto", false);

				addNationalite(indConceicao, MockPays.Espagne, indConceicao.getDateNaissance(), null);

				addPermis(indConceicao, TypePermis.ETABLISSEMENT, RegDate.get(2008, 5, 21), null, false);

				addAdresse(indConceicao, TypeAdresseCivil.COURRIER, "Rue de la poste", "39", 1020,
						MockLocalite.Renens, null, RegDate.get(2008, 4, 1), dateCorrection.getOneDayBefore());
				addAdresse(indConceicao, TypeAdresseCivil.PRINCIPALE, "Rue de la poste", "39", 1020,
						MockLocalite.Renens, null, RegDate.get(2008, 4, 1), dateCorrection.getOneDayBefore());

				addAdresse(indConceicao, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueJolimont, null,
						dateCorrection, null);
				addAdresse(indConceicao, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null,
						dateCorrection, null);
			}
		});
	}

	@Etape(id=1, descr="Construction du contribuable")
	public void etape1() throws Exception {
		final PersonnePhysique conceicao = addHabitant(noIndConceicao);
		addForFiscalPrincipal(conceicao, MockCommune.Aubonne, date(2008, 4, 1), null, MotifFor.ARRIVEE_HS, null);
		noHabConceicao = conceicao.getNumero();
	}

	@Check(id=1, descr="Vérification du contribuable créé")
	public void check1() throws Exception {
		final PersonnePhysique conceicao = (PersonnePhysique) tiersDAO.get(noHabConceicao);
		final ForFiscalPrincipal ffp = conceicao.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "L'habitant " + conceicao.getNumero() + " devrait avoir un for principal");
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For principal sur mauvaise commune");
	}

	@Etape(id=2, descr="Envoi de l'événement de correction d'adresse (avec changement de commune -> invalide)")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.CORREC_ADRESSE, noIndConceicao, dateCorrection, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification que l'événement a bien été mis en erreur")
	public void check2() throws Exception {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabConceicao);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement devrait être en erreur");
		assertEquals(1, evt.getErreurs().size(), "Il devrait y avoir une erreur");

		final EvenementCivilRegPPErreur erreur = evt.getErreurs().iterator().next();
		assertEquals("Evénement de correction d'adresse avec changement de commune", erreur.getMessage(), "Ce n'est pas l'erreur attendue");

		check1();
	}
}
