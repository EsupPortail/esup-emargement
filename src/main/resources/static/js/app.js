//insert before
function initSlimSelects() {
  document.querySelectorAll('select.slimSelectClass').forEach(function (el) {
    if (!el.slimSelect) { // prevent double init
      el.slimSelect = new SlimSelect({
        select: el,
        placeholder: el.getAttribute('data-placeholder') || 'Choisir...'
      });
    }
  });
}
  function updateDisplay(selectedDates) {
    // 2️⃣ Met à jour les badges visibles avec un format lisible
    badgesContainer.innerHTML = "";
    selectedDates.forEach(date => {
      const options = { weekday: 'short', year: 'numeric', month: 'long', day: 'numeric' };
      const literal = date.toLocaleDateString('fr-FR', options);
      const iso = date.toISOString().split('T')[0];

      const badge = document.createElement('span');
      badge.className = "badge bg-primary me-2 mb-2 badge-date";
      badge.innerHTML = `
        ${literal}
        <button type="button" class="btn-close btn-close-white btn-sm ms-2" aria-label="Supprimer" data-date="${iso}"></button>
      `;
      badgesContainer.appendChild(badge);
    });
  }
function insertBefore(el, referenceNode) {
	referenceNode.parentNode.insertBefore(el, referenceNode);
}
function confirmSubmission() {
  return confirm("Confirrmez-vous cette action?");
}
//Random id
var ID = function() {
	return '_' + Math.random().toString(36).substr(2, 9);
};
var allSelect = selectAll;
var myChartBar = null;
var suneditor0 = null;
var selectCmp = null;
var sortDate;
//==jQuery remove()
function remove(id) {
	var elem = document.getElementById(id);
	if (elem != null) {
		return elem.parentNode.removeChild(elem);
	}
}

function backToTop() {
	if(scrollTop == 'true'){
		document.body.scrollTop = 0;
		document.documentElement.scrollTop = 0;
	}
}

function stripHTMLTagsUsingDOM(html) {
  const tempDiv = document.createElement('div');
  tempDiv.innerHTML = html;
  return tempDiv.textContent || tempDiv.innerText || "";
}

//Convertion rgb-->hex
function rgb2hex(rgb) {
	rgb = rgb.match(/^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?/i);
	return (rgb && rgb.length === 4) ? "#" +
		("0" + parseInt(rgb[1], 10).toString(16)).slice(-2) +
		("0" + parseInt(rgb[2], 10).toString(16)).slice(-2) +
		("0" + parseInt(rgb[3], 10).toString(16)).slice(-2) : '';
}

function initSelectCheckBoxes(id, searchplaceholder) {
	$('#' + id).searchableOptionList({
		maxHeight: '250px',
		showSelectAll: true,
		texts: {
			noItemsAvailable: 'Aucun résultat',
			selectAll: 'Tout sélectionner',
			selectNone: 'Tout effacer',
			searchplaceholder: searchplaceholder
		}
	});
}

function checkAll() {
    var all = document.getElementById('selectAllCheckbox');
    var hiddenInput = document.getElementById('checkedValues');
    var hiddenInput2 = document.getElementById('checkedValues2');
    
    // Helper function to update the hidden input with selected values
    function updateHiddenInput() {
        let checkedCheckboxes = document.querySelectorAll('.checkboxes:checked');
        let values = Array.from(checkedCheckboxes).map(checkbox => checkbox.value);
        hiddenInput.value = values.join(',');
        if(hiddenInput2){
			 hiddenInput2.value = values.join(',');
		}
    }

    if (all != null) {
        all.addEventListener('change', function() {
            let checkboxes = document.querySelectorAll('.checkboxes');
            checkboxes.forEach(function(checkbox) {
                checkbox.checked = all.checked;
            });
            updateHiddenInput();
        });
    }

    // Add event listeners to individual checkboxes
    let checkboxes = document.querySelectorAll('.checkboxes');
    checkboxes.forEach(function(checkbox) {
        checkbox.addEventListener('change', function() {
            updateHiddenInput();
        });
    });
}

  function initializeFileInput(fileInput) {
    if (seId === "") {
        $("#files").fileinput({
            theme: "fas",
            language: "fr",
            maxFileSize: 1000,
            maxFileCount: 10,
            mainClass: "input-group-lg",
            dropZoneEnabled: false
        });
    } else {
        let rootUrlSe = `${emargementContextUrl}/supervisor/storedFile/`;
        let deleteUrl = `${emargementContextUrl}/supervisor/storedFile/delete`;

        fetch(`${rootUrlSe}${typePj}/${seId}`)
            .then(response => response.json())
            .then(data => {
                let urls = [];
                let configsArray = [];

                data.forEach(value => {
                    urls.push(`${rootUrlSe}${value.id}/photo`);
                    configsArray.push({
                        type: value.contentType.includes("pdf") ? "pdf" : "image",
                        filetype: value.contentType,
                        caption: value.filename,
                        downloadUrl: `${rootUrlSe}${value.id}/photo`,
                        size: value.fileSize,
                        key: value.id
                    });
                });

                $("#files").fileinput({
                    theme: "fas",
                    initialPreview: urls,
                    initialPreviewAsData: true,
                    initialPreviewConfig: configsArray,
                    deleteUrl: deleteUrl,
                    overwriteInitial: false,
                    language: "fr",
                    maxFileSize: 1000,
                    maxFileCount: 10,
                    mainClass: "input-group-lg",
                    allowedPreviewTypes: ['image', 'html', 'text', 'video', 'audio', 'flash', 'object', 'pdf']
                });
            })
            .catch(error => console.error("Error loading files:", error));
        }
		$("#files").on("filepredelete", function(event, key, jqXHR, data) {
		    return !confirm("Êtes-vous sûr de vouloir supprimer ce fichier ?");
		});
    }

//affinage repartition
function displayAffinage(classes, isTiers) {
	var total = 0;
	classes.forEach(function(item) {
		var number = parseInt(item.value.replace(/\s/g, ''));
		total = (total + number);
	});
	var affinageId = null;
	var tempCapacite = null;
	var currentCapacite = null;
	if (isTiers) {
		affinageId = $("#affinageTiersTemps");
		tempCapacite = $("#tempCapaciteTiers");
		currentCapacite = $("#currentCapaciteTiers");
	} else {
		affinageId = $("#affinageNotTiersTemps");
		tempCapacite = $("#tempCapaciteNotTiers");
		currentCapacite = $("#currentCapaciteNotTiers");
	}
	affinageId.removeClass("bg-light");
	affinageId.removeClass("bg-success");
	affinageId.removeClass("bg-danger");
	affinageId.removeClass("text-white");
	affinageId.removeClass("text-dark");
	var realTotal = total / 2;
	if (realTotal == currentCapacite[0].textContent) {
		affinageId.addClass("bg-success text-white");
		$("#affinerButton").removeAttr("disabled");
	} else {
		affinageId.addClass("bg-danger text-white");
		$("#affinerButton").prop("disabled", "disabled");
	}
	tempCapacite.text(realTotal);
}

function displayToast() {
	$(".userToast").on("click", function(event) {
		const toastLiveExample = document.getElementById('liveToast');
		if (toastLiveExample != null) {
			const toast = new bootstrap.Toast(toastLiveExample);
			var splitField = this.getAttribute("data-whatever").split("//");
			$("#photoPresent").prop("src", emargementContextUrl + "/supervisor/" + splitField[0] + "/photo");
			var nom = "";
			var prenom = "";
			if (splitField[1] != "null") {
				nom = splitField[1];
				prenom = splitField[2];
			}
			$("#photoPresent").prop("alt", "Photo " + prenom + " " + nom);
			$("#nomPresence").text(nom);
			$("#prenomPresence").text(prenom);
			if (splitField[3] != "null") {
				$("#numIdentifiantPresence").text(splitField[3]);
			}
			if (splitField[4] != "null") {
				$("#codeEtape").text(splitField[4]);
			}
			toast.show();
		}
	});
}

function getCalendar(calendarEl, urlEvents, editable) {
	var calendar = new FullCalendar.Calendar(calendarEl, {
		headerToolbar: {
			start: 'prev,next today',
			center: 'title',
			end: 'dayGridMonth,timeGridWeek,timeGridDay,listMonth'
		},
		navLinks: true, // can click day/week names to navigate views
		editable: editable,
		locale: 'fr',
		events: urlEvents,
		contentHeight: 600,
		themeSystem: 'bootstrap5'
	});
	calendar.render();
}

function createDateFromString(dateString) {
	const [day, month, year] = dateString.split('/').map(Number);
	const adjustedYear = year < 70 ? 2000 + year : 1900 + year;
	return new Date(adjustedYear, month - 1, day);
}

//Configs
function displayFormconfig(val, valeur) {
	var checkTrue = "";
	var checkFalse = "";
	if (valeur == "true") {
		checkTrue = "checked='checked'";
	} else if (valeur == "false") {
		checkFalse = "checked='checked'";
	}
	if (checkTrue == "" && checkFalse == "" && val == "BOOLEAN") {
		checkTrue = "checked='checked'";
	}
	var bool = '<div class="col-lg-10"><div id="boolGroup"><label class="radio-inline"><input type="radio" name="value" id="boolTrue" value="true"' + checkTrue + '/> True' +
		'</label><label class="radio-inline ml-2"><input type="radio" name="value" id="boolFalse" value="false" ' + checkFalse + ' /> False</label></div></div>';

	switch (val) {
		case 'HTML':
			remove("boolGroup");
			remove("valeur");
			remove("suneditor_valeur");
			$("#suneditor").closest("div").after("<div class='col-lg-10'><textarea class='form-control' id='valeur' name='value'></textarea></div>");
			suneditor0 = createSunEditor('valeur');
			suneditor0.setContents(valeur);
			break;
		case 'TEXT':
			if (suneditor0 != null) {
				suneditor0 = suneditor0.hide();
			}
			remove("suneditor_valeur");
			remove("boolGroup");
			remove("valeur");
			$("#suneditor").closest("div").after("<div class='col-lg-10'><textarea class='form-control' id='valeur' name='value'>" + valeur + "</textarea></div>");
			break;
		case 'BOOLEAN':
			if (suneditor0 != null) {
				suneditor0 = suneditor0.hide();
			}
			remove("boolGroup");
			remove("suneditor_valeur");
			remove("valeur");
			$("#suneditor").closest("div").after(bool);
			break;
	}
}

