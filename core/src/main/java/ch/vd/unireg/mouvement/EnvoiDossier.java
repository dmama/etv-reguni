/**
 *
 */
package ch.vd.unireg.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.unireg.tiers.CollectiviteAdministrative;

/**
 * Classe de base pour les mouvements de dossiers de type envoi
 */
@Entity
public abstract class EnvoiDossier extends MouvementDossier {

	private CollectiviteAdministrative collectiviteAdministrativeEmettrice;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "COLL_ADMIN_EMETTRICE_ID")
	@ForeignKey(name = "FK_ENV_DOS_CA_EMETT_ID")
	public CollectiviteAdministrative getCollectiviteAdministrativeEmettrice() {
		return collectiviteAdministrativeEmettrice;
	}

	public void setCollectiviteAdministrativeEmettrice(CollectiviteAdministrative ca) {
		collectiviteAdministrativeEmettrice = ca;
	}
}
