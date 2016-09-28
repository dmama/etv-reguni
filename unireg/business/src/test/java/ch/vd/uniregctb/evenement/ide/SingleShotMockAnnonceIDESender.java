package ch.vd.uniregctb.evenement.ide;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;

/**
 * Mock sender à un coup
 * @author Raphaël Marmier, 2016-09-14, <raphael.marmier@vd.ch>
 */
public class SingleShotMockAnnonceIDESender implements AnnonceIDESender {

	private boolean used = false;
	private AnnonceIDE annonceEnvoyee;
	private String msgBusinessIdUtilisee;

	@Override
	public void sendEvent(AnnonceIDE annonce, String msgBusinessId) throws AnnonceIDEException {
		if (isUsed()) {
			throw new RuntimeException("Une annonce IDE a déjà été expédiée avec ce mock sender de test spécialement prévu pour n'en permettre qu'une seule.");
		}

		this.annonceEnvoyee = annonce;
		this.msgBusinessIdUtilisee = msgBusinessId;
		used = true;
	}

	public boolean isUsed() {
		return used;
	}

	public AnnonceIDE getAnnonceEnvoyee() {
		return annonceEnvoyee;
	}

	public String getMsgBusinessIdUtilisee() {
		return msgBusinessIdUtilisee;
	}
}
