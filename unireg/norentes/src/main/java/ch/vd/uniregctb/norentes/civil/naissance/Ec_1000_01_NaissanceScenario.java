package ch.vd.uniregctb.norentes.civil.naissance;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement d'une naissance d'unn enfant
 *
 * @author jec
 *
 */
public class Ec_1000_01_NaissanceScenario extends EvenementCivilScenario {

	public static final String NAME = "1000_01_Naissance";

	private final RegDate dateNaissance = RegDate.get(2008, 12, 4);

	public Ec_1000_01_NaissanceScenario() {
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Test l'événement de Naissance d'un Bébé";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.NAISSANCE;
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			public void init() {
				MockIndividu pierre = addIndividu(123, dateNaissance, "Durand", "Pierre", true);
				setNationalite(pierre, RegDate.get(2008, 4, 12), null, MockPays.Suisse);
				addAdresse(pierre, EnumTypeAdresse.PRINCIPALE, "Av de Recordon", "1", 1012, MockLocalite.Lausanne, null, RegDate.get(2008, 4, 12), null);
			}
		});
	}

	@Etape(id=1, descr="Aucune donnée n'est chargée en base de données")
	public void etape1() {
	}

	@Check(id=1, descr="Vérifie qu'il n'y a pas de Tiers dans la base de données")
	public void check1() {
		List<Tiers> list = tiersDAO.getAll();
		assertEquals(0, list.size(), "");
	}

	@Etape(id=2, descr="Envoi de l'événement de Naissance")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.NAISSANCE, 123L, RegDate.get(2008, 4, 12), 5586);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le tiers a été créé")
	public void check2() {

		checkEtatEvtCivils(1, EtatEvenementCivil.TRAITE);

		List<Tiers> list = tiersDAO.getAll();
		assertEquals(1, list.size(), "");

		PersonnePhysique pierre = (PersonnePhysique)list.get(0);
		assertEquals(new Long(123), pierre.getNumeroIndividu(), "");
		assertEquals(0, pierre.getForsFiscaux().size(), "");
		assertEquals(0, pierre.getAdressesTiers().size(), "");
	}

}
