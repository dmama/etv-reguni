package ch.vd.uniregctb.migration.pm.rcent.model.history;

import java.math.BigInteger;
import java.util.List;

import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.registre.base.date.RegDate;

public class RCEntOrganisationHistoryElement extends RCEntHistoryElement {

	private final Organisation organisation;

	protected BigInteger cantonalId;
	protected List<Identifier> organisationIdentifier;
	protected String organisationName;
	protected String organisationAdditionalName;
	protected LegalForm legalForm;
	protected List<OrganisationLocation> organisationLocation;
	protected List<Identification> transferTo;
	protected List<Identification> transferFrom;
	protected Identification replacedBy;
	protected List<Identification> inReplacementOf;

	public RCEntOrganisationHistoryElement(RegDate beginDate, RegDate endDateDate, Organisation organisation) {
		super(beginDate, endDateDate);
		this.organisation = organisation;
	}

	public BigInteger getCantonalId() {
		return organisation.getCantonalId();
	}

	public LegalForm getLegalForm() {
		return organisation.getLegalForm();
	}

	public void setLegalForm(LegalForm value) {
		organisation.setLegalForm(value);
	}

	public List<Identifier> getOrganisationIdentifier() {
		return organisation.getOrganisationIdentifier();
	}

	public void setReplacedBy(Identification value) {
		organisation.setReplacedBy(value);
	}

	public List<Identification> getTransferTo() {
		return organisation.getTransferTo();
	}

	public String getOrganisationAdditionalName() {
		return organisation.getOrganisationAdditionalName();
	}

	public void setCantonalId(BigInteger value) {
		organisation.setCantonalId(value);
	}

	public List<OrganisationLocation> getOrganisationLocation() {
		return organisation.getOrganisationLocation();
	}

	public void setOrganisationName(String value) {
		organisation.setOrganisationName(value);
	}

	public List<Identification> getInReplacementOf() {
		return organisation.getInReplacementOf();
	}

	public List<Identification> getTransferFrom() {
		return organisation.getTransferFrom();
	}

	public Identification getReplacedBy() {
		return organisation.getReplacedBy();
	}

	public String getOrganisationName() {
		return organisation.getOrganisationName();
	}

	public void setOrganisationAdditionalName(String value) {
		organisation.setOrganisationAdditionalName(value);
	}
}
