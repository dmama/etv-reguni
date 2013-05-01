package ch.vd.uniregctb.norentes.civil.correction.adresse;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class Ec_43000_04_CorrectionAdresse_IndividuMajeurSansFor_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_43000_04_CorrectionAdresse";

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
		return "Evénement de correction d'adresse sur individu sans for (majeur, en l'occurrence)";
	}

	private static final long noIndConceicao = 899126;
	private static final RegDate naissance = RegDate.get(1968, 1, 13);
	private static final RegDate dateCorrection = RegDate.get(2008, 7, 1);

	private long noHabConceicao;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final RegDate aujourdhui = RegDate.get();
				final MockIndividu indConceicao = addIndividu(noIndConceicao, naissance, "Conceiçao", "Aparecida Porto", false);

				addOrigine(indConceicao, MockPays.Espagne.getNomCourt());
				addNationalite(indConceicao, MockPays.Espagne, indConceicao.getDateNaissance(), null);

				addPermis(indConceicao, TypePermis.ETABLISSEMENT, RegDate.get(2008, 5, 21), null, false);

				addAdresse(indConceicao, TypeAdresseCivil.COURRIER, "Rue de la poste", "39", 1020,
						MockLocalite.Renens, null, RegDate.get(2008, 4, 1), dateCorrection.getOneDayBefore());
				addAdresse(indConceicao, TypeAdresseCivil.PRINCIPALE, "Rue de la poste", "39", 1020,
						MockLocalite.Renens, null, RegDate.get(2008, 4, 1), dateCorrection.getOneDayBefore());

				addAdresse(indConceicao, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null,
						dateCorrection, null);
				addAdresse(indConceicao, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null,
						dateCorrection, null);
			}
		});
	}

	@Etape(id=1, descr="Construction du contribuable (sans for, malgré individu majeur)")
	public void etape1() throws Exception {
		final PersonnePhysique conceicao = addHabitant(noIndConceicao);
		noHabConceicao = conceicao.getNumero();
	}

	@Check(id=1, descr="Vérification du contribuable créé")
	public void check1() throws Exception {
		final PersonnePhysique conceicao = (PersonnePhysique) tiersDAO.get(noHabConceicao);
		final ForFiscalPrincipal ffp = conceicao.getDernierForFiscalPrincipal();
		assertNull(ffp, "L'habitant " + conceicao.getNumero() + " ne devrait pas avoir de for principal");
	}

	@Etape(id=2, descr="Envoi de l'événement de correction d'adresse (sur mineur sans for -> passé tout droit)")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.CORREC_ADRESSE, noIndConceicao, dateCorrection, MockCommune.Lausanne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification que l'événement a bien été mis en erreur")
	public void check2() throws Exception {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabConceicao);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "L'événement devrait être en erreur");
		assertEquals(1, evt.getErreurs().size(), "Mauvais nombre d'erreurs");

		final EvenementCivilRegPPErreur erreur = evt.getErreurs().iterator().next();
		assertNotNull(erreur, "Erreur devrait être non nulle");

		final String msg = String.format("Impossible de trouver la commune du for actif au %s de l'individu %d (ctb %s)",
													RegDateHelper.dateToDisplayString(RegDate.get()), noIndConceicao, FormatNumeroHelper.numeroCTBToDisplay(noHabConceicao));
		assertEquals(msg, erreur.getMessage(), "Mauvaise erreur");
	}
}
