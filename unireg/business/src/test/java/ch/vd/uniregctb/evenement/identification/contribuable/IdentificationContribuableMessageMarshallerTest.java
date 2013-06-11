package ch.vd.uniregctb.evenement.identification.contribuable;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class IdentificationContribuableMessageMarshallerTest extends WithoutSpringTest {

	@Test
	public void testIsForV1() throws Exception {
		Assert.assertTrue(IdentificationContribuableMessageMarshaller.isForV1("http://www.vd.ch/fiscalite/registre/identificationContribuable-v1.7"));
		Assert.assertFalse(IdentificationContribuableMessageMarshaller.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/request/2"));
		Assert.assertFalse(IdentificationContribuableMessageMarshaller.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/request/2.1"));
		Assert.assertFalse(IdentificationContribuableMessageMarshaller.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/request/12"));
		Assert.assertFalse(IdentificationContribuableMessageMarshaller.isForV1("http://www.vd.ch/fiscalite/registre/identification/contribuable/blah"));
	}
}
