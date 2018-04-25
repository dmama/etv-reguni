package ch.vd.unireg.mandataire;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.type.TypeMandat;

public class MandatView {

	private final Long idMandat;
	private final Long idAdresse;
	private final Long idTiersMandataire;
	private final TypeMandat typeMandat;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final boolean withCopy;
	private final String iban;
	private final String personneContact;
	private final String noTelContact;
	private final String libelleGenreImpot;

	public MandatView(Mandat mandat, ServiceInfrastructureService infraService) {
		this.idMandat = mandat.getId();
		this.idAdresse = null;
		this.idTiersMandataire = mandat.getObjetId();
		this.typeMandat = mandat.getTypeMandat();
		this.dateDebut = mandat.getDateDebut();
		this.dateFin = mandat.getDateFin();
		this.withCopy = mandat.getWithCopy() != null && mandat.getWithCopy();

		final CompteBancaire cf = mandat.getCompteBancaire();
		if (cf != null) {
			this.iban = IbanHelper.normalize(cf.getIban());
		}
		else {
			this.iban = null;
		}

		this.personneContact = mandat.getPersonneContact();
		this.noTelContact = mandat.getNoTelephoneContact();
		this.libelleGenreImpot = findLibelleGenreImpot(mandat.getCodeGenreImpot(), infraService);
	}

	public MandatView(AdresseMandataire mandat, ServiceInfrastructureService infraService) {
		this.idMandat = null;
		this.idAdresse = mandat.getId();
		this.idTiersMandataire = null;
		this.typeMandat = mandat.getTypeMandat();
		this.dateDebut = mandat.getDateDebut();
		this.dateFin = mandat.getDateFin();
		this.withCopy = mandat.isWithCopy();
		this.iban = null;
		this.personneContact = mandat.getPersonneContact();
		this.noTelContact = mandat.getNoTelephoneContact();
		this.libelleGenreImpot = findLibelleGenreImpot(mandat.getCodeGenreImpot(), infraService);
	}

	@Nullable
	private static String findLibelleGenreImpot(String codeGenreImpot, ServiceInfrastructureService infraService) {
		if (StringUtils.isNotBlank(codeGenreImpot)) {
			final List<GenreImpotMandataire> all = infraService.getGenresImpotMandataires();
			for (GenreImpotMandataire gi : all) {
				if (codeGenreImpot.equals(gi.getCode())) {
					return gi.getLibelle();
				}
			}
		}
		return null;
	}

	public Long getIdMandat() {
		return idMandat;
	}

	public Long getIdAdresse() {
		return idAdresse;
	}

	public Long getIdTiersMandataire() {
		return idTiersMandataire;
	}

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public boolean isWithCopy() {
		return withCopy;
	}

	public String getIban() {
		return iban;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public String getNoTelContact() {
		return noTelContact;
	}

	public String getLibelleGenreImpot() {
		return libelleGenreImpot;
	}
}
