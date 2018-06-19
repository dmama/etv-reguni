package ch.vd.unireg.evenement.ide;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public class AnnonceIDEServiceImpl implements AnnonceIDEService {

	private ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO;
	private AnnonceIDESender annonceIDESender;

	public void setReferenceAnnonceIDEDAO(ReferenceAnnonceIDEDAO referenceAnnonceIDEDAO) {
		this.referenceAnnonceIDEDAO = referenceAnnonceIDEDAO;
	}

	public void setAnnonceIDESender(AnnonceIDESender annonceIDESender) {
		this.annonceIDESender = annonceIDESender;
	}

	@Override
	public AnnonceIDE emettreAnnonceIDE(BaseAnnonceIDE proto, Etablissement etablissement) throws AnnonceIDEException {

		if (proto == null) {
			throw new IllegalArgumentException("Le prototype de la demande d'annonce doit être fournie.");
		}
		if (etablissement == null) {
			throw new IllegalArgumentException("L'établissement concerné par l'annonce doit être fournie.");
		}

		final ReferenceAnnonceIDE tmpReferenceAnnonceIDE = new ReferenceAnnonceIDE();
		tmpReferenceAnnonceIDE.setEtablissement(etablissement);

		// sauver la référence de demande d'annonce et obtenir l'entité
		final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.save(tmpReferenceAnnonceIDE);

		// Créer la véritable annonce, avec son numéro cette fois.
		final AnnonceIDE annonceIDE = new AnnonceIDE(referenceAnnonceIDE.getId(), proto, null);

		// Générer le businessId et le sauver avec la référence
		final String msgBusinessId = generateBusinessId(referenceAnnonceIDE);
		referenceAnnonceIDE.setMsgBusinessId(msgBusinessId);

		// publication de la demande d'annonce
		annonceIDESender.sendEvent(annonceIDE, msgBusinessId);

		return annonceIDE;
	}

	@NotNull
	private static String generateBusinessId(ReferenceAnnonceIDE referenceAnnonce) {
		return "unireg-req-" + referenceAnnonce.getId() + "-" + DateHelper.getCurrentDate().getTime();
	}

}