function deleteParam(urlLocation, name) {
	const [head, tail] = urlLocation.split('?');
	var url = head + '?' + tail.replace(new RegExp(`&${name}=[^&]*|${name}=[^&]*&`), '');
	return url;
}

function updatePresence(url, numEtu, currentLocation) {
	var request = new XMLHttpRequest();
	var location = (currentLocation != null) ? "&currentLocation=" + currentLocation : "";
	request.open('GET', url + "?presence=" + numEtu + location, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			if (document.getElementById("newbie") != null) {
				const data = JSON.parse(request.responseText);
				if (data != null) {
					var tc = data[0];
					var tagDate = moment(tc.tagDate).format('DD-MM-YYYY HH:mm:ss');
					var info = moment(tc.sessionEpreuve.dateExamen).format('DD-MM-YYYY') + " // " +
						tc.sessionEpreuve.nomSessionEpreuve + " // " ;
					$("#norole").addClass("d-none");
					$("#newbie").removeClass("d-none");
					$("#tagDate").text(tagDate);
					$("#sessionNewbie").text(info);
					$("#accordionNewbie").hide();
				}
			}
			deleteParam(window.location.href, "tc");
			setTimeout(function() {
				$("#displayedIdentity").addClass("d-none");
			}, 2000);
		} else {
			console.log("erreur du serveur!");
		}
	};
	request.send();
}

//Revup param GET
function $_GET(param) {
	var vars = {};
	window.location.href.replace(location.hash, '').replace(
		/[?&]+([^=&]+)=?([^&]*)?/gi, // regexp
		function(m, key, value) { // callback
			vars[key] = value !== undefined ? value : '';
		}
	);

	if (param) {
		return vars[param] ? vars[param] : null;
	}
	return vars;
}

//Configs
function createSunEditor(id) {
	const editor = SUNEDITOR.create(id, {

		// height/width of the editor
		width: '100%',
		height: '150px',

		// show/hide toolbar icons
		buttonList: [
			['font', 'fontSize', 'formatBlock'],
			['bold', 'underline', 'italic'],
			['fontColor', 'hiliteColor'],
			['align', 'horizontalRule', 'list', 'link', 'image'],
			['codeView']
		]
	});
	return editor;
}

//Couleurs pour graphiques stats
var seed = 11;

function random() {
	var x = Math.sin(seed++) * 1000;
	return x - Math.floor(x);
}
var generateColors = [];
var generateBorderColors = [];
var generateStackColors = [];
for (var k = 0; k < 100; k++) {
	var color = Math.floor(random() * 256) + ',' + Math.floor(random() * 256) + ',' + Math.floor(random() * 256);
	generateColors.push('rgba(' + color + ', 0.6)');
	generateStackColors.push('rgba(' + color + ', 0.6)');
	generateBorderColors.push('rgba(' + color + ', 1)');
}
var monthsArray = ["Sept", "Oct", "Nov", "Déc", "Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Août"];
const moisArray = [[9, 'Sept'], [10, 'Oct'], [11, 'Nov'], [12, 'Déc'], [1, 'Jan'], [2, 'Fev'],
[3, 'Mar'], [4, 'Avr'], [5, 'Mai'], [6, 'Juin'], [7, 'Juil'], [8, 'Août']
];
let moisMap = new Map(moisArray);

function chartNoData(ctx, chart) {
	ctx.save();
	ctx.textAlign = 'center';
	ctx.textBaseline = 'middle';
	ctx.font = "22px Arial";
	ctx.fillStyle = "gray";
	ctx.fillText('Aucune donnée disponible', chart.width / 2, chart.height / 2);
	ctx.restore();
}

//affiche stats
function getStats(year, param, url, id, chartType, option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, datalabels) {
	Chart.register({
		id: id,
		afterDraw: function(chart) {
			if (chart.data.datasets.length === 0 || (chart.data.datasets.length != 0 && chart.data.datasets[0].data.length === 0)) {
				var ctx = chart.ctx;
				chartNoData(ctx, chart);
			}
		}
	});
	var request = new XMLHttpRequest();
	var paramUrl = (param != null) ? '&' + param : '';
	request.open('GET', emargementContextUrl + "/" + url + "/stats/json?&anneeUniv=" + year + "&type=" + id + paramUrl, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			if (this.response != "") {
				var data = JSON.parse(this.response);
				if ((Object.keys(data[id]).length) > 0) {
					if (chartType == "multiBar") {
						multiChartStackBar(data[id], id, 3, transTooltip, formatDate, 'linear');
					} else if (chartType == "chartBar") {
						chartBar(data[id], label1, id, transTooltip, formatDate, data[data2], label2);
					} else if (chartType == "pie") {
						chartPieorDoughnut(data[id], id, chartType, option, datalabels);
					} else if (chartType == "doughnut") {
						chartPieorDoughnut(data[id], id, chartType, option, datalabels);
					} else if (chartType == "lineChart") {
						lineChart(data[id], id, fill, arrayDates, formatDate);
					}
				}
			}
		}
	}
	request.send();
}

function lineChart(data, id, fill, arrayDates, formatDate) {
	if (document.getElementById(id) != null) {
		let inlineValeurs = [];
		var inlineDatasets = [];
		var a = 8;
		const valeursArray = [["9", 0], ["10", 0], ["11", 0], ["12", 0], ["1", 0],
		["2", 0], ["3", 0], ["4", 0], ["5", 0], ["6", 0], ["7", 0], ["8", 0]
		];
		const mapArray = new Map(valeursArray);
		//dates
		var dates = arrayDates;
		for (i = 0; i < data[0].length; i++) {
			mapArray.set(data[0][i], data[1][i]);
			inlineValeurs = Array.from(mapArray.values());
		}
		inlineDatasets.push({
			label: '',
			backgroundColor: generateColors[a],
			borderColor: generateColors[a],
			pointBorderColor: "#fff",
			pointHoverBorderColor: "#fff",
			pointBackgroundColor: generateColors[a],
			data: inlineValeurs,
			spanGaps: true,
			fill: fill,
		});
		var dateLabels = dates;
		if (formatDate) {
			dateLabels = [];
			for (var ind in dates) {
				dateLabels.push(formatDateString(dates[ind]));
			};
		}
		var dataMois = {
			labels: dateLabels,
			datasets: inlineDatasets
		};
		var ctx3 = document.getElementById(id).getContext("2d");
		new Chart(ctx3, {
			type: 'line',
			data: dataMois,
		});
	}
}

function chartPieorDoughnut(data, id, type, option, datalabels) {
	if (document.getElementById(id) != null) {
		if (typeof datalabels == "undefined") {
			datalabels = false;
		}
		var legend = true;
		if (option == "legend") {
			legend = false;
		}
		var dataSets = [];
		var doughnutDataArray = [];
		dataSets.push({
			data: data[1],
			backgroundColor: generateStackColors,
			hoverBackgroundColor: generateColors
		});
		var doughnutDataArray = {
			labels: data[0],
			datasets: dataSets
		};
		var ctx3 = document.getElementById(id).getContext("2d");
		new Chart(ctx3, {
			type: type,
			data: doughnutDataArray,
			options: {
				responsive: true, animateRotate: false,
				legend: {
					display: legend
				},
				plugins: {
					datalabels: {
						backgroundColor: function(context) {
							return context.dataset.backgroundColor;
						},
						borderColor: 'white',
						borderRadius: 25,
						borderWidth: 2,
						color: 'white',
						display: function(context) {
							var dataset = context.dataset;
							var count = dataset.data.length;
							var value = dataset.data[context.dataIndex];
							return value > count * 1.5;
						},
						font: {
							weight: 'bold'
						},
						formatter: Math.round
					},
					tooltip: {
						enabled: true,
						callbacks: {
							label: function(context) {
								let total1 = context.dataset.data.reduce((x, y) => x + y)
								var currentValue = context.parsed;
								var percentage = ((currentValue / total1) * 100).toFixed(1);
								var label = ' ' + context.label || '';
								if (label) {
									label += ' : ';
								}
								label += currentValue + " (" + percentage + " %)";

								return label;
							}
						}
					}
				}
			}
		});
	}
}

function multiChartStackBar(allData, id, start, transTooltip, formatDate, scaleType) {
	if (document.getElementById(id) != null) {
		const footer = (tooltipItems) => {
			let sum = 0;

			tooltipItems.forEach(function(tooltipItem) {
				sum += tooltipItem.parsed.y;
			});
			return 'Total: ' + sum;
		};
		var dataSets = [];
		var k = 0;
		for (key in allData[1]) {
			dataSets.push({
				label: key,
				data: allData[1][key],
				backgroundColor: generateStackColors[k]
			});
			k++;
		}
		var barChartData = {
			labels: allData[0],
			datasets: dataSets
		}
		var ctx = document.getElementById(id).getContext("2d");
		new Chart(ctx, {
			type: 'bar',
			data: barChartData,
			options: {
				responsive: true,
				interaction: {
					mode: 'index'
				},
				legend: {
					display: true
				},
				tooltip: {
					callbacks: {
						footer: footer,
					}
				},
				scales: {
					x: {
						stacked: true,
					},
					y: {
						stacked: true
					}
				},
				plugins: {
					tooltip: {
						callbacks: {
							footer: footer,
						}
					}
				}
			}
		});
	}
}
function chartBar(data1, label1, id, transTooltip, formatDate, data2, label2) {
	if (document.getElementById(id) != null) {
		var listLabels = [];
		var listValeurs = [];
		var listTooltipLabels = [];
		var datasets = [{
			label: label1,
			backgroundColor: generateColors[3],
			borderColor: generateBorderColors[3],
			borderWidth: 1,
			data: data1[1],
			datalabels: {
				display: true
			}
		}];
		if (data2 != null) {
			var listValeurs2 = [];
			for (var idx2 in data2) {
				listValeurs2.push(data2[idx2]);
			};
			datasets.push({
				label: label2,
				backgroundColor: generateColors[4],
				borderColor: generateBorderColors[4],
				borderWidth: 1,
				data: listValeurs2,
				datalabels: {
					display: true
				}
			})
		}
		var barChartData = {
			labels: data1[0],
			datasets: datasets
		}
		var ctx = document.getElementById(id).getContext("2d");
		new Chart(ctx, {
			type: 'bar',
			data: barChartData,
			options: {
				responsive: true,
				plugins: {
					legend: {
						display: false
					}
				},
				scales: {
					y: {
						ticks: {
							beginAtZero: true
						}
					}
				}
			}
		});
	}
}

