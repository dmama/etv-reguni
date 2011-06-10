package ch.vd.uniregctb.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;

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
	@JoinColumn(name = "COLL_ADMIN_DEST_ID")
	@ForeignKey(name = "FK_ENV_DOS_CA_DEST_ID")
	public CollectiviteAdministrative getCollectiviteAdministrativeDestinataire() {
		return collectiviteAdministrativeDestinataire;
	}

	public void setCollectiviteAdministrativeDestinataire(CollectiviteAdministrative ca) {
		this.collectiviteAdministrativeDestinataire = ca;
	}

	@Override
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "BORDEREAU_ID", insertable = false, updatable = false, nullable = true)
	@Index(name = "IDX_MVT_DOSSIER_BORD_ID", columnNames = "BORDEREAU_ID")
	public BordereauMouvementDossier getBordereau() {
		return bordereau;
	}

	@Override
	public void setBordereau(BordereauMouvementDossier bordereau) {
		this.bordereau = bordereau;
	}
}
