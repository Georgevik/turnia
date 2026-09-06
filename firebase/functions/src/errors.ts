import { FunctionsErrorCode, HttpsError } from "firebase-functions/v2/https";

/**
 * Registry of every error code thrown by Turnia callables.
 *
 * Each value is unique per throw site so a failure can be located in the source
 * from the logs. Keep it sorted ascending and grouped by module — the last
 * entry of a block is the last code used, so the next one is simply +1.
 *
 * Ranges (keep new modules in their own range):
 *   1–99       generic, thrown by the shared checks in `requests.ts` rather than by one call site
 *   1000–1999  membership (invitations / join requests)
 *   2000–2999  notifications
 *   3000–3999  events (taking / transfers)
 *   4000–4999  shared calendar
 */
export enum TurniaErrorCode {
  // generic — the only codes that are not unique to a throw site. The function that refused is
  // already on the log line, so nothing is lost by sharing them.
  Unauthenticated = 1,
  InvalidArgument = 2,

  // membership
  JoinRequestInvitationNotFound = 1003,
  JoinRequestInvitationNotActive = 1004,
  JoinRequestInvitationExpired = 1005,
  AcceptRequestNotAdmin = 1008,
  AcceptRequestNotFound = 1009,
  RejectRequestNotAdmin = 1010,
  RejectRequestNotFound = 1011,
  LeaveGroupNotMember = 1012,
  LeaveGroupLastAdmin = 1013,
  RemoveMemberNotAdmin = 1016,
  RemoveMemberSelf = 1017,
  RemoveMemberIsAdmin = 1018,
  RemoveMemberNotMember = 1019,
  DeleteGroupNotFound = 1022,
  DeleteGroupNotAdmin = 1023,
  DeleteGroupNotEmpty = 1024,

  // events
  TakeEventNotMember = 3003,
  TakeEventSelf = 3004,
  TakeEventNotFound = 3005,
  TakeEventNotOnSwap = 3006,

  // profile
  UpdateProfileUsernameInvalid = 2003,
  UpdateProfileUsernameTaken = 2004,

  // shared calendar
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
export class TurniaHttpError extends HttpsError {
  readonly turniaErrorCode: TurniaErrorCode;

  constructor(code: FunctionsErrorCode, turniaErrorCode: TurniaErrorCode, message: string) {
    super(code, `${turniaErrorCode}: ${message}`);
    this.turniaErrorCode = turniaErrorCode;
  }
}

/**
 * The HTTP status and the wording of every code above.
 *
 * Typed against the enum, so a code without an entry here — or an entry naming a code that does not
 * exist — fails the build. That is the whole point of splitting them: the numbers stay in one
 * sorted list you can read a range off, and nothing can be thrown that the list does not know.
 */
const SPECS: Record<keyof typeof TurniaErrorCode, [FunctionsErrorCode, string]> = {
  // generic
  Unauthenticated: ["unauthenticated", "Sign in required."],
  InvalidArgument: ["invalid-argument", "Missing or malformed arguments."],

  // membership
  JoinRequestInvitationNotFound: ["not-found", "Invalid invitation code."],
  JoinRequestInvitationNotActive: ["failed-precondition", "Invitation is not active."],
  JoinRequestInvitationExpired: ["failed-precondition", "Invitation has expired."],
  AcceptRequestNotAdmin: ["permission-denied", "Only a group admin can accept requests."],
  AcceptRequestNotFound: ["not-found", "No pending join request."],
  RejectRequestNotAdmin: ["permission-denied", "Only a group admin can reject requests."],
  RejectRequestNotFound: ["not-found", "No pending join request."],
  LeaveGroupNotMember: ["failed-precondition", "You are not a member of this group."],
  LeaveGroupLastAdmin: ["failed-precondition", "The only admin cannot leave a group that still has members."],
  RemoveMemberNotAdmin: ["permission-denied", "Only a group admin can remove members."],
  RemoveMemberSelf: ["failed-precondition", "Use leaveGroup to leave a group."],
  RemoveMemberIsAdmin: ["failed-precondition", "An admin cannot be removed."],
  RemoveMemberNotMember: ["failed-precondition", "That user is not a member of this group."],
  DeleteGroupNotFound: ["not-found", "That group does not exist."],
  DeleteGroupNotAdmin: ["permission-denied", "Only a group admin can delete a group."],
  DeleteGroupNotEmpty: ["failed-precondition", "Everybody else has to leave the group before it can be deleted."],

  // events
  TakeEventNotMember: ["permission-denied", "You are not a member of this group."],
  TakeEventSelf: ["failed-precondition", "You already hold this event."],
  TakeEventNotFound: ["not-found", "Event not found."],
  TakeEventNotOnSwap: ["failed-precondition", "Event is not offered for swap."],

  // profile
  UpdateProfileUsernameInvalid: ["invalid-argument", "Invalid username."],
  UpdateProfileUsernameTaken: ["failed-precondition", "Username already taken."],

  // shared calendar
  SharedCalendarNotShared: ["permission-denied", "This calendar is not shared with you."],
  SharedCalendarRangeTooWide: ["failed-precondition", "Date range must be within 3 months."],
};

/**
 * Every error a callable can throw, ready to be thrown: `throw TurniaError.TakeEventNotFound;`.
 *
 * These are **getters, not stored instances**. An `Error` captures its stack where it is
 * constructed, so a registry of pre-built objects would hand every throw site the same stack,
 * pointing here instead of at the code that failed — and would share one mutable object across
 * concurrent invocations. Building on access costs nothing on a path that is already failing.
 */
export const TurniaError: { readonly [K in keyof typeof TurniaErrorCode]: TurniaHttpError } =
  Object.defineProperties(
    {},
    Object.fromEntries(
      (Object.keys(SPECS) as (keyof typeof TurniaErrorCode)[]).map((name) => {
        const [status, message] = SPECS[name];
        return [name, { get: () => new TurniaHttpError(status, TurniaErrorCode[name], message), enumerable: true }];
      }),
    ),
  ) as { readonly [K in keyof typeof TurniaErrorCode]: TurniaHttpError };
