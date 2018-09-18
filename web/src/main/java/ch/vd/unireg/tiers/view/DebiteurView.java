package ch.vd.unireg.tiers.view;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.MessageSource;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.utils.WebContextUtils;

public class DebiteurView implements Annulable {

	private Long numero;

	private List<String> nomCourrier;

	private String complementNom;

	private CategorieImpotSource categorieImpotSource;
	private String nomCategorie;

	private String personneContact;

	private Long id;

	private boolean annule;
	private SensRapportEntreTiers sensRapportEntreTiers;

	public DebiteurView() {
	}

	public DebiteurView(DebiteurPrestationImposable dpi, ContactImpotSource r, AdresseService adresseService, MessageSource messageSource) {
		this.annule = r.isAnnule();
		this.id = r.getId();
		this.numero = dpi.getNumero();
		this.categorieImpotSource = dpi.getCategorieImpotSource();
		this.nomCategorie = messageSource.getMessage("option.categorie.impot.source." + dpi.getCategorieImpotSource().name(), null, WebContextUtils.getDefaultLocale());
		this.personneContact = dpi.getPersonneContact();
		this.nomCourrier = buildNomCourrier(dpi, adresseService);
	}

	private static List<String> buildNomCourrier(DebiteurPrestationImposable dpi, AdresseService adresseService) {
		List<String> nomCourrier;
		try {
			nomCourrier = adresseService.getNomCourrier(dpi, null, false);
		}
		catch (AdresseException e) {
			nomCourrier = new ArrayList<>();
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

	public List<String> getNomCourrier() {
		return nomCourrier;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier = nomCourrier;
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

	public void setSensRapportEntreTiers(SensRapportEntreTiers sensRapportEntreTiers) {
		this.sensRapportEntreTiers = sensRapportEntreTiers;
	}

	public SensRapportEntreTiers getSensRapportEntreTiers() {
		return sensRapportEntreTiers;
	}
}
