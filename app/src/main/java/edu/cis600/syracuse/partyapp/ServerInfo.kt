package edu.cis600.syracuse.partyapp

import com.google.firebase.auth.FirebaseAuth


const val SERVER_IP = "10.1.97.94"
const val SERVER_PORT = "3000"
const val SERVER_BASE_URL = "http://$SERVER_IP:$SERVER_PORT"


const val URL_EVENTS_BASE = "$SERVER_BASE_URL/events"
const val URL_EVENTS_CREATE = "$SERVER_BASE_URL/event/create"
const val URL_EVENTS_UPDATE = "$SERVER_BASE_URL/event/update"
const val URL_EVENTS_DELETE = "$SERVER_BASE_URL/event/delete"
const val URL_EVENTS_FAVORITE_BASE = "$SERVER_BASE_URL/events/favorite"
const val URL_EVENTS_FAVORITE_TOGGLE = "$URL_EVENTS_FAVORITE_BASE/toggle"


const val URL_INVITEES_BASE = "$SERVER_BASE_URL/invitees"
const val URL_INVITEES_ADD = "$URL_INVITEES_BASE/add"
const val URL_INVITEES_SEARCH = "$URL_INVITEES_BASE/search"
const val URL_INVITEES_DELETE = "$URL_INVITEES_BASE/delete"

const val URL_THINGS_BASE = "$SERVER_BASE_URL/event/things"
const val URL_THINGS_ADD = "$URL_THINGS_BASE/add"
const val URL_THINGS_CLAIM = "$URL_THINGS_BASE/claim"

const val URL_USER_REGISTER = "$SERVER_BASE_URL/registerUser"

var LOGGEDIN_USER_ID = FirebaseAuth.getInstance().uid

const val EVENT_TYPE_PUBLIC = "public"
const val EVENT_TYPE_PRIVATE = "private"

const val FAVORITE_ON = "t"
const val FAVORITE_OFF = "f"


const val URL_WALMART_SEARCH = "http://api.walmartlabs.com/v1/search?apiKey=n9f8y9w2fzvgtfs2grna89n4&query="