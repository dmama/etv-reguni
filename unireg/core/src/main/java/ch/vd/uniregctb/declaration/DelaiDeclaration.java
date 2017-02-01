package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
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

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;

@Entity
@Table(name = "DELAI_DECLARATION")
public class DelaiDeclaration extends HibernateEntity implements Comparable<DelaiDeclaration>, LinkedEntity {

	private Long id;
	private RegDate dateDemande;
	private RegDate dateTraitement;
	private RegDate delaiAccordeAu;
	private EtatDelaiDeclaration etat;
	private String cleArchivageCourrier;
	private Declaration declaration;
	private boolean sursis;

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

	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "DATE_DEMANDE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate theDateDemande) {
		dateDemande = theDateDemande;
	}

	@Column(name = "DATE_TRAITEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(RegDate theDateTraitement) {
		dateTraitement = theDateTraitement;
	}

	@Column(name = "DELAI_ACCORDE_AU")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate theDelaiAccordeAu) {
		delaiAccordeAu = theDelaiAccordeAu;
	}

	@Column(name = "ETAT", length = LengthConstants.DELAI_DECL_ETAT, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public EtatDelaiDeclaration getEtat() {
		return etat;
	}

	public void setEtat(EtatDelaiDeclaration etat) {
		this.etat = etat;
	}

	@Column(name = "CLE_ARCHIVAGE_COURRIER", length = LengthConstants.CLE_ARCHIVAGE_FOLDERS)
	public String getCleArchivageCourrier() {
		return cleArchivageCourrier;
	}

	public void setCleArchivageCourrier(String cleArchivageCourrier) {
		this.cleArchivageCourrier = cleArchivageCourrier;
	}

	@Column(name = "SURSIS", nullable = false)
	public boolean isSursis() {
		return sursis;
	}

	public void setSursis(boolean sursis) {
		this.sursis = sursis;
	}

	/**
	 * Compare d'apres la date de DelaiDeclaration
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DelaiDeclaration delaiDeclaration) {
		final RegDate autreDelaiAccordeAu = delaiDeclaration.getDelaiAccordeAu();
		return - delaiAccordeAu.compareTo(autreDelaiAccordeAu);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DECLARATION_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DE_DI_DI_ID", columnNames = "DECLARATION_ID")
	public Declaration getDeclaration() {
		return declaration;
	}

	public void setDeclaration(Declaration theDeclaration) {
		declaration = theDeclaration;
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return declaration == null ? null : Collections.singletonList(declaration);
	}

	/**
	 * Comparateur qui trie les délais de déclaration du plus ancien au plus récent.
	 */
	public static class Comparator implements java.util.Comparator<DelaiDeclaration> {
		@Override
		public int compare(DelaiDeclaration o1, DelaiDeclaration o2) {

			final RegDate date1;
			final RegDate date2;
			if (o1.getDelaiAccordeAu() == null || o2.getDelaiAccordeAu() == null) {
				// si l'un des deux éléments est sans délai accordé (= délai en cours ou même refusé),
				// on compare sur les dates de demande
				date1 = o1.getDateDemande();
				date2 = o2.getDateDemande();
			}
			else {
				date1 = o1.getDelaiAccordeAu();
				date2 = o2.getDelaiAccordeAu();
			}

			if (date1 == date2) {
				// s'il y a des délais identiques annulés aux mêmes dates, on mets l'état annulé avant
				return o1.isAnnule() == o2.isAnnule() ? 0 : (o1.isAnnule() ? -1 : 1);
			}

			// de la plus petite à la plus grande
			return date1.compareTo(date2);
		}
	}
}