//jstree
function openAndCheckNodes(nodeIds) {
	$.each(nodeIds, function(index, nodeId) {
		var node = $('#jstree').jstree(true).get_node(nodeId);
		if (node) {
			$('#frmt').jstree(true).check_node(node);
			$('#frmt').jstree(true).open_node(node);
		}
	});
}

function updateJsTree(selectedData, category, idProject) {
	var selectedUrl = emargementContextUrl + "/manager/adeCampus/json?idProject=" + idProject + "&category=" + category + "&fatherId=" + selectedData;
	var request = new XMLHttpRequest();
	request.open('GET', selectedUrl, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			const data = JSON.parse(request.responseText);
			$('#frmt').jstree(true).settings.core.data = data;
			$('#frmt').jstree(true).refresh();
			$('#frmt').removeClass("d-none");
			$("#spinnerComps").addClass("d-none");
		}
	}
	request.send();
}

function getQrCodeSession(url, idImg) {
	$.ajax({
		url: url,
		method: 'GET',
		success: function(response) {
			$("#" + idImg).attr("src", "data:image/png;base64, " + response);
		},
		error: function(xhr, status, error) {
			console.error('Error:', error);
		}
	});
}

function initTablePresence(sortDate){
	var table = new DataTable("#tablePresence", {
		responsive: true,
		ordering: true,
		paging: false,
		searching: true,
		info: false,
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		}, order: [
			sortDate // Adjust the column index if needed
		],
		columnDefs: [
		    {
		        targets: 'dateItem',
		        type: 'datetime-moment',
		        render: function(data, type, row) {
		            if (!data) {
		                return ''; 
		            }
		            const formats = ['DD/MM/YY HH:mm:ss', 'DD/MM/YY HH:mm', 'HH:mm'];
		            const parsedDate = moment(data, formats, true);
		            if (!parsedDate.isValid()) {
		                return data;
		            }
		            if (type === 'sort' || type === 'type') {
		                return parsedDate.unix(); 
		            }
		            return parsedDate.format('DD/MM/YY HH:mm:ss');
		        }
		    }
		],
		drawCallback: function(settings) {
            // Check if there's data in the table
            var api = this.api();
            var hasData = api.data().any();

            // Show or hide search bar based on data presence
            $('.dataTables_filter').css('display', hasData ? 'block' : 'none');
        }
	});
	
table.on('draw', function() {
    // Find rows with the "priority" class
    var priorityRows = $('#tablePresence tbody tr.priority');

    // Get current sort order (asc/desc)
    var currentOrder = table.order()[0][1]; // 'asc' or 'desc'
    
    // Sort the priority rows manually in the same order as the table
    priorityRows.sort(function(a, b) {
        var dateA = moment($(a).find('td:eq(4)').text(), 'DD/MM/YY hh:mm:ss').unix();
        var dateB = moment($(b).find('td:eq(4)').text(), 'DD/MM/YY hh:mm:ss').unix();

        if (currentOrder === 'asc') {
            return dateA - dateB; // Ascending sort
        } else {
            return dateB - dateA; // Descending sort
        }
    }).prependTo('#tablePresence tbody'); // Move sorted priority rows to the top
});
}

function displayEvents(url, table){
	$('#displayEvents').submit(function(e) {
		e.preventDefault();
		var formData = $(this).serialize();
		$.ajax({
			type: 'GET',
			url: url,
			data: formData,
			success: function(response) {
				if(table != null){
					table.destroy();
				}
				$("#tableEvents").html(response);
				$("#spinnerLoad").addClass("d-none");
				table = $('.tableFoo').DataTable({
					columnDefs: [
						{
							targets: 1, // Column index for the checkbox column
							orderable: false, // Disable sorting on this column
							className: 'select-checkbox', // Add a class for the checkbox column
							render: function(data, type, row, meta) {
								return '<input type="checkbox"  class="data-checkbox" name="btSelectItem" value="' + row[2] + '">';
							}
						},
						{ type: 'date-eu', targets: 'dateItem'},
						{
				            className: 'dtr-control ',
				            orderable: false,
				            targets: 0
				        }
					],
					select: {
						style: 'multi', // Set the selection style (you can use 'single' or 'os' as well)
					},
					order: [[6, 'desc']], // Set the default sorting column and order
					responsive: {
					        details: {
					            type: 'column'
					        }
					    },
					ordering: true,
					paging: true,
					searching: true,
					info: false,
					language: {
						url: "/webjars/datatables-plugins/i18n/fr-FR.json"
					}
				});
			},
			error: function(error) {
				console.log('Error: ' + error);
			}
		});
	});
}

function importEvents(url) {
	$("#importBtn").off('click').on('click', function(event) {
		event.preventDefault();
		const $form = $("#displayEventsImport");
		$("#spinnerLoad").removeClass("d-none");
		$.ajax({
			url: url,
			type: "POST",
			data: $form.serialize(),
			success: function() {
				$('#displayEvents').submit();
			},
			error: function(error) {
				console.log("Error:", error);
			},
			complete: function() {
				$("#spinnerLoad").addClass("d-none");
			}
		});
	});
}

