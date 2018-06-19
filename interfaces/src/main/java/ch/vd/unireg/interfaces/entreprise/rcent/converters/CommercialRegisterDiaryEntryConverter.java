package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v3.SwissGazetteOfCommercePublication;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.entreprise.data.PublicationFOSC;

public class CommercialRegisterDiaryEntryConverter implements Function<CommercialRegisterDiaryEntry, EntreeJournalRC> {

	private static final DiaryKindOfEntryConverter DIARY_KIND_OF_ENTRY_CONVERTER = new DiaryKindOfEntryConverter();

	@NotNull
	public EntreeJournalRC apply(@NotNull CommercialRegisterDiaryEntry entry) {

		EntreeJournalRC.TypeEntree type = null;
		if (entry.getDiaryKindOfEntry() != null) {
			type = DIARY_KIND_OF_ENTRY_CONVERTER.apply(entry.getDiaryKindOfEntry());
		}

		final SwissGazetteOfCommercePublication foscPublication = entry.getSwissGazetteOfCommercePublication();
		PublicationFOSC fosc = new PublicationFOSC(foscPublication.getPublicationDate(), foscPublication.getDocumentNumber(), foscPublication.getPublicationText());

		return new EntreeJournalRC(type, entry.getDiaryEntryDate(), entry.getDiaryEntryNumber().longValue(), fosc);
	}


}
