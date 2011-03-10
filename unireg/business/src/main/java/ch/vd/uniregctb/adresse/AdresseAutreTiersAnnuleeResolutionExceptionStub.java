package ch.vd.uniregctb.adresse;

import java.sql.Timestamp;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;

/**
 * Données minimale d'une adresse autre tiers <b>annulée</b> ([UNIREG-3154]).
 * <p/>
 * Cette adresse est utilisé à la place de l'adresse complète lorsque la résolution de l'adresse est impossible (à cause de cycles, par exemple).
 */
public class AdresseAutreTiersAnnuleeResolutionExceptionStub implements AdresseGenerique {

	private AdresseAutreTiers target;
	private Source source;

	public AdresseAutreTiersAnnuleeResolutionExceptionStub(AdresseAutreTiers target) {
		Assert.isTrue(target.isAnnule()); // si l'adresse autre tiers n'est pas annulée, l'exception de résolution des adresses doit être remontée !
		this.target = target;
		this.source = new Source(SourceType.FISCALE, null);
	}

	public Long getId() {
		return target.getId();
	}

	public Source getSource() {
		return source;
	}

	public boolean isDefault() {
		return false;
	}

	public String getCasePostale() {
		return null;
	}

	public boolean isValidAt(RegDate date) {
		return target.isValidAt(date);
	}

	public RegDate getDateDebut() {
		return target.getDateDebut();
	}

	public RegDate getDateFin() {
		return target.getDateFin();
	}

	public String getLocalite() {
		return "*** adresse résolution exception ***";
	}

	public String getLocaliteComplete() {
		return null;
	}

	public String getNumero() {
		return null;
	}

	public int getNumeroOrdrePostal() {
		return 0;
	}

	public String getNumeroPostal() {
		return null;
	}

	public String getNumeroPostalComplementaire() {
		return null;
	}

	public Integer getNoOfsPays() {
		return null;
	}

	public String getRue() {
		return null;
	}

	public Integer getNumeroRue() {
		return null;
	}

	public String getNumeroAppartement() {
		return null;
	}

	public String getComplement() {
		return null;
	}

	public boolean isAnnule() {
		return true;
	}

	public CommuneSimple getCommuneAdresse() {
		return null;
	}

	public String getLogCreationUser() {
		return target.getLogCreationUser();
	}

	public Date getLogCreationDate() {
		return target.getLogCreationDate();
	}

	public String getLogModifUser() {
		return target.getLogModifUser();
	}

	public Timestamp getLogModifDate() {
		return target.getLogModifDate();
	}

	public Date getAnnulationDate() {
		return target.getAnnulationDate();
	}

	public String getAnnulationUser() {
		return target.getAnnulationUser();
	}

	public boolean isPermanente() {
		return false;
	}
}
