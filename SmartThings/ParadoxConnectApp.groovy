/*
 *  Paradox Alarm Panel integration via REST API callbacks
 *
 */

definition(
    name: "Paradox Connect",
    namespace: "bitbounce",
    author: "tracstarr",
    description: "Paradox Alarm Integration App",
    category: "My Apps",
    iconUrl: "http://pic.appvv.com/fb3361641c59ff32478ab1f0d12a9da7/175.png",
    iconX2Url: "http://pic.appvv.com/fb3361641c59ff32478ab1f0d12a9da7/175.png",
    oauth: true
)

import groovy.json.JsonBuilder

/************************************************************
*	Preferences
*/
preferences {

 	 page(name:"mainPage", title:"Paradox Connect", content: "mainPage") 
     page(name:"settingsPage", title:"Paradox Connect Settings", content: "serverSettingsPage") 
     page(name:"paradoxServiceConnectPage", title:"Paradox Service Connect", content:"paradoxServiceConnectPage", refreshTimeout:2, install:true)
     
     page(name:"deviceSelectPage", title:"Paradox Device Select", content:"deviceSelectPage")
     page(name:"statusCheckPage", content:"statusCheckPage")
     
  /*
  section("Notification events (optional):") {
    input "notifyEvents", "enum", title: "Which Events?", description: "default (none)", required: false, multiple: false,
     options:
      ['all','alarm','closed','open','closed','partitionready',
       'partitionnotready','partitionarmed','partitionalarm',
       'partitionexitdelay','partitionentrydelay'
      ]
  }*/
}

/************************************************************
*	REST Endpoint Definitions
*/
mappings {
  path("/zone/:id/:status") {
      action: [
          PUT: "updateZoneHandler"
      ]
  }
  path("/link") {
      action: [
          GET: "linkHandler",
      ]
   }
   path("/revoke") {
       action: [
           GET: "revokeHandler",
       ]
   }
}

/************************************************************
*	Install/Uninstall/Updated
*/
def installed() {
	DEBUG( "Installed with settings: ${settings}")
	initialize()
}

def updated() {
	DEBUG("Updated with settings: ${settings}")

	unsubscribe()
	initialize()
}

def initialize() {
	 DEBUG("Initialize")
     
     subscribe(location, null, lanHandler, [filterEvents:false])    
}
/************************************************************
*	Pages
*/

def mainPage()
{	
	return dynamicPage(name:"mainPage", title: "Paradox Connect", uninstall: state.isLinked, install: false) {
    			section() {
                	href ("settingsPage", title: state.isLinked? "Server Settings":"Connect", description:"Paradox server settings.")
                }
                
                if (state.isLinked)
                {
                
                	UpdateSelectedDevices()
                 	
                	section() {
                		href ("deviceSelectPage", title: "Devices", description: "Select the devices you want to control.")                       
                    }
                    
                    section() {
                    	paragraph ("Link established to your Paradox server")
                        href ("statusCheckPage", title: "Status Check", description: "Check if your Paradox Controller is connected and running correctly.")
                    }
                    
                }
                else
                {
                	section() {
                		paragraph ("No link has been established to your Paradox server. Please click Connect.")
                    }
                }
            }

}

def serverSettingsPage()
{
	return dynamicPage(name:"settingsPage", title: "Paradox Connect Settings", uninstall:false, install:false){
    		section() {
            	paragraph "Please enter your Paradox Service Endpoint details."
                input("ip", "string", title:"IP Address", description: "IP Address", required: true, defaultValue: "192.168.20.148")
                input("port", "string", title:"Port", description: "Port", defaultValue: 8876 , required: true)
                                
                if (state.isLinked)
                {
                	section(){                    	
                    	href ("statusCheckPage", title: "Status Check", description: "Check if your Paradox Controller is connected and running correctly.")
                    	href ("resetParadoxServerSettings", title: "Reset", description: "Reset your Paradox server settings and oAuth.")                        
                        href ("resetDeviceList", title: "Reset Device List", description: "Refetch devices.")                        
                    }
                }
                else
                {
                	href ("paradoxServiceConnectPage", title:"Link", description:"Try to link to your local Paradox server.")
                }
                 
            }            
            
        }
}


