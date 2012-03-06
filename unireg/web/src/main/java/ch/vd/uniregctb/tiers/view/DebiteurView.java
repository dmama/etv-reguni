package ch.vd.uniregctb.tiers.view;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.utils.WebContextUtils;

public class DebiteurView implements Annulable {

	private Long numero;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

	private String complementNom;

	private CategorieImpotSource categorieImpotSource;
	private String nomCategorie;

	private String personneContact;

	private Long id;

	private boolean annule;

	public DebiteurView() {
	}

	public DebiteurView(DebiteurPrestationImposable dpi, ContactImpotSource r, AdresseService adresseService, MessageSource messageSource) {
		this.annule = r.isAnnule();
		this.id = r.getId();
		this.numero = dpi.getNumero();
		this.categorieImpotSource = dpi.getCategorieImpotSource();
		this.nomCategorie = messageSource.getMessage("option.categorie.impot.source." + dpi.getCategorieImpotSource().name(), null, WebContextUtils.getDefaultLocale());
		this.personneContact = dpi.getPersonneContact();
		this.nomCourrier.setNomCourrier(buildNomCourrier(dpi, adresseService));
	}

	private static List<String> buildNomCourrier(DebiteurPrestationImposable dpi, AdresseService adresseService) {
		List<String> nomCourrier;
		try {
			nomCourrier = adresseService.getNomCourrier(dpi, null, false);
		}
		catch (AdresseException e) {
			nomCourrier = new ArrayList<String>();
			nomCourrier.add(e.getMessage());
		}
		return nomCourrier;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public CategorieImpotSource getCategorieImpotSource() {
		return categorieImpotSource;
	}

	public void setCategorieImpotSource(CategorieImpotSource categorieImpotSource) {
		this.categorieImpotSource = categorieImpotSource;
	}

	public String getNomCategorie() {
		return nomCategorie;
	}

	public void setNomCategorie(String nomCategorie) {
		this.nomCategorie = nomCategorie;
	}

	public String getComplementNom() {
		return complementNom;
	}

	public void setComplementNom(String complementNom) {
		this.complementNom = complementNom;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

}
