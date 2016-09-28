package ch.vd.uniregctb.evenement.ide;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public class MockAnnonceIDESender implements AnnonceIDESender {

	@Override
	public void sendEvent(AnnonceIDE annonce, String msgBusinessId) throws AnnonceIDEException {
			// Rien à faire
		}
}
