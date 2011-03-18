package ch.vd.uniregctb.editique;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.common.StreamUtils;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.editique.impl.EditiqueResultatDocumentImpl;
import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxElement;
import ch.vd.uniregctb.inbox.MockInboxService;

public class RetourImpressionToInboxTriggerTest extends WithoutSpringTest {

	private MockInboxService inboxService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		inboxService = new MockInboxService();
		inboxService.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		inboxService.destroy();
		super.onTearDown();
	}

	private static final String CONTENT = "Et oui c'est elle !";

	private static EditiqueResultatRecu buildResultat(String nomDocument) {
		return new EditiqueResultatDocumentImpl(nomDocument, MimeTypeHelper.MIME_PLAINTEXT, null, CONTENT.getBytes(), System.currentTimeMillis());
	}

	@Test
	public void testTransferToInbox() throws Exception {

		final String visa = "ELLE";
		final String description = "La plus belle...";
		final int hoursUntilExpiration = 4;
		final RetourImpressionToInboxTrigger trigger = new RetourImpressionToInboxTrigger(inboxService, visa, description, hoursUntilExpiration);
		trigger.trigger(buildResultat("TOTO"));

		final List<InboxElement> inboxContent = inboxService.getInboxContent(visa);
		Assert.assertNotNull(inboxContent);
		Assert.assertEquals(1, inboxContent.size());

		final InboxElement elt = inboxContent.get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(description, elt.getDescription());
		Assert.assertEquals("Impression locale", elt.getName());

		final InboxAttachment attachment = elt.getAttachment();
		Assert.assertNotNull(attachment);
		Assert.assertEquals(MimeTypeHelper.MIME_PLAINTEXT, attachment.getMimeType());
		Assert.assertEquals("print", attachment.getFilenameRadical());

		final ByteArrayOutputStream out = new ByteArrayOutputStream(100);
		final InputStream attachmentContent = attachment.getContent();
		try {
			StreamUtils.copy(attachmentContent, out);
		}
		finally {
			out.close();
			attachmentContent.close();
		}

		final String contenuTrouve = out.toString();
		Assert.assertEquals(CONTENT, contenuTrouve);
	}
}
