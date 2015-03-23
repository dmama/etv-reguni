package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.CasePostale;

/**
 * Données minimale d'une adresse tiers <b>annulée</b> ([UNIREG-3154][SIFISC-5319]).
 * <p/>
 * Cette adresse est utilisé à la place de l'adresse complète lorsque la résolution de l'adresse est impossible (à cause de cycles, par exemple).
 */
public class AdresseTiersAnnuleeResolutionExceptionStub implements AdresseGenerique {

	private final AdresseTiers target;
	private final Source source;

	public AdresseTiersAnnuleeResolutionExceptionStub(AdresseTiers target) {
		Assert.isTrue(target.isAnnule()); // si l'adresse autre tiers n'est pas annulée, l'exception de résolution des adresses doit être remontée !
		this.target = target;
		this.source = new Source(SourceType.FISCALE, null);
	}

	@Override
	public Long getId() {
		return target.getId();
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	@Override
	public CasePostale getCasePostale() {
		return null;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		// les adresses annulées ne doivent pas être considérées comme valides
		return !isAnnule() && target.isValidAt(date);
	}

	@Override
	public RegDate getDateDebut() {
		return target.getDateDebut();
	}

	@Override
	public RegDate getDateFin() {
		return target.getDateFin();
	}

	@Override
	public String getLocalite() {
		return "*** adresse résolution exception ***";
	}

	@Override
	public String getLocaliteComplete() {
		return null;
	}

	@Override
	public String getNumero() {
		return null;
	}

	@Override
	public Integer getNumeroOrdrePostal() {
		return 0;
	}

	@Override
	public String getNumeroPostal() {
		return null;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return null;
	}

	@Override
	public Integer getNoOfsPays() {
		return null;
	}

	@Override
	public String getRue() {
		return null;
	}

	@Override
	public Integer getNumeroRue() {
		return null;
	}

	@Override
	public String getNumeroAppartement() {
		return null;
	}

	@Override
	public String getComplement() {
		return null;
	}

	@Override
	public boolean isAnnule() {
		return true;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return null;
	}

	@Override
	public Integer getEgid() {
		return null;
	}

	@Override
	public Integer getEwid() {
		return null;
	}

	@Override
	public String getLogCreationUser() {
		return target.getLogCreationUser();
	}

	@Override
	public Date getLogCreationDate() {
		return target.getLogCreationDate();
	}

	@Override
	public String getLogModifUser() {
		return target.getLogModifUser();
	}

	@Override
	public Timestamp getLogModifDate() {
		return target.getLogModifDate();
	}

	@Override
	public Date getAnnulationDate() {
		return target.getAnnulationDate();
	}

	@Override
	public String getAnnulationUser() {
		return target.getAnnulationUser();
	}

	@Override
	public boolean isPermanente() {
		return false;
	}
}
