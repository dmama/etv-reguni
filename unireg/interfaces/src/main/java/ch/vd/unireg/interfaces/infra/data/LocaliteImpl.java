package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.fidor.xml.common.v1.Range;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.fidor.XmlUtils;

public class LocaliteImpl implements Localite, Serializable {

	private static final long serialVersionUID = 1366049941506978578L;
	
	private final Commune commune;
	private final RegDate dateFin;
	private final Integer chiffreComplementaire;
	private final Integer complementNPA;
	private final Integer npa;
	private final Integer noCommune;
	private final Integer noOrdre;
	private final String nomAbregeMajuscule;
	private final String nomAbregeMinuscule;
	private final String nomCompletMajuscule;
	private final String nomCompletMinuscule;
	private final boolean valide;

	public static LocaliteImpl get(ch.vd.infrastructure.model.Localite target) {
		if (target == null) {
			return null;
		}
		return new LocaliteImpl(target);
	}

	public static LocaliteImpl get(PostalLocality target, ServiceInfrastructureRaw serviceInfra) {
		if (target == null) {
			return null;
		}
		return new LocaliteImpl(target, serviceInfra);
	}

	private LocaliteImpl(ch.vd.infrastructure.model.Localite target) {
		this.commune = CommuneImpl.get(target.getCommuneLocalite());
		this.dateFin = RegDateHelper.get(target.getDateFinValidite());
		this.chiffreComplementaire = target.getChiffreComplementaire();
		this.complementNPA = initComplementNPA(target.getComplementNPA());
		this.npa = target.getNPA();
		this.noCommune = target.getNoCommune();
		this.noOrdre = target.getNoOrdre();
		this.nomAbregeMajuscule = target.getNomAbregeMajuscule();
		this.nomAbregeMinuscule = target.getNomAbregeMinuscule();
		this.nomCompletMajuscule = target.getNomCompletMajuscule();
		this.nomCompletMinuscule = target.getNomCompletMinuscule();
		this.valide = target.isValide();
	}

	private LocaliteImpl(PostalLocality target, ServiceInfrastructureRaw serviceInfra) {
		final DateRange rangeLocalite;
		final Range validiteLocalite = target.getValidity();
		if (validiteLocalite == null) {
			rangeLocalite = null;
		}
		else {
			rangeLocalite = new DateRangeHelper.Range(XmlUtils.toRegDate(validiteLocalite.getDateFrom()), XmlUtils.toRegDate(validiteLocalite.getDateTo()));
		}
		this.commune = findCommune(target.getMainMunicipalityId(), rangeLocalite, serviceInfra);
		this.dateFin = rangeLocalite != null ? rangeLocalite.getDateFin() : null;
		this.chiffreComplementaire = null;
		this.complementNPA = StringUtils.isBlank(target.getSwissZipCodeAddOn()) ? null : Integer.parseInt(target.getSwissZipCodeAddOn());
		this.npa = (int) target.getSwissZipCode();
		this.noCommune = target.getMainMunicipalityId();
		this.noOrdre = target.getSwissZipCodeId();
		this.nomAbregeMajuscule = target.getShortName();
		this.nomAbregeMinuscule = target.getShortName();
		this.nomCompletMajuscule = target.getLongName();
		this.nomCompletMinuscule = target.getLongName();
		this.valide = true;
	}

	private static Commune findCommune(int ofs, DateRange rangeLocalite, ServiceInfrastructureRaw serviceInfra) {
		final List<Commune> communes = serviceInfra.getCommuneHistoByNumeroOfs(ofs);
		if (communes == null || communes.isEmpty()) {
			return null;
		}
		else if (communes.size() == 1) {
			return communes.get(0);
		}
		else {
			for (Commune candidate : communes) {
				final DateRange rangeCommune = new DateRangeHelper.Range(candidate.getDateDebutValidite(), candidate.getDateFinValidite());
				if (DateRangeHelper.intersect(rangeCommune, rangeLocalite)) {
					return candidate;
				}
			}
			return null;
		}
	}

	@Override
	public Integer getChiffreComplementaire() {
		return chiffreComplementaire;
	}

	@Override
	public Commune getCommuneLocalite() {
		return commune;
	}

	@Override
	public Integer getComplementNPA() {
		return complementNPA;
	}

	private Integer initComplementNPA(Integer c) {
		if (c == null || c == 0) {	// un complément de 0 signifie pas de complément
			return null;
		}
		else {
			return c;
		}
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public Integer getNPA() {
		return npa;
	}

	@Override
	public Integer getNoCommune() {
		return noCommune;
	}

	@Override
	public Integer getNoOrdre() {
		return noOrdre;
	}

	@Override
	public String getNomAbregeMajuscule() {
		return nomAbregeMajuscule;
	}

	@Override
	public String getNomAbregeMinuscule() {
		return nomAbregeMinuscule;
	}

	@Override
	public String getNomCompletMajuscule() {
		return nomCompletMajuscule;
	}

	@Override
	public String getNomCompletMinuscule() {
		return nomCompletMinuscule;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public String toString() {
		return String.format("LocaliteImpl{npa=%d, noCommune=%d, valide=%s, nomAbregeMinuscule='%s', noOrdre=%d}", npa, noCommune, valide, nomAbregeMinuscule, noOrdre);
	}
}
