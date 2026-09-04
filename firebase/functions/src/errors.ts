import { FunctionsErrorCode, HttpsError } from "firebase-functions/v2/https";

/**
 * Registry of every error code thrown by Turnia callables.
 *
 * Each value is unique per throw site so a failure can be located in the source
 * from the logs. Keep it sorted ascending and grouped by module — the last
 * entry of a block is the last code used, so the next one is simply +1.
 *
 * Ranges (keep new modules in their own range):
 *   1000–1999  membership (invitations / join requests)
 *   2000–2999  notifications
 *   3000–3999  events (taking / transfers)
 *   4000–4999  shared calendar
 */
export enum TurniaErrorCode {
  // membership
  JoinRequestUnauthenticated = 1001,
  JoinRequestMissingCode = 1002,
  JoinRequestInvitationNotFound = 1003,
  JoinRequestInvitationNotActive = 1004,
  JoinRequestInvitationExpired = 1005,
  AcceptRequestUnauthenticated = 1006,
  AcceptRequestMissingArgs = 1007,
  AcceptRequestNotAdmin = 1008,
  AcceptRequestNotFound = 1009,

  // events
  TakeEventUnauthenticated = 3001,
  TakeEventMissingArgs = 3002,
  TakeEventNotMember = 3003,
  TakeEventSelf = 3004,
  TakeEventNotFound = 3005,
  TakeEventNotOnSwap = 3006,

  // profile
  UpdateProfileUnauthenticated = 2001,
  UpdateProfileMissingArgs = 2002,
  UpdateProfileUsernameInvalid = 2003,
  UpdateProfileUsernameTaken = 2004,

  // shared calendar
  SharedCalendarUnauthenticated = 4001,
  SharedCalendarMissingArgs = 4002,
  SharedCalendarNotShared = 4003,
  SharedCalendarRangeTooWide = 4004,
}

/**
 * Base class for Turnia callable errors.
 *
 * Wraps a Firebase {@link HttpsError} and prefixes the message with the numeric
 * {@link TurniaErrorCode}, so the client and the logs see
 * `"<turniaErrorCode>: <message>"`.
 */
export abstract class TurniaHttpError extends HttpsError {
  readonly turniaErrorCode: TurniaErrorCode;

  protected constructor(code: FunctionsErrorCode, turniaErrorCode: TurniaErrorCode, message: string) {
    super(code, `${turniaErrorCode}: ${message}`);
    this.turniaErrorCode = turniaErrorCode;
  }
}

export class HttpErrorUnauthenticated extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("unauthenticated", turniaErrorCode, message);
  }
}

export class HttpErrorPermissionDenied extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("permission-denied", turniaErrorCode, message);
  }
}

export class HttpErrorInvalidArgument extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("invalid-argument", turniaErrorCode, message);
  }
}

export class HttpErrorNotFound extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("not-found", turniaErrorCode, message);
  }
}

export class HttpErrorAlreadyExists extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("already-exists", turniaErrorCode, message);
  }
}

export class HttpErrorFailedPrecondition extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("failed-precondition", turniaErrorCode, message);
  }
}

export class HttpErrorInternal extends TurniaHttpError {
  constructor(turniaErrorCode: TurniaErrorCode, message: string) {
    super("internal", turniaErrorCode, message);
  }
}
