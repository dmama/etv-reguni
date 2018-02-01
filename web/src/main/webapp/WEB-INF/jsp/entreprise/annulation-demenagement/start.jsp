<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.AnnulationDemenagementSiegeView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.annulation.demenagement.siege">
			<fmt:param>
				<unireg:numCTB numero="${actionCommand.idEntreprise}"/>
			</fmt:param>
		</fmt:message>
		<c:choose>
			<c:when test="${entrepriseConnueAuRegistreCivil}">
				(<fmt:message key="label.entreprise.connue.registre.civil"/>)
			</c:when>
			<c:otherwise>
				(<fmt:message key="label.entreprise.inconnue.registre.civil"/>)
			</c:otherwise>
		</c:choose>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${actionCommand.idEntreprise}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" showComplements="true"/>
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
				<td width="20%" rowspan="2">
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
						<c:choose>
							<c:when test="${entrepriseConnueAuRegistreCivil}">
							<fieldset>
								<legend><span><fmt:message key="label.caracteristiques.siege.actuel"/></span></legend>
								<div style="position: relative;">
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
									<div style="position: absolute; top: 10%; left: 30%; width: 70%;">
										<div style="text-align: center; transform: rotate(-10deg); font-weight: bold; font-size: 135%;" class="warn">
											<fmt:message key="label.valeur.inchangee.civile"/>
										</div>
									</div>
								</div>
							</fieldset>
							</c:when>
							<c:otherwise>
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
							</c:otherwise>
						</c:choose>
				</td>
			</tr>
			<tr>
				<td width="40%">
					<fieldset>
						<legend><span><fmt:message key="label.caracteristiques.for.principal.actuel"/></span></legend>
						<table>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.date.debut"/></td>
								<td width="75%"><unireg:regdate regdate="${dateDebutForPrincipalActuel}"/></td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.commune.pays"/></td>
								<td width="75%">
									<c:if test="${noOfsForPrincipalActuel != null}">
										<c:choose>
											<c:when test="${typeAutoriteFiscaleForPrincipalActuel == 'COMMUNE_OU_FRACTION_VD'}">
												<unireg:commune ofs="${noOfsForPrincipalActuel}" displayProperty="nomOfficiel" date="${dateDebutForPrincipalActuel}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleForPrincipalActuel == 'COMMUNE_HC'}">
												<unireg:commune ofs="${noOfsForPrincipalActuel}" displayProperty="nomOfficielAvecCanton" date="${dateDebutForPrincipalActuel}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleForPrincipalActuel == 'PAYS_HS'}">
												<unireg:pays ofs="${noOfsForPrincipalActuel}" displayProperty="nomCourt" date="${dateDebutForPrincipalActuel}" titleProperty="noOFS"/>
											</c:when>
										</c:choose>
									</c:if>
								</td>
							</tr>
						</table>
					</fieldset>
				</td>
				<td width="40%">
					<fieldset>
						<legend><span><fmt:message key="label.caracteristiques.for.principal.precedent"/></span></legend>
						<table>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.date.debut"/></td>
								<td width="75%"><unireg:regdate regdate="${dateDebutForPrincipalPrecedent}"/></td>
							</tr>
							<tr class="<unireg:nextRowClass/>">
								<td width="25%"><fmt:message key="label.commune.pays"/></td>
								<td width="75%">
									<c:if test="${noOfsForPrincipalPrecedent != null}">
										<c:choose>
											<c:when test="${typeAutoriteFiscaleForPrincipalPrecedent == 'COMMUNE_OU_FRACTION_VD'}">
												<unireg:commune ofs="${noOfsForPrincipalPrecedent}" displayProperty="nomOfficiel" date="${dateDebutForPrincipalPrecedent}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleForPrincipalPrecedent == 'COMMUNE_HC'}">
												<unireg:commune ofs="${noOfsForPrincipalPrecedent}" displayProperty="nomOfficielAvecCanton" date="${dateDebutForPrincipalPrecedent}" titleProperty="noOFS"/>
											</c:when>
											<c:when test="${typeAutoriteFiscaleForPrincipalPrecedent == 'PAYS_HS'}">
												<unireg:pays ofs="${noOfsForPrincipalPrecedent}" displayProperty="nomCourt" date="${dateDebutForPrincipalPrecedent}" titleProperty="noOFS"/>
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

		<form:form method="post" id="recapDemenagement" name="recapDemenagement" commandName="actionCommand">
			<form:hidden path="idEntreprise"/>
			<form:hidden path="dateDebutSiegeActuel"/>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment annuler le déménagement de siège de cette entreprise ?');" />
			<!-- Fin Boutons -->

		</form:form>

	</tiles:put>

</tiles:insert>