def deviceSelectPage()
{
	if (state.gotDevices)
    {
        def ocOptions = openCloseDiscovered() ?: []
        def numOcFound = ocOptions.size() ?: 0

        def motionOptions = motionDiscovered() ?: []
        def numMotionFound = motionOptions.size() ?: 0
        
        def smokeOptions = smokeDetectorDiscovered() ?: []
        def smokeFound = smokeOptions.size() ?: 0

		return dynamicPage(name:"deviceSelectPage", title:"Device Selection", nextPage:"") {
			section("Select your device below.") {
				input "selectedOpenClose", "enum", required:false, title:"Select Open/Close Sensors (${numOcFound} found)", multiple:true, options:ocOptions
				input "selectedMotion", "enum", required:false, title:"Select Motion Sensors (${numMotionFound} found)", multiple:true, options:motionOptions
                input "selectedSmoke", "enum", required:false, title:"Select Smoke Detectors (${smokeFound} found)", multiple:true, options:smokeOptions
			}		
		}   
    }
    
    if (!state.waitOnRestCall)
    {
		api("deviceList",null)	
    }
    
	return dynamicPage(name:"deviceSelectPage", title:"Device Discovery Started!", nextPage:"", refreshInterval:3) {
		section() {
				paragraph "Getting device lists..."
			}	
	}   
    
}

def paradoxServiceConnectPage()
{
	//TRACE("${state}")
	if (!state.isLinked)
	{    	
        if (!state.connectingToService)
     	{        	
            paradoxHubConnect()
    		TRACE("Waiting for response from Paradox local service.")
            state.connectingToService = true
    	}
        
        state.connectingToService = false
        return dynamicPage(name:"paradoxServiceConnectPage", title:"Connecting to Paradox Service",refreshInterval:3) {
                section() {
                    paragraph "Please Wait"
                }
            }
    }
    else
    {
    	TRACE("Connected to Paradox Service !")
        state.connectingToService = false
        return dynamicPage(name:"paradoxServiceConnectPage", title:"Connecting to Paradox Service", install:true, uninstall: false) {
                section() {
                    paragraph "Connected. Please click Done."
                }
            }
    }
}

def resetDeviceList()
{
	state.gotDevices = false;
     return dynamicPage(name:"resetDeviceList", title:"Reset Devices") {
			section() {
				paragraph "Reset..."
			}
		}
}

def resetParadoxServerSettings()
{
	return dynamicPage(name:"resetParadoxServerSettings", title:"Reset All") {
			section() {
				paragraph "Not Implemented..."
			}
		}
}

def statusCheckPage()
{   	
    if (!state.statusCheck)
    {
      api("status",null)
      state.statusCheck = true
      return dynamicPage(name:"statusCheckPage", title:"Status Check", refreshInterval:3) {
			section() {
				paragraph "Getting Status..."
			}
		}
	}
    
    state.statusCheck = false
    
    if (!state.isLinked)
    {
        return dynamicPage(name:"statusCheckPage", title:"Status"){
    		section() {
            	paragraph "Not linked"
            }
        }	
    }
    
    return dynamicPage(name:"statusCheckPage", title:"Status"){
    		section() {
            	paragraph "Connected"
            }
    }
}

/************************************************************
*	REST Handlers
*/
void updateZoneHandler() {
  updateZone()
}

def linkHandler()
{	
    if (state.isLinked)
    {
    	return [result: "already connected"]
    }
   
   	state.isLinked = true
    state.waitOnRestCall = false
   	return [result  : "ok"]
}

def revokeHandler()
{
    INFO("Paradox service requested revoking access")
    state.isLinked = false
    state.waitOnRestCall = false
    return [result  : "ok"]
}

