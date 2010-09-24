<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.creation.fors">
  			<fmt:param><unireg:numCTB numero="${command.numeroCtb}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>	<tiles:put name="body">
		<form:form name="formFor" id="formFor">
		<fieldset><legend><span><fmt:message key="label.for.fiscal" /></span></legend>		

		<!-- Debut For -->
		<table border="0">
			<tr id="date_for_periodique"  class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
				<td>
					<script type="text/javascript">
						// [UNIREG-2507] si la date de début du for est avant 2009, on affiche un warning non-bloquant
						var anneeMinimaleForDebiteur = <c:out value="${anneeMinimaleForDebiteur}"/>;
						function dateOuverture_OnChange(element) {
							if (element) {
								var m = element.value.match(/\d{1,2}\.\d{1,2}\.(\d{4})/);
								if (m) {
									var annee = parseInt(m[1]);
									if (annee < anneeMinimaleForDebiteur) {
										Element.show('dateOuverture.warning');
									}
									else {
										Element.hide('dateOuverture.warning');
									}
								}
								else {
									Element.hide('dateOuverture.warning');
								}
							}
						}
					</script>

					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateOuverture" />
						<jsp:param name="id" value="dateOuverture" />
						<jsp:param name="onChange" value="dateOuverture_OnChange"/>
					</jsp:include>

					<span class="error" style="display:none" id="dateOuverture.warning">
						<fmt:message key="warning.for.debiteur.ouvert.avant.date">
							<fmt:param><c:out value="${anneeMinimaleForDebiteur}"/></fmt:param>
						</fmt:message>
					</span>

					<script type="text/javascript">
						// pour forcer la validation au chargement
						dateOuverture_OnChange(E$('dateOuverture'));
					</script>

				</td>
				<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateFermeture" />
						<jsp:param name="id" value="dateFermeture" />
					</jsp:include>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				
				<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
				<td id="select_type_for" >
					<form:select path="typeAutoriteFiscale" items="${typesForFiscalDPI}" id="optionTypeAutoriteFiscale"  
								 onchange="selectForFiscalDPI(this.options[this.selectedIndex].value);" />
				</td> 
				<td id="for_fraction_commune_label"><fmt:message key="label.commune.fraction"/>&nbsp;:</td>
				<td id="for_fraction_commune">
					<form:input path="libFractionCommune" id="libFractionCommune" size="25" />
					<form:errors path="libFractionCommune" cssClass="error" />
					<form:hidden path="numeroForFiscalCommune" />		
					<script type="text/javascript">
								function libCommune_onChange(row) {
									document.forms["formFor"].numeroForFiscalCommune.value = (row ? row.noTechnique : "");
								}
						</script>
					<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
						<jsp:param name="inputId" value="libFractionCommune" />
						<jsp:param name="dataValueField" value="nomMinuscule" />
						<jsp:param name="dataTextField" value="{nomMinuscule} ({noOFS})" />
						<jsp:param name="dataSource" value="selectionnerCommuneVD" />
						<jsp:param name="onChange" value="libCommune_onChange" />
						<jsp:param name="autoSynchrone" value="false"/>
					</jsp:include>
				</td>
				<td id="for_commune_label" style="display:none;"><fmt:message key="label.commune"/>&nbsp;:</td>
				<td id="for_commune" style="display:none;">
					<form:input path="libCommuneHorsCanton" id="libCommuneHorsCanton" size="25" />
					<form:errors path="libCommuneHorsCanton" cssClass="error" />
					<form:hidden path="numeroForFiscalCommuneHorsCanton" />			
					<script type="text/javascript">
								function libCommuneHorsCanton_onChange(row) {
									document.forms["formFor"].numeroForFiscalCommuneHorsCanton.value = (row ? row.noOFS : "");
								}
					</script>
					<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
						<jsp:param name="inputId" value="libCommuneHorsCanton" />
						<jsp:param name="dataValueField" value="nomMinuscule" />
						<jsp:param name="dataTextField" value="{nomMinuscule} {noTechnique} ({noOFS})" />
						<jsp:param name="dataSource" value="selectionnerCommuneHC" />
						<jsp:param name="onChange" value="libCommuneHorsCanton_onChange" />
						<jsp:param name="autoSynchrone" value="false"/>
					</jsp:include>
				</td>
			</tr>
		</table>
	</fieldset>
	<form:errors cssClass="error" />
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%"><input type="submit" id="ajouter" value="<fmt:message key="label.bouton.ajouter" />"></td>
			<td width="25%"><input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='../fiscal/edit-for-debiteur.do?id=' + ${command.numeroCtb}"></td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>	

	<script type="text/javascript">
		selectForFiscalDPI('${typeAutoriteFiscale}');
	</script>

	</tiles:put>
</tiles:insert>
