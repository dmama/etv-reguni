package ch.vd.uniregctb.parametrage;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.parametrage.PeriodeFiscaleServiceImpl;

/**
 * Test de la classe {@link PeriodeFiscaleServiceImpl}
 * 
 * @author xsifnr
 *
 */
public class PeriodeFiscaleServiceImplTest extends WithoutSpringTest{


	private PeriodeFiscaleServiceImpl service;
	private ParametreAppService mockParametreAppService; 
	private PeriodeFiscaleDAO mockDao;
	
	@Override
	public void onSetUp() throws Exception {

		mockParametreAppService = createMock(ParametreAppService.class);
		mockDao = createMock(PeriodeFiscaleDAO.class);
		service = new PeriodeFiscaleServiceImpl();
		service.setDao(mockDao);
		service.setParametreAppService(mockParametreAppService);
	}
	
	/**
	 * Tests unitaires pour {@link PeriodeFiscaleServiceImpl#initNouvellePeriodeFiscale()}
	 */
	@Test
	public void testInitNouvellePeriodeFiscale () {
		PeriodeFiscale periodeFiscale2003 = newPeriodeFiscale(2003);
		PeriodeFiscale periodeFiscale2004 = newPeriodeFiscale(2004);
		expect(mockDao.getAllDesc())
			.andReturn(Collections.<PeriodeFiscale>emptyList())
			.andReturn(Arrays.asList(periodeFiscale2003));
		expect(mockDao.save(isA(PeriodeFiscale.class)))
			.andReturn(periodeFiscale2003)
			.andReturn(periodeFiscale2004);
		expect(mockParametreAppService.getPremierePeriodeFiscale())
			.andReturn(2003); 
		replay(mockParametreAppService);
		replay(mockDao);
		PeriodeFiscale newPf1 = service.initNouvellePeriodeFiscale();
		assertEquals(2003, newPf1.getAnnee().intValue());
		checkParametrePeriodeFiscale(newPf1);
		PeriodeFiscale newPf2 = service.initNouvellePeriodeFiscale();
		assertEquals(newPf1.getAnnee().intValue() + 1, newPf2.getAnnee().intValue());
		checkParametrePeriodeFiscale(newPf2);
		verify(mockDao);
	}


	/**
	 * Verifie que la coherence ds {@link ParametrePeriodeFiscale} en fonction de leur {@link PeriodeFiscale}.
	 * Principalement que les années des termes soit bien l'année suivante.
	 * @param pf
	 */
	private void checkParametrePeriodeFiscale(PeriodeFiscale pf) {
		for (ParametrePeriodeFiscale ppf : pf.getParametrePeriodeFiscale() ) {
			assertEquals(pf.getAnnee() + 1, ppf.getTermeGeneralSommationEffectif().year());
			assertEquals(pf.getAnnee() + 1, ppf.getTermeGeneralSommationReglementaire().year());
			assertEquals(pf.getAnnee() + 1, ppf.getDateFinEnvoiMasseDI().year());			
		}
	}

	/**
	 * Instancie un objet {@link PeriodeFiscale}, avec des {@link ParametrePeriodeFiscale} par défaut
	 * 
	 * @param annee l'année de la {@link PeriodeFiscale}
	 * @return
	 */
	private PeriodeFiscale newPeriodeFiscale(int annee) {
		PeriodeFiscale pf = new PeriodeFiscale();
		pf.setId(new Long(annee));
		pf.setAnnee(annee);
		pf.setDefaultPeriodeFiscaleParametres();
		return pf;
	}

}
