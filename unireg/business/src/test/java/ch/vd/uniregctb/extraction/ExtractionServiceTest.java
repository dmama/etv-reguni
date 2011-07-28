package ch.vd.uniregctb.extraction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxElement;
import ch.vd.uniregctb.inbox.InboxService;
import ch.vd.uniregctb.inbox.MockInboxService;

@SuppressWarnings({"unchecked"})
public class ExtractionServiceTest extends BusinessTest {

	private InboxService inboxService;
	private ExtractionServiceImpl extractionService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		inboxService = new MockInboxService();

		extractionService = new ExtractionServiceImpl();
		extractionService.setHibernateTemplate(hibernateTemplate);
		extractionService.setTransactionManager(transactionManager);
		extractionService.setInboxService(inboxService);
		extractionService.setExpiration(1);
		extractionService.setThreadPoolSize(1);
		extractionService.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		extractionService.destroy();
		extractionService = null;
		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPlainExtractorEntryPoint() throws Exception {

		final class MyPlainExtractor extends BaseExtractorImpl implements PlainExtractor {

			public static final String REPONSE = "C'est ma réponse";

			@Override
			public ExtractionResult doExtraction() {
				final InputStream in = new ByteArrayInputStream(REPONSE.getBytes());
				return new ExtractionResultOk(in, "text/plain", "tip", wasInterrupted());
			}

			@Override
			public String getExtractionName() {
				return getClass().getSimpleName();
			}
		}

		// si je demande l'exécution d'un extracteur, je dois retrouver mon résultat
		final ExtractionJob job = extractionService.postExtractionQuery("MOI", new MyPlainExtractor());
		Assert.assertEquals("MOI", job.getVisa());
		Assert.assertNotNull(job.getUuid());

		// on attend quoi, une seconde ?
		Thread.sleep(1000L);

		// normalement, on devrait trouver la bonne réponse dans le service d'inbox
		final List<InboxElement> content = inboxService.getInboxContent("MOI");
		Assert.assertNotNull(content);
		Assert.assertEquals("Pas attendu assez longtemps ou réel problème ?", 1, content.size());

		final InboxElement elt = content.get(0);
		Assert.assertNotNull(elt);

		final InboxAttachment attachment = elt.getAttachment();
		Assert.assertNotNull(attachment);
		Assert.assertEquals("text/plain", attachment.getMimeType());
		Assert.assertEquals("tip", attachment.getFilenameRadical());

		final InputStream in = attachment.getContent();
		Assert.assertNotNull(in);
		try {
			final byte[] buffer = new byte[MyPlainExtractor.REPONSE.length() * 3];
			final int read = in.read(buffer);
			Assert.assertEquals(MyPlainExtractor.REPONSE, new String(buffer, 0, read));
		}
		finally {
			in.close();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testBatchExtractorEntryPoint() throws Exception {

		class MyResult implements BatchResults<Long, MyResult> {

			private final List<Long> liste = new LinkedList<Long>();

			@Override
			public void addErrorException(Long element, Exception e) {
			}

			@Override
			public void addAll(MyResult right) {
				liste.addAll(right.liste);
			}
		}

		class MyBatchExtractor extends BaseExtractorImpl implements BatchableExtractor<Long, MyResult> {

			@Override
			public MyResult createRapport(boolean rapportFinal) {
				return new MyResult();
			}

			@Override
			public BatchTransactionTemplate.Behavior getBatchBehavior() {
				return BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE;
			}

			@Override
			public List<Long> buildElementList() {
				final List<Long> liste = new ArrayList<Long>(1000);
				for (long i = 0 ; i < 1000L ; ++i) {
					liste.add(i);
				}
				return liste;
			}

			@Override
			public int getBatchSize() {
				return 100;
			}

			@Override
			public boolean doBatchExtraction(List<Long> batch, MyResult rapport) throws Exception {
				rapport.liste.addAll(batch);
				return true;
			}

			@Override
			public void afterTransactionCommit(MyResult rapportFinal, int percentProgression) {
			}

			@Override
			public InputStream getStreamForExtraction(MyResult rapportFinal) throws IOException {
				final File tempFile = File.createTempFile("testExtractionService", "dmp");
				tempFile.deleteOnExit();
				final FileOutputStream fos = new FileOutputStream(tempFile);
				final ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(rapportFinal.liste);
				out.close();
				return new FileInputStream(tempFile);
			}

			@Override
			public String getMimeType() {
				return "application/octet-stream";
			}

			@Override
			public String getFilenameRadical() {
				return "data";
			}

			@Override
			public String getExtractionName() {
				return getClass().getSimpleName();
			}
		}

		// si je demande l'exécution d'un extracteur, je dois retrouver mon résultat
		final ExtractionJob job = extractionService.postExtractionQuery("MOI", new MyBatchExtractor());
		Assert.assertEquals("MOI", job.getVisa());
		Assert.assertNotNull(job.getUuid());

		// on attend quoi, une seconde ?
		Thread.sleep(1000L);

		// normalement, on devrait trouver la bonne réponse dans le service d'inbox
		final List<InboxElement> content = inboxService.getInboxContent("MOI");
		Assert.assertNotNull(content);
		Assert.assertEquals("Pas attendu assez longtemps ou réel problème ?", 1, content.size());

		final InboxElement elt = content.get(0);
		Assert.assertNotNull(elt);

		final InboxAttachment attachment = elt.getAttachment();
		Assert.assertNotNull(attachment);
		Assert.assertEquals("application/octet-stream", attachment.getMimeType());
		Assert.assertEquals("data", attachment.getFilenameRadical());

		final InputStream in = attachment.getContent();
		Assert.assertNotNull(in);
		try {
			final ObjectInputStream oin = new ObjectInputStream(in);
			final Object array = oin.readObject();
			Assert.assertTrue(array instanceof List);

			final List<Long> liste = (List<Long>) array;
			Assert.assertEquals(1000, liste.size());
			for (int i = 0 ; i < liste.size() ; ++ i) {
				Assert.assertEquals(i, (long) liste.get(i));
			}
		}
		finally {
			in.close();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testBatchParallelExtractorEntryPoint() throws Exception {

		class MyResult implements BatchResults<Long, MyResult> {

			private final List<Long> liste = new LinkedList<Long>();

			@Override
			public void addErrorException(Long element, Exception e) {
			}

			@Override
			public void addAll(MyResult right) {
				liste.addAll(right.liste);
			}
		}

		class MyBatchParallelExtractor extends BaseExtractorImpl implements BatchableParallelExtractor<Long, MyResult> {

			@Override
			public MyResult createRapport(boolean rapportFinal) {
				return new MyResult();
			}

			@Override
			public BatchTransactionTemplate.Behavior getBatchBehavior() {
				return BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE;
			}

			@Override
			public List<Long> buildElementList() {
				final List<Long> liste = new ArrayList<Long>(1000);
				for (long i = 0 ; i < 1000L ; ++i) {
					liste.add(i);
				}
				return liste;
			}

			@Override
			public int getBatchSize() {
				return 100;
			}

			@Override
			public boolean doBatchExtraction(List<Long> batch, MyResult rapport) throws Exception {
				rapport.liste.addAll(batch);
				return true;
			}

			@Override
			public void afterTransactionCommit(MyResult rapportFinal, int percentProgression) {
			}

			@Override
			public int getNbThreads() {
				return 2;
			}

			@Override
			public InputStream getStreamForExtraction(MyResult rapportFinal) throws IOException {
				Collections.sort(rapportFinal.liste);
				final File tempFile = File.createTempFile("testExtractionService", "dmp");
				tempFile.deleteOnExit();
				final FileOutputStream fos = new FileOutputStream(tempFile);
				final ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(rapportFinal.liste);
				out.close();
				return new FileInputStream(tempFile);
			}

			@Override
			public String getMimeType() {
				return "application/octet-stream";
			}

			@Override
			public String getFilenameRadical() {
				return "data";
			}

			@Override
			public String getExtractionName() {
				return getClass().getSimpleName();
			}
		}

		// si je demande l'exécution d'un extracteur, je dois retrouver mon résultat
		final ExtractionJob job = extractionService.postExtractionQuery("MOI", new MyBatchParallelExtractor());
		Assert.assertEquals("MOI", job.getVisa());
		Assert.assertNotNull(job.getUuid());

		// on attend quoi, deux secondes ?
		Thread.sleep(2000L);

		// normalement, on devrait trouver la bonne réponse dans le service d'inbox
		final List<InboxElement> content = inboxService.getInboxContent("MOI");
		Assert.assertNotNull(content);
		Assert.assertEquals("Pas attendu assez longtemps ou réel problème ?", 1, content.size());

		final InboxElement elt = content.get(0);
		Assert.assertNotNull(elt);

		final InboxAttachment attachment = elt.getAttachment();
		Assert.assertNotNull(attachment);
		Assert.assertEquals("application/octet-stream", attachment.getMimeType());
		Assert.assertEquals("data", attachment.getFilenameRadical());

		final InputStream in = attachment.getContent();
		Assert.assertNotNull(in);
		try {
			final ObjectInputStream oin = new ObjectInputStream(in);
			final Object array = oin.readObject();
			Assert.assertTrue(array instanceof List);

			final List<Long> liste = (List<Long>) array;
			Assert.assertEquals(1000, liste.size());
			for (int i = 0 ; i < liste.size() ; ++ i) {
				Assert.assertEquals(i, (long) liste.get(i));
			}
		}
		finally {
			in.close();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInterrupt() throws Exception {

		final class MyInterruptibleExtractor extends BaseExtractorImpl implements PlainExtractor {

			public static final String REPONSE = "C'est ma réponse";

			@Override
			public ExtractionResult doExtraction() throws Exception {
				Thread.sleep(500L);     // 500ms d'attente
				final InputStream in = new ByteArrayInputStream(REPONSE.getBytes());
				return new ExtractionResultOk(in, "text/plain", "tip", wasInterrupted());
			}

			@Override
			public String getExtractionName() {
				return getClass().getSimpleName();
			}
		}

		final ExtractionJob job = extractionService.postExtractionQuery("MOI", new MyInterruptibleExtractor());
		Assert.assertFalse(job.wasInterrupted());

		Thread.sleep(100L);     // on laisse le temps au job de démarrer
		Assert.assertEquals(1, extractionService.getExtractionsEnCours("MOI").size());     // le job n'aurait-il pas encore démarré ?

		extractionService.cancelJob(job);
		Assert.assertTrue(job.wasInterrupted());

		Thread.sleep(800L);    // on laisse le temps au job de se terminer

		Assert.assertFalse(job.isRunning());
		Assert.assertTrue(job.wasInterrupted());

		final InboxElement inboxElt = inboxService.getInboxElement(job.getUuid());
		Assert.assertNotNull(inboxElt);
		Assert.assertTrue(inboxElt.getDescription().contains("interrompu"));
	}
}
