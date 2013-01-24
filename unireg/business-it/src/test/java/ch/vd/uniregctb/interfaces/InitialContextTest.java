package ch.vd.uniregctb.interfaces;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.List;

import org.junit.Test;

import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.infrastructure.service.ServiceInfrastructureHome;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertEquals;

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
	}

}
