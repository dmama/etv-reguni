package ch.vd.uniregctb.admin;

import org.springframework.web.multipart.MultipartFile;

/**
 * POJO servant de Form aux controlleurs Spring. Cette classe contient un
 * MultipartFile permettant de stocker le fichier en cours d'upload dans le
 * formulaire d'upload.
 *
 * Elle herite aussi de RechercheBean car les formulaires Upload et Recheche
 * sont sur la meme vue. Il faut donc pouvoir reinitialiser la recherche a sa
 * valeur precedente apres un Upload. C'est pour cette raison qu'un FileBean
 * herite de RechercheBean.
 *
 */
public class FileBean {

	private MultipartFile file;

	/**
	 * @return Returns the file.
	 */
	public MultipartFile getMultipartFile() {
		return file;
	}

	/**
	 * @param file
	 *            The file to set.
	 */
	public void setMultipartFile(MultipartFile file) {
		this.file = file;
	}

}
