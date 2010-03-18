package ch.vd.uniregctb.interfaces;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class ServiceCivilTest extends BusinessItTest {

	private ServiceCivilService service;

	@Override                             
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = getBean(ServiceCivilService.class, "serviceCivilService");
	}

	@Test
	public void testGetIndividu() throws Exception {

		Individu elie = service.getIndividu(333527L, 2007);
		assertNotNull(elie);
		// En 2005, il n'etait pas né.. devrait etre null!
		assertEquals("Elie", elie.getDernierHistoriqueIndividu().getPrenom());
		elie = service.getIndividu(333527L, 2005);
		// assertNull(elie);

		Individu jean = service.getIndividu(333528, 2007);
		assertNotNull(jean);
		assertEquals("Jean-Eric", jean.getDernierHistoriqueIndividu().getPrenom());
		jean = service.getIndividu(333528, 2001);
		assertNotNull(jean);
		jean = service.getIndividu(333528, 2006, EnumAttributeIndividu.CONJOINT);
		assertNotNull(jean);
		Individu sara = service.getConjoint(jean.getNoTechnique(),RegDate.get(2007,1,1));
		assertNotNull(sara);

		assertEquals("Sara", sara.getDernierHistoriqueIndividu().getPrenom());
	}
	@Test
	public void testGetNumeroIndividuConjoint(){
		Individu jeanMarc = service.getIndividu(132720L, 2006);
		assertNotNull(jeanMarc);
		Long numeroAmelie = service.getNumeroIndividuConjoint(jeanMarc.getNoTechnique(), RegDate.get(2006,1,1));
		assertEquals(null,numeroAmelie);

		numeroAmelie = service.getNumeroIndividuConjoint(jeanMarc.getNoTechnique(), RegDate.get(2008,5,27));
		assertNotNull(numeroAmelie);

		numeroAmelie = service.getNumeroIndividuConjoint(jeanMarc.getNoTechnique(), RegDate.get(2008,6,25));
		assertEquals(845875,numeroAmelie.longValue());
	}
   @Test
	public void testGetIndividuConjoint(){
		Individu jeanMarc = service.getIndividu(132720L, 2006);
		assertNotNull(jeanMarc);
		Individu conjoint = service.getConjoint(jeanMarc.getNoTechnique(), RegDate.get(2006,1,1));
	   //Celibataire
		assertEquals(null,conjoint);

		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), RegDate.get(2007,6,24));
	   //Marié
	    assertNotNull(conjoint);
		assertEquals("Amélie",conjoint.getDernierHistoriqueIndividu().getPrenom());
		assertEquals(845875,conjoint.getNoTechnique());

		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), RegDate.get(2008,6,28));
	   //Séparé
		assertNotNull(conjoint);
		assertEquals("Amélie",conjoint.getDernierHistoriqueIndividu().getPrenom());
		assertEquals(845875,conjoint.getNoTechnique());


		conjoint = service.getConjoint(jeanMarc.getNoTechnique(), RegDate.get(2009,7,28));
	   //Divorcé
		assertEquals(null,conjoint);
	   conjoint = service.getConjoint(jeanMarc.getNoTechnique(), RegDate.get(2010,3,28));

	   //Remarié
		assertNotNull(conjoint);
		assertEquals(387602,conjoint.getNoTechnique());
		
	}


}
