package ch.vd.unireg.interfaces.efacture.data;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.XmlUtils;

/**
 * Representation interne pour UNIREG de la classe {@link RegistrationRequest} de evd-25
 */
public class Demande {

	private static final String AVS13 = "AVS13";

	/**
	 * Seule la distinction entre inscription et désinscription nous intéresse, pas vrai ?
	 */
	public enum Action {
		INSCRIPTION("Inscription"),
		DESINSCRIPTION("Désinscription");

		private final String description;

		public static Action get(RegistrationMode mode) {
			switch (mode) {
				case DIRECT:
				case STANDARD:
					return INSCRIPTION;
				case UNREGISTER:
					return DESINSCRIPTION;
				default:
					throw new IllegalArgumentException("Mode de registration inconnu : " + mode);
			}
		}

		Action(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	@NotNull
	private static Map<String, String> buildAdditionalData(RegistrationRequest request) {
		Map<String, String> data = null;
		final ch.vd.evd0025.v1.Map additionalData = request.getAdditionalData();
		if (additionalData != null) {
			final List<MapEntry> entries = additionalData.getEntry();
			if (entries != null && entries.size() > 0) {
				data = new HashMap<>(entries.size());
				for (MapEntry entry : entries) {
					data.put(entry.getKey(), entry.getValue());
				}
			}
		}
		if (data == null) {
			data = Collections.emptyMap();
		}
		return data;
	}

	private final String idDemande;
	private final long ctbId;
	private final String email;
	private final RegDate dateDemande;
	private final Action action;
	private final String noAvs;
	private final BigInteger noAdherent;

	public Demande(RegistrationRequest request) {
		this.idDemande = request.getId();
		this.ctbId = Long.parseLong(request.getPayerBusinessId());
		this.dateDemande = XmlUtils.xmlcal2regdate(request.getRegistrationDate());
		this.action = Action.get(request.getRegistrationMode());
		this.email = (this.action == Action.DESINSCRIPTION ? null : request.getEmail());
		this.noAdherent = request.getEBillAccountId();

		final Map<String, String> map = buildAdditionalData(request);
		this.noAvs = map.get(AVS13);
	}

	/**
	 * Pour les tests seulement
	 */
	public Demande(String id, long ctbId, String email, RegDate dateDemande, Action action, String noAvs, BigInteger noAdherent) {
		this.idDemande = id;
		this.ctbId = ctbId;
		this.email = email;
		this.dateDemande = dateDemande;
		this.action = action;
		this.noAvs = noAvs;
		this.noAdherent = noAdherent;
	}

	public String getIdDemande() {
		return idDemande;
	}

	public long getCtbId() {
		return ctbId;
	}

	public String getEmail() {
		return email;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public Action getAction() {
		return action;
	}

	public String getNoAvs() {
		return noAvs;
	}

	public BigInteger getNoAdherent() {
		return noAdherent;
	}
}
