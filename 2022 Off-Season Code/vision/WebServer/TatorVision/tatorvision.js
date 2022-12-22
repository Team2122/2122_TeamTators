
// Pre-declare variables
var connection = null;
var ws = WebSocket;
// var serverPort = 8051 ;
var serverPort = 5800 ;

// Since its not, here we go and use the local hostname
 var serverUrl = "ws://" + window.location.hostname + ":" + serverPort ;
// var serverUrl = "ws://127.0.0.1:8051" ;
// var serverUrl = "ws://10.21.22.52:8051" ;
// var serverUrl = "ws://10.0.0.3:8051" ;
// var serverUrl = "ws://192.168.1.3:8051" ;

// Hide Unwanted Tab
$('#ball-control-tab').hide();

var writableButtonIds = [ 'sendYes', 'sendNo', 'pipeline-control', 'system-controls' ];

var videoStream = [ 'videoStream0', 'videoStream1', 'videoStream2', 'videoStream3', 'videoStream4', 'videoStream5' ] ;

var cameraControls = [ 'tatorVisionCamera', 'tatorVisionResolution', 'tatorVisionframeRate', 'orientation', 'CameraCropLeft', 'CameraCropRight', 'CameraCropTop', 'CameraCropBottom', 'auto_exposure', 'exposure_value', 'brightness_value', 'saturation_value', 'contrast_value', 'sharpness_value', 'colorbalanceR_value', 'colorbalanceB_value' ] ;

var pipelineControls = [ 'scaleFactor', 'dynamic_clipping', 'rgb_clipping', 'lightness_clipping', 'chroma_range', 'hue_range', 'L_Ref', 'a_Ref', 'b_Ref', 'threshold' ] ;

var filteringControls = [ 'fullness_filter', 'brightness_filter', 'chroma_filter', 'aspect_ratio_filter', 'pct_area_filter', 'color_filter', 'morphology', 'conn_comp', 'seg_method', 'boundary_detect', 'target_detect' ] ;

var systemControls = [ 'debugMode', 'captureMode', 'captureIncrement', 'debugFilename', 'multi_thread', 'executor_service', 'numberOfThreads', 'visionLEDstatus', 'visionLEDbrightness' ] ;

var previewControls = [ 'Shuffleboard', 'Ball_Detect', 'Target_Detect', 'Thresholded' ] ;

var applyControls = [ 'applyCamera', 'applyPipeline', 'applySystem' ] ;


function renderMultiSlider( values, controlElement  ) 
{
	var msg = ', ' + controlElement + ': [' ;			
	for( var j = 0 ; j< values.length; j++ ){	
		msg += ' ' + Number(values[j]) ;
	}
	return msg += ' ] ';
}


function extractSubStringNumbers( myString, id_string )
{
	// Extract the values from the string, space delimited
	var matches = myString.split( " " ) ;
	
	// Now update the web page slider
	slider_ID = document.getElementById( id_string );
	slider_ID.noUiSlider.set( [ matches[1] , matches[2] ] );

	// console.log( matches[0] ) ;
	// console.log( matches[1] ) ;
}

function extractPixelValues( myString, id_string1, id_string2, id_string3 )
{
	// Extract the values from the string, space delimited
	var matches = myString.split( " " ) ;
	
	// Now update the web page
	element_ID = document.getElementById( id_string1 );
	element_ID.innerHTML = matches[1] ;
	element_ID = document.getElementById( id_string2 );
	element_ID.innerHTML = matches[2] ;
	element_ID = document.getElementById( id_string3 );
	element_ID.innerHTML = matches[3] ;
}

function updateSliderValue( valueID, myValue )
{
	var value = document.getElementById(valueID);
	value.innerHTML = myValue;
}


