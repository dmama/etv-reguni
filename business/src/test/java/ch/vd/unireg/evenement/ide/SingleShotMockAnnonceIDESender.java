package ch.vd.unireg.evenement.ide;

import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;

/**
 * Mock sender à un coup
 * @author Raphaël Marmier, 2016-09-14, <raphael.marmier@vd.ch>
 */
public class SingleShotMockAnnonceIDESender implements AnnonceIDESender {

	private boolean used = false;
	private AnnonceIDEEnvoyee annonceEnvoyee;
	private String msgBusinessIdUtilisee;

	@Override
	public void sendEvent(AnnonceIDEEnvoyee annonce, String msgBusinessId) throws AnnonceIDEException {
		if (isUsed()) {
			throw new RuntimeException("Une annonce à l'IDE a déjà été expédiée avec ce mock sender de test spécialement prévu pour n'en permettre qu'une seule.");
		}

		this.annonceEnvoyee = annonce;
		this.msgBusinessIdUtilisee = msgBusinessId;
		used = true;
	}

	public boolean isUsed() {
		return used;
	}

	public AnnonceIDEEnvoyee getAnnonceEnvoyee() {
		return annonceEnvoyee;
	}

	public String getMsgBusinessIdUtilisee() {
		return msgBusinessIdUtilisee;
	}
}
