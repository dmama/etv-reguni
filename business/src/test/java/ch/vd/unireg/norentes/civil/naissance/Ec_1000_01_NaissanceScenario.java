package ch.vd.unireg.norentes.civil.naissance;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

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
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			public void init() {
				final MockIndividu pierre = addIndividu(123, dateNaissance, "Durand", "Pierre", true);
				addNationalite(pierre, MockPays.Suisse, RegDate.get(2008, 4, 12), null);
				addAdresse(pierre, TypeAdresseCivil.PRINCIPALE, "Av de Recordon", "1", 1012, MockLocalite.Lausanne, null, RegDate.get(2008, 4, 12), null);
			}
		});
	}

	@Etape(id=1, descr="Aucune donnée n'est chargée en base de données")
	public void etape1() {
	}

	@Check(id=1, descr="Vérifie qu'il n'y a pas de personne physique dans la base de données")
	public void check1() {
		final List<Tiers> list = tiersDAO.getAll();
		CollectionUtils.filter(list, object -> object instanceof PersonnePhysique);
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

		final List<Tiers> list = tiersDAO.getAll();
		CollectionUtils.filter(list, object -> object instanceof PersonnePhysique);
		assertEquals(1, list.size(), "");

		final PersonnePhysique pierre = (PersonnePhysique)list.get(0);
		assertEquals((long) 123, pierre.getNumeroIndividu(), "");
		assertEquals(0, pierre.getForsFiscaux().size(), "");
		assertEquals(0, pierre.getAdressesTiers().size(), "");
	}

}
