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
	public void trigger(EditiqueResultatRecu resultat) throws IOException {
		final InboxAttachment attachment;
		final String descriptionEffective;
		if (resultat instanceof EditiqueResultatDocument) {
			final EditiqueResultatDocument docResultat = (EditiqueResultatDocument) resultat;
			final String mimeType = docResultat.getContentType();
			attachment = new InboxAttachment(mimeType, new ByteArrayInputStream(docResultat.getDocument()), "print");
			descriptionEffective = description;
		}
		else if (resultat instanceof EditiqueResultatErreur) {
			attachment = null;
			descriptionEffective = String.format("%s, Erreur '%s'", description, ((EditiqueResultatErreur) resultat).getError());
		}
		else {
			attachment = null;
			descriptionEffective = String.format("%s, Pas de document", description);
		}
		inboxService.addDocument(visa, "Impression locale", descriptionEffective, attachment, hoursUntilExpiration);
	}

	public String getVisa() {
		return visa;
	}

	public String getDescription() {
		return description;
	}
}
