package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * @author jec
 */
@Entity
public abstract class Declaration extends DocumentFiscal implements DateRange {

	/**
	 * <p>
	 * Date de début d'imposition pour la déclaration.
	 * <p>
	 * Dans la majeure partie des cas, cette date est égale au 1er janvier de la période fiscale considérée. Elle peut être différente dans
	 * le cas d'une arrivée en cours d'année (et à ce moment-là elle est égale à la date d'arrivée).
	 * <p>
	 */
	private RegDate dateDebut;

	/**
	 * <p>
	 * Date de fin d'imposition pour la déclaration.
	 * <p>
	 * Dans la majeure partie des cas, cette date est égale au 31 décembre de la période fiscale considérée. elle peut être différente dans
	 * le cas d'un départ en cours d'année (et à ce moment-là elle est égale à la date de départ).
	 * <p>
	 */
	private RegDate dateFin;
	private PeriodeFiscale periode;
	private ModeleDocument modeleDocument;

	@Transient
	public Set<DelaiDeclaration> getDelaisDeclaration() {
		return super.getDelais().stream().map(d -> (DelaiDeclaration)d).collect(Collectors.toSet());
	}

	public void setDelaisDeclaration(Set<DelaiDeclaration> theDelais) {
		super.setDelais(new HashSet<>(theDelais));
	}

	@Transient
	public Set<EtatDeclaration> getEtatsDeclaration() {
		return super.getEtats().stream().map(e -> (EtatDeclaration)e).collect(Collectors.toSet());
	}

	public void setEtatsDeclaration(Set<EtatDeclaration> theEtats) {
		super.setEtats(new HashSet<>(theEtats));
	}

	/**
	 * @return the periode
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch=FetchType.LAZY)
	@JoinColumn(name = "PERIODE_ID")
	@ForeignKey(name = "FK_DOCFISC_PF_ID")
	public PeriodeFiscale getPeriode() {
		return periode;
	}

	/**
	 * @param thePeriode the periode to set
	 */
	public void setPeriode(PeriodeFiscale thePeriode) {
		periode = thePeriode;
	}

	/**
	 * @return the dateDebut
	 */
	@Override
	@Column(name = "DATE_DEBUT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	/**
	 * @param theDateDebut the dateDebut to set
	 */
	public void setDateDebut(RegDate theDateDebut) {
		dateDebut = theDateDebut;
	}

	/**
	 * @return the dateFin
	 */
	@Override
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	/**
	 * @param theDateFin the dateFin to set
	 */
	public void setDateFin(RegDate theDateFin) {
		dateFin = theDateFin;
	}

	/**
	 * @return the modeleDocument
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch=FetchType.LAZY)
	@JoinColumn(name = "MODELE_DOC_ID")
	@ForeignKey(name = "FK_DOCFISC_MODOC_ID")
	public ModeleDocument getModeleDocument() {
		return modeleDocument;
	}

	/**
	 * @param theModeleDocument the modeleDocument to set
	 */
	public void setModeleDocument(ModeleDocument theModeleDocument) {
		modeleDocument = theModeleDocument;
	}

	public void addEtatDeclaration(EtatDeclaration etat) {
		super.addEtat(etat);
	}

	public void addDelaiDeclaration(DelaiDeclaration delai) {
		super.addDelai(delai);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * Repris de {@link ch.vd.unireg.common.HibernateDateRangeEntity}, description avec début et fin.
	 */
	@Override
	public String toString() {
		final String dateDebutStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateDebut), "?");
		final String dateFinStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?");
		return String.format("%s (%s - %s)", getBusinessName(), dateDebutStr, dateFinStr);
	}

	@Transient
	public EtatDeclaration getDernierEtatDeclaration() {
		return (EtatDeclaration) getDernierEtat();
	}

	@Transient
	public EtatDeclaration getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal type) {
		return (EtatDeclaration) getDernierEtatOfType(type);
	}

	@Transient
	public List<EtatDeclaration> getEtatsDeclarationSorted() {
		final List<EtatDocumentFiscal> etatsSorted = getEtatsSorted();
		return etatsSorted == null ? null : etatsSorted.stream().map(e -> (EtatDeclaration) e).collect(Collectors.toList());
	}

	@NotNull
	@Transient
	public List<EtatDeclaration> getEtatsDeclarationOfType(TypeEtatDocumentFiscal type, boolean withCanceled) {
		return getEtatsOfType(type, withCanceled).stream().map(e -> (EtatDeclaration) e).collect(Collectors.toList());
	}

	@Transient
	public List<DelaiDeclaration> getDelaisDeclarationSorted() {
		return getDelaisSorted() == null ? null : getDelaisSorted().stream().map(d -> (DelaiDeclaration) d).collect(Collectors.toList());
	}

	@Transient
	public DelaiDeclaration getDernierDelaiDeclarationAccorde() {
		return (DelaiDeclaration) getDernierDelaiAccorde();
	}

	@Transient
	@Override
	@Nullable
	public Integer getAnneePeriodeFiscale() {
		return this.getPeriode() == null ? null : this.getPeriode().getAnnee();
	}

}