package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;

@Entity
public abstract class DroitProprietePersonneRF extends DroitProprieteRF {

	/**
	 * Si renseigné, la communauté à travers laquelle l'ayant-droit possède le droit de propriété.
	 */
	@Nullable
	private CommunauteRF communaute;

	@Nullable
	@ManyToOne
	@JoinColumn(name = "COMMUNAUTE_ID")
	@ForeignKey(name = "FK_DROIT_RF_COMMUNAUTE_ID")
	@Index(name = "IDX_DROIT_RF_COMM_ID")
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(@Nullable CommunauteRF communaute) {
		this.communaute = communaute;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {

		final List<?> linkedEntities = super.getLinkedEntities(context, includeAnnuled);
		if (communaute == null) {
			return linkedEntities;
		}

		// on ajoute la communauté
		final List<Object> list = new ArrayList<>(linkedEntities);
		list.add(communaute);
		return list;
	}
}
