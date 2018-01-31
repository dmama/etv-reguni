package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

@Entity
public abstract class TacheEnvoiDocument extends Tache {

	/**
	 * Type de document (précalculé) à envoyer.
	 */
	private TypeDocument typeDocument;

	// Requis par Hibernate
	public TacheEnvoiDocument() {
	}

	public TacheEnvoiDocument(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative collectivite, TypeDocument typeDocument) {
		super(etat, dateEcheance, contribuable, collectivite);
		this.typeDocument = typeDocument;
	}

	public TacheEnvoiDocument(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, TypeDocument typeDocument) {
		super(etat, dateEcheance, contribuable);
		this.typeDocument = typeDocument;
	}

	@Column(name = "DECL_TYPE_DOC", length = LengthConstants.MODELEDOC_TYPE)
	@Enumerated(EnumType.STRING)
	@Index(name = "IDX_TACHE_TYPE_DOC")
	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}
}
