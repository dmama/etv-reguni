package ch.vd.unireg.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.unireg.tiers.CollectiviteAdministrative;

@Entity
@DiscriminatorValue(value = "EnvoiVersCollAdm")
public class EnvoiDossierVersCollectiviteAdministrative extends EnvoiDossier implements ElementDeBordereau {

	private CollectiviteAdministrative collectiviteAdministrativeDestinataire;

	private BordereauMouvementDossier bordereau;

	public EnvoiDossierVersCollectiviteAdministrative() {
	}

	public EnvoiDossierVersCollectiviteAdministrative(CollectiviteAdministrative destinataire) {
		this.collectiviteAdministrativeDestinataire = destinataire;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "COLL_ADMIN_DEST_ID", foreignKey = @ForeignKey(name = "FK_ENV_DOS_CA_DEST_ID"))
	public CollectiviteAdministrative getCollectiviteAdministrativeDestinataire() {
		return collectiviteAdministrativeDestinataire;
	}

	public void setCollectiviteAdministrativeDestinataire(CollectiviteAdministrative ca) {
		this.collectiviteAdministrativeDestinataire = ca;
	}

	@Override
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "BORDEREAU_ID", insertable = false, updatable = false, nullable = true)
	public BordereauMouvementDossier getBordereau() {
		return bordereau;
	}

	@Override
	public void setBordereau(BordereauMouvementDossier bordereau) {
		this.bordereau = bordereau;
	}
}
