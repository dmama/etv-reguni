package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.vd.fidor.xml.common.v1.Range;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.fidor.XmlUtils;

public class LocaliteImpl implements Localite, Serializable {

	private static final long serialVersionUID = 7217770195343327701L;
	
	private final Commune commune;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Integer chiffreComplementaire;
	private final Integer complementNPA;
	private final Integer npa;
	private final Integer noCommune;
	private final Integer noOrdre;
	private final String nomAbrege;
	private final String nomComplet;

	public static LocaliteImpl get(PostalLocality target, Map<Integer, List<Commune>> communesByOfsId) {
		if (target == null) {
			return null;
		}
		return new LocaliteImpl(target, communesByOfsId);
	}

	private LocaliteImpl(PostalLocality target, Map<Integer, List<Commune>> communesByOfsId) {
		final DateRange rangeLocalite;
		final Range validiteLocalite = target.getValidity();
		if (validiteLocalite == null) {
			rangeLocalite = null;
		}
		else {
			rangeLocalite = new DateRangeHelper.Range(XmlUtils.toRegDate(validiteLocalite.getDateFrom()), XmlUtils.toRegDate(validiteLocalite.getDateTo()));
		}
		this.commune = findCommune(target.getMainMunicipalityId(), rangeLocalite, communesByOfsId.get(target.getMainMunicipalityId()));
		this.dateDebut = rangeLocalite != null ? rangeLocalite.getDateDebut() : null;
		this.dateFin = rangeLocalite != null ? rangeLocalite.getDateFin() : null;
		this.chiffreComplementaire = null;
		this.complementNPA = StringUtils.isBlank(target.getSwissZipCodeAddOn()) ? null : Integer.parseInt(target.getSwissZipCodeAddOn());
		this.npa = (int) target.getSwissZipCode();
		this.noCommune = target.getMainMunicipalityId();
		this.noOrdre = target.getSwissZipCodeId();
		this.nomAbrege = target.getShortName();
		this.nomComplet = target.getLongName();
	}

	private static Commune findCommune(int ofs, DateRange rangeLocalite, List<Commune> communes) {
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
	public String getNomAbrege() {
		return nomAbrege;
	}

	@Override
	public String getNom() {
		return nomComplet;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String toString() {
		return String.format("LocaliteImpl{npa=%d, noCommune=%d, validite=%s, nomAbrege='%s', noOrdre=%d}", npa, noCommune, DateRangeHelper.toDisplayString(dateDebut, dateFin), nomAbrege, noOrdre);
	}
}
