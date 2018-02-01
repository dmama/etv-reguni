package ch.vd.unireg.evenement.identification.contribuable;

/**
 * Implémentation du handler de messages d'identification de contribuable qui ne fait rien.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockIdentificationContribuableMessageHandler implements IdentificationContribuableMessageHandler {

	@Override
	public void sendReponse(IdentificationContribuable message) throws Exception {
	}
}
