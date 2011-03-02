package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Test de l'adapter de l'obtention de nationalité :
 * ---------------------------------
 * - la recuperation du type de nationalité
 *
 * @author Ludovic BERTIN
 *
 */
public class ObtentionNationaliteAdapterTest extends WithoutSpringTest {

	final static private RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(2008, 01, 01);

	final static private long NO_INDIVIDU_NATIONALITE_SUISSE = 54321L;
	final static private long NO_INDIVIDU_NATIONALITE_FRANCE = 45678L;

	/**
	 * Teste la recuperation du type de nationalité avec un individu qui a obtenue une nationalité suisse.
	 * @throws Exception
	 */
	@Test
	public void testIsNationaliteSuisseAvecNationaliteSuisse() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_NATIONALITE_SUISSE);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.NATIONALITE_SUISSE, EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_NATIONALITE, NO_INDIVIDU_NATIONALITE_SUISSE , habitant, 0L, null, 1234, null);
		ObtentionNationaliteAdapter adapter = new ObtentionNationaliteAdapter(evenement, context, null);
	}

	/**
	 * Teste la recuperation du type de nationalité avec un individu qui a obtenue une nationalité francaise.
	 * @throws Exception
	 */
	@Test
	public void testIsNationaliteSuisseAvecNationaliteFrancaise() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_NATIONALITE_FRANCE);
		EvenementCivilExterne
				evenement = new EvenementCivilExterne(1L, TypeEvenementCivil.NATIONALITE_NON_SUISSE, EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_NATIONALITE, NO_INDIVIDU_NATIONALITE_FRANCE , habitant, 0L, null, 1234, null);
		ObtentionNationaliteAdapter adapter = new ObtentionNationaliteAdapter(evenement, context, null);
	}

	// Prend le mock infrastructure par défaut
	ServiceInfrastructureService infrastructureService = new MockServiceInfrastructureService() {
		@Override
		protected void init() {
			// Pays
			pays.add(MockPays.Suisse);

			// Cantons
			cantons.add(MockCanton.Vaud);

			// Communes
			communesVaud.add(MockCommune.Lausanne);
			communesVaud.add(MockCommune.Cossonay);

			// Localités
			localites.add(MockLocalite.Lausanne);
			localites.add(MockLocalite.CossonayVille);
		}
	};

	// Crée les données du mock service civil
	ServiceCivilService serviceCivilSimple = new DefaultMockServiceCivil() {
		@Override
		protected void init() {
			MockIndividu momo = addIndividu(NO_INDIVIDU_NATIONALITE_FRANCE, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			MockIndividu julie = addIndividu(NO_INDIVIDU_NATIONALITE_SUISSE, RegDate.get(1977, 4, 19), "Goux", "Julie", false);

			/* Adresses */
			addDefaultAdressesTo(momo);
			addDefaultAdressesTo(julie);

			/* nationalites */
			addNationalite(momo, MockPays.France, DATE_OBTENTION_NATIONALITE, null, 0);
			addNationalite(julie, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null, 0);
		}
	};

	private EvenementCivilContext context = new EvenementCivilContext(serviceCivilSimple, infrastructureService, null, null, null, null, null, null, false);
}
