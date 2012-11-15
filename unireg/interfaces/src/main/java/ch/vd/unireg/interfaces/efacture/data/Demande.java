package ch.vd.unireg.interfaces.efacture.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0025.v1.MapEntry;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequest;
import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

/**
 * Representation interne pour UNIREG de la classe {@link RegistrationRequest} de evd-25
 */
public class Demande {

	static final String AVS13 = "AVS13";

	/**
	 * Seule la distinction entre inscription et désinscription nous intéresse, pas vrai ?
	 */
	public static enum Action {
		INSCRIPTION,
		DESINSCRIPTION;

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
	}

	@NotNull
	private static Map<String, String> buildAdditionalData(RegistrationRequest request) {
		Map<String, String> data = null;
		final ch.vd.evd0025.v1.Map additionalData = request.getAdditionalData();
		if (additionalData != null) {
			final List<MapEntry> entries = additionalData.getEntry();
			if (entries != null && entries.size() > 0) {
				data = new HashMap<String, String>(entries.size());
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

	public Demande(RegistrationRequest request) {

		this.idDemande = request.getId();
		this.ctbId = Long.parseLong(request.getPayerBusinessId());
		this.email = request.getEmail();
		this.dateDemande = XmlUtils.xmlcal2regdate(request.getRegistrationDate());
		this.action = Action.get(request.getRegistrationMode());

		final Map<String, String> map = buildAdditionalData(request);
		this.noAvs = map.get(AVS13);

	}

	/**
	 * Effectue les contrôles de cohérence de base pour l'objet
	 *
	 * @return le type de refus pour le contrôle en echec ou null si tout est ok
	 *
	 */
	public TypeRefusDemande performBasicValidation() {
		if (getAction() == Demande.Action.INSCRIPTION) {
			//Check Numéro AVS à 13 chiffres
			if (!AvsHelper.isValidNouveauNumAVS(getNoAvs())) {
				return TypeRefusDemande.NUMERO_AVS_INVALIDE;
			}
			//Check Date et heure de la demande
			if (getDateDemande() == null) {
				return TypeRefusDemande.DATE_DEMANDE_ABSENTE;
			}
		}
		return null;
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


}