def lanHandler(evt) {
	def description = evt.description
    def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]
    
    if (parsedEvent.body && parsedEvent.headers)
    {
    try
    {
    	def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
        
        def body = new groovy.json.JsonSlurper().parseText(bodyString)
        if (body?.errorCode)
        {
        	ERROR("[${body?.errorCode}] ${body?.message}")
		}
        else
        {
        	if (body?.action?.equalsIgnoreCase("status"))
            {               	
                state.isLinked = body?.isOk
            }
            else if (body?.action?.equalsIgnoreCase("devices"))
            {           
                body?.devices.each { 
                	
                    def d = null
                    
                	if (it?.deviceType?.equalsIgnoreCase("OpenCloseSensor"))
                    {    
                    	d = getOpenCloseDevices()                        
                    }
                    else if (it?.deviceType?.equalsIgnoreCase("MotionSensor"))
                    {       
                     	d = getMotionDevices()                		
                    }
                    else if (it?.deviceType?.equalsIgnoreCase("SmokeDetector"))
                    {
                    	d = getSmokeDetectorDevices()                		
                    }
                    else
                    {
                    	DEBUG("Ignoring current device type. " + it?.deviceType)
                    }
                                	
                	if (d != null)
                    {
                    	DEBUG("Adding to device list")
                    	d[it?.zoneId] = [zoneId: it.zoneId, name: it.name, partition: it.partition, deviceType: it.deviceType, hub: parsedEvent.hub]   
                    }
                }
                
                state.gotDevices = true
            }           
            
        }
      	
        } catch(Exception e)
        {
        	ERROR(e)
        } finally
        {
        	state.waitOnRestCall = false   
        }
    }
}

private def parseEventMessage(String description) {
	def event = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ip = valueString
			}
		}
		else if (part.startsWith('deviceAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.port = valueString
			}
		}		
		else if (part.startsWith('headers')) {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString) {
				event.headers = valueString
			}
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			def valueString = part.trim()
			if (valueString) {
				event.body = valueString
			}
		}
        else if (part.startsWith('requestId')) {
			part -= "requestId:"
			def valueString = part.trim()
			if (valueString) {
				event.requestId = valueString
			}
		}
	}

	event
}
/************************************************************
*	API and http methods
*/

