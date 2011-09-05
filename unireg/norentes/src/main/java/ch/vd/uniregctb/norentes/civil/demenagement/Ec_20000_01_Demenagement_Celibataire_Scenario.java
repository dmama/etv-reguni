package ch.vd.uniregctb.norentes.civil.demenagement;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Test norentes de l'événement "déménagement d'un célibataire dans la commune".
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_20000_01_Demenagement_Celibataire_Scenario extends EvenementCivilScenario {

	public static final String NAME = "20000_01_Demenagement";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Déménagement d'un célibataire dans la commune";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE;
	}

	private final long noIndConceicao = 899126; // Conceiçao

	protected MockIndividu indConceicao;

	private long noHabConceicao;

	private static final RegDate dateDemenagement = RegDate.get(2008, 7, 1);
	private final Commune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indConceicao = addIndividu(noIndConceicao, RegDate.get(1976, 7, 24), "Conceiçao", "Aparecida Porto", false);

				addOrigine(indConceicao, MockPays.Espagne, null, indConceicao.getDateNaissance());
				addNationalite(indConceicao, MockPays.Espagne, indConceicao.getDateNaissance(), null);

				addPermis(indConceicao, TypePermis.PROVISOIRE, RegDate.get(2008, 5, 21), null, false);

				addAdresse(indConceicao, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(2008, 4, 1), RegDate.get(2008, 6, 30));
				addAdresse(indConceicao, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						RegDate.get(2008, 4, 1), RegDate.get(2008, 6, 30));

				addAdresse(indConceicao, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeMarcelin, null,
						dateDemenagement, null);
				addAdresse(indConceicao, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null,
						dateDemenagement, null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant")
	public void etape1() {
		PersonnePhysique conceicao = addHabitant(noIndConceicao);
		noHabConceicao = conceicao.getNumero();
	}

	@Check(id=1, descr="Vérifie que l'habitant possède bien un for courant avec le mode d'imposition ORDINAIRE")
	public void check1() {
		PersonnePhysique conceicao = (PersonnePhysique) tiersDAO.get(noHabConceicao);
		ForFiscalPrincipal ffp = conceicao.getDernierForFiscalPrincipal();
		assertNull(ffp, "L'habitant " + conceicao.getNumero() + " devrait avoir aucun for");
	}

	@Etape(id=2, descr="Envoi de l'événement Déménagement dans la commune")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, noIndConceicao, dateDemenagement, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification des fors fiscaux & que l'événement civil est traité")
	public void check2() {
		final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabConceicao);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement devrait être traité");
		assertEquals(0, evt.getErreurs().size(), "Il ne devrait pas y avoir aucune erreur");

		check1();
	}
}
