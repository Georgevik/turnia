import { CallableRequest } from "firebase-functions/v2/https";
import { TurniaError } from "./errors";

/**
 * The caller's uid. Every callable in this project starts with it: there is nothing any of them
 * will do for somebody who is not signed in.
 */
export function requireUid(request: CallableRequest): string {
  const uid = request.auth?.uid;
  if (!uid) throw TurniaError.Unauthenticated;

  return uid;
}

/**
 * The named fields of `request.data`, each a non-empty string, trimmed.
 *
 * Trimmed here and not at each call site because a value that is only whitespace is as absent as
 * one that never arrived, and every payload these callables take is a flat record of required
 * strings. Anything narrower than that — a lowercased handle, a parsed date — stays with the
 * function that knows why.
 */
export function requireFields<K extends string>(
  request: CallableRequest,
  ...fields: K[]
): { [F in K]: string } {
  const data = (request.data ?? {}) as Record<string, unknown>;
  const values = {} as { [F in K]: string };

  for (const field of fields) {
    const value = data[field];
    if (typeof value !== "string" || value.trim() === "") throw TurniaError.InvalidArgument;

    values[field] = value.trim();
  }

  return values;
}
