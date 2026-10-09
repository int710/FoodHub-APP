import { Request, Response } from 'express'
import { OrderStatus, OrderType, PaymentStatus, Role } from '~/generated/prisma/enums'
import { prisma } from '~/config/prisma'
import { vnpay } from '~/config/vnpay'
import cartsServices, { CartType } from '~/services/carts.services'

import { ProductCode, VnpLocale } from 'vnpay/enums'
import { dateFormat } from 'vnpay/utils'
import {
  IpnFailChecksum,
  IpnInvalidAmount,
  IpnOrderNotFound,
  IpnSuccess,
  InpOrderAlreadyConfirmed
} from 'vnpay/constants'
import type { ReturnQueryFromVNPay } from 'vnpay/types'
import { ApiResponse } from '~/models/ApiResponse'
import paymentServices from '~/services/payments.services'
import notificationsServices from '~/services/notifications.services'
import { emitNewOrder } from '~/socket/orders/order.emitter'
import HTTP_STATUS from '~/constants/httpStatus'
import { ErrorWithStatus } from '~/models/Errors'
import { clientLink } from '~/utils/client-link'

const mapOrderTypeToCartType = (type: OrderType): CartType => {
  if (type === OrderType.DINE_IN) return CartType.DINE_IN
  if (type === OrderType.TAKEAWAY) return CartType.TAKEAWAY
  return CartType.DELIVERY
}

