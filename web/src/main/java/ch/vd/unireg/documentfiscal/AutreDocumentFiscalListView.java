package ch.vd.unireg.documentfiscal;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class AutreDocumentFiscalListView {

	private final long ctbId;
	private final List<AutreDocumentFiscalView> docs;

	public AutreDocumentFiscalListView(long id, List<AutreDocumentFiscalView> documents) {
		this.ctbId = id;
		this.docs = documents;
		this.docs.sort((o1, o2) -> {
			if (o1.isAnnule() && !o2.isAnnule()) {
				return 1;
			}
			else if (!o1.isAnnule() && o2.isAnnule()) {
				return -1;
			}
			else {
				return o2.getDateEnvoi().compareTo(o1.getDateEnvoi());
			}
		});
	}

	public long getCtbId() {
		return ctbId;
	}

	public List<AutreDocumentFiscalView> getDocs() {
		return docs;
	}
}
