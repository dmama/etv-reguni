/**
 *
 */
package ch.vd.uniregctb.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;

/**
 * Classe de base pour les mouvements de dossier de type envoi
 */
@Entity
public abstract class EnvoiDossier extends MouvementDossier {

	private static final long serialVersionUID = -5706327466542251428L;

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
