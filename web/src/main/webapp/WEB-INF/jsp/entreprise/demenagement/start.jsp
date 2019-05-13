<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="actionCommand" type="ch.vd.unireg.entreprise.complexe.DemenagementSiegeView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.traitement.demenagement.siege">
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

		<form:form method="post" id="recapDemenagement" name="recapDemenagement" modelAttribute="actionCommand">
			<form:hidden path="idEntreprise"/>

			<script type="application/javascript">

				function selectAutoriteFiscale(name, reset) {
					if (name == 'COMMUNE_OU_FRACTION_VD') {
						$('#siege_commune_vd_label').show();
						$('#siege_commune_hc_label').hide();
						$('#siege_pays_label').hide();
						if (reset) {
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							$('#nomAutoriteFiscale').val(null);
						}
						Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale', function(item) {
							$('#nomAutoriteFiscale').val(item ? item.label : null);
						});
					}
					else if (name == 'COMMUNE_HC') {
						$('#siege_commune_vd_label').hide();
						$('#siege_commune_hc_label').show();
						$('#siege_pays_label').hide();
						if (reset) {
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							$('#nomAutoriteFiscale').val(null);
						}
						Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale', function(item) {
							$('#nomAutoriteFiscale').val(item ? item.label : null);
						});
					}
					else if (name == 'PAYS_HS') {
						$('#siege_commune_vd_label').hide();
						$('#siege_commune_hc_label').hide();
						$('#siege_pays_label').show();
						if (reset) {
							$('#autoriteFiscale').val(null);
							$('#noAutoriteFiscale').val(null);
							$('#nomAutoriteFiscale').val(null);
						}
						Fors.autoCompletePaysHS('#autoriteFiscale', '#noAutoriteFiscale', function(item) {
							$('#nomAutoriteFiscale').val(item ? item.label : null);
						});
					}
				}

			</script>

			<unireg:nextRowClass reset="0"/>
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.demenagement" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut.nouveau.siege" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebutNouveauSiege" />
								<jsp:param name="id" value="dateDebutNouveauSiege" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>

					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.siege"/>&nbsp;:</td>
						<td width="75%">
							<div id="select_type_siege">
								<%--@elvariable id="typesAutoriteFiscale" type="java.util.Map<TypeAutoriteFiscale, String>"--%>
								<form:select path="typeAutoriteFiscale" items="${typesAutoriteFiscale}" id="optionTypeAutoriteFiscale"
								             onchange="selectAutoriteFiscale(this.options[this.selectedIndex].value, true);" />
							</div>
							<div id="mandatory_type_siege" style="display: none;"></div>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%">
							<label for="autoriteFiscale">
								<span id="siege_commune_vd_label"><fmt:message key="label.commune.fraction"/></span>
								<span id="siege_commune_hc_label"><fmt:message key="label.commune"/></span>
								<span id="siege_pays_label"><fmt:message key="label.pays"/></span>
								&nbsp;:
							</label>
						</td>
						<td>
							<input id="autoriteFiscale" size="25" value="${actionCommand.nomAutoriteFiscale}"/>
							<span class="mandatory">*</span>
							<form:errors path="noAutoriteFiscale" cssClass="error" />
							<form:hidden path="noAutoriteFiscale" />
							<form:hidden path="nomAutoriteFiscale" />
						</td>
					</tr>

				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="return confirm('Voulez-vous vraiment déménager le siège de cette entreprise ?');" />
			<!-- Fin Boutons -->

		</form:form>


		<script type="application/javascript">
			$(function() {
				selectAutoriteFiscale('${actionCommand.typeAutoriteFiscale}', false);
			});
		</script>

	</tiles:put>

</tiles:insert>