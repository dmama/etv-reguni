package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.EntityKey;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

@Entity
public abstract class RepresentationLegale extends RapportEntreTiers {

	private static final String PUPILLE = "pupille";

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

	@Column(name = "TIERS_TUTEUR_ID")
	@Index(name = "IDX_RET_TRS_TUT_ID", columnNames = "TIERS_TUTEUR_ID")
	@ForeignKey(name = "FK_RET_TRS_TUT_ID")
	public Long getAutoriteTutelaireId() {
		return autoriteTutelaireId;
	}

	public void setAutoriteTutelaireId(Long autoriteTutelaireId) {
		this.autoriteTutelaireId = autoriteTutelaireId;
	}

	public void setAutoriteTutelaire(Tiers theAutoriteTutelaire) {
		autoriteTutelaireId = (theAutoriteTutelaire == null ? null : theAutoriteTutelaire.getId());
	}

	@SuppressWarnings({"unchecked"})
	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {

		if (!includeAnnuled && isAnnule()) {
			return null;
		}

		List list = super.getLinkedEntities(context, includeAnnuled);
		if (autoriteTutelaireId != null) {
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(new EntityKey(Tiers.class, autoriteTutelaireId));
		}
		
		return list;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return PUPILLE;
	}
}