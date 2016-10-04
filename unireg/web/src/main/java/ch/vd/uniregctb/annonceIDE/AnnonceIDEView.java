package ch.vd.uniregctb.annonceIDE;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

/**
 * Vue web d'une annonce IDE.
 */
public class AnnonceIDEView {

	private Long numero;
	private TypeAnnonce type;
	private Date dateAnnonce;
	private UtilisateurView utilisateur;
	private ServiceIDEView serviceIDE;
	private StatutView statut;
	private TypeDeSite typeDeSite;
	private String noIde;
	private String noIdeRemplacant;
	private String noIdeEtablissementPrincipal;
	private RaisonDeRadiationRegistreIDE raisonDeRadiation;
	private String commentaire;
	private InformationOrganisationView informationOrganisation;
	private ContenuView contenu;

	public AnnonceIDEView() {
	}

	public AnnonceIDEView(@NotNull AnnonceIDEEnvoyee annonce) {
		this.numero = annonce.getNumero();
		this.type = annonce.getType();
		this.dateAnnonce = annonce.getDateAnnonce();
		this.utilisateur = new UtilisateurView(annonce.getUtilisateur());
		this.serviceIDE = new ServiceIDEView(annonce.getInfoServiceIDEObligEtendues());
		this.statut = new StatutView(annonce.getStatut());
		this.typeDeSite = annonce.getTypeDeSite();
		this.noIde = formatNoIDE(annonce.getNoIde());
		this.noIdeRemplacant = formatNoIDE(annonce.getNoIdeRemplacant());
		this.noIdeEtablissementPrincipal = formatNoIDE(annonce.getNoIdeEtablissementPrincipal());
		this.raisonDeRadiation = annonce.getRaisonDeRadiation();
		this.commentaire = annonce.getCommentaire();
		this.informationOrganisation = InformationOrganisationView.get(annonce.getInformationOrganisation());
		this.contenu = ContenuView.get(annonce.getContenu());
	}

	@Nullable
	private static String formatNoIDE(@Nullable NumeroIDE no) {
		if (no == null) {
			return null;
		}
		return no.toString();
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public TypeAnnonce getType() {
		return type;
	}

	public void setType(TypeAnnonce type) {
		this.type = type;
	}

	public Date getDateAnnonce() {
		return dateAnnonce;
	}

	public void setDateAnnonce(Date dateAnnonce) {
		this.dateAnnonce = dateAnnonce;
	}

	public UtilisateurView getUtilisateur() {
		return utilisateur;
	}

	public void setUtilisateur(UtilisateurView utilisateur) {
		this.utilisateur = utilisateur;
	}

	public ServiceIDEView getServiceIDE() {
		return serviceIDE;
	}

	public void setServiceIDE(ServiceIDEView serviceIDE) {
		this.serviceIDE = serviceIDE;
	}

	public StatutView getStatut() {
		return statut;
	}

	public void setStatut(StatutView statut) {
		this.statut = statut;
	}

	public TypeDeSite getTypeDeSite() {
		return typeDeSite;
	}

	public void setTypeDeSite(TypeDeSite typeDeSite) {
		this.typeDeSite = typeDeSite;
	}

	public String getNoIde() {
		return noIde;
	}

	public void setNoIde(String noIde) {
		this.noIde = noIde;
	}

	public String getNoIdeRemplacant() {
		return noIdeRemplacant;
	}

	public void setNoIdeRemplacant(String noIdeRemplacant) {
		this.noIdeRemplacant = noIdeRemplacant;
	}

	public String getNoIdeEtablissementPrincipal() {
		return noIdeEtablissementPrincipal;
	}

	public void setNoIdeEtablissementPrincipal(String noIdeEtablissementPrincipal) {
		this.noIdeEtablissementPrincipal = noIdeEtablissementPrincipal;
	}

	public RaisonDeRadiationRegistreIDE getRaisonDeRadiation() {
		return raisonDeRadiation;
	}

	public void setRaisonDeRadiation(RaisonDeRadiationRegistreIDE raisonDeRadiation) {
		this.raisonDeRadiation = raisonDeRadiation;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	public InformationOrganisationView getInformationOrganisation() {
		return informationOrganisation;
	}

	public void setInformationOrganisation(InformationOrganisationView informationOrganisation) {
		this.informationOrganisation = informationOrganisation;
	}

	public ContenuView getContenu() {
		return contenu;
	}

	public void setContenu(ContenuView contenu) {
		this.contenu = contenu;
	}
}
