package ch.vd.uniregctb.evenement.identification.contribuable;

/**
 * Implémentation du handler de messages d'identification de contribuable qui ne fait rien.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockIdentificationContribuableMessageHandler implements IdentificationContribuableMessageHandler {

	public void sendReponse(IdentificationContribuable message) throws Exception {
	}

	public void setDemandeHandler(DemandeHandler handler) {
	}
}
