package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.evd0022.v1.Identifier;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class UidNumberExtractorTest {

	private static final String CH_UID_KEY = "CH.IDE";

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void extractUidNumber() {
		List<Identifier> identifiers = Arrays.asList(
				new Identifier("CH.XYZ", "identity_1"),
				new Identifier("ABCD", "identity_2"),
				new Identifier("CH.IDE", "identity_3"),
				new Identifier("CH.ZVF", "identity_4"));
		String result = new UidNumberExtractor().apply(identifiers);
		assertThat(result, equalTo("identity_3"));
	}
}