function getImageLocation( e )
{
	bounds=this.getBoundingClientRect();
	
	var left=bounds.left;
	var top=bounds.top;

	var x = event.pageX - left - window.scrollX;
	var y = event.pageY - top - window.scrollY;

	var cw=this.clientWidth ;
	var ch=this.clientHeight ;
	var iw=this.naturalWidth ;
	var ih=this.naturalHeight ;
	
	var xPosition=Math.floor( x/cw*iw ) ;
	var yPosition=Math.floor( y/ch*ih ) ;
	
	var pixelX = document.getElementById('pixel_X');
	var pixelY = document.getElementById('pixel_Y');

	pixelX.innerHTML = xPosition;
	pixelY.innerHTML = yPosition;
		
    console.log("Image Location : " + xPosition + "," + yPosition);

	var msg = "{ type : tatorGetCIELabPixelValues, X_Y_Coordinates : [" + xPosition + " " + yPosition + "] }" ;
	connection.send( msg );	
}

function buildJSONresponse( control, elementList )
{  
  var msg = '{ type: ' + control ;

  for (var i = 0; i < elementList.length; i++) {

	object_state = document.getElementById( elementList[i] ) ;

	if ( object_state != null )
	{
		if( object_state.value == null ){
			if ( object_state.hasOwnProperty('noUiSlider') )
			{
				var values = object_state.noUiSlider.get();
				msg += renderMultiSlider( values, elementList[i] ) ;

			} else {	
				msg += ', ' + elementList[i] + ': ' + object_state.innerHTML +' ' ;			
			}
		} else {		
			msg += ', ' + elementList[i] + ': ' ;
			
			if ( object_state.type == "checkbox" ){
				if ( object_state.checked == true ){
					msg += '"on"' ;
				} else {
					msg += '"off"' ;
				}
			}else if ( object_state.type == "text" ){
				msg += '"' + object_state.value +'"' ;
			}else{
				msg += object_state.value;
			}
		}
	}
  }
  
  msg += '}' ;
  
  return msg ;  
}

function buildSlider( functionID, sliderID, valueID )
{
	var slider = document.getElementById(sliderID);
	var value = document.getElementById(valueID);
	value.innerHTML = slider.value;
	
	// slider.style.background = '#c0392b';
		
	slider.oninput = function() 
	{
	  value.innerHTML = this.value;

	  // Extract the information for the slider
	  var response = [ valueID ] ;	  
	  msg = buildJSONresponse( functionID, response ) ;
	  connection.send( msg );
	}	
}

function updateVideoStreamURL()
{
	//Update the video feed
	var video0 = document.getElementById("videoStream0");
	var video1 = document.getElementById("videoStream1");
	var video2 = document.getElementById("videoStream2");
	var video3 = document.getElementById("videoStream3");
	var video4 = document.getElementById("videoStream4");
	var video5 = document.getElementById("videoStream5");

	var new_video_1 = "http://" + window.location.hostname + ":1181/stream.mjpg" ;
	var new_video_2 = "http://" + window.location.hostname + ":1182/stream.mjpg" ;
	
	// Set the Video Stream
	video0.src = new_video_1;
	video1.src = new_video_1;
	video2.src = new_video_1;
	video3.src = new_video_1;
	video4.src = new_video_1;
	video5.src = new_video_2;
	
}

// ------------------------------------------
// Code setting up the Camera Control Tab
// ------------------------------------------

buildSlider( 'cameraControl', 'exposure_slider', 'exposure_value');
buildSlider( 'cameraControl', 'brightness_slider', 'brightness_value');
buildSlider( 'cameraControl', 'saturation_slider', 'saturation_value');
buildSlider( 'cameraControl', 'contrast_slider', 'contrast_value');
buildSlider( 'cameraControl', 'sharpness_slider', 'sharpness_value');
buildSlider( 'cameraControl', 'colorbalanceR_slider', 'colorbalanceR_value');
buildSlider( 'cameraControl', 'colorbalanceB_slider', 'colorbalanceB_value');

var applyCamera = document.getElementById('applyCamera');

applyCamera.onclick = function() {
	
  var msg = buildJSONresponse( 'cameraControl', cameraControls ) ;
  connection.send( msg );
  
}


