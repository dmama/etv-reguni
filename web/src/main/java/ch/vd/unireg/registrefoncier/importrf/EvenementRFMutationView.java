package ch.vd.unireg.registrefoncier.importrf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierImportService;
import ch.vd.unireg.supergra.EntityKey;
import ch.vd.unireg.supergra.EntityType;

public class EvenementRFMutationView {

	private final Long id;
	private final EtatEvenementRF etat;
	private final TypeEntiteRF typeEntite;
	private final TypeMutationRF typeMutation;
	private final String idRF;
	@Nullable
	private final EntityKey entityKey;
	private final String xmlContent;
	@Nullable
	private final String errorMessage;
	@Nullable
	private final String callstack;

	public EvenementRFMutationView(@NotNull EvenementRFMutation right, @NotNull RegistreFoncierImportService importServiceRF) {
		this.id = right.getId();
		this.etat = right.getEtat();
		this.typeEntite = right.getTypeEntite();
		this.typeMutation = right.getTypeMutation();
		this.idRF = right.getIdRF();
		this.entityKey = getEntityKey(right.getTypeEntite(), right.getIdRF(), right.getVersionRF(), importServiceRF);
		this.xmlContent = right.getXmlContent();
		this.errorMessage = right.getErrorMessage();
		this.callstack = right.getCallstack();
	}

	@Nullable
	private static EntityKey getEntityKey(@NotNull TypeEntiteRF typeEntiteRF, @NotNull String idRF, @Nullable String versionRF, @NotNull RegistreFoncierImportService importServiceRF) {

		final HibernateEntity entity = importServiceRF.findEntityForMutation(typeEntiteRF, idRF, versionRF);
		if (entity == null) {
			return null;
		}

		final EntityType type;
		final Long id;
		if (entity instanceof AyantDroitRF) {
			type = EntityType.AyantDroitRF;
			id = ((AyantDroitRF) entity).getId();
		}
		else if (entity instanceof DroitRF) {
			type = EntityType.DroitRF;
			id = ((DroitRF) entity).getId();
		}
		else if (entity instanceof ImmeubleRF) {
			type = EntityType.ImmeubleRF;
			id = ((ImmeubleRF) entity).getId();
		}
		else if (entity instanceof BatimentRF) {
			type = EntityType.BatimentRF;
			id = ((BatimentRF) entity).getId();
		}
		else if (entity instanceof CommuneRF) {
			type = EntityType.CommuneRF;
			id = ((CommuneRF) entity).getId();
		}
		else {
			throw new IllegalArgumentException("Type d'entité RF inconnue = [" + entity.getClass().getSimpleName() + "]");
		}

		return new EntityKey(type, id);
	}

	public Long getId() {
		return id;
	}

	public EtatEvenementRF getEtat() {
		return etat;
	}

	public TypeEntiteRF getTypeEntite() {
		return typeEntite;
	}

	public TypeMutationRF getTypeMutation() {
		return typeMutation;
	}

	public String getIdRF() {
		return idRF;
	}

	@Nullable
	public EntityKey getEntityKey() {
		return entityKey;
	}

	public String getXmlContent() {
		return xmlContent;
	}

	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	@Nullable
	public String getCallstack() {
		return callstack;
	}
}
