package ch.vd.unireg.evenement.civil.interne.mariage;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.MockTiersDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test le gestionnaire d'événement du même nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class MariageTest extends WithoutSpringTest {

	private static final long INDIVIDU_MARIE_SEUL = 12345L; //pierre
	private static final long INDIVIDU_MARIE = 54321L; //momo
	private static final long INDIVIDU_MARIE_CONJOINT = 23456L;//bea
	private static final long INDIVIDU_PACSE = 56789L;//david
	private static final Long INDIVIDU_PACSE_CONJOINT = 45678L;//julien

	private MockTiersDAO tiersDAO = new MockTiersDAO();

	// Prend le mock infrastructure par défaut
	ServiceInfrastructureService infrastructureService = new ServiceInfrastructureImpl(new MockServiceInfrastructureService() {
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

			// Rues
			rues.add(MockRue.CossonayVille.CheminDeRiondmorcel);
			rues.add(MockRue.Lausanne.AvenueDeBeaulieu);
		}
	}, tiersDAO);

	// Crée les données du mock service civil
	ServiceCivilService serviceCivil = new ServiceCivilImpl(infrastructureService, new DefaultMockServiceCivil());
	private EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, tiersDAO, Mockito.mock(AuditManager.class));
	private EvenementCivilOptions options = new EvenementCivilOptions(false);

	@Test
	public void testGetConjointMariageSeul() throws Exception {
		PersonnePhysique pierre = new PersonnePhysique(true);
		pierre.setNumero(INDIVIDU_MARIE_SEUL);
		EvenementCivilRegPP evenementsCivils = new EvenementCivilRegPP(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE_SEUL, 0L, 1234, null );
		Mariage adapter = new Mariage(evenementsCivils, context, options);
		assertNull("le conjoint d'un marié seul ne doit pas exister", adapter.getConjoint());
	}

	@Test
	public void testGetConjointMariage() throws Exception {
		PersonnePhysique momo = new PersonnePhysique(true);
		momo.setNumero(INDIVIDU_MARIE);
		PersonnePhysique bea = new PersonnePhysique(true);
		bea.setNumero(INDIVIDU_MARIE_CONJOINT);
		EvenementCivilRegPP evenementsCivils = new EvenementCivilRegPP(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE, INDIVIDU_MARIE_CONJOINT, 1234, null );
		Mariage adapter = new Mariage(evenementsCivils, context, options);
		assertNotNull("le conjoint d'un marié doit exister", adapter.getConjoint());
	}

	@Test
	public void testGetConjointMariageHomo() throws Exception {
		PersonnePhysique david = new PersonnePhysique(true);
		david.setNumero(INDIVIDU_PACSE);
		PersonnePhysique julien = new PersonnePhysique(true);
		julien.setNumero(INDIVIDU_PACSE_CONJOINT);
		EvenementCivilRegPP evenementsCivils = new EvenementCivilRegPP(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_PACSE, INDIVIDU_PACSE_CONJOINT, 1234, null );
		Mariage adapter = new Mariage(evenementsCivils, context, options);
		assertNotNull("le conjoint d'un marié doit exister", adapter.getConjoint());
	}
}
