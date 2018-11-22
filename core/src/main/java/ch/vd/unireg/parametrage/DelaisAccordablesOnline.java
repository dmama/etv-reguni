package ch.vd.unireg.parametrage;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import ch.vd.unireg.common.HibernateEntity;

/**
 * Délais accordables pour les demandes de délais online (e-Délai) valables pendant une plage temporelle déterminée.
 */
@Entity
@Table(name = "PARAMETRE_DELAIS_ONLINE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DISCRIMINATOR", discriminatorType = DiscriminatorType.STRING)
public abstract class DelaisAccordablesOnline extends HibernateEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	private ParametreDemandeDelaisOnline parent;

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

	// configuration hibernate : le parent possède les plages
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "PARAM_PF_DELAI_ID", insertable = false, updatable = false)
	@Index(name = "IDX_PARAM_PF_DELAI_PERIODE_ID", columnNames = "PARAM_PF_DELAI_ID")
	public ParametreDemandeDelaisOnline getParent() {
		return parent;
	}

	public void setParent(ParametreDemandeDelaisOnline parent) {
		this.parent = parent;
	}
}
