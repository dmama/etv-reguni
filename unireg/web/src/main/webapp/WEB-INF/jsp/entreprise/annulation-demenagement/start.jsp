<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.demenagement.siege">
			<fmt:param>
				<unireg:numCTB numero="${command.idEntreprise}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.idEntreprise}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
		<unireg:nextRowClass reset="0"/>

		<table border="0">
			<tr>
				<td width="40%">
					<fieldset>
						<legend><span><fmt:message key="label.caracteristiques.siege.actuel"/></span></legend>
						<table>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.date.debut"/></td>
								<td width="75%"><unireg:regdate regdate="${dateDebutSiegeActuel}"/></td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.commune.pays"/></td>
								<td width="75%">
									<c:if test="${noOfsSiegeActuel != null}">
										<c:choose>
											<c:when test="${typeAutoriteFiscaleSiegeActuel == 'COMMUNE_OU_FRACTION_VD'}">
												<unireg:commune ofs="${noOfsSiegeActuel}" displayProperty="nomOfficiel" date="${dateDebutSiegeActuel}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleSiegeActuel == 'COMMUNE_HC'}">
												<unireg:commune ofs="${noOfsSiegeActuel}" displayProperty="nomOfficielAvecCanton" date="${dateDebutSiegeActuel}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleSiegeActuel == 'PAYS_HS'}">
												<unireg:pays ofs="${noOfsSiegeActuel}" displayProperty="nomCourt" date="${dateDebutSiegeActuel}" titleProperty="noOFS"/>
											</c:when>
										</c:choose>
									</c:if>
								</td>
							</tr>
						</table>
					</fieldset>
				</td>
				<td width="20%">
					<table id="flecheRemplacement" cellpadding="0" cellspacing="0">
						<tr>
							<td style="width:1em;"/>
							<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"/>
							<td id="flecheMilieu" class="fleche_milieu"><fmt:message key="label.annule.au.profit.de"/></td>
							<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"/>
							<td style="width:1em;"/>
						</tr>
					</table>
				</td>
				<td width="40%">
					<fieldset>
						<legend><span><fmt:message key="label.caracteristiques.siege.precedent"/></span></legend>
						<table>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.date.debut"/></td>
								<td width="75%"><unireg:regdate regdate="${dateDebutSiegePrecedent}"/></td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.commune.pays"/></td>
								<td width="75%">
									<c:if test="${noOfsSiegePrecedent != null}">
										<c:choose>
											<c:when test="${typeAutoriteFiscaleSiegePrecedent == 'COMMUNE_OU_FRACTION_VD'}">
												<unireg:commune ofs="${noOfsSiegePrecedent}" displayProperty="nomOfficiel" date="${dateDebutSiegePrecedent}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleSiegePrecedent == 'COMMUNE_HC'}">
												<unireg:commune ofs="${noOfsSiegePrecedent}" displayProperty="nomOfficielAvecCanton" date="${dateDebutSiegePrecedent}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleSiegePrecedent == 'PAYS_HS'}">
												<unireg:pays ofs="${noOfsSiegePrecedent}" displayProperty="nomCourt" date="${dateDebutSiegePrecedent}" titleProperty="noOFS"/>
											</c:when>
										</c:choose>
									</c:if>
								</td>
							</tr>
						</table>
					</fieldset>
				</td>
			</tr>
		</table>

		<form:form method="post" id="recapDemenagement" name="recapDemenagement">
			<form:hidden path="idEntreprise"/>
			<form:hidden path="dateDebutSiegeActuel"/>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment annuler le déménagement de siège de cette entreprise ?');" />
			<!-- Fin Boutons -->

		</form:form>

	</tiles:put>

</tiles:insert>