// ------------------------------------------


// ------------------------------------------
// Code setting up the Pipeline Control Tab
// ------------------------------------------

// videoStream0.addEventListener("click", getImageLocation, false);
videoStream1.addEventListener("click", getImageLocation, false);

var clip_RGB = document.getElementById('rgb_clipping');
var clip_L = document.getElementById('lightness_clipping');
var clip_chroma = document.getElementById('chroma_range');
var clip_hue = document.getElementById('hue_range');

noUiSlider.create(clip_RGB, {
	start: [ 0, 255],
	connect: true,
	range: {
		'min': 0,
		'max': 255
	},
    pips: {mode: 'count', values: 11}
});

noUiSlider.create(clip_L, {
	start: [0, 100],
	connect: true,
	range: {
		'min': 0,
		'max': 100
	},
    pips: {mode: 'count', values: 11}
});

noUiSlider.create(clip_chroma, {
	start: [25,75],
	connect: true,
	range: {
		'min': 0,
		'max': 128
	},
    pips: {mode: 'count', values: 5}
});

noUiSlider.create(clip_hue, {
	start: [200, 300],
	connect: true,
	range: {
		'min': 0,
		'max': 360
	},
    pips: {mode: 'count', values: 10}
});

var applyPipeline = document.getElementById('applyPipeline');

applyPipeline.onclick = function() {
	
  var msg = buildJSONresponse( 'pipelineControl', pipelineControls ) ;
  connection.send( msg );  
  
}

// ------------------------------------------


// ------------------------------------------
// Code setting up the Filtering Control Tab
// ------------------------------------------

var brightness_filter = document.getElementById('brightness_filter');
var chroma_filter = document.getElementById('chroma_filter');
var pct_area_filter = document.getElementById('pct_area_filter');
var fullness_filter = document.getElementById('fullness_filter');
var aspect_ratio_filter = document.getElementById('aspect_ratio_filter');

noUiSlider.create(brightness_filter, {
	start: [ 0, 100 ],
	connect: true,
	range: {
		'min': 0,
		'max': 100
	},
    pips: {mode: 'count', values: 6}
});

noUiSlider.create(pct_area_filter, {
	start: [0, 3],
	connect: true,
	range: {
		'min': 0,
		'max': 3
	},
    pips: {mode: 'count', values: 4}
});

noUiSlider.create(fullness_filter, {
	start: [0,100],
	connect: true,
	range: {
		'min': 0,
		'max': 100
	},
    pips: {mode: 'count', values: 6}
});

noUiSlider.create(aspect_ratio_filter, {
	start: [0, 10],
	connect: true,
	range: {
		'min': 0,
		'max': 10
	},
    pips: {mode: 'count', values: 6}
});

noUiSlider.create(chroma_filter, {
	start: [0, 128],
	connect: true,
	range: {
		'min': 0,
		'max': 128
	},
    pips: {mode: 'count', values: 6}
});
var applyFiltering = document.getElementById('applyFiltering');

applyFiltering.onclick = function() {
	
  var msg = buildJSONresponse( 'filteringControl', filteringControls ) ;
  connection.send( msg );  
  
}

// ------------------------------------------


// ------------------------------------------
// Code setting up the System Control Tab
// ------------------------------------------

var applySystem = document.getElementById('applySystem');
applySystem.onclick = function() 
{  	
  var msg = buildJSONresponse( 'systemControl', systemControls ) ;
  connection.send( msg ); 
}

var saveConfig = document.getElementById('saveConfig');
saveConfig.onclick = function() 
{
  var msg = '{ type : saveConfiguration }' ;  
  connection.send( msg );   
}

var loadConfig = document.getElementById('loadConfig');
loadConfig.onclick = function() 
{
  var msg = '{ type : loadConfiguration }' ;  
  connection.send( msg );   
}

