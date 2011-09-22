package ch.vd.uniregctb.identification.contribuable;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;

import static org.junit.Assert.assertEquals;

public class IdentificationContribuableHelperTest extends BusinessTest {
	private IdentificationContribuableHelper helper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		helper = getBean(IdentificationContribuableHelper.class, "identificationContribuableHelper");

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testgetPremierMot() throws Exception {
		String mot = "jean-renée";
		assertEquals("jean", helper.getPremierMot(mot));
		mot =  "jean renée";
		assertEquals("jean", helper.getPremierMot(mot));

		mot="jean";
		assertEquals("jean", helper.getPremierMot(mot));
	}

}
