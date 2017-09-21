package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

@Entity
public abstract class AutreDocumentFiscal extends DocumentFiscal {

	@Transient
	public RegDate getDateEnvoi() {
		throw new UnsupportedOperationException("TODO: Rechercher la date d'envoi dans l'état émis du document.");
	}

	public void setDateEnvoi(RegDate dateEnvoi) {
		throw new UnsupportedOperationException("TODO: Stocker la date d'envoi dans l'état émis du document.");
	}

	@Transient
	public String getCleArchivage() {
		throw new UnsupportedOperationException("TODO: Rechercher la clé dans l'état émis du document.");
	}

	public void setCleArchivage(String cleArchivage) {
		throw new UnsupportedOperationException("TODO: Stocker la clé dans l'état émis du document.");
	}

	@Transient
	public String getCleDocument() {
		throw new UnsupportedOperationException("TODO: Rechercher la clé dans l'état émis du document.");
	}

	public void setCleDocument(String cleDocument) {
		throw new UnsupportedOperationException("TODO: Stocker la clé dans l'état émis du document.");
	}

	@ManyToOne
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DOCFISC_TRS_ID", columnNames = "TIERS_ID")
	public Entreprise getEntreprise() {
		return (Entreprise) getTiers();
	}

	public void setEntreprise(Entreprise entreprise) {
		setTiers(entreprise);
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return getEntreprise() == null ? null : Collections.singletonList(getEntreprise());
	}

	@Transient
	public TypeEtatDocumentFiscal getEtat() {
		return getDernierEtat().getType();
	}

	@Transient
	public Integer getPeriodeFiscale() {
		return Optional.ofNullable(getDateEnvoi())
				.map(RegDate::year)
				.orElse(null);
	}

	@Transient
	@Override
	public boolean isSommable() {
		return false;
	}

	@Transient
	@Override
	public boolean isRappelable() {
		return false;
	}
}
