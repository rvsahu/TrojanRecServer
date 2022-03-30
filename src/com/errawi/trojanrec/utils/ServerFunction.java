package com.errawi.trojanrec.utils;

/**
 * Enumerates the functions that the server could need to perform for the client
 */
public enum ServerFunction {
    CONNECT, LOGIN, CHECK_IF_LOGGED_IN, GET_PROFILE_INFO, GET_CURRENT_BOOKINGS,
    GET_PREVIOUS_BOOKINGS, GET_WAIT_LIST, GET_CENTRE_TIME_SLOTS, MAKE_BOOKING,
    CANCEL_BOOKING, CANCEL_WAIT_LIST, POLL_FOR_NOTIFICATIONS, CLOSE
}