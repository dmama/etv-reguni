package ch.vd.unireg.interfaces.entreprise.data;

import java.util.Date;

import org.jetbrains.annotations.Nullable;

/**
 * @author Raphaël Marmier, 2016-09-30, <raphael.marmier@vd.ch>
 */
public class ProtoAnnonceIDE extends AnnonceIDEData {

	public ProtoAnnonceIDE(TypeAnnonce type, Date dateAnnonce, Utilisateur utilisateur, TypeEtablissementCivil typeEtablissementCivil, Statut statut,
	                       InfoServiceIDEObligEtendues infoServiceIDEObligEtendues) {
		super(type, dateAnnonce, utilisateur, typeEtablissementCivil, statut, infoServiceIDEObligEtendues);
	}

	public ProtoAnnonceIDE(BaseAnnonceIDE modele, @Nullable Statut statut) {
		super(modele, statut);
	}
}
