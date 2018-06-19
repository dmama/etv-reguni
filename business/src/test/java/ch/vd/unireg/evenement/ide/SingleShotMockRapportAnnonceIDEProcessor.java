package ch.vd.unireg.evenement.ide;

import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;

/**
 * @author Raphaël Marmier, 2016-10-07, <raphael.marmier@vd.ch>
 */
public class SingleShotMockRapportAnnonceIDEProcessor implements ReponseIDEProcessor {

	private boolean used = false;

	private AnnonceIDEEnvoyee annonceIDE;

	@Override
	public void traiterReponseAnnonceIDE(AnnonceIDEEnvoyee annonceIDE) throws ReponseIDEProcessorException {
		if (isUsed()) {
			throw new RuntimeException("Une annonce à l'IDE a déjà été traitée avec ce mock processor de test spécialement prévu pour n'en permettre qu'un seul.");
		}
		this.annonceIDE = annonceIDE;
		this.used = true;
	}

	public AnnonceIDEEnvoyee getAnnonceIDE() {
		return annonceIDE;
	}

	public boolean isUsed() {
		return used;
	}
}
