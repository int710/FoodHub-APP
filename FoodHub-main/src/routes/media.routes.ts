import { Router } from 'express'
import mediasController from '~/controllers/media.controllers'
import { authenticate } from '~/middlewares/auth.middlewares'
import { requireRole } from '~/middlewares/rbac.middlewares'
import { requestHandler } from '~/utils/requestHandler'

const mediasRouter = Router()

mediasRouter.post(
  '/upload-image',
  authenticate,
  requireRole('ADMIN'),
  requestHandler(mediasController.uploadImage)
)

export default mediasRouter
