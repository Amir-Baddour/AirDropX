/** Same rules as the server (UserInputValidator.password): 8 to 128 characters, with a letter and a number. */
export const PASSWORD_MIN = 8
export const PASSWORD_MAX = 128

export function passwordError(password: string): string | null {
  if (password.length < PASSWORD_MIN) return `At least ${PASSWORD_MIN} characters`
  if (password.length > PASSWORD_MAX) return `At most ${PASSWORD_MAX} characters`
  if (!/\p{L}/u.test(password) || !/\p{N}/u.test(password)) return 'Use at least one letter and one number'
  return null
}
