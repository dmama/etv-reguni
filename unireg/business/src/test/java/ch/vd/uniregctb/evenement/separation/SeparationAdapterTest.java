package ch.vd.uniregctb.evenement.separation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

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
		EvenementCivilData evenementsCivils = new EvenementCivilData(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE_SEUL, pierre, 0L, null, 1234, null );
		SeparationAdapter adapter = new SeparationAdapter();
		adapter.init(evenementsCivils, serviceCivil, infrastructureService, null);
		assertNull("le conjoint d'un marié seul ne doit pas exister", adapter.getAncienConjoint());
	}
	
	@Test
	public void testGetAncienConjointMariage() throws Exception {
		PersonnePhysique momo = new PersonnePhysique(true);
		momo.setNumero(INDIVIDU_MARIE);
		EvenementCivilData evenementsCivils = new EvenementCivilData(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_MARIE, momo, 0L, null, 1234, null );
		SeparationAdapter adapter = new SeparationAdapter();
		adapter.init(evenementsCivils, serviceCivil, infrastructureService, null);
		assertNotNull("le conjoint d'un marié doit exister", adapter.getAncienConjoint());		
	}
	
	@Test
	public void testGetAncienConjointMariageHomo() throws Exception {
		PersonnePhysique david = new PersonnePhysique(true);
		david.setNumero(INDIVIDU_PACSE);
		EvenementCivilData evenementsCivils = new EvenementCivilData(1L, TypeEvenementCivil.MARIAGE, EtatEvenementCivil.A_TRAITER,
				RegDate.get(2000, 12, 19), INDIVIDU_PACSE, david, 0L, null, 1234, null );
		SeparationAdapter adapter = new SeparationAdapter();
		adapter.init(evenementsCivils, serviceCivil, infrastructureService, null);
		assertNotNull("le conjoint d'un pacsé doit exister", adapter.getAncienConjoint());		
	}
}