var resetConfig = document.getElementById('resetConfig');
resetConfig.onclick = function() 
{
  var msg = '{ type : resetConfiguration }' ;  
  connection.send( msg );   
}



// ------------------------------------------


// ------------------------------------------
// Code setting up the Footer Bar
// ------------------------------------------

/*
var applyAll = document.getElementById('applyAll');

applyAll.onclick = function() {
	
  var msg = '' ;
  
  msg = buildJSONresponse( 'cameraControl', cameraControls ) ;
  connection.send( msg );

  msg = buildJSONresponse( 'pipelineControl', pipelineControls ) ;
  connection.send( msg );

  msg = buildJSONresponse( 'systemControl', systemControls ) ;
  connection.send( msg );
  
}
*/

function updateManualExposureView() {
  // console.log( $('#auto_exposure').val() ) ;	
  if ($('#auto_exposure').val() === "yes") {
    $('#exposure_title').collapse('hide');
    $('#exposure_slider').collapse('hide');
	$('#exposure_value').collapse('hide');
  } else {
    $('#exposure_title').collapse('show');
    $('#exposure_slider').collapse('show');
	$('#exposure_value').collapse('show');
  }
}

function updateDynamicClippingView() {
  // console.log( $('#dynamic_clipping').val() ) ;	
  if ($('#dynamic_clipping').val() === "yes") {
    $('#rgb_clipping').collapse('hide');
	$('#rgb_clipping_label').collapse('hide');
  } else {
    $('#rgb_clipping').collapse('show');
	$('#rgb_clipping_label').collapse('show');
  }
}

$('#visionLEDstatus').change(function() {

  var object_state = document.getElementById( 'visionLEDstatus' ) ;
  setTatorLEDState( object_state );
});

function setTatorLEDState( object_state )
{
	if ( object_state.checked == true ){
		var msg = '{ type : systemControl , visionLEDstatus : \"on\" }' ;  
	} else {
		var msg = '{ type : systemControl , visionLEDstatus : \"off\" }' ;  
	}	
	connection.send( msg );   
}

$('#dynamic_clipping').change(function() {
  updateDynamicClippingView();
});

$('#auto_exposure').change(function() {
  updateManualExposureView();
});

function setTatorEWSViewingState( state )
{
  var msg = '{ type : tatorEWSViewingState , EWS_Mode : ' + state + '}' ;  
  connection.send( msg );   
}

function getTatorVisionState( )
{
  var msg = '{ type : tatorVisionState }' ;  
  connection.send( msg );   
}

function setOrietationViewMode( orientation )
{
	// Get the browser dimensions
	var w = window.innerWidth;
	var h = window.innerHeight;

	var width=640 ; 
	var height=480 ; 
	var scalefactor = 1.0 ;

	if ( (w/3) < width  ) {
		scalefactor = (w/3) / width ; 
	}

	// Set the viewing mode for the video feed
	switch ( orientation ) 
	{
		case '0'   : 
			document.getElementById("orientation").value = "0";  
			width = 640 ; height = 480 ; break ;
		case '180' : 
			document.getElementById("orientation").value = "180";
			width = 640 ; height = 480 ; break ;
		case '90'  : 
			document.getElementById("orientation").value = "90"; 
			width = 480 ; height = 640 ; break ;			
		case '270'  : 
			document.getElementById("orientation").value = "270"; 
			width = 480 ; height = 640 ; break ;
	}
	
	var video_w = (scalefactor*width).toString() + "px" ;
	var video_h = (scalefactor*height).toString() + "px" ;
	
	for (var i = 0; i < videoStream.length; i++) {
		document.getElementById(videoStream[i]).style.width = video_w ;
		document.getElementById(videoStream[i]).style.height = video_h ;
	}

	// Now scale the Full Frame Video Feeds
	var scalefactor_w = w/width ; 
	var scalefactor_h = h/height ;
	scalefactor = Math.min( scalefactor_w, scalefactor_h ) * 0.8 ;
	
	var video_w = (scalefactor*width).toString() + "px" ;
	var video_h = (scalefactor*height).toString() + "px" ;
	
	document.getElementById('videoStream4').style.width = video_w ;
	document.getElementById('videoStream4').style.height = video_h ;

	document.getElementById('videoStream5').style.width = video_w ;
	document.getElementById('videoStream5').style.height = video_h ;
	
}

