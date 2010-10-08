/**
 *
 */
package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.EntityKey;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BFZjYBFMEd2nzO4G1YQacw"
 */
@Entity
public abstract class RepresentationLegale extends RapportEntreTiers {

	private static final long serialVersionUID = -8038494520739506010L;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_J4Dr0RFMEd2nzO4G1YQacw"
	 */
	private Long autoriteTutelaireId;

	protected RepresentationLegale() {
		// vide, nécessaire pour la persistence
	}

	protected RepresentationLegale(RepresentationLegale representationLegale) {
		super(representationLegale);
		this.autoriteTutelaireId = representationLegale.autoriteTutelaireId;
	}

	public RepresentationLegale(RegDate dateDebut, RegDate dateFin, PersonnePhysique sujet, Tiers repesentant, CollectiviteAdministrative autoriteTutelaire) {
		super(dateDebut, dateFin, sujet, repesentant);
		this.autoriteTutelaireId = (autoriteTutelaire == null ? null : autoriteTutelaire.getId());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the autoriteTutelaire
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_J4Dr0RFMEd2nzO4G1YQacw?GETTER"
	 */
	@Column(name = "TIERS_TUTEUR_ID")
	@Index(name = "IDX_RET_TRS_TUT_ID", columnNames = "TIERS_TUTEUR_ID")
	@ForeignKey(name = "FK_RET_TRS_TUT_ID")
	public Long getAutoriteTutelaireId() {
		// begin-user-code
		return autoriteTutelaireId;
		// end-user-code
	}

	public void setAutoriteTutelaireId(Long autoriteTutelaireId) {
		this.autoriteTutelaireId = autoriteTutelaireId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theAutoriteTutelaire the autoriteTutelaire to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_J4Dr0RFMEd2nzO4G1YQacw?SETTER"
	 */
	public void setAutoriteTutelaire(Tiers theAutoriteTutelaire) {
		// begin-user-code
		autoriteTutelaireId = (theAutoriteTutelaire == null ? null : theAutoriteTutelaire.getId());
		// end-user-code
	}

	@SuppressWarnings({"unchecked"})
	@Override
	@Transient
	public List<?> getLinkedEntities() {
		final List list = super.getLinkedEntities();
		if (autoriteTutelaireId != null) {
			list.add(new EntityKey(Tiers.class, autoriteTutelaireId));
		}
		return list;
	}
}