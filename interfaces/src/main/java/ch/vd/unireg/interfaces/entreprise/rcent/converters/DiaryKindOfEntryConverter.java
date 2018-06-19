package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.DiaryKindOfEntry;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;

/**
 * @author Raphaël Marmier, 2016-06-13, <raphael.marmier@vd.ch>
 */
public class DiaryKindOfEntryConverter extends BaseEnumConverter<DiaryKindOfEntry, EntreeJournalRC.TypeEntree> {

	@Override
	@NotNull
	protected EntreeJournalRC.TypeEntree convert(@NotNull DiaryKindOfEntry value) {
		switch (value) {
		case AUTRE: return EntreeJournalRC.TypeEntree.AUTRE;
		case NORMAL: return EntreeJournalRC.TypeEntree.NORMAL;
		case RECTIFICATION: return EntreeJournalRC.TypeEntree.RECTIFICATION;
		case COMPLEMENT: return EntreeJournalRC.TypeEntree.COMPLEMENT;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}

}
