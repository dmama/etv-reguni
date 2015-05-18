package ch.vd.uniregctb.migration.pm.rcent.model;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.CommercialRegisterNameTranslation;
import ch.vd.evd0022.v1.CommercialRegisterStatus;
import ch.vd.evd0022.v1.Topic;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.ValuesDateRanges;
import ch.vd.uniregctb.migration.pm.historizer.container.SingleValueDateRanges;

public class RCEntCommercialRegisterData {

	private SingleValueDateRanges<String> name; // Todo: Assurer unique à un temps T
	private ValuesDateRanges<String> otherName;
	private ValuesDateRanges<CommercialRegisterNameTranslation> nameTranslation; // Plusieurs noms peuvent cohabiter
	private SingleValueDateRanges<CommercialRegisterStatus> status; // Todo: Assurer unique à un temps T
	private SingleValueDateRanges<CommercialRegisterEntryStatus> entryStatus;
	private RegDate entryDate; // Date de première entrée. Solidaire du
	private RegDate cancellationDate;
	private SingleValueDateRanges<Address> legalAddress;
	private SingleValueDateRanges<String> weblink;
	private SingleValueDateRanges<String> purpose;
	private SingleValueDateRanges<CommercialRegisterDiaryEntry> diaryEntry; // Todo: controler si on peut vraiment le traiter comme un bloc.
	private SingleValueDateRanges<Topic> topic;
	private SingleValueDateRanges<Capital> capital;
	private SingleValueDateRanges<RegDate> byLawsDate;

}
