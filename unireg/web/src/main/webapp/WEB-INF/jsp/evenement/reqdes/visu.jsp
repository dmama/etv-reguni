<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.reqdes.unite.traitement.caracteristiques" /></tiles:put>
	<tiles:put name="displayedTitleSuffix">
		<a href="doc-ut.do?id=${uniteTraitement.id}&url_memorize=false" class="pdf" id="print-doc-${uniteTraitement.id}" onclick="Link.tempSwap(this, '#disabled-print-doc-${uniteTraitement.id}');">&nbsp;</a>
		<span class="pdf-grayed" id="disabled-print-doc-${uniteTraitement.id}" style="display: none;">&nbsp;</span>
	</tiles:put>
  	<tiles:put name="body">

	  	<unireg:nextRowClass reset="1"/>

		<!-- Début données de l'acte -->
		<fieldset>
			<legend><span><fmt:message key="label.reqdes.donnees.acte"/></span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.reqdes.date.acte"/>&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${uniteTraitement.dateActe}"/></td>
					<td width="25%"><fmt:message key="label.reqdes.numero.minute" />&nbsp;:</td>
					<td width="25%"><c:out value="${uniteTraitement.numeroMinute}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.reqdes.notaire"/>&nbsp;:</td>
					<td width="25%"><c:out value="${uniteTraitement.notaire.nomPrenom} (${uniteTraitement.visaNotaire})"/></td>
					<td width="25%"><fmt:message key="label.reqdes.operateur" />&nbsp;:</td>
					<td width="25%">
						<c:choose>
							<c:when test="${uniteTraitement.operateur != null}">
								<c:out value="${uniteTraitement.operateur.nomPrenom} (${uniteTraitement.visaOperateur})"/>
							</c:when>
							<c:otherwise>
								-
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin données de l'acte -->

	    <!-- Début Données spécifiques à l'unité de traitement -->
	    <fieldset>
		    <legend><span><fmt:message key="label.reqdes.unite.traitement"/></span></legend>
		    <table>
			    <tr class="<unireg:nextRowClass/>">
				    <td width="25%"><fmt:message key="label.reqdes.unite.traitement.etat"/>&nbsp;:</td>
				    <td width="25%">
					    <fmt:message key="option.etat.traitement.reqdes.${uniteTraitement.etat}"/>
					    <unireg:consulterLog entityNature="UniteTraitementReqDes" entityId="${uniteTraitement.id}"/>
				    </td>
				    <td width="25%"><fmt:message key="label.reqdes.unite.traitement.date.traitement"/>&nbsp;:</td>
				    <td width="25%"><fmt:formatDate value="${uniteTraitement.dateTraitement}" pattern="dd.MM.yyyy HH:mm:ss" /></td>
			    </tr>
		    </table>

		    <c:if test="${not empty uniteTraitement.erreurs}">
			    <display:table name="uniteTraitement.erreurs" id="row">
				    <c:if test="${empty row.callstack}">
					    <display:column property="message" titleKey="label.erreur" class="${row.cssClass}"/>
				    </c:if>
				    <c:if test="${not empty row.callstack}">
					    <display:column titleKey="label.erreur" class="${row.cssClass}">
						    <unireg:callstack headerMessage="${row.message} " callstack="${row.callstack}" />
					    </display:column>
				    </c:if>
			    </display:table>
		    </c:if>
	    </fieldset>
	    <!-- Fin Données spécifiques à l'unité de traitement -->

	    <!-- Début Parties prenantes -->
	    <c:forEach var="partiePrenante" items="${uniteTraitement.partiesPrenantes}">
		    <fieldset>
			    <legend><span><fmt:message key="label.reqdes.unite.traitement.partie.prenante"/></span></legend>
			    <table>
				    <tr>
					    <!-- les détails de la partie prenante -->
					    <td width="50%" valign="top">
						    <unireg:nextRowClass reset="1"/>
						    <table>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.nom"/>&nbsp;:</td>
								    <td width="50%"><c:out value="${partiePrenante.nomPrenom.nom}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.nom.naissance"/>&nbsp;:</td>
								    <td width="50%"><c:out value="${partiePrenante.nomNaissance}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.prenoms"/>&nbsp;:</td>
								    <td width="50%"><c:out value="${partiePrenante.nomPrenom.prenom}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.date.naissance"/>&nbsp;:</td>
								    <td width="50%"><unireg:regdate regdate="${partiePrenante.dateNaissance}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.sexe"/>&nbsp;:</td>
								    <td width="50%"><c:if test="${partiePrenante.sexe != null}"><fmt:message key="option.sexe.${partiePrenante.sexe}"/></c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.date.deces"/>&nbsp;:</td>
								    <td width="50%"><unireg:regdate regdate="${partiePrenante.dateDeces}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.nouveau.numero.avs"/>&nbsp;:</td>
								    <td width="50%"><unireg:numAVS numeroAssureSocial="${partiePrenante.avs}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.nom.mere"/>&nbsp;:</td>
								    <td width="50%"><c:if test="${partiePrenante.nomPrenomsMere != null}"><c:out value="${partiePrenante.nomPrenomsMere.nomPrenom}"/></c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.nom.pere"/>&nbsp;:</td>
								    <td width="50%"><c:if test="${partiePrenante.nomPrenomsPere != null}"><c:out value="${partiePrenante.nomPrenomsPere.nomPrenom}"/></c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.etat.civil"/>&nbsp;:</td>
								    <td width="50%">
									    <c:if test="${partiePrenante.etatCivil != null}">
										    <fmt:message key="option.etat.civil.${partiePrenante.etatCivil}"/>
										    <c:if test="${partiePrenante.dateSeparation != null}">
											    (<fmt:message key="label.reqdes.separation.le"><fmt:param><unireg:regdate regdate="${partiePrenante.dateSeparation}"/></fmt:param></fmt:message>)
										    </c:if>
									    </c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.date.dernier.changement.etat.civil"/>&nbsp;:</td>
								    <td width="50%"><unireg:regdate regdate="${partiePrenante.dateEtatCivil}"/></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.nationalite"/>&nbsp;:</td>
								    <td width="50%"><c:if test="${partiePrenante.ofsPaysNationalite != null}"><unireg:pays ofs="${partiePrenante.ofsPaysNationalite}" displayProperty="nomCourt"/></c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.origine"/>&nbsp;:</td>
								    <td width="50%"><c:if test="${partiePrenante.origine != null}"><c:out value="${partiePrenante.origine.nomCommuneAvecCanton}"/></c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.categorie.etranger"/>&nbsp;:</td>
								    <td width="50%"><c:if test="${partiePrenante.categorieEtranger != null}"><fmt:message key="option.categorie.etranger.${partiePrenante.categorieEtranger}"/></c:if></td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.conjoint"/>&nbsp;:</td>
								    <td width="50%">
									    <c:if test="${partiePrenante.nomPrenomConjoint != null}">
										    <c:out value="${partiePrenante.nomPrenomConjoint.nomPrenom}"/>
										    <c:if test="${partiePrenante.conjointAutrePartiePrenante}">
											    (<fmt:message key="label.reqdes.autre.partie.prenante"/>)
										    </c:if>
									    </c:if>
								    </td>
							    </tr>
							    <tr class="<unireg:nextRowClass/>">
								    <td width="50%"><fmt:message key="label.reqdes.source"/>&nbsp;:</td>
								    <td width="50%">
									    <c:choose>
										    <c:when test="${partiePrenante.sourceCivile}">
											    <fmt:message key="label.reqdes.source.civile"/>
										    </c:when>
										    <c:when test="${partiePrenante.numeroContribuable != null}">
											    <fmt:message key="label.reqdes.source.contribuable"><fmt:param><unireg:numCTB numero="${partiePrenante.numeroContribuable}" link="true"/></fmt:param></fmt:message>
										    </c:when>
										    <c:otherwise>
											    <fmt:message key="label.reqdes.creation"/>
											    <c:if test="${partiePrenante.numeroContribuableCree != null}">
												    <img src="<c:url value='/images/right-arrow.png'/>" style="height:1em;"/>
												    <unireg:numCTB numero="${partiePrenante.numeroContribuableCree}" link="true"/>
											    </c:if>
										    </c:otherwise>
									    </c:choose>
								    </td>
							    </tr>
						    </table>
					    </td>

					    <!-- adresse de résidence et rôles -->
					    <td with="50%" valign="top">
						    <fieldset>
							    <legend><span><fmt:message key="label.reqdes.residence"/></span></legend>
							    <unireg:nextRowClass reset="1"/>
							    <table>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.adresse.complement"/>&nbsp;:</td>
									    <td width="50%"><c:out value="${partiePrenante.titre}"/></td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.rue"/>&nbsp;:</td>
									    <td width="50%"><c:out value="${partiePrenante.rue}"/></td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.numero.maison"/>&nbsp;:</td>
									    <td width="50%"><c:out value="${partiePrenante.numeroMaison}"/></td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.numero.appartement"/>&nbsp;:</td>
									    <td width="50%"><c:out value="${partiePrenante.numeroAppartement}"/></td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.localite"/>&nbsp;:</td>
									    <td width="50%"><c:out value="${partiePrenante.localite}"/></td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.npa"/>&nbsp;:</td>
									    <td width="50%">
										    <c:choose>
											    <c:when test="${partiePrenante.numeroPostalComplementaire != null}">
												    <c:out value="${partiePrenante.numeroPostal}-${partiePrenante.numeroPostalComplementaire}"/>
											    </c:when>
											    <c:otherwise>
												    <c:out value="${partiePrenante.numeroPostal}"/>
											    </c:otherwise>
										    </c:choose>
									    </td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.reqdes.numero.ordre.postal"/>&nbsp;:</td>
									    <td width="50%"><c:out value="${partiePrenante.numeroOrdrePostal}"/></td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <td width="50%"><fmt:message key="label.case.postale"/>&nbsp;:</td>
									    <td width="50%">
										    <c:if test="${partiePrenante.texteCasePostale != null}">
											    <c:out value="${partiePrenante.texteCasePostale} ${partiePrenante.casePostale}"/>
										    </c:if>
									    </td>
								    </tr>
								    <tr class="<unireg:nextRowClass/>">
									    <c:choose>
										    <c:when test="${partiePrenante.ofsCommune != null}">
											    <td width="50%"><fmt:message key="label.commune"/>&nbsp;:</td>
											    <td width="50%"><unireg:commune ofs="${partiePrenante.ofsCommune}" displayProperty="nomOfficielAvecCanton" date="${uniteTraitement.dateActe}"/></td>
										    </c:when>
										    <c:otherwise>
											    <td width="50%"><fmt:message key="label.pays"/>&nbsp;:</td>
											    <td width="50%">
												    <c:choose>
													    <c:when test="${partiePrenante.ofsPays != null}">
														    <unireg:pays ofs="${partiePrenante.ofsPays}" displayProperty="nomCourt" date="${uniteTraitement.dateActe}"/>
													    </c:when>
													    <c:otherwise>
														    <fmt:message key="label.reqdes.pays.inconnu"/>
													    </c:otherwise>
												    </c:choose>
											    </td>
										    </c:otherwise>
									    </c:choose>
								    </tr>
							    </table>
						    </fieldset>

						    <fieldset>
							    <legend><span><fmt:message key="label.reqdes.partie.prenante.roles"/></span></legend>
							    <unireg:nextRowClass reset="1"/>
							    <display:table list="${partiePrenante.roles}" id="role">
								    <display:column titleKey="label.reqdes.partie.prenante.role">
									    <fmt:message key="label.reqdes.partie.prenante.role.${role.type}"/>
								    </display:column>
								    <display:column titleKey="label.reqdes.inscription.mode">
									    <fmt:message key="label.reqdes.inscription.mode.${role.modeInscription}"/>
								    </display:column>
								    <display:column titleKey="label.reqdes.inscription.type">
									    <fmt:message key="label.reqdes.inscription.type.${role.typeInscription}"/>
								    </display:column>
								    <display:column titleKey="label.reqdes.description">
									    <c:out value="${role.libelleInscription}"/>
								    </display:column>
								    <display:column titleKey="label.commune">
									    <unireg:commune ofs="${role.ofsCommune}" displayProperty="nomOfficiel" date="${uniteTraitement.dateActe}"/>
								    </display:column>
							    </display:table>
						    </fieldset>
					    </td>
				    </tr>
			    </table>
		    </fieldset>
	    </c:forEach>
	    <!-- Fin parties prenantes -->

        <!-- Début des boutons -->
	    <input type="button" value="<fmt:message key='label.bouton.retour'/>" onClick="document.location='list.do';" />
	    <c:if test="${uniteTraitement.recyclable}">
		    <form:form method="post" action="recycler.do" style="display: inline">
			    <input type="hidden" name="id" value="${uniteTraitement.id}"/>
			    <fmt:message key="label.bouton.recycler" var="labelBoutonRecyler"/>
			    <input type="submit" name="recycler" value="${labelBoutonRecyler}"/>
		    </form:form>
	    </c:if>
	    <c:if test="${uniteTraitement.forceable}">
		    <form:form method="post" action="forcer.do" style="display: inline">
			    <input type="hidden" name="id" value="${uniteTraitement.id}"/>
			    <fmt:message key="label.bouton.forcer" var="labelBoutonForcer"/>
			    <input type="submit" name="forcer" value="${labelBoutonForcer}" onclick="return confirm('Voulez-vous réellement forcer l\'état de cette unité de traitement ?');"/>
		    </form:form>
	    </c:if>
	    <!-- Fin des boutons -->

	</tiles:put>
</tiles:insert>
