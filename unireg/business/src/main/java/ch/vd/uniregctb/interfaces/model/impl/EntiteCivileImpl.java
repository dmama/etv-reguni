package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.model.CommuneSimple;
import ch.vd.infrastructure.model.Pays;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class EntiteCivileImpl implements EntiteCivile, Serializable {

	private static final long serialVersionUID = 8648205824787291869L;
	
	private Collection<Adresse> adresses;

	/**
	 * [SIFISC-35]
	 * Classe interne de wrapping utilisée pour faire en sorte que les adresses
	 * qui ont une date de fin dans le futur soient considérées comme sans date de fin
	 */
	private static final class AdresseWrapperSansDateFin implements ch.vd.common.model.Adresse {

		private final ch.vd.common.model.Adresse target;

		private AdresseWrapperSansDateFin(ch.vd.common.model.Adresse target) {
			this.target = target;
		}

		@Override
		public String getCasePostale() {
			return target.getCasePostale();
		}

		@Override
		public Date getDateDebutValidite() {
			return target.getDateDebutValidite();
		}

		@Override
		public Date getDateFinValidite() {
			// c'est tout le but de cette classe...
			return null;
		}

		@Override
		public String getLieu() {
			//noinspection deprecation
			return target.getLieu();
		}

		@Override
		public String getLocalite() {
			//noinspection deprecation
			return target.getLocalite();
		}

		@Override
		public String getLocaliteCompletMinuscule() {
			return target.getLocaliteCompletMinuscule();
		}

		@Override
		public String getLocaliteAbregeMinuscule() {
			return target.getLocaliteAbregeMinuscule();
		}

		@Override
		public String getNpa() {
			//noinspection deprecation
			return target.getNpa();
		}

		@Override
		public String getNumero() {
			return target.getNumero();
		}

		@Override
		public int getNumeroOrdrePostal() {
			return target.getNumeroOrdrePostal();
		}

		@Override
		public String getNumeroPostal() {
			return target.getNumeroPostal();
		}

		@Override
		public String getNumeroPostalComplementaire() {
			return target.getNumeroPostalComplementaire();
		}

		@Override
		public Pays getPays() {
			return target.getPays();
		}

		@Override
		public String getRue() {
			return target.getRue();
		}

		@Override
		public Integer getNumeroOfsRue() {
			//noinspection deprecation
			return target.getNumeroOfsRue();
		}

		@Override
		public Integer getNumeroTechniqueRue() {
			return target.getNumeroTechniqueRue();
		}

		@Override
		public String getNumeroAppartement() {
			return target.getNumeroAppartement();
		}

		@Override
		public String getTitre() {
			return target.getTitre();
		}

		@Override
		public EnumTypeAdresse getTypeAdresse() {
			return target.getTypeAdresse();
		}

		@Override
		public CommuneSimple getCommuneAdresse() {
			return target.getCommuneAdresse();
		}

		@Override
		public String getEgid() {
			return target.getEgid();
		}

		@Override
		public String getEwid() {
			return target.getEwid();
		}
	}

	public EntiteCivileImpl(ch.vd.registre.civil.model.EntiteCivile target) {
		this.adresses = new ArrayList<Adresse>();
		if (target.getAdresses() != null) {
			final Date now = DateHelper.getCurrentDate();
			for (Object o : target.getAdresses()) {
				ch.vd.common.model.Adresse a = (ch.vd.common.model.Adresse) o;

				// [SIFISC-35] les adresses civiles qui débutent dans le futur doivent être ignorées, et les adresses
				// qui se terminent dans le futur doivent être considérées comme n'ayant pas de fin
				if (a.getDateDebutValidite() == null || a.getDateDebutValidite().compareTo(now) <= 0) {
					if (a.getDateFinValidite() != null && a.getDateFinValidite().compareTo(now) > 0) {
						a = new AdresseWrapperSansDateFin(a);
					}
					this.adresses.add(AdresseImpl.get(a));
				}
			}
		}
	}

	public EntiteCivileImpl(EntiteCivileImpl right, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			this.adresses = right.adresses;
		}
		else  {
			this.adresses = Collections.emptyList();
		}
	}

	@Override
	public Collection<Adresse> getAdresses() {
		return adresses;
	}

	public void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts) {
		if (parts != null && parts.contains(AttributeIndividu.ADRESSES)) {
			adresses = individu.getAdresses();
		}
	}
}
