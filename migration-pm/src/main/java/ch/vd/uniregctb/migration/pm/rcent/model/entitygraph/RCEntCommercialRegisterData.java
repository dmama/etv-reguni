package ch.vd.uniregctb.migration.pm.rcent.model.entitygraph;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.CommercialRegisterNameTranslation;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Topic;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryElement;

public class RCEntCommercialRegisterData extends RCEntHistoryElement {

	private String name;
	private String otherName;
	private CommercialRegisterNameTranslation nameTranslation; // Plusieurs noms peuvent cohabiter
	private CommercialRegisterStatus status;
	private CommercialRegisterEntryStatus entryStatus;
	private RegDate entryDate; // Date de première entrée. Solidaire du
	private RegDate cancellationDate;
	private Address legalAddress;
	private String weblink;
	private String purpose;
	private CommercialRegisterDiaryEntry diaryEntry;
	private Topic topic;
	private Capital capital;
	private RegDate byLawsDate;

	public RCEntCommercialRegisterData(RegDate beginDate, RegDate endDateDate) {
		super(beginDate, endDateDate);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOtherName() {
		return otherName;
	}

	public void setOtherName(String otherName) {
		this.otherName = otherName;
	}

	public CommercialRegisterNameTranslation getNameTranslation() {
		return nameTranslation;
	}

	public void setNameTranslation(CommercialRegisterNameTranslation nameTranslation) {
		this.nameTranslation = nameTranslation;
	}

	public CommercialRegisterStatus getStatus() {
		return status;
	}

	public void setStatus(CommercialRegisterStatus status) {
		this.status = status;
	}

	public CommercialRegisterEntryStatus getEntryStatus() {
		return entryStatus;
	}

	public void setEntryStatus(CommercialRegisterEntryStatus entryStatus) {
		this.entryStatus = entryStatus;
	}

	public RegDate getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(RegDate entryDate) {
		this.entryDate = entryDate;
	}

	public RegDate getCancellationDate() {
		return cancellationDate;
	}

	public void setCancellationDate(RegDate cancellationDate) {
		this.cancellationDate = cancellationDate;
	}

	public Address getLegalAddress() {
		return legalAddress;
	}

	public void setLegalAddress(Address legalAddress) {
		this.legalAddress = legalAddress;
	}

	public String getWeblink() {
		return weblink;
	}

	public void setWeblink(String weblink) {
		this.weblink = weblink;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public CommercialRegisterDiaryEntry getDiaryEntry() {
		return diaryEntry;
	}

	public void setDiaryEntry(CommercialRegisterDiaryEntry diaryEntry) {
		this.diaryEntry = diaryEntry;
	}

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}

	public Capital getCapital() {
		return capital;
	}

	public void setCapital(Capital capital) {
		this.capital = capital;
	}

	public RegDate getByLawsDate() {
		return byLawsDate;
	}

	public void setByLawsDate(RegDate byLawsDate) {
		this.byLawsDate = byLawsDate;
	}
}
