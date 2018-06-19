package ch.vd.unireg.evenement.ide;

import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public class MockAnnonceIDESender implements AnnonceIDESender {

	@Override
	public void sendEvent(AnnonceIDEEnvoyee annonce, String msgBusinessId) throws AnnonceIDEException {
			// Rien à faire
		}
}
