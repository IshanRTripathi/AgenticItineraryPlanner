import { v4 as uuidv4 } from 'uuid';

const SESSION_STORAGE_KEY = 'trip_planner_session_id';

/**
 * Get the current session ID.
 * If one doesn't exist in sessionStorage, a new one is generated and stored.
 */
export const getSessionId = (): string => {
    let sessionId = sessionStorage.getItem(SESSION_STORAGE_KEY);

    if (!sessionId) {
        sessionId = uuidv4();
        sessionStorage.setItem(SESSION_STORAGE_KEY, sessionId);
    }

    return sessionId;
};

/**
 * Reset the session ID (generate a new one).
 * Useful for explicit session termination or logout.
 */
export const resetSessionId = (): string => {
    const newSessionId = uuidv4();
    sessionStorage.setItem(SESSION_STORAGE_KEY, newSessionId);
    return newSessionId;
};
