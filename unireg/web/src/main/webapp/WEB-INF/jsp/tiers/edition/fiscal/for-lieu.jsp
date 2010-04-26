<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
	<td id="select_type_for" >
		<form:select path="typeAutoriteFiscale" items="${typesForFiscal}" id="optionTypeAutoriteFiscale" 
					 onchange="selectForFiscal(this.options[this.selectedIndex].value);" />
	</td> 
	<td id="type_for_fraction" style="display:none;"><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD" /></td>
	<td id="type_for_hs" style="display:none;"><fmt:message key="option.type.for.fiscal.PAYS_HS" /></td>
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
			<jsp:param name="dataTextField" value="{nomMinuscule} ({noTechnique})" />
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
	<td id="for_pays_label" style="display:none;"><fmt:message key="label.pays"/>&nbsp;:</td>
	<td id="for_pays" style="display:none;">
		<form:input path="libPays" id="libPays" size="25" />
		<form:errors path="libPays" cssClass="error" />
		<form:hidden path="numeroForFiscalPays" />			
		<script type="text/javascript">
					function libPays_onChange(row) {
						document.forms["formFor"].numeroForFiscalPays.value = (row ? row.noOFS : "");
					}
		</script>
		<jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
			<jsp:param name="inputId" value="libPays" />
			<jsp:param name="dataValueField" value="nomMinuscule" />
			<jsp:param name="dataTextField" value="{nomMinuscule} ({noOFS})" />
			<jsp:param name="dataSource" value="selectionnerPays" />
			<jsp:param name="onChange" value="libPays_onChange" />
		</jsp:include>
	</td>
</tr>