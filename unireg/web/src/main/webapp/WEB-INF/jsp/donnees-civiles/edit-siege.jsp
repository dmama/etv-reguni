<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.SiegeView.Edit"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.civil.siege">
  			<fmt:param><unireg:numCTB numero="${command.entrepriseId}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${command.entrepriseId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false" titre="Caractéristiques de l'entreprise"/>

		<table border="0"><tr valign="top">
		<td>
			<form:form id="editDomicileForm" commandName="command" action="edit.do">
				<fieldset>
					<legend><span><fmt:message key="label.entreprise.siege" /></span></legend>

					<form:hidden path="id"/>
					<form:hidden path="etablissementId"/>
					<form:hidden path="entrepriseId"/>
					<form:hidden path="dateDebut"/>
					<form:hidden path="peutEditerDateFin"/>

					<script type="text/javascript">
						function selectAutoriteFiscale(name, reset) {
							if (name == 'COMMUNE_OU_FRACTION_VD') {
								$('#domicile_commune_vd_label').show();
								$('#domicile_commune_hc_label').hide();
								$('#domicile_pays_label').hide();
								if (reset) {
									<c:choose>
										<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">
											var nom = '<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}" escapeMode="javascript"/>';
											$('#autoriteFiscale').val(nom);
											$('#noAutoriteFiscale').val(${command.noAutoriteFiscale});
											$('#nomAutoriteFiscale').val(nom);
										</c:when>
										<c:otherwise>
											$('#autoriteFiscale').val(null);
											$('#noAutoriteFiscale').val(null);
											$('#nomAutoriteFiscale').val(null);
										</c:otherwise>
									</c:choose>
								}
								Fors.autoCompleteCommunesVD('#autoriteFiscale', '#noAutoriteFiscale', function(item) {
									$('#nomAutoriteFiscale').val(item ? item.label : null);
								});
							}
							else if (name == 'COMMUNE_HC') {
								$('#domicile_commune_vd_label').hide();
								$('#domicile_commune_hc_label').show();
								$('#domicile_pays_label').hide();
								if (reset) {
									<c:choose>
										<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC'}">
											var nom = '<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}" escapeMode="javascript"/>';
											$('#autoriteFiscale').val(nom);
											$('#noAutoriteFiscale').val(${command.noAutoriteFiscale});
											$('#nomAutoriteFiscale').val(nom);
										</c:when>
										<c:otherwise>
											$('#autoriteFiscale').val(null);
											$('#noAutoriteFiscale').val(null);
											$('#nomAutoriteFiscale').val(null);
										</c:otherwise>
									</c:choose>
								}
								Fors.autoCompleteCommunesHC('#autoriteFiscale', '#noAutoriteFiscale', function(item) {
									$('#nomAutoriteFiscale').val(item ? item.label : null);
								});
							}
							else if (name == 'PAYS_HS') {
								$('#domicile_commune_vd_label').hide();
								$('#domicile_commune_hc_label').hide();
								$('#domicile_pays_label').show();
								if (reset) {
									<c:choose>
										<c:when test="${command.typeAutoriteFiscale == 'PAYS_HS'}">
											var nom = '<unireg:pays ofs="${command.noAutoriteFiscale}" displayProperty="nomCourt" date="${command.dateDebut}" escapeMode="javascript"/>';
											$('#autoriteFiscale').val(nom);
											$('#noAutoriteFiscale').val(${command.noAutoriteFiscale});
											$('#nomAutoriteFiscale').val(nom);
										</c:when>
										<c:otherwise>
											$('#autoriteFiscale').val(null);
											$('#noAutoriteFiscale').val(null);
											$('#nomAutoriteFiscale').val(null);
										</c:otherwise>
									</c:choose>
								}
								Fors.autoCompletePaysHS('#autoriteFiscale', '#noAutoriteFiscale', function(item) {
									$('#nomAutoriteFiscale').val(item ? item.label : null);
								});
							}
						}
					</script>

					<!-- Debut For -->
					<table border="0">
						<unireg:nextRowClass reset="0"/>
						<tr class="<unireg:nextRowClass/>" >
							<td width="20%"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
							<td width="30%"><unireg:regdate regdate="${command.dateDebut}"/></td>
							<td width="20%"><fmt:message key="label.date.fermeture" />&nbsp;:</td>
							<td width="30%">
								<c:choose>
									<c:when test="${command.peutEditerDateFin}">
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="dateFin" />
											<jsp:param name="id" value="dateFin" />
										</jsp:include>
									</c:when>
									<c:otherwise>
										<form:hidden path="dateFin"/>
										<unireg:regdate regdate="${command.dateFin}"/>
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
									             onchange="selectAutoriteFiscale(this.options[this.selectedIndex].value, true);" />
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
								<span style="color: red;">*</span>
								<form:errors path="noAutoriteFiscale" cssClass="error" />
								<form:hidden path="noAutoriteFiscale" />
								<form:hidden path="nomAutoriteFiscale"/>
							</td>
						</tr>
					</table>
				</fieldset>

				<table border="0">
					<tr>
						<td width="25%">&nbsp;</td>
						<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
						<td width="25%"><unireg:buttonTo name="Retour" action="/civil/entreprise/edit.do" params="{id:${command.entrepriseId}}" method="GET"/> </td>
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
			$(function() {
				selectAutoriteFiscale('${command.typeAutoriteFiscale}', true);
			});
		</script>

	</tiles:put>
</tiles:insert>
