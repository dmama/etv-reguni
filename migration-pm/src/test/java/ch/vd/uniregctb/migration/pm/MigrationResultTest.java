package ch.vd.uniregctb.migration.pm;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class MigrationResultTest {

	@Test
	public void testIntroductionPrefixeDansMessages() throws Exception {
		final MigrationResult base = new MigrationResult();
		final MigrationResultProduction avecPrefixe = base.withMessagePrefix("Mon préfixe");

		// on balance toutes les combinaisons !
		for (MigrationResultMessage.CategorieListe cat : MigrationResultMessage.CategorieListe.values()) {
			for (MigrationResultMessage.Niveau niveau : MigrationResultMessage.Niveau.values()) {
				base.addMessage(cat, niveau, String.format("Message sans préfixe %s/%s", cat, niveau));
				avecPrefixe.addMessage(cat, niveau, String.format("Message avec préfixe %s/%s", cat, niveau));
			}
		}

		// récupération de tous les messages reçus dans une liste linéaire
		final List<MigrationResultMessage> messages = Arrays.stream(MigrationResultMessage.CategorieListe.values())
				.map(base::getMessages)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		Assert.assertNotNull(messages);
		Assert.assertEquals(2 * MigrationResultMessage.Niveau.values().length * MigrationResultMessage.CategorieListe.values().length, messages.size());

		// vérification des messages reçus
		for (int i = 0 ; i < messages.size() ; ++ i) {
			final int indexCat = (i / (2 * MigrationResultMessage.Niveau.values().length)) % MigrationResultMessage.CategorieListe.values().length;
			final int indexNiveau = (i / 2) % MigrationResultMessage.Niveau.values().length;
			final boolean isPrefixExpected = i % 2 == 1;

			final MigrationResultMessage.CategorieListe expectedCat = MigrationResultMessage.CategorieListe.values()[indexCat];
			final MigrationResultMessage.Niveau expectedNiveau = MigrationResultMessage.Niveau.values()[indexNiveau];

			final MigrationResultMessage msg = messages.get(i);
			Assert.assertEquals(expectedNiveau, msg.getNiveau());
			if (isPrefixExpected) {
				Assert.assertEquals(Integer.toString(i), String.format("Mon préfixe : Message avec préfixe %s/%s", expectedCat, expectedNiveau), msg.getTexte());
			}
			else {
				Assert.assertEquals(Integer.toString(i), String.format("Message sans préfixe %s/%s", expectedCat, expectedNiveau), msg.getTexte());
			}
		}
	}

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
		final MigrationResult mr = new MigrationResult();

		// enregistrement de la structure

		mr.registerPreTransactionCommitCallback(MyDataToConsolidate.class,
		                                        1,
		                                        d -> d.key,
		                                        (d1, d2) -> new MyDataToConsolidate(d1.key, String.format("%s,%s", d1.msg, d2.msg)),
		                                        d -> mr.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.INFO, String.format("%d -> %s", d.key, d.msg)));

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

		Assert.assertEquals(0, mr.getMessages(MigrationResultMessage.CategorieListe.GENERIQUE).size());     // pour l'instant, il n'y a rien

		// consolidation

		mr.consolidatePreTransactionCommitRegistrations();

		// validation du résultat

		final List<MigrationResultMessage> msgs = mr.getMessages(MigrationResultMessage.CategorieListe.GENERIQUE);
		Assert.assertNotNull(msgs);
		Assert.assertEquals(2, msgs.size());
		{
			final MigrationResultMessage msg = msgs.get(0);
			Assert.assertEquals(MigrationResultMessage.Niveau.INFO, msg.getNiveau());
			Assert.assertEquals("42 -> La,Réponse,A,La,Grande,Question,De,L',Univers", msg.getTexte());
		}
		{
			final MigrationResultMessage msg = msgs.get(1);
			Assert.assertEquals(MigrationResultMessage.Niveau.INFO, msg.getNiveau());
			Assert.assertEquals("1 -> Hein?,Aaaah ok...", msg.getTexte());
		}
	}
}
