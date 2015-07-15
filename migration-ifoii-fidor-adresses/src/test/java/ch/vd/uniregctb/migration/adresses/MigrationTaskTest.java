package ch.vd.uniregctb.migration.adresses;

import org.junit.Assert;
import org.junit.Test;

public class MigrationTaskTest {

	@Test
	public void testCanonize() throws Exception {
		Assert.assertEquals("Avenue du bois 15", MigrationTask.canonize("Av.du bois 15"));
		Assert.assertEquals("Avenue du bois 15", MigrationTask.canonize("Av. du bois 15"));
		Assert.assertEquals("Boulevard des Italiens 12d", MigrationTask.canonize("Bvd.des Italiens 12d"));
		Assert.assertEquals("Boulevard des Italiens 12d", MigrationTask.canonize("Bvd. des Italiens 12d"));
		Assert.assertEquals("Chemin des blés 12d", MigrationTask.canonize("Ch.des blés 12d"));
		Assert.assertEquals("Chemin des blés 12d", MigrationTask.canonize("Ch. des blés 12d"));
		Assert.assertEquals("Place du marché", MigrationTask.canonize("Pl.du marché"));
		Assert.assertEquals("Place du marché", MigrationTask.canonize("Pl. du marché"));
		Assert.assertEquals("Quai Aristide Briand", MigrationTask.canonize("Q.Aristide Briand"));
		Assert.assertEquals("Quai Aristide Briand", MigrationTask.canonize("Q. Aristide Briand"));
		Assert.assertEquals("Rue du pont", MigrationTask.canonize("R.du pont"));
		Assert.assertEquals("Rue du pont", MigrationTask.canonize("R. du pont"));
		Assert.assertEquals("Route du canal", MigrationTask.canonize("Rte.du canal"));
		Assert.assertEquals("Route du canal", MigrationTask.canonize("Rte. du canal"));
		Assert.assertEquals("Route du canal", MigrationTask.canonize("Rte du canal"));
		Assert.assertEquals("Rtedu canal", MigrationTask.canonize("Rtedu canal"));      // pas reconnu...
		Assert.assertEquals("Chemin de l'arsenal", MigrationTask.canonize("Ch.de l' arsenal"));
		Assert.assertEquals("Chemin de l'arsenal", MigrationTask.canonize("Ch. de l'arsenal"));
		Assert.assertEquals("Chemin des Rondze-Mulets", MigrationTask.canonize("Ch. des Rondze- Mulets"));
		Assert.assertEquals("Chemin des Rondze-Mulets", MigrationTask.canonize("Ch. des Rondze-  Mulets"));
		Assert.assertEquals("Sentier des philosophes", MigrationTask.canonize("Sent.des philosophes"));
		Assert.assertEquals("Sentier des philosophes", MigrationTask.canonize("Sent. des philosophes"));
		Assert.assertEquals("Bahnhofstrasse 18", MigrationTask.canonize("Bahnhofstr. 18"));
		Assert.assertEquals("Bahnhofstrasse 18", MigrationTask.canonize("Bahnhofstr.18"));
		Assert.assertEquals("Gustav-Heinemann-strasse 18", MigrationTask.canonize("Gustav-Heinemann-str. 18"));
		Assert.assertEquals("Gustav-Heinemann-strasse 18", MigrationTask.canonize("Gustav-Heinemann-str.18"));
	}
}
