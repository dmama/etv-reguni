/**
 *
 */
package ch.vd.uniregctb.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.type.Localisation;

/**
 * Classe de base des mouvements de dossiers de type "réception"
 */
@Entity
public abstract class ReceptionDossier extends MouvementDossier {

	private static final long serialVersionUID = 1869381562928472165L;

	private CollectiviteAdministrative collectiviteAdministrativeReceptrice;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "COLL_ADMIN_RECEPTRICE_ID")
	@ForeignKey(name = "FK_REC_DOS_CA_ID")
	public CollectiviteAdministrative getCollectiviteAdministrativeReceptrice() {
		return collectiviteAdministrativeReceptrice;
	}

	public void setCollectiviteAdministrativeReceptrice(CollectiviteAdministrative collectiviteAdministrativeReceptrice) {
		this.collectiviteAdministrativeReceptrice = collectiviteAdministrativeReceptrice;
	}

	@Transient
	public abstract Localisation getLocalisation();
}
