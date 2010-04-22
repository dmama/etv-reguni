package ch.vd.uniregctb.interfaces;

import static junit.framework.Assert.assertEquals;

import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.Test;

import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.infrastructure.service.ServiceInfrastructureHome;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class InitialContextTest extends WithoutSpringTest {

	@Test
	public void testContextConnection() throws Exception {

		boolean run = false;
		if (run) {
			connect();
		}
	}

	private void connect() throws Exception {

		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
		map.put(Context.PROVIDER_URL, "t3://solve61v:64910");
		InitialContext ctx = new InitialContext(map);

		// Infra
		ServiceInfrastructureHome home = (ServiceInfrastructureHome)ctx.lookup("ejb/ch.vd.infrastructure.service.ServiceInfrastructure-1.8");
		ServiceInfrastructure service = home.create();
		List<?> pays = service.getListePays();
		int n = pays.size();
		assertEquals(257, n);

		// Individu
		//ServiceCivilHome homeI = (ServiceCivilHome)ctx.lookup("ejb/ch.vd.registre.civil.service.ServiceCivil-1.6");
		//ServiceCivil serviceCivil = homeI.create();
		//Individu ind = serviceCivil.getIndividu(333527, 2007);
		//Date date = ind.getDateNaissance();
		//String str = date.toString();

		//ind = serviceCivil.getIndividu(333528, 2007);
		//HistoriqueIndividu histo = ind.getDernierHistoriqueIndividu();
		//String p = histo.getPrenom();

		//ind = serviceCivil.getIndividu(333528, 2007, new EnumAttributeIndividu[] { EnumAttributeIndividu.CONJOINT });
		//histo = ind.getDernierHistoriqueIndividu();
		//Individu ind2 = ind.getConjoint();
		//String sara = ind2.getDernierHistoriqueIndividu().getPrenom();
	}

}
