package ch.vd.uniregctb.evenement.iam;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.evenement.cedi.EvenementCedi;
import ch.vd.uniregctb.type.ModeCommunication;

/**
 * Données utiles à Unireg extraites de l'événement JMS envoyé par le portail IAM suite à l'enregistrement d'un débiteur.
 */
public class InfoEmployeur{



	public static ModeCommunication fromTypeSaisie(String code) {
				if (StringUtils.isBlank(code)) {
					return null;
				}

				if ("EF".equals(code)) {
					return ModeCommunication.ELECTRONIQUE;
				}
				else if ("SL".equals(code)) {
					return ModeCommunication.SITE_WEB;
				}

				return null;
			}


	private long noEmployeur;
	private Long logicielId;
	private ModeCommunication modeCommunication;


	public long getNoEmployeur() {
		return noEmployeur;
	}

	public void setNoEmployeur(long noEmployeur) {
		this.noEmployeur = noEmployeur;
	}

	public Long getLogicielId() {
		return logicielId;
	}

	public void setLogicielId(Long logicielId) {
		this.logicielId = logicielId;
	}

	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	@Override
	public String toString() {
		return "InfoEmployeur{" +
				"noEmployeur=" + noEmployeur +
				", logicielId=" + logicielId +
				", Mode communication=" + modeCommunication +
				'}';
	}
}
