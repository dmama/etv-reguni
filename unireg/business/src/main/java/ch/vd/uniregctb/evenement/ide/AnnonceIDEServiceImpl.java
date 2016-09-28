package ch.vd.uniregctb.evenement.ide;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.ModeleAnnonceIDE;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Etablissement;

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
	public AnnonceIDERCEnt emettreAnnonceIDE(ModeleAnnonceIDE modele, Etablissement etablissement) {

		Assert.notNull(modele, "Le modèle de la demande d'annonce doit être fournie.");
		Assert.notNull(etablissement, "L'établissement concerné par l'annonce doit être fournie.");

		final ReferenceAnnonceIDE tmpReferenceAnnonceIDE = new ReferenceAnnonceIDE();
		tmpReferenceAnnonceIDE.setEtablissement(etablissement);

		// sauver la référence de demande d'annonce et obtenir l'entité
		final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.save(tmpReferenceAnnonceIDE);

		// Créer la véritable annonce, avec son numéro cette fois.
		final AnnonceIDERCEnt annonceIDE = new AnnonceIDERCEnt(referenceAnnonceIDE.getId(), modele, null);

		// Générer le businessId et le sauver avec la référence
		final String msgBusinessId = generateBusinessId(referenceAnnonceIDE);
		referenceAnnonceIDE.setMsgBusinessId(msgBusinessId);

		// publication de la demande d'annonce
		try {
			annonceIDESender.sendEvent(annonceIDE, msgBusinessId);
		}
		catch (AnnonceIDEException e) {
			throw new RuntimeException(
					String.format("Erreur survenue lors de la demande d'annonce %d à l'IDE pour l'etablissement %s.", annonceIDE.getNumero(), FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero())),
					e);
		}

		return annonceIDE;
	}

	@NotNull
	private static String generateBusinessId(ReferenceAnnonceIDE referenceAnnonce) {
		return "unireg-req-" + referenceAnnonce.getId() + "-" + DateHelper.getCurrentDate().getTime();
	}

}
