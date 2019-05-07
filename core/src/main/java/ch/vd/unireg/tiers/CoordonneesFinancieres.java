package ch.vd.unireg.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

@Entity
@Table(name = "COORDONNEE_FINANCIERE")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT")),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class CoordonneesFinancieres extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<CoordonneesFinancieres> {

	private Long id;
	/**
	 * Titulaire du compte bancaire ou du compte postal
	 */
	private String titulaire;
	/**
	 * L'identification du compte bancaire ou postal
	 */
	private CompteBancaire compteBancaire;
	private Tiers tiers;

	public CoordonneesFinancieres() {
	}

	@SuppressWarnings("CopyConstructorMissesField")
	public CoordonneesFinancieres(@NotNull CoordonneesFinancieres right) {
		this.titulaire = right.getTitulaire();
		this.compteBancaire = (right.compteBancaire == null ? null : new CompteBancaire(right.compteBancaire));
	}

	public CoordonneesFinancieres(@Nullable String titulaire, @Nullable String iban, @Nullable String bicSwift) {
		if (titulaire == null && iban == null && bicSwift == null) {
			throw new IllegalArgumentException("Tous les paramètres sont nuls.");
		}
		this.titulaire = titulaire;
		this.compteBancaire = new CompteBancaire(iban, bicSwift);
	}

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

	@Column(name = "TITULAIRE", length = LengthConstants.TIERS_PERSONNE)
	public String getTitulaire() {
		return titulaire;
	}

	public void setTitulaire(String theTitulaireCompteBancaire) {
		titulaire = theTitulaireCompteBancaire;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "iban", column = @Column(name = "IBAN", length = LengthConstants.TIERS_NUMCOMPTE)),
			@AttributeOverride(name = "bicSwift", column = @Column(name = "BIC_SWIFT", length = LengthConstants.TIERS_ADRESSEBICSWIFT))
	})
	public CompteBancaire getCompteBancaire() {
		return compteBancaire;
	}

	public void setCompteBancaire(CompteBancaire compteBancaire) {
		this.compteBancaire = compteBancaire;
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})

	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_COORDFIN_TIERS_ID", columnNames = "TIERS_ID")
	@ForeignKey(name = "FK_COORDFIN_TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}

	@Override
	public CoordonneesFinancieres duplicate() {
		return new CoordonneesFinancieres(this);
	}
}
