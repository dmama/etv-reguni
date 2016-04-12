<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.EditDomicileView"--%>
<%--@elvariable id="peutEditerDateFin" type="boolean"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.civil.domicile">
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false" titre="Caractéristiques de l'établissement"/>

		<table border="0"><tr valign="top">
		<td>
			<form:form id="editDomicileForm" commandName="command" action="edit.do">
				<fieldset>
					<legend><span><fmt:message key="label.etablissement.domicile" /></span></legend>

					<form:hidden path="id"/>
					<form:hidden path="tiersId" value="${command.tiersId}"/>
					<form:hidden path="dateDebut" value="${RegDateHelper.StringFormat.DISPLAY(command.dateDebut)}"/>

					<script type="text/javascript">

						function selectAutoriteFiscale(name) {
							if (name == 'COMMUNE_OU_FRACTION_VD') {
								$('#domicile_commune_vd_label').show();
								$('#domicile_commune_hc_label').hide();
								$('#domicile_pays_label').hide();
								$('#autoriteFiscale').val(null);
								$('#noAutoriteFiscale').val(null);
								Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale');
							}
							else if (name == 'COMMUNE_HC') {
								$('#domicile_commune_vd_label').hide();
								$('#domicile_commune_hc_label').show();
								$('#domicile_pays_label').hide();
								$('#autoriteFiscale').val(null);
								$('#noAutoriteFiscale').val(null);
								Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale');
							}
							else if (name == 'PAYS_HS') {
								$('#domicile_commune_vd_label').hide();
								$('#domicile_commune_hc_label').hide();
								$('#domicile_pays_label').show();
								$('#autoriteFiscale').val(null);
								$('#noAutoriteFiscale').val(null);
								Fors.autoCompletePaysHS('#autoriteFiscale', '#noAutoriteFiscale');
							}
						}
					</script>

					<!-- Debut For -->
					<table border="0">
						<unireg:nextRowClass reset="0"/>
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
							<td><unireg:regdate regdate="${command.dateDebut}"/></td>
							<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
							<td>
								<c:choose>
									<c:when test="${peutEditerDateFin}">
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="dateFin" />
											<jsp:param name="id" value="dateFin" />
										</jsp:include>
									</c:when>
									<c:otherwise>
										<unireg:regdate regdate="${command.dateFin}"/>
										<form:hidden path="dateFin" value="${RegDateHelper.StringFormat.DISPLAY(command.dateFin)}"/>
									</c:otherwise>
								</c:choose>
							</td>
						</tr>

						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.type.domicile"/>&nbsp;:</td>
							<td>
								<div id="select_type_for">
									<%--@elvariable id="typesDomicileFiscal" type="java.util.Map<TypeAutoriteFiscale, String>"--%>
									<form:select path="typeAutoriteFiscale" items="${typesDomicileFiscal}" id="optionTypeAutoriteFiscale"
									             onchange="selectAutoriteFiscale(this.options[this.selectedIndex].value);" />
										<form:errors path="typeAutoriteFiscale" cssClass="error" />
								</div>
							</td>
							<td>
								<label for="autoriteFiscale">
									<span id="domicile_commune_vd_label"><fmt:message key="label.commune.fraction"/></span>
									<span id="domicile_commune_hc_label"><fmt:message key="label.commune"/></span>
									<span id="domicile_pays_label"><fmt:message key="label.pays"/></span>
									&nbsp;:
								</label>
							</td>
							<td>
								<input id="autoriteFiscale" size="25" />
								<form:errors path="noAutoriteFiscale" cssClass="error" />
								<form:hidden path="noAutoriteFiscale" />
							</td>
						</tr>
					</table>
				</fieldset>

				<script type="text/javascript">
					// on initialise l'auto-completion de l'autorité fiscale
		 			selectAutoriteFiscale('${command.typeAutoriteFiscale}');
				</script>

				<table border="0">
					<tr>
						<td width="25%">&nbsp;</td>
						<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
						<td width="25%"><unireg:buttonTo name="Retour" action="/civil/etablissement/edit.do" params="{id:${command.tiersId}}" method="GET"/> </td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>
			</form:form>

		</td>
		<td id="actions_column" style="display:none">
			<div id="actions_list"></div>
		</td>
		</tr></table>

		<script type="text/javascript">
			<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">
				$('#autoriteFiscale').val('<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}" escapeMode="javascript"/>');
				Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale');
			</c:if>
			<c:if test="${command.typeAutoriteFiscale == 'COMMUNE_HC'}">
				$('#autoriteFiscale').val('<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}" escapeMode="javascript"/>');
				Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale');
			</c:if>
			<c:if test="${command.typeAutoriteFiscale == 'PAYS_HS'}">
				$('#autoriteFiscale').val('<unireg:pays ofs="${command.noAutoriteFiscale}" displayProperty="nomCourt" date="${command.dateDebut}" escapeMode="javascript"/>');
				Fors.autoCompletePaysHS('#autoriteFiscale', '#noAutoriteFiscale');
			</c:if>
		</script>

	</tiles:put>
</tiles:insert>
