package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.WebContextUtils;

public class DiView {

	private Long tiersId;
	private String codeControle;
	private int periodeFiscale;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeDocument typeDocument;
	private String typeDocumentMessage;
	private List<DelaiDeclarationView> delais;
	private List<EtatDeclarationView> etats;

	public DiView(DeclarationImpotOrdinaire di, MessageSource messageSource) {
		this.tiersId = di.getTiers().getId();
		this.codeControle = di.getCodeControle();
		this.periodeFiscale = di.getPeriode().getAnnee();
		this.dateDebut = di.getDateDebut();
		this.dateFin = di.getDateFin();
		this.typeDocument = di.getTypeDeclaration();
		this.typeDocumentMessage = messageSource.getMessage("option.type.document." + this.typeDocument.name(), null, WebContextUtils.getDefaultLocale());
		this.delais = initDelais(di.getDelais(), di.getPremierDelai());
		this.etats = initEtats(di.getEtats(), messageSource);
	}

	private static List<DelaiDeclarationView> initDelais(Set<DelaiDeclaration> delais, RegDate premierDelai) {
		final List<DelaiDeclarationView> list = new ArrayList<DelaiDeclarationView>();
		for (DelaiDeclaration delai : delais) {
			final DelaiDeclarationView delaiView = new DelaiDeclarationView(delai);
			delaiView.setFirst(premierDelai == delai.getDelaiAccordeAu());
			list.add(delaiView);
		}
		Collections.sort(list);
		return list;
	}

	private static List<EtatDeclarationView> initEtats(Set<EtatDeclaration> etats, MessageSource messageSource) {
		final List<EtatDeclarationView> list = new ArrayList<EtatDeclarationView>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDeclarationView(etat, messageSource));
		}
		Collections.sort(list);
		return list;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public String getCodeControle() {
		return codeControle;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public String getTypeDocumentMessage() {
		return typeDocumentMessage;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public List<EtatDeclarationView> getEtats() {
		return etats;
	}
}