// Handle Read-Only and Writable buttons
$('#Shuffleboard').click(function() {
  var $this = $(this);
  if ($this.hasClass('active')) return;

  updateEWSStateButtons( 'Shuffleboard' ) ; 
  setTatorEWSViewingState( 'Shuffleboard' );
});

$('#Target_Detect').click(function() {
  var $this = $(this);
  if ($this.hasClass('active')) return;

  updateEWSStateButtons( 'Target_Detect' ) ; 
  setTatorEWSViewingState( 'Target_Detect' );
});

$('#Thresholded').click(function() {
  var $this = $(this);
  if ($this.hasClass('active')) return;

  updateEWSStateButtons( 'Thresholded' ) ; 
  setTatorEWSViewingState( 'Thresholded' );
});

$('#Ball_Detect').click(function() {
  var $this = $(this);
  if ($this.hasClass('active')) return;

  updateEWSStateButtons( 'Ball_Detect' ) ; 
  setTatorEWSViewingState( 'Ball_Detect' );
});

function updateEWSStateButtons( this_control ) {

  // Deactivate the unselected controls
  for (var i = 0; i < previewControls.length; i++) {
	$('#' + previewControls[i]).removeClass('active').prop('aria-pressed', false);
  } 
  // Now update the selected Control
  $('#'+this_control).addClass('active').prop('aria-pressed', true)
}


function displayConnected() {
  $('#connectionBadge').removeClass('badge-secondary').addClass('badge-primary').text('Connected');

  for (var i = 0; i < cameraControls.length; i++) {
	$('#' + cameraControls[i]).prop('disabled', false);
  }

  for (var i = 0; i < pipelineControls.length; i++) {
	$('#' + pipelineControls[i]).prop('disabled', false);
  }

  for (var i = 0; i < systemControls.length; i++) {
	$('#' + systemControls[i]).prop('disabled', false);
  }  

  for (var i = 0; i < previewControls.length; i++) {
	$('#' + previewControls[i]).prop('disabled', false);
  } 

  for (var i = 0; i < applyControls.length; i++) {
	$('#' + applyControls[i]).prop('disabled', false);
  } 
  
  // Force an update on the video feeds
  updateVideoStreamURL()
}

function displayDisconnected() {
  $('#connectionBadge').removeClass('badge-primary').addClass('badge-secondary').text('Disconnected');
  
  for (var i = 0; i < cameraControls.length; i++) {
	$('#' + cameraControls[i]).prop('disabled', true);
  }

  for (var i = 0; i < pipelineControls.length; i++) {
	$('#' + pipelineControls[i]).prop('disabled', true);
  }

  for (var i = 0; i < systemControls.length; i++) {
	$('#' + systemControls[i]).prop('disabled', true);
  }
  
  for (var i = 0; i < previewControls.length; i++) {
	$('#' + previewControls[i]).prop('disabled', true);
  }  
  
  for (var i = 0; i < applyControls.length; i++) {
	$('#' + applyControls[i]).prop('disabled', true);
  } 
}

function connError(source){
	source.src = "TeamTators.png";
	source.onerror = ""; 
	return true; 
}


// WebSocket automatic reconnection timer
var reconnectTimerId = 0;

