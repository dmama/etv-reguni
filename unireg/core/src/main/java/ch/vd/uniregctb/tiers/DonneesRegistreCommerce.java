package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

@Entity
@Table(name = "DONNEES_RC")
public class DonneesRegistreCommerce extends HibernateDateRangeEntity implements LinkedEntity {

	private Long id;
	private Entreprise entreprise;
	private String raisonSociale;
	private FormeJuridiqueEntreprise formeJuridique;

	// TODO : code noga, ... ?

	public DonneesRegistreCommerce() {
	}

	public DonneesRegistreCommerce(RegDate dateDebut, RegDate dateFin, String raisonSociale, FormeJuridiqueEntreprise formeJuridique) {
		super(dateDebut, dateFin);
		this.raisonSociale = raisonSociale;
		this.formeJuridique = formeJuridique;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false)
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Column(name = "RAISON_SOCIALE", length = LengthConstants.TIERS_NOM)
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@Column(name = "FORME_JURIDIQUE", length = LengthConstants.PM_FORME)
	@Enumerated(EnumType.STRING)
	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
