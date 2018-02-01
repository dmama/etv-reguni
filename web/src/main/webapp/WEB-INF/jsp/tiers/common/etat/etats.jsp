<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:if test="${not empty command.etats}">
<fieldset>
	<legend><span><fmt:message key="label.etats" /></span></legend>
	
	<display:table 	name="command.etats" id="etat" pagesize="10" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
		<display:column titleKey="label.date.obtention" >
				<unireg:regdate regdate="${etat.dateObtention}" />
				<c:if test="${!etat.annule && etat.etat == 'SOMME'}">
					&nbsp;
					(<c:out value="${etat.dateEnvoiCourrierMessage}"/>)
				</c:if>
		</display:column>
 		<display:column titleKey="label.etat">
				<fmt:message key="option.etat.avancement.f.${etat.etat}" />
			<c:if test="${!etat.annule && etat.etat == 'SOMME'}">
				&nbsp;
				<a href="../declaration/copie-conforme-sommation.do?idEtat=${etat.id}&url_memorize=false" class="pdf" id="copie-sommation-${etat.id}" onclick="Link.tempSwap(this, '#disabled-copie-sommation-${etat.id}');">&nbsp;</a>
				<span class="pdf-grayed" id="disabled-copie-sommation-${etat.id}" style="display: none;">&nbsp;</span>
			</c:if>
		</display:column>
 		<display:column titleKey="label.source">
			<c:if test="${etat.etat == 'RETOURNE'}">
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