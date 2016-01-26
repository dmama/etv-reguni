<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.specificites"/></span></legend>

	<span class="error">Attention ! Les données ci-dessous correspondent aux états de l'entreprise... A adapter !!</span>

	<c:if test="${not empty command.entreprise.etats}">

		<display:table name="${command.entreprise.etats}" id="etatPM" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column titleKey="label.date.obtention">
				<unireg:regdate regdate="${etatPM.dateObtention}"/>
			</display:column>
			<display:column titleKey="label.type">
				<fmt:message key="option.etat.entreprise.${etatPM.type}"/>
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="EtatEntreprise" entityId="${etatPM.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>