function connectTatorSocket()
{
	// Check to see if the connection is already open
    if (connection && connection.readyState !== WebSocket.CLOSED) 
		return;
	
	// Make sure that it is a valid port
	// if (window.location.port !== '') {
	//	  serverUrl += ":8051";
	//}

	// Create the new WebSocket connection
	connection = new WebSocket(serverUrl, 'tatorvision');
	console.log( "Web Socket Connection Initiated : " + serverUrl ) ;

	connection.onopen = function(evt)
	{
		if (reconnectTimerId) {
			window.clearInterval(reconnectTimerId);
			reconnectTimerId = 0;
		}
		console.log( "Displaying Connected" ) ;
		displayConnected();
		getTatorVisionState();
	};

	connection.onclose = function(evt)
	{	
		if (!reconnectTimerId) {
			reconnectTimerId = setInterval(function() { connectTatorSocket(); }, 2000);
		}
  
		console.log( "Displaying Disconnected" ) ;
		displayDisconnected();
	};

	// WebSocket incoming message handling
	connection.onmessage = function(evt) 
	{

		// Parse the websocket message
		// console.log( "Parsing Message" ) ;
		// console.log( evt.data ) ;

		var msg = JSON.parse(evt.data);
			
		// If the message is NULL, return
		if (msg === null) {
			return;
		}	

		if( msg.type == "cameraControl" || msg.type == "updateSystemControls" )
		{		

			switch ( msg.tatorVisionCamera ) {
				case '0' : document.getElementById("tatorVisionCamera").value = "0"; break ;
				case '1' : document.getElementById("tatorVisionCamera").value = "1"; break ;
			}
					
			switch ( msg.tatorVisionResolution ) {
				case '640x480' : document.getElementById("tatorVisionResolution").value = "640x480"; break ;
				case '320x240' : document.getElementById("tatorVisionResolution").value = "320x240"; break ;
			}

			switch ( msg.tatorVisionframeRate ) {
				case '15' : document.getElementById("tatorVisionframeRate").value = "15"; break ;
				case '30' : document.getElementById("tatorVisionframeRate").value = "30"; break ;
			}
					
			setOrietationViewMode( msg.orientation )

			updateSliderValue( 'exposure_value', msg.exposure_value);
			slider_ID = document.getElementById('exposure_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.exposure_value;			

			updateSliderValue( 'brightness_value', msg.brightness_value);
			slider_ID = document.getElementById('brightness_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.brightness_value;			

			updateSliderValue( 'saturation_value', msg.saturation_value);
			slider_ID = document.getElementById('saturation_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.saturation_value;			

			updateSliderValue( 'contrast_value', msg.contrast_value);
			slider_ID = document.getElementById('contrast_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.contrast_value;			

			updateSliderValue( 'sharpness_value', msg.sharpness_value);
			slider_ID = document.getElementById('sharpness_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.sharpness_value;			
			
			updateSliderValue( 'colorbalanceR_value', msg.colorbalanceR_value);
			slider_ID = document.getElementById('colorbalanceR_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.colorbalanceR_value;			

			updateSliderValue( 'colorbalanceB_value', msg.colorbalanceB_value);
			slider_ID = document.getElementById('colorbalanceB_slider');
			slider = document.querySelector("slider_ID[type='range']");
 		    slider_ID.value = msg.colorbalanceB_value;			

			// Update the Crop Attributes
			$('#CameraCropLeft').val(msg.CameraCropLeft);
			$('#CameraCropRight').val(msg.CameraCropRight);
			$('#CameraCropTop').val(msg.CameraCropTop);
			$('#CameraCropBottom').val(msg.CameraCropBottom);
			
		}
		
		if( msg.type == "systemControl" || msg.type == "updateSystemControls" )
		{		

			// console.log( msg );

			if ( msg.debugMode == "1" )
				document.getElementById('debugMode').checked = true;
			else
				document.getElementById('debugMode').checked = false;
				
			if ( msg.captureMode == "1" )
				document.getElementById('captureMode').checked = true;
			else
				document.getElementById('captureMode').checked = false;

			$('#captureIncrement').val(msg.captureIncrement);
			$('#debugFilename').val(msg.debugFilename);

			if ( msg.multi_thread == "1" )
				document.getElementById('multi_thread').checked = true;
			else
				document.getElementById('multi_thread').checked = false;
				
			if ( msg.executor_service == "1" )
				document.getElementById('executor_service').checked = true;
			else
				document.getElementById('executor_service').checked = false;

			$('#numberOfThreads').val(msg.numberOfThreads);

			if ( msg.visionLEDstatus == "1" )
				document.getElementById('visionLEDstatus').checked = true;
			else
				document.getElementById('visionLEDstatus').checked = false;

			$('#visionLEDbrightness').val(msg.visionLEDbrightness);			
			
		}
		
		if( msg.type == "pipelineControl" || msg.type == "updateSystemControls" )
		{	

			$('#scaleFactor').val(msg.scaleFactor);

			if ( msg.dynamic_clipping == "1" )
				$('#dynamic_clipping').val("yes");
			else
				$('#dynamic_clipping').val("no");

			extractSubStringNumbers( msg.rgb_clipping, 'rgb_clipping' ) ;
			extractSubStringNumbers( msg.lightness_clipping, 'lightness_clipping' ) ;
			extractSubStringNumbers( msg.chroma_range, 'chroma_range');
			extractSubStringNumbers( msg.hue_range, 'hue_range');

			$('#L_Ref').val(msg.L_Ref);
			$('#a_Ref').val(msg.a_Ref);
			$('#b_Ref').val(msg.b_Ref);
			$('#threshold').val(msg.threshold);

			updateDynamicClippingView();
			updateManualExposureView();
		
		}	
		
		if( msg.type == "filteringControl" || msg.type == "updateSystemControls" )
		{	

			extractSubStringNumbers( msg.fullness_filter, 'fullness_filter' ) ;
			extractSubStringNumbers( msg.chroma_filter, 'chroma_filter' ) ;
			extractSubStringNumbers( msg.brightness_filter, 'brightness_filter' ) ;
			extractSubStringNumbers( msg.aspect_ratio_filter, 'aspect_ratio_filter');
			extractSubStringNumbers( msg.pct_area_filter, 'pct_area_filter');

			switch ( msg.color_filter ) {
				case 'Green' : document.getElementById("color_filter").value = "Green"; break ;
				case 'Red' : document.getElementById("color_filter").value = "Red"; break ;
				case 'Blue' : document.getElementById("color_filter").value = "Blue"; break ;
				case 'Yellow' : document.getElementById("color_filter").value = "Yellow"; break ;
				default : document.getElementById("color_filter").value = "Any"; break ;
			}

			$('#morphology').val(msg.morphology);
			$('#conn_comp').val(msg.conn_comp);
			$('#boundary_detect').val(msg.boundary_detect);				
			$('#target_detect').val(msg.target_detect);	
			$('#seg_method').val(msg.seg_method);		

		}

		if( msg.type == "updateSystemControls" )
		{	
			console.log( msg.EWSViewingMode ) ; 
			
			switch ( msg.EWSViewingMode ) {
				case 'Target_Detect' : 
					updateEWSStateButtons( 'Target_Detect' ) ; 
					break;
				case 'Ball_Detect' :
					updateEWSStateButtons( 'Ball_Detect' ) ; 
					break;
				case 'Thresholded' :
					updateEWSStateButtons( 'Thresholded' ) ; 
					break;
				case 'Shuffleboard' :
				default :
					updateEWSStateButtons( 'Shuffleboard' ) ; 
					break;
			}
		}
		
		if( msg.type == "pixelColorValues" )
		{		
			extractPixelValues( msg.RGBValues, "pixel_R", "pixel_G", "pixel_B" ) ;
			extractPixelValues( msg.CIELabValues, "pixel_CIE_L", "pixel_CIE_a", "pixel_CIE_b" ) ;
			extractPixelValues( msg.CIELChValues, "pixel_CIE_L2", "pixel_CIE_C", "pixel_CIE_h" ) ;		
		}	
		
	}
}

updateVideoStreamURL();
updateDynamicClippingView();
connectTatorSocket();

