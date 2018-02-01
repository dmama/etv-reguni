package ch.vd.unireg.evenement.identification.contribuable;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class IdentificationContribuableMessageDispatcherTest extends WithoutSpringTest {

	@Test
	public void testIsForV1() throws Exception {
		Assert.assertTrue(IdentificationContribuableMessageDispatcher.isForV1("http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7"));
		Assert.assertFalse(IdentificationContribuableMessageDispatcher.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/request/2"));
		Assert.assertFalse(IdentificationContribuableMessageDispatcher.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/request/2.1"));
		Assert.assertFalse(IdentificationContribuableMessageDispatcher.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/request/12"));
		Assert.assertFalse(IdentificationContribuableMessageDispatcher.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/blah"));
	}
}
