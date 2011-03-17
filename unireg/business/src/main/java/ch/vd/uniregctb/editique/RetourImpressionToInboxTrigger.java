package ch.vd.uniregctb.editique;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxService;

/**
 * Trigger utilisé pour envoyer un retour d'impression un peu lent vers l'inbox du demandeur
 */
public class RetourImpressionToInboxTrigger implements RetourImpressionTrigger {

	private final InboxService inboxService;
	private final String visa;
	private final String description;
	private final int hoursUntilExpiration;

	public RetourImpressionToInboxTrigger(InboxService inboxService, String visa, String description, int hoursUntilExpiration) {
		this.inboxService = inboxService;
		this.visa = visa;
		this.description = description;
		this.hoursUntilExpiration = hoursUntilExpiration;
	}

	@Override
	public void trigger(EditiqueResultat resultat) throws IOException {
		final String mimeType = resultat.getContentType();
		final InboxAttachment attachment = new InboxAttachment(mimeType, new ByteArrayInputStream(resultat.getDocument()), "print");
		inboxService.addDocument(visa, "Impression locale", description, attachment, hoursUntilExpiration);
	}
}
