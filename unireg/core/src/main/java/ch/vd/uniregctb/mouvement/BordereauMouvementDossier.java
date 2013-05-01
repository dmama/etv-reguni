package ch.vd.uniregctb.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;

@Entity
@Table(name = "BORDEREAU_MVT_DOSSIER")
public class BordereauMouvementDossier extends HibernateEntity {

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * Le contenu du bordereau = mouvements de dossiers
	 * (tous les mouvements insérés ici doivent implémenter l'interface ch.vd.uniregctb.mouvement.ElementDeBordereau)
	 */
	private Set<MouvementDossier> contenu;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "BORDEREAU_ID")
	@ForeignKey(name = "FK_MVT_DOSSIER_BORD_ID")
	public Set<MouvementDossier> getContenu() {
		return contenu;
	}

	public void setContenu(Set<MouvementDossier> contenu) {
		this.contenu = contenu;
	}

	/**
	 * Déduit du premier mouvement trouvé dans le contenu
	 */
	@Transient
	public CollectiviteAdministrative getExpediteur() {
		final CollectiviteAdministrative exp;
		if (contenu != null && !contenu.isEmpty()) {
			final MouvementDossier mvt = contenu.iterator().next();
			if (mvt instanceof EnvoiDossier) {
				exp = ((EnvoiDossier) mvt).getCollectiviteAdministrativeEmettrice();
			}
			else if (mvt instanceof ReceptionDossier) {
				exp = ((ReceptionDossier) mvt).getCollectiviteAdministrativeReceptrice();
			}
			else {
				throw new RuntimeException("Type de mouvement inconnu : " + mvt.getClass());
			}
		}
		else {
			exp = null;
		}
		return exp;
	}

	/**
	 * Déduit du premier mouvement trouvé dans le contenu (null dans le cas d'un mouvement vers les archives)
	 */
	@Transient
	public CollectiviteAdministrative getDestinataire() {
		final CollectiviteAdministrative dest;
		if (contenu != null && !contenu.isEmpty()) {
			final MouvementDossier mvt = contenu.iterator().next();
			if (mvt instanceof EnvoiDossierVersCollectiviteAdministrative) {
				dest = ((EnvoiDossierVersCollectiviteAdministrative) mvt).getCollectiviteAdministrativeDestinataire();
			}
			else {
				dest = null;
			}
		}
		else {
			dest = null;
		}
		return dest;
	}

	@Transient
	public int getNombreMouvementsEnvoyes() {
		return contenu != null ? contenu.size() : 0;
	}

	@Transient
	public int getNombreMouvementsRecus() {
		int compteRecus = 0;
		if (contenu != null) {
			for (MouvementDossier mvt : contenu) {
				if (mvt.getEtat() == EtatMouvementDossier.RECU_BORDEREAU) {
					++ compteRecus;
				}
			}
		}
		return compteRecus;
	}
}
