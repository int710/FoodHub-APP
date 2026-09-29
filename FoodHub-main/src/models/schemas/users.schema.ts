import z from 'zod'
import { REGEX_PASSWORD } from '~/constants/regex'

// Register
export const registerBodySchema = z
  .object({
    email: z.email({ error: 'Email không đúng định dạng' }),
    password: z
      .string()
      .trim()
      .min(6, 'Mật khẩu tối thiểu 6 kí tự')
      .max(30, { error: 'Mật khẩu tối đa 30 kí tự' })
      .regex(REGEX_PASSWORD, { error: ' Mật khẩu phải bao gồm chữ cái, số và kí tự đặc biệt' }),
    confirmPassword: z.string({ error: 'Vui lòng xác nhận mật khẩu' }).trim(),
    name: z.string().trim().max(100, { error: 'Tên không được dài quá 100 kí tự' }),
    phone: z.string().trim().min(8, 'Số điện thoại không hợp lệ').max(20).optional(),
    dateOfBirth: z.coerce.date({ error: 'Ngày sinh không đúng định dạng (YYYY-MM-DD)' }).optional()
  })
  .refine((data) => data.password === data.confirmPassword, {
    error: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword']
  })
export const registerReqSchema = z.object({
  body: registerBodySchema
})
export type RegisterRequestBody = z.infer<typeof registerBodySchema>

// Login
export const loginBodySchema = z.object({
  email: z.email({ error: 'Email không đúng định dạng' }),
  password: z.string().trim().min(6, { error: 'Mật khẩu tối thiểu 6 kí tự' })
})
export const loginReqBody = z.object({
  body: loginBodySchema
})
export type LoginReqBody = z.infer<typeof loginBodySchema>

// Logout or RefreshToken
const refreshTokenSchema = z.object({
  refresh_token: z.string({ error: 'RefreshToken is invalid' })
})
export type RefreshTokenReq = z.infer<typeof refreshTokenSchema>
export const logoutReqBody = z.object({
  body: refreshTokenSchema
})
export type LogoutReqBody = z.infer<typeof refreshTokenSchema>

// Verify-email
const verifyEmailSchema = z.object({
  verify_email_token: z.string().min(1)
})
export const verifyEmailReq = z.object({ body: verifyEmailSchema })
export type VerifyEmailReq = z.infer<typeof verifyEmailSchema>

// email users
const emailSchema = z.object({ email: z.email({ error: 'Email không hợp lệ' }).min(1) })
export const emailReq = z.object({
  body: emailSchema
})
export type EmailReqBody = z.infer<typeof emailSchema>

// forgot password token
const resetPasswordSchema = z
  .object({
    forgot_password_token: z.string({ error: 'Forgot password token is invalid' }),
    new_password: z
      .string()
      .trim()
      .min(6, 'Mật khẩu tối thiểu 6 kí tự')
      .max(30, { error: 'Mật khẩu tối đa 30 kí tự' })
      .regex(REGEX_PASSWORD, { error: ' Mật khẩu phải bao gồm chữ cái, số và kí tự đặc biệt' }),
    confirmNewPassword: z.string({ error: 'Vui lòng xác nhận mật khẩu' }).trim()
  })
  .refine((data) => data.new_password === data.confirmNewPassword, {
    error: 'Mật khẩu xác nhận không khớp',
    path: ['confirmNewPassword']
  })

export const resetPasswordReq = z.object({ body: resetPasswordSchema })
export type ResetPasswordReq = z.infer<typeof resetPasswordSchema>

export const updateProfileReq = z.object({
  body: z.object({
    name: z.string().trim().min(1).max(100).optional(),
    phone: z.string().trim().max(20).nullable().optional(),
    dateOfBirth: z.coerce.date().nullable().optional(),
    avatar: z.url().nullable().optional()
  }).refine((value) => Object.keys(value).length > 0, 'Cần ít nhất một thông tin cần cập nhật')
})
export type UpdateProfileReq = z.infer<typeof updateProfileReq>['body']
