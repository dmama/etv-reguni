package ch.vd.uniregctb.evenement.civil.interne.separation;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SeparationAdapterTest extends WithoutSpringTest {

	private static final long INDIVIDU_MARIE_SEUL = 12345L; //pierre
	private static final long INDIVIDU_MARIE = 54321L; //momo
	private static final long INDIVIDU_PACSE = 56789L;//david
	
	// Prend le mock infrastructure par défaut
	ServiceInfrastructureService infrastructureService = new DefaultMockServiceInfrastructureService();

	// Crée les données du mock service civil
	ServiceCivilService serviceCivil = new DefaultMockServiceCivil();

	@Test
	public void testGetAncienConjointMariageSeul() throws Exception {
		PersonnePhysique pierre = new PersonnePhysique(true);
		pierre.setNumero(INDIVIDU_MARIE_SEUL);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE_SEUL, pierre, 0L, null, 1234, null );
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, null, null, null, false);
		SeparationAdapter adapter = new SeparationAdapter(evenementsCivils, context, null);
		assertNull("le conjoint d'un marié seul ne doit pas exister", adapter.getAncienConjoint());
	}
	
	@Test
	public void testGetAncienConjointMariage() throws Exception {
		PersonnePhysique momo = new PersonnePhysique(true);
		momo.setNumero(INDIVIDU_MARIE);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE, momo, 0L, null, 1234, null );
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, null, null, null, false);
		SeparationAdapter adapter = new SeparationAdapter(evenementsCivils, context, null);
		assertNotNull("le conjoint d'un marié doit exister", adapter.getAncienConjoint());
	}
	
	@Test
	public void testGetAncienConjointMariageHomo() throws Exception {
		PersonnePhysique david = new PersonnePhysique(true);
		david.setNumero(INDIVIDU_PACSE);
		EvenementCivilExterne evenementsCivils = new EvenementCivilExterne(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_PACSE, david, 0L, null, 1234, null );
		final EvenementCivilContext context = new EvenementCivilContext(serviceCivil, infrastructureService, null, null, null, false);
		SeparationAdapter adapter = new SeparationAdapter(evenementsCivils, context, null);
		assertNotNull("le conjoint d'un pacsé doit exister", adapter.getAncienConjoint());
	}
}
