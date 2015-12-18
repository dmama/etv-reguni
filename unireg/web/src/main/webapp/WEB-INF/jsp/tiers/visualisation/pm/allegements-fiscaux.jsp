<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.allegements.fiscaux"/></span></legend>

	<c:if test="${empty command.allegementsFiscaux}">
		<fmt:message key="no.data" />
	</c:if>

	<c:if test="${not empty command.allegementsFiscaux}">
	
		<display:table name="${command.allegementsFiscaux}" id="allegement" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<unireg:regdate regdate="${allegement.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${allegement.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type.impot">
				<fmt:message key="option.allegement.fiscal.type.impot.${allegement.typeImpot}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type.collectivite">
				<fmt:message key="option.allegement.fiscal.type.collectivite.${allegement.typeCollectivite}"/>
				<c:if test="${allegement.typeCollectivite == 'COMMUNE' && allegement.noOfsCommune != null}">
					&nbsp;(<unireg:commune ofs="${allegement.noOfsCommune}" displayProperty="nomOfficiel" titleProperty="noOFS"/>)
				</c:if>
			</display:column>
			<display:column sortable="true" titleKey="label.allegements.fiscaux.pourcentage" sortProperty="pourcentage">
				<c:if test="${allegement.pourcentage != null}">
					<fmt:formatNumber maxIntegerDigits="3" minIntegerDigits="1" maxFractionDigits="2" minFractionDigits="0" value="${allegement.pourcentage}"/>&nbsp;%
				</c:if>
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="AllegementFiscal" entityId="${allegement.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>
