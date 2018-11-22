package ch.vd.unireg.parametrage;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

/**
 * Paramètrage des délais accordables pour les demandes de délais online (e-Délai) pour un type de contribuable et une période fiscale déterminée.
 */
@Entity
@DiscriminatorValue(value = "ONLINE")
public class ParametreDemandeDelaisOnline extends ParametrePeriodeFiscale {

	public enum Type {
		PP,
		PM
	}

	private Type typeTiers;
	private Set<DelaisAccordablesOnline> periodesDelais;

	// nécessaire pour Hibernate
	public ParametreDemandeDelaisOnline() {
	}

	public ParametreDemandeDelaisOnline(ParametreDemandeDelaisOnline right) {
		super(right);
	}

	/**
	 * @return les délais accordables découpés par périodes temporelle déterminées.
	 */
	// configuration hibernate : le paramètre possède les périodes
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "PARAM_PF_DELAI_ID")
	@ForeignKey(name = "FK_PARAM_PF_DELAI_PERIODE_ID")
	public Set<DelaisAccordablesOnline> getPeriodesDelais() {
		return periodesDelais;
	}

	public void setPeriodesDelais(Set<DelaisAccordablesOnline> periodesDelais) {
		this.periodesDelais = periodesDelais;
	}

	public void addPeriodeDelais(@NotNull DelaisAccordablesOnline periode) {
		if (periodesDelais == null) {
			periodesDelais = new HashSet<>();
		}
		periodesDelais.add(periode);
	}

	@Column(name = "TYPE_TIERS", length = 2)
	@Enumerated(value = EnumType.STRING)
	public Type getTypeTiers() {
		return typeTiers;
	}

	public void setTypeTiers(Type typeTiers) {
		this.typeTiers = typeTiers;
	}
}
