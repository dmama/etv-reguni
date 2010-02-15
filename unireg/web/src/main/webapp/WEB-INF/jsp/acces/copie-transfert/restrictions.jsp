<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:if test="${not empty command.droitsAccesView}">
<fieldset>
	<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<display:table
				name="command.droitsAccesView" id="restriction" pagesize="10" 
				requestURI="${url}"
				class="display">
		
			<display:column sortable ="true" titleKey="label.type.restriction">
				<c:if test="${restriction.annule}"><strike></c:if>
					<fmt:message key="option.type.droit.acces.${restriction.type}"  />
				<c:if test="${restriction.annule}"></strike></c:if>
			</display:column>
			
			<display:column sortable ="true" titleKey="label.numero.contribuable" >
				<c:if test="${restriction.annule}"><strike></c:if>
					<unireg:numCTB numero="${restriction.numeroCTB}" />
				<c:if test="${restriction.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.prenom.nom">
				<c:if test="${restriction.annule}"><strike></c:if>
					<c:out value="${restriction.prenomNom}" />
				<c:if test="${restriction.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.localite">
				<c:if test="${restriction.annule}"><strike></c:if>
					<c:out value="${restriction.localite}" />
				<c:if test="${restriction.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance">
				<c:if test="${restriction.annule}"><strike></c:if>
					<unireg:date date="${restriction.dateNaissance}"></unireg:date>
				<c:if test="${restriction.annule}"></strike></c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.lecture.seule">
				<input type="checkbox" name="lectureSeule" value="True"   
						<c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
			
		</display:table>
</fieldset>
</c:if>