const getClientIp = (req: Request): string => {
  let ip =
    (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim() ||
    req.socket.remoteAddress ||
    '127.0.0.1'

  ip = ip.replace(/^::ffff:/, '')
  if (ip === '::1' || ip.includes(':')) ip = '127.0.0.1'

  return ip
}

function assertCanAccessOrder<T extends { customerId: string | null }>(
  req: Request,
  order: T | null
): asserts order is T {
  if (!order) {
    throw new ErrorWithStatus({
      httpStatusCode: HTTP_STATUS.NOT_FOUND,
      message: 'Không tìm thấy đơn'
    })
  }
  const actor = req.decoded_authorization
  const isHost = actor?.role === Role.ADMIN || actor?.role === Role.STAFF
  if (!isHost && order.customerId !== actor?.user_id) {
    throw new ErrorWithStatus({
      httpStatusCode: HTTP_STATUS.FORBIDDEN,
      message: 'Bạn không có quyền truy cập thanh toán của đơn này'
    })
  }
}

const paymentController = {
  async createPaymentUrl(req: Request, res: Response) {
    const { orderCode } = req.body as { orderCode?: string }

    if (!orderCode) {
      return res.status(400).json({ message: 'Thiếu orderCode' })
    }

    const order = await prisma.order.findFirst({
      where: { orderCode },
      include: { payments: true }
    })
    assertCanAccessOrder(req, order)

    if (order.status !== OrderStatus.PENDING_PAYMENT) {
      return res.status(400).json({
        message: `Đơn đang ở trạng thái ${order.status}, không thể tạo lại link`
      })
    }

    const paymentUrl = vnpay.buildPaymentUrl({
      vnp_Amount: Number(order.totalAmount.toString()),
      vnp_IpAddr: getClientIp(req),
      vnp_TxnRef: order.orderCode,
      vnp_OrderInfo: `Thanh toan FoodHub ${order.orderCode}`,
      vnp_OrderType: ProductCode.Other,
      vnp_ReturnUrl: process.env.VNPAY_RETURN_URL!,
      vnp_Locale: VnpLocale.VN,
      vnp_CreateDate: dateFormat(new Date()),
      vnp_ExpireDate: dateFormat(new Date(Date.now() + 15 * 60 * 1000))
    })

    return res.json({
      message: 'Tạo URL thành công',
      paymentUrl,
      orderCode
    })
  },

  async paymentReturn(req: Request, res: Response) {
    const verify = vnpay.verifyReturnUrl(req.query as ReturnQueryFromVNPay)

    if (!verify.isVerified) {
      return res.redirect(clientLink('payment/failed', { reason: 'invalid_checksum' }))
    }
    if (!verify.isSuccess) {
      return res.redirect(clientLink('payment/failed', {
        orderCode: String(verify.vnp_TxnRef || ''),
        code: String(verify.vnp_ResponseCode || '')
      }))
    }
    return res.redirect(clientLink('payment/success', {
      orderCode: String(verify.vnp_TxnRef || '')
    }))
  },

  async paymentIpn(req: Request, res: Response) {
    try {
      const verify = vnpay.verifyIpnCall(req.query as ReturnQueryFromVNPay)

      if (!verify.isVerified) {
        return res.status(200).json(IpnFailChecksum)
      }

      const order = await prisma.order.findFirst({
        where: { orderCode: verify.vnp_TxnRef as string }
      })

      if (!order) {
        return res.status(200).json(IpnOrderNotFound)
      }

      const payment = await prisma.payment.findFirst({
        where: { orderId: order.id }
      })

      if (!payment) {
        return res.status(200).json(IpnOrderNotFound)
      }

      // log ra để check
      console.log('CHECK AMOUNT', {
        orderAmount: Number(order.totalAmount.toString()),
        vnpAmount: verify.vnp_Amount,
        txnRef: verify.vnp_TxnRef
      })

      if (Number(order.totalAmount.toString()) !== Number(verify.vnp_Amount)) {
        return res.status(200).json(IpnInvalidAmount)
      }

      if (payment.status === PaymentStatus.PAID ||
        (order.status !== OrderStatus.PENDING_PAYMENT && order.status !== OrderStatus.CANCELLED)) {
        return res.status(200).json(InpOrderAlreadyConfirmed)
      }

      if (verify.isSuccess && verify.vnp_ResponseCode === '00') {
        const paymentApplied = await paymentServices.applyVnpayResult(
          order.id, payment.id, true, verify.vnp_TxnRef as string, req.query
        )

        if (!paymentApplied) {
          return res.status(200).json(InpOrderAlreadyConfirmed)
        }

        if (paymentApplied === OrderStatus.CANCELLED) {
          console.error('[VNPay] Payment received after cancellation; manual refund required:', order.orderCode)
          return res.status(200).json(IpnSuccess)
        }

        await notificationsServices.createOrderStatusNotification({
          orderId: order.id,
          previousStatus: OrderStatus.PENDING_PAYMENT,
          status: OrderStatus.PENDING_CONFIRMATION
        }).catch((error) => {
          console.error('[Notification] Failed to create payment success notification:', error)
        })
        await notificationsServices.createOrderCreated(order.id).catch((error) => {
          console.error('[Notification] Failed to create paid order notification:', error)
        })
        emitNewOrder({ orderId: order.id, orderCode: order.orderCode })

        try {
          const cartType = mapOrderTypeToCartType(order.type)
          await cartsServices.clearCart(cartType, order.customerId || order.sessionId)
        } catch (error) {
          console.error('Clear cart after payment failed:', error)
        }

        return res.status(200).json(IpnSuccess)
      }

      const failureApplied = await paymentServices.applyVnpayResult(
        order.id, payment.id, false, verify.vnp_TxnRef as string, req.query
      )
      if (!failureApplied) return res.status(200).json(InpOrderAlreadyConfirmed)

      await notificationsServices.createOrderStatusNotification({
        orderId: order.id,
        previousStatus: OrderStatus.PENDING_PAYMENT,
        status: OrderStatus.PAYMENT_FAILED
      }).catch((error) => {
        console.error('[Notification] Failed to create payment failure notification:', error)
      })

      return res.status(200).json(IpnSuccess)
    } catch (error) {
      console.error('IPN error', error)
      return res.status(200).json(IpnFailChecksum)
    }
  },

  async detailPayment(req: Request<{ orderId: string }>, res: Response) {
    const { orderId } = req.params
    const order = await prisma.order.findUnique({
      where: { id: orderId },
      select: { customerId: true }
    })
    assertCanAccessOrder(req, order)
    const result = await paymentServices.detailPayment(orderId)
    return res.json(ApiResponse('Chi tiết thanh toán hóa đơn', result))
  },

  async cashConfirm(req: Request<{ orderId: string }>, res: Response) {
    const { orderId } = req.params
    const staffId = req.decoded_authorization?.user_id as string

    const data = await paymentServices.cashConfirm({ orderId, staffId })
    await notificationsServices.createOrderStatusNotification({
      orderId,
      previousStatus: OrderStatus.PENDING_CONFIRMATION,
      status: OrderStatus.CONFIRMED
    }).catch((error) => {
      console.error('[Notification] Failed to create cash confirmation notification:', error)
    })
    return res.json(ApiResponse('Xác nhận thanh toán thành công', data))
  }

}

export default paymentController