function setupModal(modalId, fields) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.addEventListener('show.bs.modal', event => {
      const button = event.relatedTarget;
      fields.forEach(({ attr, elementId }) => {
        const value = button.getAttribute(attr);
        const element = document.getElementById(elementId);
        if (element) {
          if (element.tagName === 'INPUT') {
            element.value = value;
          } else {
            element.textContent = value;
          }
        }
      });
    });
  }
}
//==jQuery document.ready
document.addEventListener('DOMContentLoaded', function() {
	//Messages modal bilan CSv
	var dialogMsg = document.querySelector('#modalBilanCsvBody');
	if (dialogMsg != null) {
		$('#modalBilanCsv').modal('show');
	}
	var modalMsg = document.querySelector('#modalMsg');
	if (modalMsg != null) {
		$('#modalMsg').modal('show');
	}
	var idArray = [];
	var checkboxes = document.querySelectorAll('.case');
	var selectAll = document.getElementById("selectall");
	var listeIds = document.getElementById("listeIds");
	if (selectAll != null) {
		selectAll.addEventListener("click", function() {
			idArray = [];
			for (var i = 0, n = checkboxes.length; i < n; i++) {
				if (!checkboxes[i].disabled) {
					checkboxes[i].checked = this.checked;
				}
			}
			idArray = Array.from(document.querySelectorAll("input[name='case']")).filter(function(checkbox) {
				return checkbox.checked;
			}).map(function(checkbox) {
				return checkbox.value;
			});
			if (listeIds != null) {
				listeIds.value = idArray;
			}
		});

		Array.from(checkboxes).forEach(function(link) {
			return link.addEventListener('click', function(event) {
				if (idArray.indexOf(this.value) < 0) {
					idArray.push(this.value);
				} else {
					idArray.splice(idArray.indexOf(this.value), 1);
				}
				if (!this.checked) {
					selectAll.checked = false;
				}
				if (listeIds != null) {
					listeIds.value = idArray;
				}
			});
		});
	}

	var suneditor = document.getElementById("pdfArea");
	var editor1 = null;
	if (suneditor != null) {
		editor1 = createSunEditor('pdfArea');
		editor2 = createSunEditor('emailArea');
	}

	//Click events
	document.addEventListener('click', function(event) {
		if (event.target.matches('.presenceCheck')) {
			var isPresent = event.target.checked;
			var isPresentValue = isPresent + ',' + event.target.value;
			updatePresence(emargementContextUrl + "/updatePresents", isPresentValue, null);
		}
		if (event.target.matches('.presenceChecker')) {
			var isPresent = event.target.checked;
			var isPresentValue = isPresent + ',' + event.target.value;
			updatePresence(emargementContextUrl + "/updatePresentsTagChecker", isPresentValue, null);
		}
		if (event.target.matches('#pdfPreview')) {
			var htmltemplate = document.getElementById("htmltemplate");
			htmltemplate.value = editor1.getContents();
			var formPdfConvoc = document.getElementById("formPdfConvoc");
			formPdfConvoc.submit();
		}
		if (event.target.matches('#submitEmailConvocation')) {
			event.preventDefault();
			var bodyMsg = document.getElementById("bodyMsg");
			bodyMsg.value = editor2.getContents();
			var htmltemplatePdf = document.getElementById("htmltemplatePdf");
			htmltemplatePdf.value = editor1.getContents();
			var sendConvocation = document.getElementById("sendConvocation");
			var subject = document.getElementById("subject");
			var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
			if (subject.value.length == 0) {
				alert("Vous devez rentrer un sujet");
			} else if (isPreviewPdfOk!=null && !isPreviewPdfOk.checked) {
				alert("Vous devez valider le PDF de convocation");
			} else if (sendConvocation !=null){
				sendConvocation.submit();
			}else if (document.getElementById("sendCommunication") !=null){
				document.getElementById("sendCommunication").submit();
			}
		}
		if (event.target.matches('#submitEmailConvsignes')) {
			event.preventDefault();
			var bodyMsg = document.getElementById("bodyMsg");
			bodyMsg.value = editor2.getContents();
			var htmltemplatePdf = document.getElementById("htmltemplatePdf");
			htmltemplatePdf.value = editor1.getContents();
			var sendConvocation = document.getElementById("sendConsignes");
			var subject = document.getElementById("subject");
			var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
			if (subject.value.length == 0) {
				alert("Vous devez rentrer un sujet");
			} else if (!isPreviewPdfOk.checked) {
				alert("Vous devez valider le PDF de consignes");
			} else {
				sendConvocation.submit();
			}
		}

	}, false);

	//Configs
	var boolGroup = document.getElementById("boolGroup");
	if (boolGroup != null) {
		boolGroup.style.display = "none";
	}
	var valeur = document.getElementById("hiddenValeur");
	if (valeur != null) {
		var typeConfig = document.querySelector("input[type=radio][name=type]:checked");
		if (typeConfig != null) {
			displayFormconfig(typeConfig.value, valeur.value);
		}

		var radioType = document.querySelectorAll('input[type=radio][name=type]');
		Array.from(radioType).forEach(function(link) {
			return link.addEventListener('click', function(event) {
				displayFormconfig(this.value, valeur.value);
			});
		});
	}

	//configs
	var appliConfig = document.getElementById('configForm');
	if (appliConfig != null) {
		appliConfig.addEventListener('submit', function(e) {
			e.preventDefault();
			var radioType = document.querySelectorAll('input[type=radio][name=type]:checked')[0].value;
			if (radioType == "HTML") {
				document.getElementById("valeur").value = suneditor0.getContents();
			}
			appliConfig.submit();
		});
	}
	//submit export inscrits
	var submitExport = document.getElementById('submitExport')
	if (submitExport) {
		var importBtn = document.getElementById('import');
		$(".statusExport").hide();
		importBtn.addEventListener('click', function(e) {
			var sessionEpreuve = document.getElementById('sessionEpreuve');
			if (sessionEpreuve.value == "") {
				e.preventDefault();
				alert("Vous devez choisir une Session!");
			} else {
				$(".statusExport").show();
				submitExport.submit();
			}
		});
	}

	var importUsersLdapform = document.getElementById('importUsersLdapform');
	if (importUsersLdapform != null) {
		var importBtn = document.getElementById('importLdap');
		var nbSelected = $("#usersGroupLdap option:selected").length;
		$(".statusExportLdap").hide();
		importBtn.addEventListener('click', function(e) {
			var importAgents = $("#importTagchecker").prop('checked')? true : false;
			var sessionEpreuveLdap = document.getElementById('sessionEpreuveLdap');
			if (sessionEpreuveLdap.value == "" && !importAgents) {
				e.preventDefault();
				alert("Vous devez choisr une Session!");
			} else {
				if (nbSelected > 0) {
					$(".statusExportLdap").show();
					importUsersLdapform.submit();
				}
			}
		});
	}
	var importGroupe = document.getElementById('importGroupe');
	$(".statusExportGroupe").hide();
	if (importGroupe != null) {
		importGroupe.addEventListener('click', function(e) {
			var sessionEpreuve = document.getElementById('sessionEpreuveGroupe');
			if (sessionEpreuve.value == "") {
				e.preventDefault();
				alert("Vous devez choisr une Session!");
			} else {
				$(".statusExportGroupe").show();
				submitExport.submit();
			}
		});
	}
	var importCsv = document.getElementById('importCsv');
	$(".statusExportCsv").hide();
	if (importCsv != null) {
		importCsv.addEventListener('click', function(e) {
			var importAgents = $("#importTagchecker").prop('checked')? true : false;
			var sessionEpreuve = document.getElementById('sessionEpreuveCsv');
			if (sessionEpreuve.value == "" && !importAgents) {
				e.preventDefault();
				alert("Vous devez choisr une Session!");
			} else {
				$(".statusExportCsv").show();
				submitExport.submit();
			}
		});
	}
	$('#selectAll').on("click", function() {
		var selectedItems = 0;
		var nbSelected = $("#usersGroupLdap option:selected").length;
		$('#usersGroupLdap option').each(function() {
			if (!this.disabled && this.value != '') {
				$(this).prop('selected', (nbSelected == 0) ? true : false);
				selectedItems = $(this).prop('selected') ? $("#ldapMembersSize").text() : 0;
			}
		});
		$("#selectedUsers").text(selectedItems);
	});

	$('#usersGroupLdap option').on("click", function() {
		nbSelected = $("#usersGroupLdap option:selected").length;
		$("#selectedUsers").text(nbSelected);
	});

	// tableau inscrits
	var tempsAmenage = document.getElementById('tempsAmenage');
	var repartition = document.getElementById('repartition');
	if (tempsAmenage != null) {
		tempsAmenage.addEventListener('change', function(e) {
			document.getElementById('formSearch').submit();
		});
	}
	if (repartition != null) {
		repartition.addEventListener('change', function(e) {
			document.getElementById('formSearch').submit();
		});
	}

	var helpForm = document.getElementById('helpForm');
	if (helpForm != null) {
		var suneditor = createSunEditor('value');
		helpForm.addEventListener('submit', function(e) {
			e.preventDefault();
			document.getElementById("value").value = suneditor.getContents();
			helpForm.submit();
		});
	}

	//stats
	var year = $("#anneeUnivSelect option:selected").val();
	if (document.getElementById('statsCharts') != null) {
		var url = "manager";
		$("body").css("background-color", "#f2f2f2");
		//getStats(year, param, null,id, chartType,  option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth)
		getStats(year, null, url, "sessionEpreuvesByCampus", "pie");
		getStats(year, null, url, "sessionLocationByLocation", "doughnut");
		getStats(year, null, url, "tagCheckersByContext", "pie", "legend");
		getStats(year, null, url, "presenceByContext", "pie");
		getStats(year, null, url, "sessionEpreuveByYearMonth", "lineChart", null, null, false, null, null, null, true, monthsArray);
		getStats(year, null, url, "countTagCheckByYearMonth", "lineChart", null, null, false, null, null, null, true, monthsArray);
		getStats(year, null, url, "countTagChecksByTypeBadgeage", "doughnut");
		getStats(year, null, url, "countTagCheckBySessionLocationBadgedAndPerson", "doughnut");
		getStats(year, null, url, "countSessionEpreuveByType", "pie");
	}
	//stats superadmin
	if (document.getElementById('statsSuperAdminCharts') != null) {
		var url = "superadmin";
		$("body").css("background-color", "#f2f2f2");
		//getStats(param, null,id, chartType,  spinner, option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth)
		getStats(year, null, url, "sessionEpreuvesByContext", "multiBar");
		getStats(year, null, url, "countTagChecksByContext", "multiBar");
		getStats(year, null, url, "countLocationsByContext", "chartBar");
		getStats(year, null, url, "countUserAppsByContext", "multiBar");
		getStats(year, null, url, "countCampusesByContext", "chartBar");
		getStats(year, null, url, "countSessionEpreuveByTypeByContext", "multiBar");
	}

	//stats session epreuve
	$('[id^=modalChartSeBtn]').on('click', function(e) {
		var url = "manager";
		var splitId = this.id.split("-");
		var canvas = document.getElementById("countTagChecksByTimeBadgeage");
		canvas.remove();
		$('#chartListSession').append("<canvas id='countTagChecksByTimeBadgeage' style='height: 394px; width: 789px;' height='394' width='789'></canvas>");
		getStats(null, "&param=" + splitId[1], url, "countTagChecksByTimeBadgeage", "chartBar");
	})
	$('#modal-chart').on('shown.bs.modal', function(event) {
		var button = $(event.relatedTarget) // Button that triggered the modal
		var title = button.data('whatever') // Extract info from data-* attributes
		var modal = $(this)
		modal.find('.modal-title').text(title)
	})
	
	const setupCalendar = (calendarId, urlPath) => {
	    const calendarEl = document.getElementById(calendarId);
	    const viewCalendar = document.getElementById('viewCalendar');
	
	    if (calendarEl && viewCalendar) {
	        const getParam = () => {
	            if (calendarId === 'calendar') {
	                return viewCalendar.value === 'mine' ? '?view=mine' : '';
	            } else if (calendarId === 'calendarAll') {
	                return viewCalendar.value ? `?view=${viewCalendar.value}` : '';
	            }
	            return '';
	        };
	
	        const updateCalendar = () => {
	            const paramCalendar = getParam();
	            getCalendar(calendarEl, `${emargementContextUrl}${urlPath}${paramCalendar}`, true);
	        };
	        viewCalendar.addEventListener("change", updateCalendar);
	        updateCalendar();
	    }
	};
	setupCalendar('calendar', '/manager/calendar/events');
	setupCalendar('calendarAll', '/superadmin/calendar/events');
	setupCalendar('calendarSup', '/supervisor/calendar/events');

	//Pagination --->rajout tous
	if (document.getElementById('pagination') != null) {
		if (allSelect != null && allSelect > 10) {
			var location = window.location;
			var dataUrl = location.pathname;
			var allSelectUrl = "";
			if (location.search == "") {
				allSelectUrl = dataUrl + "?size=" + allSelect;
			} else {
				var url = new URL(location);
				var query_string = url.search;
				var search_params = new URLSearchParams(query_string);
				search_params.set('size', allSelect);
				url.search = search_params;
				allSelectUrl = url.pathname + url.search;
				search_params.set('size', "10");
				url.search = search_params;
				selectUrl10 = url.pathname + url.search;
			}
			$("#pagination select").prepend("<option value='" + allSelect + "' data-page-size-url='" + allSelectUrl + "'>Tous</option>");
		}
		$("#pagination option:selected").removeProp("selected");
		var sizeSelected = ($_GET("size") != null) ? $_GET("size") : allSelect;
		$("#pagination option[value='" + sizeSelected + "']").attr('selected', 'selected');
	}

	if (document.getElementById('extractionPage') !== null) {
	    const slimConfigs = [
	        { elements: ['codCmp', 'sessionEpreuve', 'sessionEpreuveLdap', 'sessionEpreuveGroupe', 'sessionEpreuveCsv', 'groupe', 'sessionLocationCsv', 'sessionLocationLdap', 'sessionLocationGroupe', 'sessionLocation'] },
	        { elements: ['codSes', 'codAnu'], options: { showSearch: false } },
	        { elements: ['codEtp', 'codElp', 'codExtGpe'], options: { allowDeselect: true, enabled: true } }
	    ];
	    slimConfigs.forEach(config => {
	        config.elements.forEach(id => {
	            const element = document.getElementById(id);
	            if (element) {
	                const options = config.options || {};
	                const slimSelectInstance = new SlimSelect({ select: `#${id}`, ...options });
	                
	                // Enable SlimSelect if required
	                if (options.enabled) {
	                    slimSelectInstance.enable();
	                }
	            }
	        });
	    });
	}
	
	const slimSelectConfigs = [
	    { id: 'suList', placeholder: 'Rechercher Utilisateur' },
	    { id: 'gpId', placeholder: 'Rechercher groupe' },
	    { id: 'sessionLocationExpected', placeholder: 'Rechercher Lieu' },
	    { id: 'sessionLocationExpected2', placeholder: 'Rechercher Lieu' },
	    { id: 'blackListGroupe', placeholder: 'Rechercher Groupe' },
		{ id: 'motifAbsence'}
	];
	slimSelectConfigs.forEach(config => {
	    const element = document.getElementById(config.id);
	    if (element) {
	        new SlimSelect({
	            select: `#${config.id}`,
	            placeholder: config.placeholder
	        });
	    }
	});

	//Groupes
	if (document.getElementById('addMembersGroupe') != null) {
		var slimArray2 = ['sessionEpreuveGroupes', 'groupes', 'groupes2'];

		slimArray2.forEach(function(item, index, array) {
			var select2 = '#' + item;
			if (document.getElementById(item) != null) {
				new SlimSelect({
					select: select2,
					placeholder: '--Choisir--'
				});
			}
		});
	}

	//Presence
	if (document.getElementById('presencePage') != null || document.getElementById('unknownPage') != null) {
		const uuid = ID();
		const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
		sortDate = (triBadgeage == 'true')? [0, 'asc'] : [4, 'desc'];
		initTablePresence(sortDate);
		eventSource.addEventListener('tc', response => {
			var tagCheck = JSON.parse(response.data);
			if (!tagCheck.isBlacklisted) {
				var person = tagCheck.person;;
				var guest = tagCheck.guest;
				var sessionId = tagCheck.sessionEpreuve.id;
				var sessionLocationExpected = (tagCheck.sessionLocationExpected != null) ? tagCheck.sessionLocationExpected.id : null;
				var identifiant = "";
				var varUrl = "";
				if (person != null) {
					identifiant = person.eppn;
					varUrl = person.eppn;
				} else if (guest != null) {
					identifiant = guest.email;
					varUrl = "inconnu";
				}
				var url = emargementContextUrl + "/supervisor/" + varUrl + "/photo";
				var nom = "";
				var prenom = "";
				if (person != null && person.nom != null) {
					nom = person.nom.toUpperCase();
					prenom = person.prenom;
				} else if (guest != null && guest.nom != null) {
					nom = guest.nom.toUpperCase();
					prenom = guest.prenom;
				}
				if (sessionLocationExpected != null) {
					if (tagCheck.tagChecker!=null && tagCheck.tagChecker.userApp.eppn==eppnAuth){
						var displayedIdentity = sessionLocationExpected + "_displayedIdentity2";
						var displayedIdentity2 = $("#" + displayedIdentity);
						displayedIdentity2.removeClass("d-none");
						displayedIdentity2.find("img").prop("src", url);
						displayedIdentity2.find("img").prop("alt", identifiant);
						displayedIdentity2.find('#prenomPresence3').text(prenom);
						displayedIdentity2.find('#nomPresence3').text(nom);
						if (person != null && person.numIdentifiant != null) {
							displayedIdentity2.find('#numIdentifiantPresence2').text('N° ' + person.numIdentifiant);
						}
						const toastLiveExample = document.getElementById(displayedIdentity);
						const toast = new bootstrap.Toast(toastLiveExample, { 'delay': 2000 })
						toast.show();
					}
					$("body").scrollTop();
				}
				var urlLocation = (sessionLocationExpected != null) ? sessionLocationExpected : $_GET("location");
				var url = emargementContextUrl + "/supervisor/presence?sessionEpreuve=" + sessionId +
					"&location=" + urlLocation + "&update=" + tagCheck.id;
				$("#resultsBlock" + sessionLocationExpected).load(url, function(responseText, textStatus, XMLHttpRequest) {
					backToTop();
					if (tagCheck.tagChecker!=null && tagCheck.tagChecker.userApp.eppn==eppnAuth){
						displayToast();
					}
					initTablePresence(sortDate);
				});
			}
		}, false);

		eventSource.addEventListener('sl', response => {
			var sl = JSON.parse(response.data);
			$("#sl" + sl.id).html(sl.nbPresentsSessionLocation);
		}, false);

		eventSource.addEventListener('message', this.onmessage = function(e) {
			var splitData = e.data.split("@@");
			var percent = parseFloat(splitData[1]).toFixed(2);
			$("#progressBar" + splitData[0]).css("width", percent + '%');
			$("#progressBar" + splitData[0]).text(percent + '%');
		});
		eventSource.addEventListener('total', response => {
			var total = response.data;
			var splitData1 = total.split("@@");
			$("#totalPresent" + splitData1[0]).text(splitData1[1]);
		}, false);

		eventSource.addEventListener('customMsg', response => {
			var customMsg = response.data;
			if (customMsg != "") {
				$("#customMsg").removeClass("d-none").addClass("show");
				setTimeout(function() {
					$("#customMsg").addClass("d-none");
				}, 2000);
			}
		}, false);

		eventSource.addEventListener('tagChecker', response => {
				var tagChecker = JSON.parse(response.data);
				var sessionId = tagChecker.sessionLocation.sessionEpreuve.id;
				var urlLocation =  $_GET("location");
				var url = emargementContextUrl + "/supervisor/presence?sessionEpreuve=" + sessionId +
					"&location=" + urlLocation + "&tcer=true&update=" + tagChecker.id;
				$("#resultsBlock" +  tagChecker.sessionLocation.id).load(url, function(responseText, textStatus, XMLHttpRequest) {
					backToTop();
					displayToast();
					sortDate = (triBadgeage == 'true')? [0, 'asc'] : [4, 'desc'];
					initTablePresence(sortDate);
				});
		}, false);

		if (document.getElementById('searchTagCheck') != null) {
			var selectPresence = new SlimSelect({
				select: '#searchTagCheck',
				allowDeselect: true
			});
		}

		initSlimSelects();
		
		$("#searchTagCheck").change(function() {
			$("#collapseTable").removeClass("d-none");
			$("#collapseTable table tbody").empty();
			$("#" + this.value).clone().appendTo("#collapseTable table tbody");
		});
		$(document).on("click", "#toto", function(event) {
			if (window.confirm('Confirmez-vous la suppression cet individu?')) {
				var trParent = $(this)[0].parentElement.parentElement;
				var id = trParent.id;
				var request = new XMLHttpRequest();
				request.open('POST', emargementContextUrl + "/supervisor/tagCheck/" + id, true);
				request.onload = function() {
					if (request.status >= 200 && request.status < 400) {
						$("#" + id).hide();
					}
				}
				request.send();
			}
		});
	}

	//select context
	var selectContext = document.getElementById('selectContext');
	if (selectContext != null) {
		selectContext.addEventListener("change", function() {
			var text = $(this).find("option:selected").text();
			if (text == "Ordre") {
				window.location.href = this.value;
			} else {
				this.form.submit();
			}
		})
	}

	//Changement année univ
	$("#anneeUnivSelect").on("change", function(e) {
		var annee = this.value;
		window.location.href = window.location.pathname + "?anneeUniv=" + annee;
	});

	//procuration
	$(document).on('click', '.proxyPersonCheck', function(e) {
		e.preventDefault();
		var splitProxy = null;

		if (typeof $(this).attr("data-whatever") == "undefined") {
			splitProxy = this.value.split(",");
			$("#substituteId option[value='']").prop("selected", "selected");
		} else {
			splitProxy = $(this).attr("data-whatever").split(",");
			let el = document.querySelector('#substituteId');
			$("#substituteId option[value='" + splitProxy[2] + "']").prop("selected", "selected");
		}

		$("#procurationPerson").text(splitProxy[1]);
		$("#tcId").val(splitProxy[0]);

		const offcanvasElementList = document.getElementById('offcanvasExample')
		const offcanvasList = new bootstrap.Offcanvas(offcanvasElementList);
		offcanvasList.show();
	});

	if (document.getElementById('substituteId') != null) {
		var select = new SlimSelect({
			select: '#substituteId'
		})
	}

	var fileInput = document.querySelector('#files');
    initializeFileInput(fileInput) ;
	  

	//Select all !!
	$("#selectAll").click(function() {
		$('.notUsed').find("input[type=checkbox]").prop('checked', $(this).prop('checked'));
	});

	$('.notUsed').find("input[type=checkbox]").click(function() {
		if (!$(this).prop("checked")) {
			$("#selectAll").prop("checked", false);
		}
	});

	//Préférence : voir sessions antérieure
	$(document).on('change', '#oldSessionsCheck', function() {
		var value = (this.checked) ? "true" : "false";
		var redirect = window.location.origin + window.location.pathname;
		var request = new XMLHttpRequest();
		request.open('GET', emargementContextUrl + "/supervisor/prefs/updatePrefs?pref=seeOldSessions&value=" + value, true);
		request.onload = function() {
			if (request.status >= 200 && request.status < 400) {
				window.location.href = redirect;
			}
		}
		request.send();
	});
	//Foemulaire présence
	$("#presencePage #location").on("change", function() {
		$("#presenceForm").submit();
	});

	var dialogMsg = document.querySelector('#modalUser');
	if (dialogMsg != null) {
		$('#modalUser').modal('show');
	}
	
	const myOffcanvas = document.getElementById('offcanvasWebcam')
	if(myOffcanvas != null){
		myOffcanvas.addEventListener('shown.bs.offcanvas', event => {
			var qrCodeCam = document.getElementById("qrCodeCam");
			if (qrCodeCam != null) {
				var video = document.createElement("video");
				var canvasElement = document.getElementById("canvas");
				var canvas = canvasElement.getContext("2d");
				var loadingMessage = document.getElementById("loadingMessage");
		
				function drawLine(begin, end, color) {
					canvas.beginPath();
					canvas.moveTo(begin.x, begin.y);
					canvas.lineTo(end.x, end.y);
					canvas.lineWidth = 4;
					canvas.strokeStyle = color;
					canvas.stroke();
				}
		
				// Use facingMode: environment to attemt to get the front camera on phones
				navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } }).then(function(stream) {
					video.srcObject = stream;
					video.setAttribute("playsinline", true); // required to tell iOS safari we don't want fullscreen
					video.play();
					requestAnimationFrame(tick);
				});
				let isCodeScanned = false;
				function tick() {
					loadingMessage.innerText = "⌛ Loading video..."
					if (video.readyState === video.HAVE_ENOUGH_DATA) {
						loadingMessage.hidden = true;
						canvasElement.hidden = false;
						canvasElement.height = video.videoHeight;
						canvasElement.width = video.videoWidth;
						canvas.drawImage(video, 0, 0, canvasElement.width, canvasElement.height);
						var imageData = canvas.getImageData(0, 0, canvasElement.width, canvasElement.height);
						var code = jsQR(imageData.data, imageData.width, imageData.height, {
							inversionAttempts: "dontInvert",
						});
						if (code) {
							drawLine(code.location.topLeftCorner, code.location.topRightCorner, "#FF3B58");
							drawLine(code.location.topRightCorner, code.location.bottomRightCorner, "#FF3B58");
							drawLine(code.location.bottomRightCorner, code.location.bottomLeftCorner, "#FF3B58");
							drawLine(code.location.bottomLeftCorner, code.location.topLeftCorner, "#FF3B58");
						}
						if (!isCodeScanned) {
							if (code) {
								var value = code.data;
								var sessionData = qrCodeCam.getAttribute("data-qrcode");
								if (sessionData != null) {
									var splitCode = value.split("value=");
									value = splitCode[1] + "@@@" + sessionData;
								}
								var prefix = "";
								if (qrCodeCam.hasAttribute('data-role')) {
									prefix = emargementContextUrl + "/";
								}
								var location = document.getElementById("location");
								var valLocation = (location != null) ? location.value : null;
								updatePresence(prefix + "updatePresents", value, valLocation);
								isCodeScanned = true;
								console.log("QR Code scanned once.");
								setTimeout(() => {
									isCodeScanned = false;
									console.log("Simulated scan. Code scanned only once.");
								}, 2000); //
							} else {
								isCodeScanned = false; // Reset the flag if no QR code is found
							}
						}
					}
					requestAnimationFrame(tick);
				}
				tick();
			}
		})
	}
	//Affinage reparttion
	$(".affinage").inputSpinner();

	$(".affinage").on("input", function(event) {
		var max = parseInt($(this).prop("max"));
		var current = parseInt($(this).val());
		var newPercent = $(this)[0].closest("span").nextElementSibling;
		var percentVal = (current * 100) / max;
		newPercent.textContent = Math.round(percentVal) + ' %';
		$(this).removeClass('is-valid');
		$(this).removeClass('is-invalid');
		if (current <= max) {
			$(this).addClass('is-valid');
		} else {
			$(this).addClass('is-invalid');
		}

		//Not tiers
		var notTiers = document.querySelectorAll('[id^="spinner_false"]');
		if (notTiers.length > 0) {
			displayAffinage(notTiers, false);
		}

		//Tiers
		var isTiers = document.querySelectorAll('[id^="spinner_true"]');
		if (isTiers.length > 0) {
			displayAffinage(isTiers, true);
		}

		var submitAffinage = document.getElementById("affinerButton");
		if (submitAffinage != null) {
		    submitAffinage.addEventListener('click', function(e) {
		        var formAffinage = document.getElementById("formAffinage");

		        // Récupère le bouton radio sélectionné
		        var selectedRadio = document.querySelector('input[name="tagCheckOrder"]:checked');

		        if (selectedRadio) {
		            // Supprime les anciens inputs "tagCheckOrder" du formulaire
		            var oldInputs = formAffinage.querySelectorAll('input[name="tagCheckOrder"]');
		            oldInputs.forEach(el => el.remove());

		            // Crée un hidden input pour transmettre la valeur
		            var hiddenInput = document.createElement("input");
		            hiddenInput.type = "hidden";
		            hiddenInput.name = "tagCheckOrder";
		            hiddenInput.value = selectedRadio.value;

		            formAffinage.appendChild(hiddenInput);
		        }

		        formAffinage.submit();
		    });
		}
	})

	$('input[type=radio][name=tagCheckOrder]').on("change", function(event) {
		var tagCheckOrder = $(this).val();
		$("#alphaOrderAffinage").val(tagCheckOrder);
		$("#alphaOrderRepartition").val(tagCheckOrder);
	});

	$(".contextualHelp").on("mouseover", function(event) {
		$(this).tooltip("show");
		$(this).prop("title", "");
	});

	if (document.getElementById("sessionSearch") != null) {
		$("#formSearch .form-select").on("change", function(event) {
			$("#searchBox").val(null);
			document.getElementById("formSearch").submit();
		});
		$("#formSearch #resetSearch").on("click", function(event) {
			$("#searchBox").val(null);
			$("#statut").val("");
			$("#typeSession").val("");
			$("#campus").val("");
			$("#view").val("");
			var maxValue = -Infinity; 
			$('#anneeUniv option').each(function() {
			    var optionValue = parseFloat($(this).val()); // Convert to number
			    if (!isNaN(optionValue) && optionValue > maxValue) {
			        maxValue = optionValue;
			    }
			});
			$('#anneeUniv').val(maxValue);
			document.getElementById("formSearch").submit();
		});
		$("#sessionSearch .btn-check").on("change", function(event) {
			document.getElementById("formSearch").submit();
		});
	}

	//Import event ADE
	$("#codeSalleSelect").on("change", function(event) {
		document.getElementById("displaySalles").submit();
	});

	$("#displayEvents").on("submit", function(event) {
		$("#spinnerLoad").removeClass("d-none");
	});

	//Import event ADE
	$("#strDateMin").on("change", function() {
		var strDateMin = $("#strDateMin").val();
		$("#strDateMinImport").val(strDateMin);
		if ($("#strDateMax").val() == "") {
			$("#strDateMax").val(strDateMin);
			$("#strDateMaxImport").val(strDateMin);
		}
	});
	$("#strDateMax").on("change", function() {
		$("#strDateMaxImport").val($("#strDateMax").val());
	});
	const uuid = ID();
	const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
	eventSource.addEventListener('nbImportSession', response => {
		var total = response.data;
		$("#nbImportSession").text("Imports ADE : " + total);
	}, false);

	//affiche photo
	displayToast();

	//jstree adecampus
	$('#frmt').on('changed.jstree', function(e, data) {
	    var checkedNodes = data.selected.slice(); // copie pour sécurité
	    var nodesToRemove = [];

	    // Supprimer les nœuds parents si leurs enfants sont sélectionnés (ton code original)
	    checkedNodes.forEach(function(nodeId) {
	        var node = data.instance.get_node(nodeId);
	        if (node.children.length > 0) {
	            node.children.forEach(function(childId) {
	                if (checkedNodes.includes(childId)) {
	                    nodesToRemove.push(nodeId);
	                }
	            });
	        }
	    });
	    nodesToRemove.forEach(function(nodeId) {
	        var index = checkedNodes.indexOf(nodeId);
	        if (index > -1) checkedNodes.splice(index, 1);
	    });

	    var r = [];               // IDs des nœuds finaux sélectionnés (feuilles/tronc sélectionné)
	    var text = [];            // textes (ton code)
	    var allLibelles = [];     // libellés texte complets (ton code)
	    var firstLevelIds = [];   // => nos premiers niveaux après la racine (résultat voulu)

	    checkedNodes.forEach(function(nodeId) {
	        var node = data.instance.get_node(nodeId);
	        text.push(node.text);
	        r.push(node.id);

	        // --- Code original (texte du chemin) ---
	        let hierarchy = [];
	        node.parents.slice().reverse().forEach(function(parentId) {
	            if (parentId !== "#") {
	                var parentNode = data.instance.get_node(parentId);
	                hierarchy.push(parentNode.text);
	            }
	        });
	        hierarchy.push(node.text);
	        var fullText = hierarchy.join(">");
	        allLibelles.push(fullText);
	        // --- fin code original ---

	        // --- Nouveau : construire le chemin en IDs dans l'ordre root -> ... -> node ---
	        // node.parents est dans l'ordre enfant -> ... -> '#' ; on inverse et on retire '#'
	        var pathFromRoot = node.parents.slice().reverse().filter(function(p){ return p !== "#"; });
	        // ajouter l'ID du noeud sélectionné à la fin pour avoir le chemin complet
	        pathFromRoot.push(node.id);

	        // Si le chemin contient au moins 2 éléments, l'élément d'index 1 est le premier sous la racine
	        // (si pathFromRoot = [root, b, c, d], on prend pathFromRoot[1] => b)
	        var firstAfterRoot = pathFromRoot.length > 1 ? pathFromRoot[1] : pathFromRoot[0];
	        firstLevelIds.push(firstAfterRoot);
	    });

	    // dédupliquer les IDs (si plusieurs descendants de la même branche ont été cochés)
	    var uniqueFirstLevel = Array.from(new Set(firstLevelIds));

	    // Mise à jour des champs du formulaire (ton code)
	    $("#idList").val(r.join(','));
	    $("#idListImport").val(r.join(','));
	    $("#idListTask").val(r.join(','));
	    $("#textListTask").val(allLibelles.join(','));

	    // Stocker le résultat voulu (IDs du premier niveau après la racine) dans un champ caché
	    $("#firstLevelIds").val(uniqueFirstLevel.join(','));

	    // debug
	    console.log("Sélection finale (IDs) :", r);
	    console.log("Chemins texte :", allLibelles);
	    console.log("Tous firstAfterRoot (possiblement dupliqués) :", firstLevelIds);
	    console.log("Unique firstLevelIds (résultat final) :", uniqueFirstLevel);

	}).jstree({
	    "checkbox": {
	        "keep_selected_style": false,
	        "three_state": false,
	        "cascade": "up"
	    },
	    "plugins": ["checkbox", "sort", "state"],
	    'core': {
	        'data': []
	    }
	});

	var table = $('.tableSalles').DataTable({
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: false,
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		},
		columnDefs: [
			{
				targets: 0,
				orderable: false,
				className: 'select-checkbox', 
				render: function(data, type, row, meta) {
					return '<input type="checkbox"  class="data-checkbox" name="btSelectItem" value="' + row[1] + '">';
				}
			},
			{ type: 'date-eu', targets: 'dateItem'}
		]
	});

	$("#codeComposante1").on("change", function(event) {
		const selectedData = $(this).val();
		$("#codeFormation").val($(this).val());
		$("input[id^=hiddenCodeComposante]").val(selectedData);
		if (selectedData == "myEvents") {
			$('#frmt').addClass("d-none");
		} else {
			if(selectedData!="") {
				$("#spinnerComps").removeClass("d-none");
				updateJsTree(selectedData, "trainee", $("#idProject").val());
			}
		}
		table.clear().draw();
	});
	$("#codeFormation").on("change", function(event) {
		const selectedData = $(this).val();
		$("#codeComposante1").val("");
		$("input[id^=hiddenCodeComposante]").val(selectedData);
		if (selectedData == "myEvents") {
			$('#frmt').addClass("d-none");
		} else {
			if(selectedData!="") {
				$("#spinnerComps").removeClass("d-none")
				updateJsTree(selectedData, category6, $("#idProject").val());
			}
		}
		table.clear().draw();
	});

	$('#checkAll').on('click', function() {
		var checked = this.checked;
		$('.data-checkbox').prop('checked', checked);
	});
	$('.data-checkbox').on('click', function() {
		var allChecked = $('.data-checkbox:checked').length === $('.data-checkbox').length;
		$('#checkAll').prop('checked', allChecked);
	});
	var urlEvents = (document.getElementById("userEvents") != null)? "/supervisor/events/adeCampus" : "/manager/adeCampus/Events";
	displayEvents(emargementContextUrl + urlEvents, table);
	var urlEventsimport = (document.getElementById("userEvents") != null)? "/supervisor/events/adeCampus/importEvents" : "/manager/adeCampus/importEvents";
	importEvents(emargementContextUrl + urlEventsimport);

	var qrCodeDisplay = document.getElementById("qrCodeDisplay");
	if (qrCodeDisplay != null) {
		var url = emargementContextUrl + "/supervisor/qrCodeSession/" + currentLocation;
		getQrCodeSession(url, "imgQrCode");
		setInterval(function() { getQrCodeSession(url, "imgQrCode") }, qrcodeChange);
	}

	$('#userPage .modal').on('show.bs.modal', function(event) {
		var id = this.id.replace("qrCodeModal", "");
		var dataEppn = this.getAttribute("data-eppn");
		var dataSession = this.getAttribute("data-session");
		var url = emargementContextUrl + "/user/qrCode/" + dataEppn + "/" + dataSession;
		var imgQrCodeUser = "imgQrCodeUser" + id;
		getQrCodeSession(url, imgQrCodeUser);
		var interval = setInterval(function() {
			getQrCodeSession(url, imgQrCodeUser);
		}, qrcodeChange);
		$('#userPage .modal').on('hidden.bs.modal', function() {
			clearInterval(interval);
		});
	});
	
	//Recherche assiduité
	if(document.getElementById("assiduitePage") != null || document.getElementById("recherchePage") != null){
		var title = '';
		var dataTableOptions = {
			responsive: true,
			ordering: true,
			paging: true,
			searching: true,
			info: false,
			order: [
			       [4, 'desc'], // First order by column 7 in descending order
			       [7, 'asc']   // Then order by column 3 in ascending order
			],
			language: {
				url: "/webjars/datatables-plugins/i18n/fr-FR.json"
			},
			columnDefs: [
				{
					targets: 'dateItem',
					type: 'datetime-moment', // Use the datetime-moment plugin
					render: function(data, type, row) {
						if (!data) { // Check if data is empty
							return ''; // Return empty string if data is empty
						}
						if (type === 'sort' || type === 'type') {
							return moment(data, 'DD/MM/YY').unix();
						}
						return moment(data, 'DD/MM/YY').format('DD/MM/YY');
					}
				}
			],
			pageLength: 100,
			lengthMenu: [
				[10, 25, 50, -1],
				[10, 25, 50, 'All']
			],
			dom: '<"top d-flex justify-content-between"fB>rt<"bottom"flp><"clear">',
			buttons: [
			    {
			        extend: 'csv',
			        className: 'btn btn-success btn-sm ms-2', // if you want the same styling like before
			        exportOptions: {
			            orthogonal: 'filter',
			            columns: ':not(.exclude)'
			        },
			        title: function() { return 'assiduite'; }
			    }
			]
		}
	}
	$('table.assiduite').DataTable(dataTableOptions);
	$('table.assiduite2').DataTable(dataTableOptions);
	$('.tableTasks').DataTable({
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: false,
		dom: 'frtilp',
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		}, columnDefs: [
			{
				targets: 'dateItem',
				type: 'datetime-moment', 
				render: function(data, type, row) {
					if (!data) {
						return '';
					}
					if (type === 'sort' || type === 'type') {
						return moment(data, 'DD/MM/YYYY').unix();
					}
					return moment(data, 'DD/MM/YYYY').format('DD/MM/YYYY');
				}
			},
			{ targets: 'no-sort', orderable: false }
		]
	});
	
	$('.tableCleanup').DataTable({
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: false,
		dom: 'frtilp',
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		}, columnDefs: [
			{
				targets: 'dateItem',
				type: 'datetime-moment', 
				render: function(data, type, row) {
					if (!data) {
						return '';
					}
					if (type === 'sort' || type === 'type') {
						return moment(data, 'DD/MM/YY').unix();
					}
					return moment(data, 'DD/MM/YY').format('DD/MM/YY');
				}
			},
			{ targets: 'no-sort', orderable: false }
		]
	});
	
	$('.tableAbsence').DataTable({
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: true,
		dom: 'frtilp',
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		}, columnDefs: [
			{
				targets: 'dateItem',
				type: 'datetime-moment', 
				render: function(data, type, row) {
					if (!data) {
						return '';
					}
					if (type === 'sort' || type === 'type') {
						return moment(data, 'DD/MM/YY').unix();
					}
					return moment(data, 'DD/MM/YY').format('DD/MM/YY');
				}
			},
			{ targets: 'no-sort', orderable: false }
		],
		initComplete: function() {
			let api = this.api();
			let searchContainer = document.querySelector('.dataTables_filter');
			if (!searchContainer) return; 
			searchContainer.classList.add('d-flex', 'justify-content-between', 'align-items-center', 'w-100');
			let filterWrapper = document.createElement('div');
			filterWrapper.classList.add('d-flex', 'gap-2');
			[3, 4, 5].forEach(function(index) {
				let column = api.column(index);
				let columnName = column.header().textContent.trim();
				// Create a Bootstrap column container for the select
				let colDiv = document.createElement('div');
				colDiv.classList.add('col-auto');
				let select = document.createElement('select');
				select.classList.add('form-control', 'form-select', 'w-auto'); // Bootstrap styling
				select.innerHTML = '<option value="">' + columnName + '</option>'; // default option
				column
					.data()
					.unique()
					.sort()
					.each(function(d) {
						select.add(new Option(stripHTMLTagsUsingDOM(d)));
					});
				select.addEventListener('change', function() {
                    var val = $.fn.dataTable.util.escapeRegex(select.value);

                    // Par défaut, on cherche juste si le mot est "contenu" (pour Motif qui a des balises HTML)
                    var searchVal = val;

                    // MAIS pour le Statut (index 5), on force la recherche STRICTE
                    // pour éviter que "JUSTIFIE" ne ressorte "INJUSTIFIE"
                    if (index === 5) {
                        searchVal = val ? '^' + val + '$' : '';
                    }

                    column.search(searchVal, true, false).draw();
				});
				colDiv.appendChild(select);
				filterWrapper.appendChild(select);
			});
			let searchWrapper = document.createElement('div');
			searchWrapper.classList.add('ms-auto');
			let searchInput = searchContainer.querySelector('input');
			if (searchInput) {
				searchWrapper.appendChild(searchInput);
			}
			searchContainer.innerHTML = '';
			searchContainer.appendChild(filterWrapper);
			searchContainer.appendChild(searchWrapper);
		}
	});
	
	$(".searchEtu").on('click', function() {
		$("#searchField").val(this.text);
	});
	
	//Select sessionEpreuve 
	checkAll();
	
	//Assiduité dateRange
	var start = moment();
    var end = moment();
	if(datesRangeSelect != ''){
		var splitRange = datesRangeSelect.split("@");
		start = moment(splitRange[0], "YYYY-MM-DD");
		end = moment(splitRange[1], "YYYY-MM-DD");
	}

    function cb(start, end) {
		if (start && end) {
	        $('#reportrange span').html(start.format('DD-MM-YYYY') + ' - ' + end.format('DD-MM-YYYY'));
	    }
    }

    $('#reportrange').daterangepicker({
        startDate: start,
        endDate: end,
        ranges: {
			"Aujourd'hui": [moment(), moment()],
			"Hier": [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
			'Cette semaine': [
	            moment().startOf('isoWeek'),  // Monday of the current week
	            moment().endOf('isoWeek')     // Sunday of the current week
	        ],
			'Mois courant': [moment().startOf('month'), moment().endOf('month')],
			'Mois dernier': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
	        'Année universitaire': [
	            // If current month is January-August, show the range of the previous September to this August
	            moment().month(8).startOf('month').subtract(moment().month() < 8 ? 1 : 0, 'year').startOf('day'),
	            moment().month(7).endOf('month').add(moment().month() >= 8 ? 1 : 0, 'year').endOf('day')
	        ]
        },
	    locale: {
	        customRangeLabel: 'Dates personnalisées',
	        applyLabel: 'Valider',
        	cancelLabel: 'Annuler'
	    }
    }, cb);

    cb(start, end);
    
	$('#reportrange').on('apply.daterangepicker', function(ev, picker) {
		$("#datesRange").val(picker.startDate.format('YYYY-MM-DD') + "@" + picker.endDate.format('YYYY-MM-DD'));
		$("#formSearch").submit();
	});
	
	$(".assiduiteControl").on("change", function(event) {
		$("#formSearch").submit();
	});
	
	$('#searchAssiduite').focus(function() {
		if ($(this).val() !== '') {
			$(this).val(''); 
			$("#searchField").val("");
			$("#searchValue").val("");
		}
	});
	//assiduité page
	if(document.getElementById("assiduitePage")){
		['groupe', 'sessionEpreuve'].forEach(id => {
		    if (document.getElementById(id)) {
		        new SlimSelect({ select: `#${id}` });
		    }
		});
	}
	
	$("#clearFilters").on("click", function(event) {
		window.location.href = window.location.origin + window.location.pathname;
	});
	$("#updateSecondTag").on("change", function(event) {
		$("#formSecondTag").submit();
	});
	
	initSelectCheckBoxes('my-select', 'Rechercher dans tous les agents');
	initSelectCheckBoxes('my-select2', 'Rechercher dans ceux déjà utilisés');
	
	$('#tableSessionsHttp').DataTable({
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: false,
		dom: 'frtilp',
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		},columnDefs: [
			{
				targets: 'dateItem',
				type: 'datetime-moment', // Use the datetime-moment plugin
				render: function(data, type, row) {
					if (!data) { // Check if data is empty
						return ''; // Return empty string if data is empty
					}
					if (type === 'sort' || type === 'type') {
						return moment(data, 'DD/MM/YY HH:mm:ss').unix();
					}
					return moment(data, 'DD/MM/YY HH:mm:ss').format('DD/MM/YY HH:mm:ss');
				}
			},
			{ targets: 'no-sort', orderable: false }
		]
	});
	//assiduité
	if(document.getElementById("assiduitePage") != null){
		document.getElementById("searchUrl").value=window.location.search;
		document.getElementById("searchUrl2").value=window.location.search;
	}
	//ADE
	function handleSelectChange(elementId, action) {
        const element = document.getElementById(elementId);
        if (element) {
            element.addEventListener("change", function () {
                if (typeof action === "function") {
                    action(); // Call function action (form submission case)
                } else {
                    window.location.href = `${emargementContextUrl}${action}?idProjet=${this.value}`;
                }
            });
        }
    }

	if(document.getElementById("projet") != null){
		$("#projet").on("change", function(event) {
				$("#projetForm").submit();
		});
	}
	if(document.getElementById("projetParam") != null){
		$("#projetParam").on("change", function(event) {
				window.location.href = emargementContextUrl + "/manager/adeCampus/params?idProjet=" + this.value;
		});
	}
	if(document.getElementById("projetTasks") != null){
		$("#projetTasks").on("change", function(event) {
				window.location.href = emargementContextUrl + "/manager/adeCampus/tasks?idProjet=" + this.value;
		});
	}
	if(document.getElementById("projetSalles") != null){
		$("#projetSalles").on("change", function(event) {
				window.location.href = emargementContextUrl + "/manager/adeCampus/salles?idProjet=" + this.value;
		});
	}
	//absences dans page surveillant
	setupModal('absenceTagCheckModal', [
	  { attr: 'data-bs-tcid', elementId: 'tcIdAbsence' },
	  { attr: 'data-bs-nom', elementId: 'nomAbsence' }
	]);

	setupModal('commentTagCheckModal', [
	  { attr: 'data-bs-tcid', elementId: 'tcIdComment' },
	  { attr: 'data-bs-nom', elementId: 'nomComment' },
	  { attr: 'data-bs-comment', elementId: 'tcComment' }
	]);
	
	//Couleur motif absence
	$('.demo').minicolors({
        theme: 'bootstrap',  // Use Bootstrap theme
        position: 'bottom left',
        format: 'hex',
        opacity: false
    });

	const hash = window.location.hash;
	if (hash.startsWith("#openmodal-")) {
		const id = hash.replace("#openmodal-", "");
		fetch(emargementContextUrl + `/manager/sessionEpreuve/${id}?modal=true`)
			.then(res => res.text())
			.then(html => {
				const modalEl = document.getElementById("show-modal");
				modalEl.querySelector(".modal-content").innerHTML = html;
				new bootstrap.Modal(modalEl).show();
				history.replaceState(null, '', window.location.pathname + window.location.search);
			});
	}
	if (hash === "#openmodal") {
		const modalEl = document.getElementById("show-modal");
		new bootstrap.Modal(modalEl).show();
		history.replaceState(null, '', window.location.pathname + window.location.search);
	}
	document.addEventListener('click', function(event) {
		if (event.target.classList.contains('closeModal')) {
			window.location.reload();
		}
	});
	//enable tooltips
	const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
	const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))
	
	//Communication
	if(document.getElementById("includePdf")){
		document.getElementById("includePdf").addEventListener("change", function () {
	   			var htmltemplate = document.getElementById("htmltemplate");
				htmltemplate.value = editor1.getContents();
				var htmltemplatePdf = document.getElementById("htmltemplatePdf");
				htmltemplatePdf.value = editor1.getContents();
	    });
	}
	
	
	//Autocomplete
	if (document.querySelector('#suPage, #presencePage, #userAppPage, #locationPage, #sessionEpreuvePage, #recherchePage, #addMembersPage, #extractionPage, #createSuperAdminPage, #createTagCheckPage, #createUserApp, #createAbsence, #assiduitePage, #tagChecksListPage')) {
		
	    var searchBoxes = document.querySelectorAll(".searchBox"); // Get all search inputs
		var formSearch = document.getElementById("formSearch");
	    searchBoxes.forEach(function (searchBox) {
	        var awesomplete = new Awesomplete(searchBox, { minChars: 3, maxItems: 100, autoFirst: true });
	
	        searchBox.addEventListener("focus", function () {
	            this.value = "";
	        });
	
	        document.body.addEventListener("htmx:afterSwap", function (event) {
	            var searchResults = document.querySelector(`#${searchBox.getAttribute("hx-target").substring(1)}`);
	            if (searchResults && event.detail.target === searchResults) {
	                var items = Array.from(searchResults.getElementsByClassName("result-item"));
	                var suggestions = items.map(item => ({
	                    label: item.innerHTML,
	                    value: item.dataset.value
	                }));
	                awesomplete.list = suggestions;
	                searchResults.hidden = true;
	            }
	        });
	
	        searchBox.addEventListener("awesomplete-selectcomplete", function (event) {
	            var dataValue = event.text.value.trim();
	            var selectedValue = dataValue.split("//")[0].trim();
	            this.value = selectedValue;
	
	            // Get the closest hidden input related to this search box
	            var typeSearch = document.getElementById("typeSearch");
	            if (typeSearch) {
	                typeSearch.value = this.getAttribute("data-typeSearch") || "";
	            }
				searchBoxes.forEach(input => {
				                if (input !== searchBox) {
				                    input.disabled = true;
				                }
				            });
				if (document.querySelector("#createSuperAdminPage, #createTagCheckPage, #createUserApp, #createAbsence, #addMembersPage")){
					var splitResult = dataValue.split("//");
					this.value = splitResult[0].toString().trim();
					if(document.querySelector("#createTagCheckPage, #createUserApp, #createAbsence")){
						$("#eppn").val(splitResult[0].toString().trim());
					}
					if(document.getElementById("resultEppn")){
						$("#resultEppn").html(splitResult[1] + " " + splitResult[2]);
					}
					if(document.getElementById("tcIdentity")){
						$("#tcIdentity").html(splitResult[2].toString().trim() + ' ' + splitResult[1].toString().trim());
					}
				}else{
					var splitResult = dataValue.split("//");
					if(document.getElementById("searchField")){
						var split3 = (splitResult[3] == undefined)? ''  : " // " + splitResult[3];
						this.value = splitResult[1] + " " + splitResult[2] + split3;
						$("#searchField").val(splitResult[1] + " " + splitResult[2] + split3);
						$("#searchValue").val(splitResult[0]);
					}else if(document.getElementById("tagChecksListPage")){
						$("#eppn").val(splitResult[0].toString().trim());
					}
					formSearch.submit();
				}
	        });
	    });
	}
	var createTagCheckForm = document.getElementById("createTagCheckForm");
	if (createTagCheckForm != null) {
		createTagCheckForm.addEventListener("submit", function(e) {
			const eppn = document.getElementById("searchString").value.trim();
			const numIdentifiant = document.getElementById("numIdentifiant").value.trim();

			if (!eppn && !numIdentifiant) {
				e.preventDefault();
				alert("Veuillez renseigner soit l’Eppn, soit le N° identifiant.");
			}
		});
	}
	
	//Duplicate sessions
	const inputVisible = document.getElementById("datepicker");

	if(inputVisible != null){
		const fp = flatpickr(inputVisible, {
			mode: "multiple",
			dateFormat: "Y-m-d", // format technique (pour le hidden)
			locale: "fr",
			altInput: false, // désactivé car on veut gérer nous-mêmes l’affichage
			onChange: updateDisplay
		});
		const badgesContainer = document.getElementById("badgesContainer");
		// 3️⃣ Suppression d’une date via le badge
		badgesContainer.addEventListener("click", (e) => {
			if (e.target.classList.contains("btn-close")) {
				const dateToRemove = e.target.getAttribute("data-date");
				const newDates = fp.selectedDates.filter(d => d.toISOString().split('T')[0] !== dateToRemove);
				fp.setDate(newDates);
				updateDisplay(newDates);
			}
		});
	}
});

