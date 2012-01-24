<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">Vue chronologique des adresses</tiles:put>

	<tiles:put name="body">
	
		<p style="text-align: center; color:red;">Attention: cette page est une aide pour les développeurs de Unireg. Il ne s'agit en aucune manière d'une page officielle, et aucun support n'est prévu.</p>

		<a href="<c:url value="/tiers/visu.do?id=" /><c:out value="${command.tiersId}" />" >Retour à la visualisation</a>

		<div id="legend" class="timeline">
			<table >
				<tr><td class="civile">Lausanne</td><td>Adresse en provenance du registre civil</td></tr>
				<tr><td class="fiscale">Lausanne</td><td>Adresse en provenance du registre fiscal</td></tr>
				<tr><td class="representation">Lausanne</td><td>Adresse résultant d'une représentation conventionnelle</td></tr>
				<tr><td class="tutelle">Lausanne</td><td>Adresse résultant d'une représentation légale (tutelle, curatelle ou conseil légal)</td></tr>
				<tr><td class="principal">Lausanne</td><td>Adresse du principal dans le cas d'un ménage commun</td></tr>
				<tr><td class="conjoint">Lausanne</td><td>Adresse du conjoint dans le cas d'un ménage commun</td></tr>
				<tr><td class="pm">Lausanne</td><td>Adresse en provenance du registre des personnes morales</td></tr>
				<tr><td class="contribuable">Lausanne</td><td>Adresse du contribuable associé à une débiteur</td></tr>
			</table>
		</div>

		<display:table name="${command.adresseEnvoi.lignes}" id="ligne" class="list" cellspacing="2" >
			<display:column>${ligne}</display:column>
		</display:table>

		<c:if test="${fn:length(command.exceptions) > 0}">
			<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
				<tr><td class="heading iepngfix">Les adresses n'ont pas toutes pu être calculées pour les raisons suivantes:</td></tr>
				<tr><td class="details"><ul>
				<c:forEach var="err" items="${command.exceptions}">
					<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${err.message}"/></li>
				</c:forEach>
				</ul></td></tr>
			</table>
		</c:if>

		<script>
			$(function() {
				$("#adresses").tabs({cookie:{}});
				$("#legend").dialog({title: 'Légende', position: ['right','bottom']});
			});
		</script>

		<div id="adresses">
			<ul>
				<c:forEach var="table" varStatus="i" items="${command.tables}">
					<li><a href="#tabs-${i.index}"><c:out value="${table.nom}"/></a></li>
				</c:forEach>
			</ul>

			<c:forEach var="table" varStatus="i" items="${command.tables}">
				<div id="tabs-${i.index}">

					<table class="timeline">
						<tr>
							<th>Période</th>
							<c:forEach var="colonne" varStatus="status" items="${table.columns}" >
								<th><c:out value="${colonne.description}"/></th>
							</c:forEach>
						</tr>

						<c:set var="cellCounter" value="${0}" />

						<c:forEach var="ligne" varStatus="status" items="${table.rows}" >
							<tr>
								<%-- période --%>
								<td class="periode">
									<c:if test="${ligne.periode.dateDebut != null}">
										<unireg:date date="${ligne.periode.dateDebut}" />
									</c:if>
									<c:if test="${ligne.periode.dateDebut == null}">
										...
									</c:if>
									<br/>
									<c:if test="${ligne.periode.dateFin != null}">
										<unireg:date date="${ligne.periode.dateFin}" />
									</c:if>
									<c:if test="${ligne.periode.dateFin == null}">
										...
									</c:if>
								</td>

								<c:forEach var="cell" varStatus="status" items="${ligne.columns}" >
									<c:set var="cellCounter" value="${cellCounter + 1}" />
									<c:choose>
										<c:when test="${cell.filler}">
											<td class="filler" />
										</c:when>
										<c:when test="${!cell.span && !cell.filler}">
											<c:set var="range" value="${cell.range}" />
											<td class="<c:out value="${fn:toLowerCase(range.source.type)}" />" rowspan="<c:out value="${cell.longueurAffichage}" />">
												<span class="staticTip" id="range-${cellCounter}">
													<c:out value="${range.rue}"/> <c:out value="${range.numero}"/><br/><c:out value="${range.numeroPostal}"/> <c:out value="${range.localiteComplete}"/>
												</span>
												<div id="range-${cellCounter}-tooltip" style="display:none;">
													Id : <b><c:out value="${range.id}"/></b><br/>
													Ouverture : <b><unireg:date date="${range.dateDebut}"/></b><br/>
													Fermeture : <b><unireg:date date="${range.dateFin}"/></b><br/>
													Complément : <b><c:out value="${range.complement}"/></b><br/>
													Case Postale : <b><c:out value="${range.casePostale}"/></b><br/>
													Rue : <b><c:out value="${range.rue}"/></b><br/>
													Numéro : <b><c:out value="${range.numero}"/></b><br/>
													NPA : <b><c:out value="${range.numeroPostal}"/></b><br/>
													Localité : <b><c:out value="${range.localiteComplete}"/></b><br/>
													NPA Cpl. : <b><c:out value="${range.numeroPostalComplementaire}"/></b><br/>
													No Rue : <b><c:out value="${range.numeroRue}"/></b><br/>
													No Ordre Poste : <b><c:out value="${range.numeroOrdrePostal}"/></b><br/>
													No Ofs Pays : <b><c:out value="${range.noOfsPays}"/></b><br/>
													Egid : <b><c:out value="${range.egid}"/></b><br/>
													Ewid : <b><c:out value="${range.ewid}"/></b><br/>
													Source : <b><c:out value="${range.source.type}"/></b><br/>
													Défaut : <b><c:out value="${range.default}"/></b><br/>
													Permanente : <b><c:out value="${range.permanente}"/></b><br/>
												</div>
											</td>
										</c:when>
										<c:when test="${cell.span}">
											<%-- rien à mettre, le rowspan est automatiquement rempli --%>
										</c:when>
									</c:choose>
								</c:forEach>

							</tr>
						</c:forEach>
					</table>

				</div>
			</c:forEach>
		</div>


		<script>
			$(function() {
				Tooltips.activate_static_tooltips();
			});
		</script>

	</tiles:put>
	
</tiles:insert>
