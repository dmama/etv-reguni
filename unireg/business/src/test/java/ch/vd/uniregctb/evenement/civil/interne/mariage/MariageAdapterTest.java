package ch.vd.uniregctb.evenement.civil.interne.mariage;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCanton;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test le gestionnaire d'événement du même nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class MariageAdapterTest extends WithoutSpringTest {

	private static final long INDIVIDU_MARIE_SEUL = 12345L; //pierre
	private static final long INDIVIDU_MARIE = 54321L; //momo
	private static final long INDIVIDU_MARIE_CONJOINT = 23456L;//bea
	private static final long INDIVIDU_PACSE = 56789L;//david
	private static final Long INDIVIDU_PACSE_CONJOINT = 45678L;//julien


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

			// Rues
			rues.add(MockRue.CossonayVille.CheminDeRiondmorcel);
			rues.add(MockRue.Lausanne.AvenueDeBeaulieu);
		}
	};

	// Crée les données du mock service civil
	ServiceCivilService serviceCivil = new DefaultMockServiceCivil();

	@Test
	public void testGetConjointMariageSeul() throws Exception {
		PersonnePhysique pierre = new PersonnePhysique(true);
		pierre.setNumero(INDIVIDU_MARIE_SEUL);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE_SEUL, pierre, 0L, null, 1234, null );
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, null, null, false);
		MariageAdapter adapter = new MariageAdapter(evenementsCivils, context, null);
		assertNull("le conjoint d'un marié seul ne doit pas exister", adapter.getConjoint());
	}

	@Test
	public void testGetConjointMariage() throws Exception {
		PersonnePhysique momo = new PersonnePhysique(true);
		momo.setNumero(INDIVIDU_MARIE);
		PersonnePhysique bea = new PersonnePhysique(true);
		bea.setNumero(INDIVIDU_MARIE_CONJOINT);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE, momo, INDIVIDU_MARIE_CONJOINT, bea, 1234, null );
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, null, null, false);
		MariageAdapter adapter = new MariageAdapter(evenementsCivils, context, null);
		assertNotNull("le conjoint d'un marié doit exister", adapter.getConjoint());
	}

	@Test
	public void testGetConjointMariageHomo() throws Exception {
		PersonnePhysique david = new PersonnePhysique(true);
		david.setNumero(INDIVIDU_PACSE);
		PersonnePhysique julien = new PersonnePhysique(true);
		julien.setNumero(INDIVIDU_PACSE_CONJOINT);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_PACSE, david, INDIVIDU_PACSE_CONJOINT, julien, 1234, null );
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, null, null, false);
		MariageAdapter adapter = new MariageAdapter(evenementsCivils, context, null);
		assertNotNull("le conjoint d'un marié doit exister", adapter.getConjoint());
	}
}
