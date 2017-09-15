package ch.vd.uniregctb.declaration;

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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uCd8AOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uCd8AOqeEdySTq6PFlf9jQ"
 */
@Entity
public abstract class Declaration extends DocumentFiscal implements DateRange {

	/**
	 * <!-- begin-user-doc -->
	 * <p>
	 * Date de début d'imposition pour la déclaration.
	 * <p>
	 * Dans la majeure partie des cas, cette date est égale au 1er janvier de la période fiscale considérée. Elle peut être différente dans
	 * le cas d'une arrivée en cours d'année (et à ce moment-là elle est égale à la date d'arrivée).
	 * <p>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XJ1FcOqgEdySTq6PFlf9jQ"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc -->
	 * <p>
	 * Date de fin d'imposition pour la déclaration.
	 * <p>
	 * Dans la majeure partie des cas, cette date est égale au 31 décembre de la période fiscale considérée. elle peut être différente dans
	 * le cas d'un départ en cours d'année (et à ce moment-là elle est égale à la date de départ).
	 * <p>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ajGHUOqgEdySTq6PFlf9jQ"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R75A8uqgEdySTq6PFlf9jQ"
	 */
	private PeriodeFiscale periode;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pEXLgS4DEd2H4bonmeBdag"
	 */
	private ModeleDocument modeleDocument;

	@Transient
	public Set<DelaiDeclaration> getDelaisDeclaration() {
		// begin-user-code
		return super.getDelais().stream().map(d -> (DelaiDeclaration)d).collect(Collectors.toSet());
		// end-user-code
	}

	public void setDelaisDeclaration(Set<DelaiDeclaration> theDelais) {
		// begin-user-code
		super.setDelais(new HashSet<>(theDelais));
		// end-user-code
	}

	@Transient
	public Set<EtatDeclaration> getEtatsDeclaration() {
		// begin-user-code
		return super.getEtats().stream().map(e -> (EtatDeclaration)e).collect(Collectors.toSet());
		// end-user-code
	}

	public void setEtatsDeclaration(Set<EtatDeclaration> theEtats) {
		// begin-user-code
		super.setEtats(new HashSet<>(theEtats));
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the periode
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R75A8uqgEdySTq6PFlf9jQ?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch=FetchType.LAZY)
	@JoinColumn(name = "PERIODE_ID")
	@ForeignKey(name = "FK_DOCFISC_PF_ID")
	public PeriodeFiscale getPeriode() {
		// begin-user-code
		return periode;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param thePeriode the periode to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R75A8uqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setPeriode(PeriodeFiscale thePeriode) {
		// begin-user-code
		periode = thePeriode;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateDebut
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XJ1FcOqgEdySTq6PFlf9jQ?GETTER"
	 */
	@Override
	@Column(name = "DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateDebut the dateDebut to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_XJ1FcOqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateFin
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ajGHUOqgEdySTq6PFlf9jQ?GETTER"
	 */
	@Override
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateFin the dateFin to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ajGHUOqgEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the modeleDocument
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pEXLgS4DEd2H4bonmeBdag?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH }, fetch=FetchType.LAZY)
	@JoinColumn(name = "MODELE_DOC_ID")
	@ForeignKey(name = "FK_DOCFISC_MODOC_ID")
	public ModeleDocument getModeleDocument() {
		// begin-user-code
		return modeleDocument;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theModeleDocument the modeleDocument to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pEXLgS4DEd2H4bonmeBdag?SETTER"
	 */
	public void setModeleDocument(ModeleDocument theModeleDocument) {
		// begin-user-code
		modeleDocument = theModeleDocument;
		// end-user-code
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
	 * Repris de {@link ch.vd.uniregctb.common.HibernateDateRangeEntity}, description avec début et fin.
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
}