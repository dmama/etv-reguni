<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:if test="${not empty command.etats}">
<fieldset>
	<legend><span><fmt:message key="label.etats" /></span></legend>
	
	<display:table 	name="command.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.obtention" >
				<unireg:regdate regdate="${etat.dateObtention}" />
				<c:if test="${!etat.annule && etat.etat == 'SOMMEE'}">
					&nbsp;
					(<fmt:message key="label.date.envoi.courrier">
					 	<fmt:param><unireg:date date="${etat.dateEnvoiCourrier}"/></fmt:param>
					</fmt:message>)
				</c:if>
		</display:column>
 		<display:column titleKey="label.etat">
				<fmt:message key="option.etat.avancement.${etat.etat}" />
			<c:if test="${!etat.annule && etat.etat == 'SOMMEE'}">
				&nbsp;
				<a href="../declaration/copie-conforme-sommation.do?idEtat=${etat.id}" class="pdf" id="copie-sommation-${etat.id}" onclick="Link.tempSwap(this, '#disabled-copie-sommation-${etat.id}');">&nbsp;</a>
				<span class="pdf-grayed" id="disabled-copie-sommation-${etat.id}" style="display: none;">&nbsp;</span>
			</c:if>
		</display:column>
 		<display:column titleKey="label.source">
			<c:if test="${etat.etat == 'RETOURNEE'}">
				<c:if test="${etat.source == null}">
					<fmt:message key="option.source.quittancement.UNKNOWN" />
				</c:if>
				<c:if test="${etat.source != null}">
					<fmt:message key="option.source.quittancement.${etat.source}" />
				</c:if>
			</c:if>
		</display:column>
		<display:column style="action">
			<unireg:consulterLog entityNature="EtatDeclaration" entityId="${etat.id}"/>
		</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

</fieldset>
</c:if>