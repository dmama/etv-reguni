package ch.vd.unireg.documentfiscal;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

@Entity
public abstract class AutreDocumentFiscal extends DocumentFiscal {

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public RegDate getDateEnvoi() {
		return Optional.ofNullable(getEtatEmis())
				.map(EtatAutreDocumentFiscalEmis::getDateObtention)
				.orElse(null);
	}

	public void setDateEnvoi(RegDate dateEnvoi) {
		final EtatDocumentFiscal etat = getDernierEtatOfType(TypeEtatDocumentFiscal.EMIS);
		if (etat == null) {
			addEtat(new EtatAutreDocumentFiscalEmis(dateEnvoi));
		}
		else {
			etat.setDateObtention(dateEnvoi);
		}
	}

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public String getCleArchivage() {
		return Optional.ofNullable(getEtatEmis())
				.map(EtatAutreDocumentFiscalEmis::getCleArchivage)
				.orElse(null);
	}

	public void setCleArchivage(String cleArchivage) {
		sauverCleArchivage(TypeEtatDocumentFiscal.EMIS, cleArchivage);
	}

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public String getCleDocument() {
		return Optional.ofNullable(getEtatEmis())
				.map(EtatAutreDocumentFiscalEmis::getCleDocument)
				.orElse(null);
	}

	public void setCleDocument(String cleDocument) {
		sauverCleDocument(TypeEtatDocumentFiscal.EMIS, cleDocument);
	}

	/**
	 * Sauve la clé d'archivage sur le dernier état du type demandé pour le document. Vérifie la présence de l'état qui doit nécessairement avoir été créé par
	 * ailleur. Vérifie l'absence de clé d'archivage dans l'état trouvé, car pas d'impression de document sans transition d'état.
	 */
	protected <E extends EtatDocumentFiscalAvecDocumentArchive> void sauverCleArchivage(TypeEtatDocumentFiscal type, String cleArchivage) {
		final EtatDocumentFiscalAvecDocumentArchive etat = (EtatDocumentFiscalAvecDocumentArchive) getDernierEtatOfType(type);
		if (etat == null) {
			throw new ProgrammingException(String.format("L'état du document n°%s introuvable! Document fiscal incomplet.", getId()));
		}
		if (etat.getCleArchivage() != null) {
			throw new ProgrammingException(String.format("Une clé d'archivage est déjà présente dans le dernier état du document fiscal n°%s! ", getId()));
		}
		etat.setCleArchivage(cleArchivage);
	}

	/**
	 * Sauve la clé de document (Repelec) sur le dernier état du type demandé pour le document. Vérifie la présence de l'état qui doit nécessairement avoir été créé par
	 * ailleur. Vérifie l'absence de clé de document dans l'état trouvé, car pas d'impression de document sans transition d'état.
	 */
	protected <E extends EtatDocumentFiscalAvecDocumentArchive> void sauverCleDocument(TypeEtatDocumentFiscal type, String cleDocument) {
		final EtatDocumentFiscalAvecDocumentArchive etat = (EtatDocumentFiscalAvecDocumentArchive) getDernierEtatOfType(type);
		if (etat == null) {
			throw new ProgrammingException(String.format("L'état %s du document n°%s introuvable! Document fiscal incomplet.", type, getId()));
		}
		if (etat.getCleDocument() != null) {
			throw new ProgrammingException(String.format("Une clé de document est déjà présente dans le dernier état %s du document fiscal n°%s! ", type, getId()));
		}
		etat.setCleDocument(cleDocument);
	}

	@ManyToOne
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	public Entreprise getEntreprise() {
		return (Entreprise) getTiers();
	}

	public void setEntreprise(Entreprise entreprise) {
		setTiers(entreprise);
	}

	@Transient
	@Nullable
	protected EtatAutreDocumentFiscalEmis getEtatEmis() {
		final List<EtatDocumentFiscal> etatsEmis = getEtatsOfType(TypeEtatDocumentFiscal.EMIS, false);
		if (etatsEmis.isEmpty()) {
			return null;
		}
		else {
			return (EtatAutreDocumentFiscalEmis) etatsEmis.get(0);
		}
	}

	@Transient
	@Nullable
	protected EtatAutreDocumentFiscalRappele getEtatRappele() {
		final List<EtatDocumentFiscal> etatsRappele = getEtatsOfType(TypeEtatDocumentFiscal.RAPPELE, false);
		if (etatsRappele.isEmpty()) {
			return null;
		}
		else {
			return (EtatAutreDocumentFiscalRappele) etatsRappele.get(0);
		}
	}

	@Transient
	@Nullable
	protected EtatAutreDocumentFiscalRetourne getEtatRetourne() {
		final List<EtatDocumentFiscal> etatsRappele = getEtatsOfType(TypeEtatDocumentFiscal.RETOURNE, false);
		if (etatsRappele.isEmpty()) {
			return null;
		}
		else {
			return (EtatAutreDocumentFiscalRetourne) etatsRappele.get(0);
		}
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return getEntreprise() == null ? null : Collections.singletonList(getEntreprise());
	}

	@Transient
	public TypeEtatDocumentFiscal getEtat() {
		final EtatDocumentFiscal dernierEtat = getDernierEtat();
		return dernierEtat != null ? dernierEtat.getType() : null;
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

	@Transient
	@Override
	@Nullable
	public Integer getAnneePeriodeFiscale() {
		return this.getPeriodeFiscale();
	}
}
