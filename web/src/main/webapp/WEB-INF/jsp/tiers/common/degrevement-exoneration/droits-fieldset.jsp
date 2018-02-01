<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="droits" type="java.util.List<ch.vd.unireg.registrefoncier.allegement.DroitView>"--%>

<fieldset class="information">
	<legend><span><fmt:message key="label.droits.rf"/></span></legend>

	<display:table name="${droits}" id="droit" class="display" decorator="ch.vd.unireg.decorator.TableAnnulableDateRangeDecorator">
		<display:column titleKey="label.date.debut">
			<unireg:regdate regdate="${droit.dateDebut}"/>
		</display:column>
		<display:column titleKey="label.date.fin">
			<unireg:regdate regdate="${droit.dateFin}"/>
		</display:column>
		<display:column titleKey="label.droit.type">
			<c:out value="${droit.type}"/>
		</display:column>
		<display:column titleKey="label.droit.regime">
			<c:if test="${droit.regimePropriete != null}">
				<fmt:message key="option.rf.genre.propriete.${droit.regimePropriete}"/>
			</c:if>
		</display:column>
		<display:column titleKey="label.droit.part">
			<c:if test="${droit.part != null}">
				<c:out value="${droit.part.numerateur}"/>&nbsp;/&nbsp;<c:out value="${droit.part.denominateur}"/>
			</c:if>
		</display:column>
		<display:column class="action" style="width: 5%;">
			<unireg:consulterLog entityNature="DroitRF" entityId="${droit.id}"/>
		</display:column>
	</display:table>

</fieldset>
