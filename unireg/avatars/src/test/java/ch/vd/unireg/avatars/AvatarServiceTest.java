package ch.vd.unireg.avatars;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class AvatarServiceTest extends WithoutSpringTest {

	/**
	 * On vérifie que tous les types d'avatar ont une image accessible
	 */
	@Test
	public void testImagePath() throws Exception {
		for (TypeAvatar type : TypeAvatar.values()) {

			// sans lien
			{
				final String path = AvatarServiceImpl.getImagePath(type, false);
				final URL url = AvatarServiceImpl.class.getResource(path);
				Assert.assertNotNull(type.name(), url);
			}

			// avec lien
			{
				final String path = AvatarServiceImpl.getImagePath(type, true);
				final URL url = AvatarServiceImpl.class.getResource(path);
				Assert.assertNotNull(type.name(), url);
			}
		}
	}
}