//absences dans assiduité
let slimSelectInstance = null;
document.addEventListener('htmx:afterSwap', function(event) {
	if (event.detail.target.id !== "searchResults" && document.getElementById("motifAbsence") && document.getElementById("createAbsence")==null){
	    setTimeout(() => {
	        if (slimSelectInstance) {
	            slimSelectInstance.destroy();
	        }
	        slimSelectInstance = new SlimSelect({ select: '#motifAbsence' });
	    }, 100);
	}
	if (document.getElementById("blackListGroupe")!=null){
		if (event.target.id === "modal-content") {
			new SlimSelect({
				select: '#blackListGroupe'
			});

			let fileInput = document.querySelector('#files');
			if (fileInput) {
				initializeFileInput(fileInput);
			}
		}
     }
	 if (document.getElementById("modal-step3")){
	     initSelectCheckBoxes('my-select', 'Rechercher dans tous les agents');
	     initSelectCheckBoxes('my-select2', 'Rechercher dans ceux déjà utilisés');
	 }
	 if (event.target.querySelector('select.slimSelectClass')) {
	    initSlimSelects();
	  }
	if (document.getElementById('extractionPage') !== null) {
		const slimConfigs = [
			{ elements: ['codCmp', 'sessionEpreuve', 'sessionEpreuveLdap', 'sessionEpreuveGroupe', 'sessionEpreuveCsv', 'groupe', 'sessionLocationCsv', 'sessionLocationLdap', 'sessionLocationGroupe', 'sessionLocation'] },
			{ elements: ['codSes', 'codAnu'], options: { showSearch: false } },
			{ elements: ['codEtp', 'codElp', 'codExtGpe'], options: { allowDeselect: true, enabled: true } }
		];
		slimConfigs.forEach(config => {
			config.elements.forEach(id => {
				const element = document.getElementById(id);
				if (element) {
					// Détruire l’ancienne instance si déjà créée
					if (element.slim) {
						element.slim.destroy();
					}
					const options = config.options || {};
					const slim = new SlimSelect({ select: `#${id}`, ...options });
					if (options.enabled) {
						slim.enable();
					}
					element.slim = slim;
				}
			});
		});
	}
});

document.addEventListener('htmx:afterSettle', function(evt) {
	if (document.getElementById("modal-step3")){
		$('#my-select').hide();
		$('#my-select2').hide();
	}
});

document.addEventListener("htmx:afterRequest", function(event) {
	if (document.getElementById("presencePage")){
		let locationSelect = document.getElementById("location");
		if (locationSelect.children.length > 1) {
			locationSelect.removeAttribute("disabled");
		} else {
			locationSelect.setAttribute("disabled", "true");
		}
	}
});