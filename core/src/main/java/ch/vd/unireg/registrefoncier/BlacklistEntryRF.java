package ch.vd.unireg.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;

/**
 * Identifiant d'un élément RF qui doit être ignoré à l'import.
 */
@Entity
@Table(name = "RF_BLACKLIST", uniqueConstraints = @UniqueConstraint(name = "IDX_BLACKLIST_TYPE_ID_RF", columnNames = {"ID_RF", "TYPE_ENTITE"}))
public class BlacklistEntryRF {

	private Long id;
	private TypeEntiteRF type;
	private String idRF;
	private String reason;

	public BlacklistEntryRF() {
	}

	public BlacklistEntryRF(@NotNull TypeEntiteRF type, @NotNull String idRF, @NotNull String reason) {
		this.type = type;
		this.idRF = idRF;
		this.reason = reason;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE_ENTITE", length = LengthConstants.RF_TYPE_ENTITE, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeEntiteRF getType() {
		return type;
	}

	public void setType(TypeEntiteRF type) {
		this.type = type;
	}

	@Column(name = "ID_RF", length = LengthConstants.RF_ID_RF)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@Column(name = "REASON", length = LengthConstants.RF_REASON)
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
