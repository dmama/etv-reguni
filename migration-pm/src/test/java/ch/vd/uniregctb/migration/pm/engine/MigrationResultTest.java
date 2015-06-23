package ch.vd.uniregctb.migration.pm.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;
import ch.vd.uniregctb.migration.pm.log.LoggedElementAttribute;

public class MigrationResultTest {

	private static class MyDataToConsolidate {
		private int key;
		private String msg;

		public MyDataToConsolidate(int key, String msg) {
			this.key = key;
			this.msg = msg;
		}
	}

	@Test
	public void testConsolisationData() throws Exception {
		final MigrationResult mr = new MigrationResult(null);       // normalement, on n'a ici pas besoin de graphe... Ca pêtera si jamais !

		// enregistrement de la structure

		mr.registerPreTransactionCommitCallback(MyDataToConsolidate.class,
		                                        1,
		                                        d -> d.key,
		                                        (d1, d2) -> new MyDataToConsolidate(d1.key, String.format("%s,%s", d1.msg, d2.msg)),
		                                        d -> mr.addMessage(LogCategory.EXCEPTIONS, LogLevel.INFO, String.format("%d -> %s", d.key, d.msg)));

		// enregistrement des données

		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "La"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "Réponse"));

		mr.addPreTransactionCommitData(new MyDataToConsolidate(1, "Hein?"));

		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "A"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "La"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "Grande"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "Question"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "De"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "L'"));
		mr.addPreTransactionCommitData(new MyDataToConsolidate(42, "Univers"));

		mr.addPreTransactionCommitData(new MyDataToConsolidate(1, "Aaaah ok..."));

		Assert.assertEquals(0, mr.getMessages(LogCategory.EXCEPTIONS).size());     // pour l'instant, il n'y a rien

		// consolidation

		mr.consolidatePreTransactionCommitRegistrations();

		// validation du résultat

		final List<LoggedElement> msgs = mr.getMessages(LogCategory.EXCEPTIONS);
		Assert.assertNotNull(msgs);
		Assert.assertEquals(2, msgs.size());
		{
			final LoggedElement msg = msgs.get(0);
			Assert.assertEquals(Arrays.asList(LoggedElementAttribute.NIVEAU, LoggedElementAttribute.MESSAGE), msg.getItems());

			final Map<LoggedElementAttribute, Object> itemValues = msg.getItemValues();
			Assert.assertEquals(LogLevel.INFO, itemValues.get(LoggedElementAttribute.NIVEAU));
			Assert.assertEquals("42 -> La,Réponse,A,La,Grande,Question,De,L',Univers", itemValues.get(LoggedElementAttribute.MESSAGE));
		}
		{
			final LoggedElement msg = msgs.get(1);
			Assert.assertEquals(Arrays.asList(LoggedElementAttribute.NIVEAU, LoggedElementAttribute.MESSAGE), msg.getItems());

			final Map<LoggedElementAttribute, Object> itemValues = msg.getItemValues();
			Assert.assertEquals(LogLevel.INFO, itemValues.get(LoggedElementAttribute.NIVEAU));
			Assert.assertEquals("1 -> Hein?,Aaaah ok...", itemValues.get(LoggedElementAttribute.MESSAGE));
		}
	}
}