private def getHostAddress() {
    return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

def toJson(Map m)
{
	return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

def toQueryString(Map m)
{
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

private def api(method, args) {
		
    def methods = [
		'status': 
			[uri:"/status", 
          		type: 'get'],
		'configure': 
			[uri:"/configure", 
          		type: 'put'],
		'reset': 
			[uri:"/configure/reset", 
          		type: 'get'],
        'deviceList': 
			[uri:"/devices", 
          		type: 'get'],
               
		]
        
	def request = methods.getAt(method)
 	
    state.waitOnRestCall = true
    
    try {
		
		if (request.type == 'put') {
			putapi(args,request.uri)
		} else if (request.type == 'get') {
			getapi(request.uri)
		} 
	} catch (Exception e) {
		ERROR("doRequest> " + e)		
	}    	
}

private getapi(uri) {
  
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri,
    headers: [	HOST:getHostAddress(),
    			"Accept":"application/json"
                ]
  )
  
  sendHubCommand(hubAction)  
}

private putapi(params, uri) {
		
	def hubAction = new physicalgraph.device.HubAction(
		method: "PUT",
		path: uri,
		body: toJson(params),
		headers: [Host:getHostAddress(), "Content-Type":"application/json" ]
		)
	sendHubCommand(hubAction)    
}
/************************************************************
*	Functions
*/

def deleteChildren(selected, existing)
{
	// given all known devices, search the list of selected ones, if the device isn't selected, see if it exists as a device, if it does, remove it.
    existing.each { device ->
    	def dni = app.id + "/zone" + device.value.zoneId
        def sel
        
        if (selected)
        {
        	sel = selected.find { dni == it }            
        }
        
        if (!sel)
        {
        	def d = getChildDevice( dni )
            if (d)
            {
            	DEBUG("Deleting device " + dni )
           		deleteChildDevice( dni )
            }        	
        }
    }
}

def DeleteChildDevicesNotSelected()
{
	deleteChildren(selectedOpenClose, getOpenCloseDevices())
    deleteChildren(selectedMotion, getMotionDevices())
	deleteChildren(selectedSmoke, getSmokeDetectorDevices())
}

def UpdateSelectedDevices()
{
	DeleteChildDevicesNotSelected()    
    	
    createNewDevices(selectedOpenClose, getOpenCloseDevices(), "Paradox Open/Close Sensor")
    createNewDevices(selectedMotion, getMotionDevices(), "Paradox Motion Sensor")
    createNewDevices(selectedSmoke, getSmokeDetectorDevices(), "Paradox Smoke Detector")    
}

private def createNewDevices(selected, existing, deviceType)
{
	if (selected)
    {    	
     	selected.each { dni ->
        	def d = getChildDevice(dni)
            if (!d)
            {
            	def newDevice
            	newDevice = existing.find { (app.id + "/zone" + it.value.zoneId) == dni}
                d = addChildDevice("bitbounce",deviceType, dni, newDevice?.value.hub, [name: newDevice?.value.name])
                DEBUG("Created new " + deviceType)
            }           
        }      
    }
}

def generateAccessToken() {
    
    if (resetOauth) {
    	DEBUG( "Reseting Access Token")
    	state.accessToken = null
        resetOauth = false
    }
    
	if (!state.accessToken) {
    	createAccessToken()
        TRACE( "Creating new Access Token: $state.accessToken")
    }
  
}

private paradoxHubConnect()
{
	DEBUG("Connecting to Paradox Local Hub")
    
    generateAccessToken()
      
    def params = ["Location": "Home","AppId" : "${app.id}", "AccessToken" : "${state.accessToken}"]
    api("configure", params)
 
}

Map openCloseDiscovered() {
	def sensors =  getOpenCloseDevices()
	def map = [:]
	
    sensors.each {
        def value = "${it?.value?.name}"
        def key = app.id +"/zone"+ it?.value?.zoneId
        map["${key}"] = value
    }
	
	map
}

Map motionDiscovered() {
	def motion =  getMotionDevices()
	def map = [:]
	
    motion.each {
        def value = "${it?.value?.name}"
        def key = app.id +"/zone"+ it?.value?.zoneId 
        map["${key}"] = value
    }

	map
}

Map smokeDetectorDiscovered() {
	def smoke =  getSmokeDetectorDevices()
	def map = [:]
	
    smoke.each {
        def value = "${it?.value?.name}"
        def key = app.id +"/zone"+ it?.value?.zoneId 
        map["${key}"] = value
    }

	map
}


def getOpenCloseDevices() {
	state.openCloseDevices = state.openCloseDevices ?: [:]
}

def getMotionDevices() {
	state.motionSensors = state.motionSensors ?: [:]
}

def getSmokeDetectorDevices() {
	state.smokeSensors = state.smokeSensors ?: [:]
}

/************************************************************
*	Alarm Functions
*/
private updateZone() 
{	
	def zoneId = params.id
    def zoneStatus = params.status

	def childDevices = getAllChildDevices()
    
    if (childDevices)
    {
    	def child = childDevices.find { (app.id + "/zone" + zoneId) == it.deviceNetworkId}
        if (child)
        {        	
        	child.zone("${zoneStatus}")
        }        
    }
}

/************************* DEBUGGING **********************/

private TRACE(message)
{
	log.trace(message)
}

private DEBUG(message)
{
	log.debug(message)
}

private WARN(message)
{
	log.warn(message)
}

private ERROR(message)

{
	log.error(message)
}