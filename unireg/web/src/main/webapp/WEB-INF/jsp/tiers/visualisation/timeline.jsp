<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>

<c:if test="${command.forPrint}">
	<c:set var="templateUrl" value="/WEB-INF/jsp/templates/templateIFrame.jsp" />
</c:if>
<c:if test="${!command.forPrint}">
	<c:set var="templateUrl" value="/WEB-INF/jsp/templates/template.jsp" />
</c:if>

<tiles:insert template="${templateUrl}">

	<tiles:put name="head"><script type="text/javascript" src="<c:url value="/js/tiers.js"/>"></script></tiles:put>
	<tiles:put name="title">Vue chronologique des fors fiscaux et des assujettissements</tiles:put>

	<tiles:put name="body">
	
		<c:if test="${command.forPrint}">
				<h1><c:out value="${command.title}"/></h1>
				<h3><c:out value="${command.description}"/></h3>
		</c:if>
		<c:if test="${!command.forPrint}">
			<p style="text-align: center; color:red;">Attention: cette page est une aide pour les développeurs de Unireg. Il ne s'agit en aucune manière d'une page officielle, et aucun support n'est prévu.</p>

			<a href="<c:url value="/tiers/visu.do?id=" /><c:out value="${command.tiersId}" />" >Retour à la visualisation</a>

			<display:table name="${command.adresse.lignes}" id="ligne" class="list" cellspacing="2" >
				<display:column>${ligne}</display:column>
			</display:table>
		</c:if>


		<c:if test="${fn:length(command.exceptions) > 0}">
			<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
				<tr><td class="heading">Les assujettissements n'ont pas tous pu être calculés pour les raisons suivantes:</td></tr>
				<tr id="val_errors"><td class="details"><ul>
				<c:forEach var="err" items="${command.exceptions}">
					<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${err.message}"/></li>
				</c:forEach>
				</ul></td></tr>
			</table>
		</c:if>

		<div id="legend">
			<table>
			 	<tr><th colspan="2">Légende</th></tr>
				<tr><td class="principal_vd">Lausanne</td><td>For fiscal principal dans le canton de Vaud</td></tr>
				<tr><td class="principal_hc">Berne</td><td>For fiscal principal hors canton</td></tr>
				<tr><td class="principal_hs">Bruxelles</td><td>For fiscal principal hors suisse</td></tr>
			</table>
		</div>

		<table id="timeline">
			<tr>
				<th>Période</th>
				<th>Fors Principaux</th>
				<th colspan="<c:out value="${command.table.forsSecondairesSize}"/>">Fors Secondaires</th>
				<th>Fors de Gestion</th>
				<th>Assujettissements</th>
				<th>Périodes d'imposition</th>
			</tr>
			<c:forEach var="ligne" varStatus="status" items="${command.table.rows}" >
				<tr>
				 	<%-- période --%>
					<td class="periode">
						<unireg:date date="${ligne.periode.dateDebut}" /><br/><unireg:date date="${ligne.periode.dateFin}" />
					</td>
					
					<%-- fors principaux --%>
					<c:choose>
						<c:when test="${ligne.forPrincipal.filler}">			
							<td class="filler" />
						</c:when>
						<c:when test="${!ligne.forPrincipal.span && !ligne.forPrincipal.filler}">
							<c:set var="fp" value="${ligne.forPrincipal.range}" />
							<c:choose>
								<c:when test="${fp.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">			
									<td class="principal_vd" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
									    <a href="#tooltip" class="staticTip" id="ffp-${fp.id}">
										    <unireg:infra entityId="${fp.numeroOfsAutoriteFiscale}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
										</a>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b> - <b>${fp.motifOuverture}</b><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b> - <b>${fp.motifFermeture}</b><br/>
										    Motif de rattachement : <b>${fp.motifRattachement}</b><br/>
										    Mode d'imposition : <b>${fp.modeImposition}</b>
										</div>
									</td>
								</c:when>
								<c:when test="${fp.typeAutoriteFiscale == 'COMMUNE_HC'}">
									<td class="principal_hc" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
									    <a href="#tooltip" class="staticTip" id="ffp-${fp.id}">
    										<unireg:infra entityId="${fp.numeroOfsAutoriteFiscale}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
										</a>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b> - <b>${fp.motifOuverture}</b><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b> - <b>${fp.motifFermeture}</b><br/>
										    Motif de rattachement : <b>${fp.motifRattachement}</b><br/>
										    Mode d'imposition : <b>${fp.modeImposition}</b>
										</div>
									</td>
								</c:when>
								<c:when test="${fp.typeAutoriteFiscale == 'PAYS_HS'}">
									<td class="principal_hs" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
									    <a href="#tooltip" class="staticTip" id="ffp-${fp.id}">
    										<unireg:infra entityId="${fp.numeroOfsAutoriteFiscale}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
										</a>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b> - <b>${fp.motifOuverture}</b><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b> - <b>${fp.motifFermeture}</b><br/>
										    Motif de rattachement : <b>${fp.motifRattachement}</b><br/>
										    Mode d'imposition : <b>${fp.modeImposition}</b>
										</div>
									</td>
								</c:when>
							</c:choose>
						</c:when>
						<c:when test="${ligne.forPrincipal.span}">
							<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
						</c:when>
					</c:choose>
					
					<%-- fors secondaires --%>
					<c:forEach var="fs" items="${ligne.forsSecondaires}" >
						<c:choose>
							<c:when test="${fs.filler}">			
								<td class="filler" />
							</c:when>
							<c:when test="${!fs.span && !fs.filler}">
								<td class="secondaire" rowspan="<c:out value="${fs.longueurAffichage}" />">
                                    <a href="#tooltip" class="staticTip" id="ffs-${fs.range.id}">
    									<unireg:infra entityId="${fs.range.numeroOfsAutoriteFiscale}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
                                    </a>
                                    <div id="ffs-${fs.range.id}-tooltip" style="display:none;">
                                        For fiscal secondaire <b>#${fs.range.id}</b><br/>
                                        Ouverture : <b><unireg:date date="${fs.range.dateDebut}"/></b> - <b>${fs.range.motifOuverture}</b><br/>
                                        Fermeture : <b><unireg:date date="${fs.range.dateFin}"/></b> - <b>${fs.range.motifFermeture}</b><br/>
                                        Motif de rattachement : <b>${fs.range.motifRattachement}</b><br/>
                                    </div>
								</td>
							</c:when>
							<c:when test="${fs.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
							</c:when>
						</c:choose>
					</c:forEach>

					<%-- fors de gestion --%>
					<c:choose>
						<c:when test="${ligne.forGestion.filler}">			
							<td class="filler" />
						</c:when>
						<c:when test="${!ligne.forGestion.span && !ligne.forGestion.filler}">
							<c:set var="fg" value="${ligne.forGestion.range}" />
							<td class="gestion" rowspan="<c:out value="${ligne.forGestion.longueurAffichage}" />">
                                <a href="#tooltip" class="staticTip" id="fg-${fg.dateDebut}">
    								<unireg:infra entityId="${fg.noOfsCommune}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
                                </a>
                                <div id="fg-${fg.dateDebut}-tooltip" style="display:none;">
                                    Ouverture : <b><unireg:date date="${fg.dateDebut}"/></b><br/>
                                    Fermeture : <b><unireg:date date="${fg.dateFin}"/></b><br/>
                                    Sous-jacent : <b>for fiscal #${fg.sousjacent.id}</b><br/>
                                </div>
							</td>
						</c:when>
						<c:when test="${ligne.forGestion.span}">
							<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
						</c:when>
					</c:choose>

					<%-- assujettissements --%>
					<c:choose>
						<c:when test="${ligne.assujettissement.filler}">			
							<td class="filler" />
						</c:when>
						<c:when test="${!ligne.assujettissement.span && !ligne.assujettissement.filler}">
							<c:set var="a" value="${ligne.assujettissement.range}" />
							<td class="assujettissement" rowspan="<c:out value="${ligne.assujettissement.longueurAffichage}" />">
                                <a href="#tooltip" class="staticTip" id="a-${a.dateDebut}">
    								<c:out value="${a.description}" />
                                </a>
                                <div id="a-${a.dateDebut}-tooltip" style="display:none;">
                                    Début : <b><unireg:date date="${a.dateDebut}"/></b>
                                    <c:if test="${a.motifFractDebut != null}">
                                        - <b>${a.motifFractDebut}</b>
                                    </c:if>
                                    <br/>
                                    Fin : <b><unireg:date date="${a.dateFin}"/></b>
                                    <c:if test="${a.motifFractFin != null}">
                                         - <b>${a.motifFractFin}</b>
                                    </c:if>
                                    <br/>
                                </div>
							</td>
						</c:when>
						<c:when test="${ligne.assujettissement.span}">
							<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
						</c:when>
					</c:choose>

					<%-- périodes d'imposition --%>
					<c:choose>
						<c:when test="${ligne.periodeImposition.filler}">			
							<td class="filler" />
						</c:when>
						<c:when test="${!ligne.periodeImposition.span && !ligne.periodeImposition.filler}">
							<c:set var="pi" value="${ligne.periodeImposition.range}" />
							<td class="periodeImposition" rowspan="<c:out value="${ligne.periodeImposition.longueurAffichage}" />">
                                <a href="#tooltip" class="staticTip" id="pi-${pi.dateDebut}">
                                    <c:out value="${pi.typeContribuable}" /> /
                                    <c:out value="${pi.typeDocument}" />
                                    <c:if test="${pi.optionnelle}">
                                        (optionnelle)
                                    </c:if>
                                    <c:if test="${pi.remplaceeParNote}">
                                        (remplacée par note)
                                    </c:if>
                                    <c:if test="${pi.diplomateSuisse}">
                                        (diplomate suisse)
                                    </c:if>
                                </a>
                                <div id="pi-${pi.dateDebut}-tooltip" style="display:none;">
                                    Début : <b><unireg:date date="${pi.dateDebut}"/></b><br/>
                                    Fin : <b><unireg:date date="${pi.dateFin}"/></b><br/>
                                    Type de contribuable : <b>${pi.typeContribuable}</b><br/>
                                    Type de document : <b>${pi.typeDocument}</b><br/>
                                    Qualification : <b>${pi.qualification}</b><br/>
                                    Adresse de retour : <b>${pi.adresseRetour}</b><br/>
                                    Optionnelle : <b>${pi.optionnelle}</b><br/>
                                    Remplacée par note : <b>${pi.remplaceeParNote}</b>
                                </div>
							</td>
						</c:when>
						<c:when test="${ligne.periodeImposition.span}">
							<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
						</c:when>
					</c:choose>
					
				</tr>
			</c:forEach>
		</table>
			
	</tiles:put>
	
</tiles:insert>
