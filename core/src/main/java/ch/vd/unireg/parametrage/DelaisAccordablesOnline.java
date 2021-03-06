package ch.vd.unireg.parametrage;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateEntity;

/**
 * Délais accordables pour les demandes de délais online (e-Délai) valables pendant une plage temporelle déterminée.
 */
@Entity
@Table(name = "PARAMETRE_DELAIS_ONLINE", indexes = @Index(name = "IDX_PARAM_PF_DELAI_PERIODE_ID", columnList = "PARAM_PF_DELAI_ID"))
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
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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
	@JoinColumn(name = "PARAM_PF_DELAI_ID", insertable = false, updatable = false, nullable = false)
	public ParametreDemandeDelaisOnline getParent() {
		return parent;
	}

	public void setParent(ParametreDemandeDelaisOnline parent) {
		this.parent = parent;
	}

	/**
	 * Fait une copie du paramètre en l'adaptant pour la période fiscale spécifiée.
	 *
	 * @param periodeFiscale une période fiscale
	 * @return une copie du paramètre courant.
	 */
	@NotNull
	public abstract DelaisAccordablesOnline duplicateFor(int periodeFiscale);
}
