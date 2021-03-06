package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockInfrastructureConnector;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.MockTiersDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * Test de l'adapter de l'obtention de nationalité :
 * ---------------------------------
 * - la recuperation du type de nationalité
 *
 * @author Ludovic BERTIN
 *
 */
public class ObtentionNationaliteTest extends WithoutSpringTest {

	static final private RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(2008, 1, 1);

	static final private long NO_INDIVIDU_NATIONALITE_SUISSE = 54321L;
	static final private long NO_INDIVIDU_NATIONALITE_FRANCE = 45678L;

	/**
	 * Teste la recuperation du type de nationalité avec un individu qui a obtenue une nationalité suisse.
	 * @throws Exception
	 */
	@Test
	public void testIsNationaliteSuisseAvecNationaliteSuisse() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_NATIONALITE_SUISSE);
		EvenementCivilRegPP
				evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.NATIONALITE_SUISSE, EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_NATIONALITE, NO_INDIVIDU_NATIONALITE_SUISSE , 0L, 1234, null);
		ObtentionNationalite adapter = new ObtentionNationaliteSuisse(evenement, context, options);
	}

	/**
	 * Teste la recuperation du type de nationalité avec un individu qui a obtenue une nationalité francaise.
	 * @throws Exception
	 */
	@Test
	public void testIsNationaliteSuisseAvecNationaliteFrancaise() throws Exception {
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumero(NO_INDIVIDU_NATIONALITE_FRANCE);
		EvenementCivilRegPP
				evenement = new EvenementCivilRegPP(1L, TypeEvenementCivil.NATIONALITE_NON_SUISSE, EtatEvenementCivil.A_TRAITER, DATE_OBTENTION_NATIONALITE, NO_INDIVIDU_NATIONALITE_FRANCE , 0L, 1234, null);
		ObtentionNationalite adapter = new ObtentionNationaliteNonSuisse(evenement, context, options);
	}

	private MockTiersDAO tiersDAO = new MockTiersDAO();

	// Prend le mock infrastructure par défaut
	ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new MockInfrastructureConnector() {
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
	}, tiersDAO);

	// Crée les données du mock service civil
	ServiceCivilService serviceCivilSimple = new ServiceCivilImpl(infrastructureService, new DefaultMockIndividuConnector() {
		@Override
		protected void init() {
			MockIndividu momo = addIndividu(NO_INDIVIDU_NATIONALITE_FRANCE, RegDate.get(1961, 3, 12), "Durant", "Maurice", true);
			MockIndividu julie = addIndividu(NO_INDIVIDU_NATIONALITE_SUISSE, RegDate.get(1977, 4, 19), "Goux", "Julie", false);

			/* Adresses */
			addDefaultAdressesTo(momo);
			addDefaultAdressesTo(julie);

			/* nationalites */
			addNationalite(momo, MockPays.France, DATE_OBTENTION_NATIONALITE, null);
			addNationalite(julie, MockPays.Suisse, DATE_OBTENTION_NATIONALITE, null);
		}
	});

	private EvenementCivilContext context = new EvenementCivilContext(serviceCivilSimple, infrastructureService, tiersDAO, Mockito.mock(AuditManager.class));
	private EvenementCivilOptions options = new EvenementCivilOptions(false);
}
