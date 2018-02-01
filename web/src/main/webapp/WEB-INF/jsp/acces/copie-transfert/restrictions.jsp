<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:if test="${not empty command.droitsAccesView}">
<fieldset>
	<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<display:table
				name="command.droitsAccesView" id="restriction" pagesize="25" sort="external"
				requestURI="${url}" partialList="true" size="command.size"
				class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
		
			<display:column sortable="false" titleKey="label.type.restriction">
				<fmt:message key="option.type.droit.acces.${restriction.type}"  />
			</display:column>			
			<display:column sortable="false" titleKey="label.numero.contribuable" sortProperty="numeroCTB">
				<unireg:numCTB numero="${restriction.numeroCTB}" />
			</display:column>
			<display:column sortable="false" titleKey="label.nom.raison">
				<c:choose>
					<c:when test="${restriction.erreur != null}">
						<span class="erreur"><c:out value="${restriction.erreur}"/></span>
					</c:when>
					<c:otherwise>
						<c:out value="${restriction.prenomNom}" />
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column sortable="false" titleKey="label.localite">
				<c:out value="${restriction.localite}" />
			</display:column>
			<display:column sortable="false" titleKey="label.date.naissance.ou.rc" sortProperty="dateNaissance">
				<unireg:date date="${restriction.dateNaissance}"/>
			</display:column>
			<display:column sortable="false" titleKey="label.lecture.seule">
				<input type="checkbox" name="lectureSeule" value="true"
				       <c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
			</display:column>
			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
			
		</display:table>
</fieldset>
</c:if>