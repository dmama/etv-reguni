<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.selection.immeuble"/></span></legend>

	<c:choose>
		<c:when test="${not empty command.communesImmeubles}">

			<table border="0" style="margin-left: 10px;">
				<tr>
					<td style="width: 15%;">
						<fmt:message key="label.commune"/>&nbsp;:
					</td>
					<td style="width: 20%;">
						<select onchange="DegrevementExoneration.onCommuneSelected(this.options[this.selectedIndex].value);" id="selectCommune">
							<option disabled selected><fmt:message key="label.choisir.commune"/></option>
							<c:forEach items="${command.communesImmeubles}" var="commune">
								<option value="${commune.noOfs}"><c:out value="${commune.nom}"/></option>
							</c:forEach>
						</select>
						<span class="mandatory">*</span>
					</td>
					<td style="width: 35%; display: none;" class="withParcelle">
						<fmt:message key="label.parcelle"/>&nbsp;:
						<select id="selectNoParcelle" style="width: 10ex;" onchange="DegrevementExoneration.onParcelleSelected();">
							<option></option>
						</select>
						&dash;
						<select id="selectIndex1" style="width: 6ex;" onchange="DegrevementExoneration.onParcelleSelected();">
							<option></option>
						</select>
						&dash;
						<select id="selectIndex2" style="width: 6ex;" onchange="DegrevementExoneration.onParcelleSelected();">
							<option></option>
						</select>
						&dash;
						<select id="selectIndex3" style="width: 6ex;" onchange="DegrevementExoneration.onParcelleSelected();">
							<option></option>
						</select>
					</td>
					<td>&nbsp;</td>
				</tr>
			</table>

			<div id="immeubles-sur-commune" style="display: none;">
				<fieldset>
					<legend><span><fmt:message key="label.immeubles"/></span></legend>
					<div id="immeubles-table"></div>
				</fieldset>
			</div>

			<script type="application/javascript">
				const DegrevementExoneration = {

					onCommuneSelected: function(ofsCommune) {
						var queryString = DegrevementExoneration.buildQueryString(ofsCommune, null, null, null, null);
						$.get(App.curl(queryString), function(choix) {
							const count = choix.immeubles.length;
							if (count == 0) {
								$('#immeubles-sur-commune').hide();
								$('.withParcelle').hide();
							}
							else {
								$('#immeubles-table').html(DegrevementExoneration.htmlTableImmeubles(choix.immeubles));
								$('#immeubles-sur-commune').show();

								$('#selectNoParcelle').html(DegrevementExoneration.htmlSelectNumeros(choix.numerosParcelles));
								$('#selectIndex1').html(DegrevementExoneration.htmlSelectNumeros(choix.numerosIndex1));
								$('#selectIndex2').html(DegrevementExoneration.htmlSelectNumeros(choix.numerosIndex2));
								$('#selectIndex3').html(DegrevementExoneration.htmlSelectNumeros(choix.numerosIndex3));
								$('.withParcelle').show();
							}
						}, 'json');
					},

					onParcelleSelected: function() {
						const ofsCommune = this.selectedValue('selectCommune');
						const noParcelle = this.selectedValue('selectNoParcelle');
						const index1 = this.selectedValue('selectIndex1');
						const index2 = this.selectedValue('selectIndex2');
						const index3 = this.selectedValue('selectIndex3');
						const queryString = DegrevementExoneration.buildQueryString(ofsCommune, noParcelle, index1, index2, index3);
						$.get(App.curl(queryString), function(choix) {
							$('#immeubles-table').html(DegrevementExoneration.htmlTableImmeubles(choix.immeubles));
						}, 'json');
					},

					selectedValue: function(id) {
						const select = $('#' + id)[0];
						return select.options[select.selectedIndex].value;
					},

					buildQueryString: function(ofsCommune, noParcelle, index1, index2, index3) {
						var queryString = '/degrevement-exoneration/immeubles.do?ctb=${command.entreprise.id}&ofsCommune=' + ofsCommune;
						if (noParcelle != null && noParcelle != '') {
							queryString += '&noParcelle=' + noParcelle;
						}
						if (index1 != null && index1 != '') {
							queryString += '&index1=' + index1;
						}
						if (index2 != null && index2 != '') {
							queryString += '&index2=' + index2;
						}
						if (index3 != null && index3 != '') {
							queryString += '&index3=' + index3;
						}
						queryString += '&' + new Date().getTime();
						return queryString;
					},

					htmlTableImmeubles: function(immeubles) {
						const count = immeubles.length;
						var html;
						if (count > 0) {
							html = '<table class="display">';
							html += '<tr><th>Parcelle</th><th>Date de début du premier droit connu</th><th>Date de fin du dernier droit connu</th><th>Nature de l&#39immeuble</th><th>Référence EF</th><th>Estimation fiscale (CHF)</th></tr>';
							for (var i = 0; i < count; ++i) {
								const immeuble = immeubles[i];
								html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') + '">';
								html += '<td style="width: 10%;"><a href="#" onclick="DegrevementExoneration.showDegrevementExoneration(' + immeuble.id + ');">' + immeuble.noParcelleComplet + '</a></td>';
								html += '<td style="width: 10%;">' + RegDate.format(immeuble.dateDebutDroit) + '</td>';
								html += '<td style="width: 10%;">' + RegDate.format(immeuble.dateFinDroit) + '</td>';
								html += '<td>' + StringUtils.escapeHTML(immeuble.nature) + '</td>';
								html += '<td style="width: 10%;">' + StringUtils.escapeHTML(immeuble.referenceEstimationFiscale) + '</td>';
								html += '<td style="text-align: right; width: 10%;">' + (immeuble.estimationFiscale == null ? '' : NumberFormat.toString(immeuble.estimationFiscale, "'", '.')) + '</td>';
								html += '</tr>';
							}
							html += '</table>';
						}
						else {
							html = '<span style="font-style: italic;"><fmt:message key="label.aucun.immeuble.pour.criteres"/></span>';
						}
						return html;
					},

					htmlSelectNumeros: function(numeros) {
						var html = '<option selected></option>';
						const count = numeros.length;
						for (var i = 0 ; i < count ; ++ i) {
							html += '<option value="' + numeros[i] + '">' + numeros[i] + '</option>'
						}
						return html;
					},

					showDegrevementExoneration: function(idImmeuble) {
						const dialog = Dialog.create_dialog_div('visu-degrevement-exoneration-dialog');
						dialog.load(App.curl('/degrevement-exoneration/visu.do?idCtb=${command.entreprise.id}&idImmeuble=' + idImmeuble + '&' + new Date().getTime()), null, function() {
							Histo.toggleRowsIsHistoFromClass('degrevements', 'histoDegrevements', 'histo-only');
							Histo.toggleRowsIsHistoFromClass('exonerations', 'histoExonerations', 'histo-only');
							Histo.toggleRowsIsHistoFromClass('demandesDegrevement', 'histoDemandesDegrevement', 'histo-only');
						});

						dialog.dialog({
							              title: 'Détail des dégrèvements et exonérations sur un immeuble',
							              width: 1500,
							              modal: true,
							              resizable: false,
							              position: {
							              	  my: 'top',
								              at: 'top',
								              of: '#bandeauTiers'
							              },
							              buttons: {
								              Ok: function() {
									              dialog.dialog("close");
								              }
							              }
						              });
					}
				};

				<%-- Initialisation de la page si la session contient des informations sur le contribuable --%>
				<%--@elvariable id="degExoVisuCriteria" type="ch.vd.uniregctb.registrefoncier.DegrevementExonerationVisuSessionData"--%>
				<c:set var="degExoVisuCriteria" value="${sessionScope.degExoVisuCriteria}"/>
				<c:if test="${degExoVisuCriteria != null && degExoVisuCriteria.idContribuable == command.entreprise.id}">
				$(function() {
					const queryString = DegrevementExoneration.buildQueryString(${degExoVisuCriteria.ofsCommune}, '${degExoVisuCriteria.noParcelle}', '${degExoVisuCriteria.index1}', '${degExoVisuCriteria.index2}', '${degExoVisuCriteria.index3}');
					$('#selectCommune').val(${degExoVisuCriteria.ofsCommune});
					$.get(App.curl(queryString), function(choix) {
						$('#immeubles-table').html(DegrevementExoneration.htmlTableImmeubles(choix.immeubles));
						$('#immeubles-sur-commune').show();

						const selectNoParcelle = $('#selectNoParcelle');
						const selectIndex1 = $('#selectIndex1');
						const selectIndex2 = $('#selectIndex2');
						const selectIndex3 = $('#selectIndex3');
						selectNoParcelle.html(DegrevementExoneration.htmlSelectNumeros(choix.numerosParcelles));
						selectIndex1.html(DegrevementExoneration.htmlSelectNumeros(choix.numerosIndex1));
						selectIndex2.html(DegrevementExoneration.htmlSelectNumeros(choix.numerosIndex2));
						selectIndex3.html(DegrevementExoneration.htmlSelectNumeros(choix.numerosIndex3));

						<c:if test="${degExoVisuCriteria.noParcelle != null}">
						selectNoParcelle.val(${degExoVisuCriteria.noParcelle});
						</c:if>
						<c:if test="${degExoVisuCriteria.index1 != null}">
						selectIndex1.val(${degExoVisuCriteria.index1});
						</c:if>
						<c:if test="${degExoVisuCriteria.index2 != null}">
						selectIndex2.val(${degExoVisuCriteria.index2});
						</c:if>
						<c:if test="${degExoVisuCriteria.index3 != null}">
						selectIndex3.val(${degExoVisuCriteria.index3});
						</c:if>

						$('.withParcelle').show();
					}, 'json');
				});
				</c:if>
			</script>

		</c:when>
		<c:otherwise>
			<span style="font-style: italic;"><fmt:message key="label.aucun.immeuble.connu"/></span>
		</c:otherwise>
	</c:choose>

</fieldset>

