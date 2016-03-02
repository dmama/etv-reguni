package ch.vd.uniregctb.documentfiscal;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AutreDocumentFiscalView {

	private final long id;
	private final long tiersId;
	private final TypeEtatAutreDocumentFiscal etat;
	private final RegDate dateEnvoi;
	private final RegDate dateRetour;
	private final RegDate delaiRetour;
	private final RegDate dateRappel;
	private final String libelleTypeDocument;
	private final String libelleSousType;

	public static AutreDocumentFiscalView of(AutreDocumentFiscal document, MessageSource messageSource) {
		if (document == null) {
			return null;
		}
		if (document instanceof LettreBienvenue) {
			return new AutreDocumentFiscalView((LettreBienvenue) document, messageSource);
		}
		else {
			throw new IllegalArgumentException("Type de document fiscal non-encore supporté : " + document.getClass().getName());
		}
	}

	private AutreDocumentFiscalView(LettreBienvenue lettreBienvenue, MessageSource messageSource) {
		this.id = lettreBienvenue.getId();
		this.tiersId = lettreBienvenue.getEntreprise().getId();
		this.etat = lettreBienvenue.getEtat();
		this.dateEnvoi = lettreBienvenue.getDateEnvoi();
		this.dateRetour = lettreBienvenue.getDateRetour();
		this.delaiRetour = lettreBienvenue.getDelaiRetour();
		this.dateRappel = lettreBienvenue.getDateRappel();
		this.libelleTypeDocument = messageSource.getMessage("label.autre.document.fiscal.lettre.bienvenue", null, WebContextUtils.getDefaultLocale());
		this.libelleSousType = messageSource.getMessage("label.autre.document.fiscal.lettre.bienvenue.type." + lettreBienvenue.getType(), null, WebContextUtils.getDefaultLocale());
	}

	public long getId() {
		return id;
	}

	public long getTiersId() {
		return tiersId;
	}

	public TypeEtatAutreDocumentFiscal getEtat() {
		return etat;
	}

	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public RegDate getDateRappel() {
		return dateRappel;
	}

	public String getLibelleTypeDocument() {
		return libelleTypeDocument;
	}

	public String getLibelleSousType() {
		return libelleSousType;
	}
}
